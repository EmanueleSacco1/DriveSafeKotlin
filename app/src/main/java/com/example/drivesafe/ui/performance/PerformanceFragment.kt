package com.example.drivesafe.ui.performance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import androidx.lifecycle.ViewModelProvider

import com.example.drivesafe.databinding.FragmentPerformanceBinding
import com.example.drivesafe.R
import com.example.drivesafedb.ui.performance.PerformanceViewModel

/**
 * [Fragment] for the Driving Performance screen.
 * This fragment displays real-time driving metrics such as current speed, average speed,
 * maximum speed, and counts of sudden braking and acceleration events.
 * It interacts with the system's [LocationManager] to obtain location and speed data,
 * and uses a [PerformanceViewModel] for data processing, storage, and UI updates.
 */
class PerformanceFragment : Fragment() {

    /**
     * View binding instance for `fragment_performance.xml`.
     * This property is nullable because the binding object is only valid
     * between `onCreateView` and `onDestroyView`.
     */
    private var _binding: FragmentPerformanceBinding? = null

    /**
     * Provides a non-null reference to the view binding.
     * This accessor should only be used when the fragment's view is available
     * (i.e., between `onCreateView` and `onDestroyView`).
     */
    private val binding get() = _binding!!

    /**
     * Reference to the [PerformanceViewModel] associated with this fragment.
     * This ViewModel holds and processes the driving performance data.
     */
    private lateinit var viewModel: PerformanceViewModel

    /**
     * Reference to the system's [LocationManager], used to request location updates
     * and retrieve speed information from GPS.
     */
    private lateinit var locationManager: LocationManager

    /**
     * A unique integer code used to identify the location permission request result.
     */
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    /**
     * Called to create and return the view hierarchy associated with the fragment.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previously saved state.
     * @return The root [View] for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPerformanceBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this)[PerformanceViewModel::class.java]

        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        observeViewModel()

        startLocationUpdates()

        return binding.root
    }

    /**
     * Configures the observers for the [LiveData] objects exposed by the [PerformanceViewModel].
     * Each observer is tied to the fragment's view lifecycle ([viewLifecycleOwner])
     * to ensure updates only happen when the view is active and visible.
     */
    private fun observeViewModel() {
        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            binding.speedValue.text = getString(R.string.speed_value_format, speed)
        }

        viewModel.averageSpeed.observe(viewLifecycleOwner) { averageSpeed ->
            binding.avgSpeedValue.text = getString(R.string.speed_value_format, averageSpeed)
        }

        viewModel.maxSpeed.observe(viewLifecycleOwner) { maxSpeed ->
            binding.maxSpeedValue.text = getString(R.string.speed_value_format, maxSpeed)
        }

        viewModel.brakingCount.observe(viewLifecycleOwner) { count ->
            binding.brakeCountValue.text = count.toString()
        }

        viewModel.accelerationCount.observe(viewLifecycleOwner) { count ->
            binding.accelCountValue.text = count.toString()
        }

        viewModel.notificationMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Manages the process of obtaining necessary location permissions and subsequently
     * requesting and receiving location updates from the [LocationManager].
     */
    @SuppressLint("MissingPermission") // Suppress warning as permission is checked before requesting updates.
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            Log.w("PerformanceFragment", "ACCESS_FINE_LOCATION permission not granted, requesting.")
            return
        }

        Log.d("PerformanceFragment", "ACCESS_FINE_LOCATION permission granted, requesting updates.")
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                10f,
                locationListener,
                Looper.getMainLooper()
            )
            Log.d("PerformanceFragment", "Requested location updates from GPS_PROVIDER.")

        } catch (ex: SecurityException) {
            Log.e("PerformanceFragment", "SecurityException during location updates request.", ex)
            Toast.makeText(requireContext(), R.string.location_security_error, Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Callback method invoked by the operating system with the result of a permission request
     * initiated by [ActivityCompat.requestPermissions].
     *
     * @param requestCode The integer request code originally supplied to [requestPermissions].
     * @param permissions The array of requested permissions.
     * @param grantResults The array with the results of the request ([PackageManager.PERMISSION_GRANTED] or [PackageManager.PERMISSION_DENIED]).
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PerformanceFragment", "Location permission granted by user.")
                    startLocationUpdates()
                } else {
                    Log.w("PerformanceFragment", "Location permission denied by user.")
                    Toast.makeText(
                        requireContext(),
                        R.string.location_permission_needed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Internal implementation of the [LocationListener] interface.
     * This object receives callbacks from the [LocationManager] whenever new location data is available
     * or the status of a provider changes.
     */
    private val locationListener = object : LocationListener {
        /**
         * Called when the device's location has changed and new data is available.
         * This is the primary callback method for receiving location updates.
         *
         * @param location The [Location] object containing the new data (e.g., latitude, longitude, speed, etc.).
         */
        override fun onLocationChanged(location: Location) {
            viewModel.updateLocation(location)
            Log.d(
                "PerformanceFragment",
                "Location Update Received: Lat=${location.latitude}, Lon=${location.longitude}, Speed=${location.speed} m/s"
            )
        }

        /**
         * Called when the status of a location provider changes.
         * This method is deprecated in more recent Android APIs but is included for compatibility
         * with older Android versions.
         *
         * @param provider The name of the location provider.
         * @param status The new status of the provider.
         * @param extras A Bundle containing extra information about the status change.
         */
        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        /**
         * Called when a location provider is enabled by the user (e.g., GPS is turned on).
         *
         * @param provider The name of the enabled provider.
         */
        override fun onProviderEnabled(provider: String) {
            Log.d("PerformanceFragment", "Location provider enabled: $provider")
        }

        /**
         * Called when a location provider is disabled by the user (e.g., GPS is turned off).
         *
         * @param provider The name of the disabled provider.
         */
        override fun onProviderDisabled(provider: String) {
            Log.d("PerformanceFragment", "Location provider disabled: $provider")
            if (provider == LocationManager.GPS_PROVIDER) {
                Toast.makeText(requireContext(), R.string.gps_disabled_message, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * This method is crucial for cleaning up resources and preventing memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
            Log.d("PerformanceFragment", "Location updates removed in onDestroyView.")
        }
        _binding = null
    }
}