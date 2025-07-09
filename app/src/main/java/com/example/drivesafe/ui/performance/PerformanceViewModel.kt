package com.example.drivesafedb.ui.performance

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.abs

/**
 * [ViewModel] for the Performance screen.
 * It holds, manages, and calculates driving performance data
 * such as current speed, average speed, maximum speed, and counts of
 * sudden braking/acceleration events, based on incoming location updates.
 */
class PerformanceViewModel : ViewModel() {

    /**
     * Private [MutableLiveData] holding the current speed of the vehicle in kilometers per hour (km/h).
     * Initialized to 0.
     */
    private val _speed = MutableLiveData(0)

    /**
     * Public [LiveData] exposed to the UI for observing the current speed.
     */
    val speed: LiveData<Int> = _speed

    /**
     * Private [MutableLiveData] holding the calculated average speed in kilometers per hour (km/h)
     * since the data was last reset. Initialized to 0.
     */
    private val _averageSpeed = MutableLiveData(0)

    /**
     * Public [LiveData] exposed to the UI for observing the average speed.
     */
    val averageSpeed: LiveData<Int> = _averageSpeed

    /**
     * Private [MutableLiveData] holding the cumulative count of sudden braking events.
     * Initialized to 0.
     */
    private val _brakingCount = MutableLiveData(0)

    /**
     * Public [LiveData] exposed to the UI for observing the braking event count.
     */
    val brakingCount: LiveData<Int> = _brakingCount

    /**
     * Private [MutableLiveData] holding the cumulative count of sudden acceleration events.
     * Initialized to 0.
     */
    private val _accelerationCount = MutableLiveData(0)

    /**
     * Public [LiveData] exposed to the UI for observing the acceleration event count.
     */
    val accelerationCount: LiveData<Int> = _accelerationCount

    /**
     * Private [MutableLiveData] holding the maximum speed reached in kilometers per hour (km/h)
     * since the data was last reset. Initialized to 0.
     */
    private val _maxSpeed = MutableLiveData(0)

    /**
     * Public [LiveData] exposed to the UI for observing the maximum speed.
     */
    val maxSpeed: LiveData<Int> = _maxSpeed

    /**
     * Private [MutableLiveData] for sending generic notification messages to the UI
     * (e.g., for status updates or errors).
     */
    private val _notificationMessage = MutableLiveData<String>()

    /**
     * Public [LiveData] exposed to the UI for observing notification messages.
     */
    val notificationMessage: LiveData<String> = _notificationMessage

    /**
     * Internal list to store speed samples in meters per second (m/s) for average speed calculation.
     */
    private val speedSamples = mutableListOf<Float>()

    /**
     * Stores the last recorded speed in meters per second (m/s) for calculating speed changes (delta).
     */
    private var lastSpeed: Float = 0f

    /**
     * Threshold for detecting "sudden" acceleration or deceleration events.
     * A change in speed exceeding this value (in m/s²) within a short period is considered sudden.
     * This value can be adjusted to tune sensitivity.
     */
    private val accelerationThreshold = 3.0f

    /**
     * Internal variable to keep track of the maximum speed recorded in meters per second (m/s).
     */
    private var currentMaxSpeed: Float = 0f

    /**
     * Updates all performance data based on a new [Location] object received.
     * This method is typically called whenever the device's location changes significantly.
     *
     * @param location The [Location] object provided by the LocationManager, containing the current speed and other data.
     */
    fun updateLocation(location: Location) {
        val currentSpeed = location.speed

        _speed.value = (currentSpeed * 3.6).toInt()
        speedSamples.add(currentSpeed)
        if (speedSamples.isNotEmpty()) {
            _averageSpeed.value = (speedSamples.average() * 3.6).toInt()
        } else {
            _averageSpeed.value = 0
        }

        if (currentSpeed > currentMaxSpeed) {
            currentMaxSpeed = currentSpeed
            _maxSpeed.value = (currentMaxSpeed * 3.6).toInt()
        }

        val delta = currentSpeed - lastSpeed

        if (delta > accelerationThreshold) {
            _accelerationCount.value = _accelerationCount.value?.plus(1)
        }

        else if (abs(delta) > accelerationThreshold && delta < 0) {
            _brakingCount.value = _brakingCount.value?.plus(1)
        }

        lastSpeed = currentSpeed
    }

    /**
     * Resets all performance data metrics to their initial default values (typically zero).
     * This is useful for starting a new measurement session.
     */
    fun resetData() {
        speedSamples.clear()
        _speed.value = 0
        _averageSpeed.value = 0
        _accelerationCount.value = 0
        _brakingCount.value = 0
        _maxSpeed.value = 0
        currentMaxSpeed = 0f
        lastSpeed = 0f
        _notificationMessage.value = null
    }

    /**
     * Called automatically by the Android system when the ViewModel is no longer in use
     * and is about to be destroyed (e.g., when the associated Fragment/Activity is finished).
     * Used to perform final cleanup operations.
     */
    override fun onCleared() {
        super.onCleared()
        resetData()
    }
}