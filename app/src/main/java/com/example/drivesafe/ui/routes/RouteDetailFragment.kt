package com.example.drivesafe.ui.routes

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.drivesafe.R
import com.example.drivesafe.databinding.FragmentRouteDetailBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint // Alias for Firestore's GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A [Fragment] that displays the detailed information of a selected driving route,
 * including its path on a map and various statistics.
 */
class RouteDetailFragment : Fragment() {

    /**
     * View binding instance for accessing layout elements.
     * The backing property `_binding` is nullable to manage its lifecycle.
     */
    private var _binding: FragmentRouteDetailBinding? = null

    /**
     * A non-null accessor for the view binding instance.
     * This property should only be accessed between `onCreateView` and `onDestroyView`.
     */
    private val binding get() = _binding!!

    /**
     * The OpenStreetMap map view used to display the route path.
     */
    private lateinit var mapView: MapView

    /**
     * Arguments passed to this fragment via Navigation Component,
     * containing the route ID to display.
     */
    private val args: RouteDetailFragmentArgs by navArgs()

    /**
     * Firebase Firestore instance for fetching route data.
     */
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Called when the fragment is first created.
     * Initializes the OSMDroid configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = requireActivity().packageName
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        mapView = binding.mapviewDetail

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)

        return view
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been restored.
     * Fetches route details based on the provided route ID.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val routeId = args.routeId
        if (routeId != null) {
            fetchRouteDetails(routeId)
        } else {
            Toast.makeText(requireContext(), "Error: Route ID not available.", Toast.LENGTH_SHORT)
                .show()
            findNavController().navigateUp()
        }
    }

    /**
     * Fetches the details of a specific route from Firebase Firestore.
     *
     * @param routeId The ID of the route to fetch.
     */
    private fun fetchRouteDetails(routeId: String) {
        firestore.collection("routes").document(routeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Retrieve path points and timestamps.
                    val pathPointsFirestore = document["pathPoints"] as? List<FirestoreGeoPoint>
                    val startTime = document["startTime"] as? Long
                    val endTime = document["endTime"] as? Long

                    // Retrieve numerical statistics, handling potential type mismatches.
                    val totalDistanceAny = document["totalDistance"]
                    val averageSpeedAny = document["averageSpeed"]
                    val maxSpeedAny = document["maxSpeed"]

                    val totalDistance = when (totalDistanceAny) {
                        is Number -> totalDistanceAny.toFloat()
                        else -> null
                    }
                    val averageSpeed = when (averageSpeedAny) {
                        is Number -> averageSpeedAny.toFloat()
                        else -> null
                    }
                    val maxSpeed = when (maxSpeedAny) {
                        is Number -> maxSpeedAny.toFloat()
                        else -> null
                    }

                    Log.d(
                        "RouteDetailFragment",
                        "Fetched totalDistance: $totalDistanceAny -> $totalDistance"
                    )
                    Log.d(
                        "RouteDetailFragment",
                        "Fetched averageSpeed: $averageSpeedAny -> $averageSpeed"
                    )
                    Log.d("RouteDetailFragment", "Fetched maxSpeed: $maxSpeedAny -> $maxSpeed")

                    // Convert Firestore GeoPoints to OSMDroid GeoPoints.
                    val routePoints = pathPointsFirestore?.map {
                        GeoPoint(it.latitude, it.longitude)
                    } ?: emptyList()

                    if (routePoints.isNotEmpty()) {
                        // Draw the route path on the map.
                        drawRouteOnMap(routePoints)
                    } else {
                        Log.d("RouteDetailFragment", "No points in route for ID: $routeId")
                    }

                    // Display the fetched route details in the UI.
                    displayRouteDetails(
                        startTime,
                        endTime,
                        totalDistance,
                        averageSpeed,
                        maxSpeed
                    )
                } else {
                    Toast.makeText(requireContext(), "Route not found.", Toast.LENGTH_SHORT).show()
                    Log.d("RouteDetailFragment", "No route found with ID: $routeId")
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error loading route: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("RouteDetailFragment", "Error loading route", e)
                findNavController().navigateUp()
            }
    }

    /**
     * Draws the given list of geographical points as a polyline on the map.
     * Also zooms the map to fit the entire route.
     *
     * @param routePoints The list of [GeoPoint]s representing the route.
     */
    private fun drawRouteOnMap(routePoints: List<GeoPoint>) {
        val polyline = Polyline().apply {
            setPoints(routePoints)
            color = Color.BLUE
            width = 8f
        }
        mapView.overlays.add(polyline)
        mapView.invalidate()

        if (routePoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(routePoints)
            mapView.zoomToBoundingBox(boundingBox.increaseByScale(1.2f), true)
        }
    }

    /**
     * Displays the route's statistics in the corresponding TextViews in the UI.
     *
     * @param startTime The start timestamp of the route.
     * @param endTime The end timestamp of the route.
     * @param totalDistance The total distance covered during the route (in meters).
     * @param averageSpeed The average speed during the route (in m/s).
     * @param maxSpeed The maximum speed achieved during the route (in m/s).
     */
    private fun displayRouteDetails(
        startTime: Long?,
        endTime: Long?,
        totalDistance: Float?,
        averageSpeed: Float?,
        maxSpeed: Float?
    ) {
        val df = DecimalFormat("#.##")
        val dateFormat =
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        binding.startTimeTextView.text = startTime?.let { dateFormat.format(Date(it)) } ?: "N/A"
        binding.endTimeTextView.text = endTime?.let { dateFormat.format(Date(it)) } ?: "N/A"

        val durationMillis = if (startTime != null && endTime != null) endTime - startTime else 0L
        val durationMinutes = durationMillis / (1000.0 * 60.0)
        binding.durationTextView.text = df.format(durationMinutes) + " minutes"

        binding.totalDistanceTextView.text =
            totalDistance?.let { df.format(it / 1000.0) + " km" } ?: "N/A"

        // Display average speed in km/h, or "N/A".
        binding.averageSpeedTextView.text =
            averageSpeed?.let { df.format(it * 3.6) + " km/h" } ?: "N/A"

        // Display maximum speed in km/h, or "N/A".
        binding.maxSpeedTextView.text = maxSpeed?.let { df.format(it * 3.6) + " km/h" } ?: "N/A"
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Resumes the map view to handle lifecycle events.
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    /**
     * Called when the fragment is no longer actively interacting with the user.
     * Pauses the map view to handle lifecycle events.
     */
    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up resources used by the map view and clears the binding.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
        _binding = null
    }
}