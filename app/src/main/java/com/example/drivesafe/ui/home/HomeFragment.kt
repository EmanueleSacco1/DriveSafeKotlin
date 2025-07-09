package com.example.drivesafe.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.drivesafe.R
import com.example.drivesafe.data.model.Route
import com.example.drivesafe.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

/**
 * Request code for location permissions.
 */
private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

/**
 * Interval for location updates in milliseconds.
 */
private const val LOCATION_UPDATE_INTERVAL: Long = 1000

/**
 * Fastest interval for location updates in milliseconds.
 */
private const val LOCATION_FASTEST_INTERVAL: Long = 500

/**
 * A [Fragment] responsible for displaying a map, tracking user location,
 * and recording driving routes. It integrates with Google Play Services
 * for location updates and Firebase for route storage.
 */
class HomeFragment : Fragment() {

    /**
     * View binding instance for accessing layout elements.
     * The backing property `_binding` is nullable to manage its lifecycle.
     */
    private var _binding: FragmentHomeBinding? = null

    /**
     * A non-null accessor for the view binding instance.
     * This property should only be accessed between `onCreateView` and `onDestroyView`.
     */
    private val binding get() = _binding!!

    /**
     * Instance of the HomeViewModel for managing UI-related data.
     * Uncomment the following line if you decide to use the ViewModel for speed display.
     */
    // private val homeViewModel: HomeViewModel by viewModels()

    /**
     * Firebase Authentication instance for user authentication.
     */
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Firebase Firestore instance for database operations.
     */
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * The OpenStreetMap map view.
     */
    private lateinit var mapView: MapView

    /**
     * Overlay for displaying the user's current location on the map.
     */
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    /**
     * Default geographical point to center the map if no location is available.
     */
    private val defaultStartPoint = GeoPoint(43.0, 12.0)

    /**
     * Flag indicating whether route recording is active.
     */
    private var isRecording = false

    /**
     * A mutable list to store the geographical points (latitude, longitude) of the recorded path.
     */
    private var pathPoints: MutableList<GeoPoint> = mutableListOf()

    /**
     * The timestamp when the route recording started (in milliseconds).
     */
    private var startTime: Long = 0L

    /**
     * The timestamp when the route recording ended (in milliseconds).
     */
    private var endTime: Long = 0L

    /**
     * The total distance covered during the recorded route (in meters).
     */
    private var totalDistance: Float = 0f

    /**
     * The maximum speed achieved during the recorded route (in m/s).
     */
    private var maxSpeed: Float = 0f

    /**
     * Stores the last known location update, used for distance calculation.
     */
    private var lastLocation: Location? = null

    /**
     * Client for interacting with the Fused Location Provider API.
     */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * The LocationRequest object for specifying the parameters of location updates.
     * Moved here to be used for continuous speed updates.
     */
    private lateinit var locationRequest: LocationRequest

    /**
     * Callback for receiving location updates from [FusedLocationProviderClient].
     */
    private lateinit var locationCallback: LocationCallback

    /**
     * Called when the fragment is first created.
     * Initializes the Fused Location Provider client and sets up the location update callback.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .build()

        locationCallback = object : LocationCallback() {
            /**
             * Called when new location data is available.
             *
             * @param locationResult The result of the location update, containing one or more locations.
             */
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(
                        "HomeFragment",
                        "Location update: Lat=${location.latitude}, Lon=${location.longitude}, Speed=${location.speed} m/s"
                    )

                    updateSpeedIndicator(location.speed)

                    if (isRecording) {
                        val currentGeoPoint = GeoPoint(location.latitude, location.longitude)
                        pathPoints.add(currentGeoPoint)

                        lastLocation?.let { prevLocation ->
                            val distance = prevLocation.distanceTo(location)
                            totalDistance += distance
                        }
                        lastLocation = location

                        if (location.hasSpeed() && location.speed > maxSpeed) {
                            maxSpeed = location.speed
                        }
                    }
                }
            }
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mapView = binding.mapview

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(12.0)
        mapController.setCenter(defaultStartPoint)

        // Initialize and add the user's location overlay to the map.
        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            mapView
        )
        mapView.overlays.add(myLocationOverlay)

        // Add a scale bar overlay to the map for distance reference.
        val scaleBarOverlay = ScaleBarOverlay(mapView)
        scaleBarOverlay.setCentred(true)
        scaleBarOverlay.setScaleBarOffset(
            resources.displayMetrics.widthPixels / 2,
            10
        )
        mapView.overlays.add(scaleBarOverlay)

        // Add a compass overlay to the map for orientation.
        val compassOverlay = CompassOverlay(
            requireContext(),
            InternalCompassOrientationProvider(requireContext()),
            mapView
        )
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

        // Add a rotation gesture overlay to enable map rotation.
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.setEnabled(true)
        mapView.overlays.add(rotationGestureOverlay)

        // Set up the click listener for the 'recenter' button.
        binding.recenterButton.setOnClickListener {
            if (myLocationOverlay.isMyLocationEnabled && myLocationOverlay.myLocation != null) {
                mapView.controller.animateTo(myLocationOverlay.myLocation)
                mapView.controller.setZoom(16.0)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location not available. Please ensure GPS is active and permissions are granted.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        requestLocationPermissions()

        return root
    }

    /**
     * Updates the circular speed indicator on the UI.
     * Converts speed from meters per second to kilometers per hour.
     *
     * @param speedMps The current speed in meters per second.
     */
    private fun updateSpeedIndicator(speedMps: Float) {
        val speedKmh = speedMps * 3.6f // Convert m/s to km/h.
        binding.circularSpeedIndicator.text = "${speedKmh.toInt()}" // Display speed as an integer.
    }

    /**
     * Starts the route recording process.
     * This method is annotated with `@SuppressLint("MissingPermission")` because permission is checked programmatically.
     */
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // Check for location permissions before starting recording.
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                requireContext(),
                "Location permissions not granted. Cannot start recording.",
                Toast.LENGTH_LONG
            ).show()
            requestLocationPermissions()
            return
        }

        isRecording = true // Set the recording flag to true.

        binding.recordButton.setImageResource(R.drawable.ic_stop)
        binding.recordButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.red_500
            )
        )
        Toast.makeText(requireContext(), "Recording started!", Toast.LENGTH_SHORT).show()

        // Reset recording variables for the new route.
        pathPoints.clear()
        totalDistance = 0f
        maxSpeed = 0f
        startTime = System.currentTimeMillis()
        lastLocation = null

    }

    /**
     * Stops the route recording process.
     * Calculates route statistics and attempts to save the route to Firestore.
     */
    private fun stopRecording() {
        isRecording = false
        endTime = System.currentTimeMillis()

        binding.recordButton.setImageResource(R.drawable.ic_record)
        binding.recordButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.teal_200
            )
        )

        if (pathPoints.isNotEmpty()) {
            val durationSeconds = (endTime - startTime) / 1000.0
            val averageSpeed =
                if (durationSeconds > 0) (totalDistance / durationSeconds).toFloat() else 0f

            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Save the recorded route to Firestore.
                saveRouteToFirestore(
                    userId,
                    pathPoints,
                    startTime,
                    endTime,
                    totalDistance,
                    averageSpeed,
                    maxSpeed
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "User not authenticated. Cannot save route.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Route too short or no points recorded.",
                Toast.LENGTH_SHORT
            ).show()
        }

        Toast.makeText(requireContext(), "Recording stopped!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Re-enables location overlays and follows location if permissions are granted.
     * Also restarts continuous location updates if the fragment is resumed and permissions are granted.
     */
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    /**
     * Called when the fragment is no longer actively interacting with the user.
     * Pauses the map view and stops continuous location updates to save battery.
     */
    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up resources, disables location overlays, and detaches the map view.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        myLocationOverlay.disableMyLocation()
        myLocationOverlay.disableFollowLocation()
        mapView.onDetach()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        _binding = null
    }

    /**
     * Requests `ACCESS_FINE_LOCATION` permission from the user if not already granted.
     * If permission is granted, it enables the location overlay and starts updates.
     */
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            enableLocationOverlayAndStartUpdates()
        }
    }

    /**
     * Callback for the result of the permission request.
     *
     * @param requestCode The request code passed in [ActivityCompat.requestPermissions].
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationOverlayAndStartUpdates()
                Toast.makeText(requireContext(), "Location permission granted!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission denied. GPS may not work and recording is disabled.",
                    Toast.LENGTH_LONG
                ).show()
                isRecording = false
                binding.recordButton.setImageResource(R.drawable.ic_record)
                binding.recordButton.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.teal_200
                    )
                )
                if (::fusedLocationClient.isInitialized) {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        }
    }

    /**
     * Enables the MyLocationOverlay on the map and starts following the user's location.
     * Also animates the map to the user's first fixed location, and crucially,
     * starts continuous location updates for the speed indicator.
     * This method is annotated with `@SuppressLint("MissingPermission")` because permission is checked before calling.
     */
    @SuppressLint("MissingPermission")
    private fun enableLocationOverlayAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            myLocationOverlay.runOnFirstFix {
                activity?.runOnUiThread {
                    if (myLocationOverlay.myLocation != null) {
                        mapView.controller.animateTo(myLocationOverlay.myLocation)
                        mapView.controller.setZoom(16.0)
                    }
                }
            }
        }
    }

    /**
     * Saves the recorded route data to Firebase Firestore.
     *
     * @param userId The ID of the authenticated user.
     * @param path A list of `GeoPoint` objects representing the recorded path.
     * @param startTime The timestamp when the route recording started.
     * @param endTime The timestamp when the route recording ended.
     * @param totalDistance The total distance covered during the route in meters.
     * @param averageSpeed The average speed during the route in m/s.
     * @param maxSpeed The maximum speed achieved during the route in m/s.
     */
    private fun saveRouteToFirestore(
        userId: String,
        path: List<org.osmdroid.util.GeoPoint>,
        startTime: Long,
        endTime: Long,
        totalDistance: Float,
        averageSpeed: Float,
        maxSpeed: Float
    ) {
        val routeId = firestore.collection("routes").document().id

        val routeData = Route(
            id = routeId,
            userId = userId,
            timestamp = System.currentTimeMillis(),
            pathPoints = path.map {
                com.google.firebase.firestore.GeoPoint(
                    it.latitude,
                    it.longitude
                )
            },
            startTime = startTime,
            endTime = endTime,
            totalDistance = totalDistance,
            averageSpeed = averageSpeed,
            maxSpeed = maxSpeed
        )

        // Save the route data to the "routes" collection in Firestore.
        firestore.collection("routes")
            .document(routeId)
            .set(routeData)
            .addOnSuccessListener {
                Log.d("HomeFragment", "Route successfully saved with ID: $routeId")
                Toast.makeText(requireContext(), "Route saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("HomeFragment", "Error saving route", e)
                Toast.makeText(
                    requireContext(),
                    "Error saving route: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}