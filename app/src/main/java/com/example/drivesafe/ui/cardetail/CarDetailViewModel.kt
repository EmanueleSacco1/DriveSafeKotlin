package com.example.drivesafe.ui.cardetail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.lifecycle.map
import kotlinx.coroutines.launch

import com.example.drivesafe.data.local.Car
import com.example.drivesafe.data.local.CarDao
import com.example.drivesafe.R

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * ViewModel for the Car Detail screen.
 * Holds and manages the UI-related data and database operations for a single car.
 * Receives the car ID and the DAO as dependencies in the constructor, provided by the Factory.
 */
class CarDetailViewModel(private val carId: Long, private val carDao: CarDao) : ViewModel() {

    /**
     * Private Flow (_carFlow) that retrieves the car from the database based on the ID.
     * The Flow emits a new value whenever the car in the database changes.
     * It is nullable (Car?) because the car might not exist for the given ID.
     */
    private val _carFlow: Flow<Car?> = carDao.getCarById(carId)

    /**
     * Public LiveData (car) exposed to the UI.
     * It is derived from _carFlow using the asLiveData() extension, converting the reactive Flow into observable LiveData.
     */
    val car: LiveData<Car?> = _carFlow.asLiveData()

    /**
     * LiveData containing the calculated expiration date for the RCA (1 year after the paid date).
     * Derived from _carFlow.map (transforms Flow<Car?> into Flow<Date?>) and then converted to LiveData.
     */
    val rcaExpirationDate: LiveData<Date?> =
        _carFlow.map { car -> calculateRcaExpirationDate(car?.rcaPaidDate) }.asLiveData()

    /**
     * LiveData containing the countdown string for the RCA (e.g., "3 mesi e 15 giorni").
     * Derived from the rcaExpirationDate LiveData using the LiveData.map extension.
     */
    val rcaCountdown: LiveData<String> = rcaExpirationDate.map { expirationDate ->
        calculateCountdownString(expirationDate?.time)
    }

    /**
     * LiveData containing the countdown string for the Revision.
     * Derived from _carFlow.map and converted to LiveData.
     */
    val revisionCountdown: LiveData<String> =
        _carFlow.map { car -> calculateCountdownString(car?.nextRevisionDate) }.asLiveData()

    /**
     * LiveData containing the countdown string for the Bollo.
     * Derived from _carFlow.map and converted to LiveData.
     */
    val bolloCountdown: LiveData<String> =
        _carFlow.map { car -> calculateCountdownString(car?.bolloExpirationDate) }.asLiveData()

    /**
     * Initialization block: Executed when the ViewModel is created.
     */
    init {
        Log.d("CarDetailViewModel", "init: ViewModel created with carId: $carId")
    }

    /**
     * Calculates the RCA expiration date based on the paid date timestamp.
     * @param rcaPaidTimestamp Long timestamp (in milliseconds) of the date the RCA was paid. Nullable.
     * @return A Date object representing the expiration date (1 year after payment), or null if the paid timestamp is null.
     */
    private fun calculateRcaExpirationDate(rcaPaidTimestamp: Long?): Date? {
        if (rcaPaidTimestamp == null) {
            return null
        }
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = rcaPaidTimestamp
        calendar.add(Calendar.YEAR, 1)
        return calendar.time
    }

    /**
     * Calculates a descriptive countdown string until an expiration date.
     * @param expirationTimestamp Long timestamp (in milliseconds) of the expiration date. Nullable.
     * @return A string (e.g., "Oggi!", "30", "3 e 15", "") based on the day difference.
     */
    private fun calculateCountdownString(expirationTimestamp: Long?): String {
        if (expirationTimestamp == null) {
            return ""
        }
        val expirationCal = Calendar.getInstance().apply { timeInMillis = expirationTimestamp }
        val currentCal = Calendar.getInstance()
        // Clear time components to compare only whole dates.
        expirationCal.set(Calendar.HOUR_OF_DAY, 0)
        expirationCal.set(Calendar.MINUTE, 0)
        expirationCal.set(Calendar.SECOND, 0)
        expirationCal.set(Calendar.MILLISECOND, 0)
        currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0)
        currentCal.set(Calendar.SECOND, 0)
        currentCal.set(Calendar.MILLISECOND, 0)
        // Calculate the time difference in milliseconds.
        val diffInMillis = expirationCal.timeInMillis - currentCal.timeInMillis
        // Convert the difference to days.
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        // Return a formatted string based on the day difference.
        return when {
            diffInDays < 0 -> "" // If the date is in the past, return empty string (the "Expired!" status is handled in the UI).
            diffInDays == 0L -> "Oggi!" // If it expires today.
            diffInDays < 30 -> diffInDays.toString() // If less than 30 days remain, show only the days.
            else -> { // If 30 or more days remain.
                val months =
                    diffInDays / 30 // Calculate the number of whole months (approximation).
                val remainingDays = diffInDays % 30 // Calculate the remaining days.
                val parts = mutableListOf<String>() // List to build the string.
                if (months > 0) {
                    parts.add("$months")
                } // Add months if > 0.
                if (remainingDays > 0) {
                    parts.add("$remainingDays")
                } // Add days if > 0.
                parts.joinToString(" e ") // Join months and days with " e ".
            }
        }
    }

    /**
     * Determines the expiration status of an event (e.g., RCA, Revision, Bollo) based on its timestamp.
     * @param expirationTimestamp Long timestamp (in milliseconds) of the expiration date. Nullable.
     * @return An ExpirationStatus enum value (EXPIRED, TODAY, NEAR, FUTURE, UNKNOWN).
     */
    fun getExpirationStatus(expirationTimestamp: Long?): ExpirationStatus {
        // If the timestamp is null, the status is UNKNOWN.
        if (expirationTimestamp == null) {
            return ExpirationStatus.UNKNOWN
        }
        // Create Calendar instances for the expiration date and the current date.
        val expirationCal = Calendar.getInstance().apply { timeInMillis = expirationTimestamp }
        val currentCal = Calendar.getInstance()
        // Clear time components to compare only dates.
        expirationCal.set(Calendar.HOUR_OF_DAY, 0)
        expirationCal.set(Calendar.MINUTE, 0)
        expirationCal.set(Calendar.SECOND, 0)
        expirationCal.set(Calendar.MILLISECOND, 0)
        currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0)
        currentCal.set(Calendar.SECOND, 0)
        currentCal.set(Calendar.MILLISECOND, 0)
        // Determine the status by comparing dates and the difference in days.
        return when {
            expirationCal.before(currentCal) -> ExpirationStatus.EXPIRED // If the date is in the past.
            expirationCal.equals(currentCal) -> ExpirationStatus.TODAY // If the date is today.
            else -> { // If the date is in the future.
                val diffInMillis = expirationCal.timeInMillis - currentCal.timeInMillis
                val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                if (diffInDays <= 30) {
                    ExpirationStatus.NEAR // If 30 days or less remain.
                } else {
                    ExpirationStatus.FUTURE // If more than 30 days remain.
                }
            }
        }
    }

    /**
     * Validates the data of a Car object to check for consistency (e.g., future dates, positive numbers).
     * @param carData The Car object with data to validate.
     * @param context The application context, used to access string resources for error messages.
     * @return The resource ID (Int) of a string corresponding to the error message if validation fails,
     * or null if validation is successful.
     */
    fun validateCarData(carData: Car, context: Context): Int? {
        // Validate next revision date: cannot be in the past.
        val nextRevisionDate = carData.nextRevisionDate
        if (nextRevisionDate != null) {
            val revisionCal = Calendar.getInstance().apply { timeInMillis = nextRevisionDate }
            val nowCal = Calendar.getInstance()
            // Clear time components to compare only dates.
            revisionCal.set(Calendar.HOUR_OF_DAY, 0)
            nowCal.set(Calendar.HOUR_OF_DAY, 0)
            revisionCal.set(Calendar.MINUTE, 0)
            nowCal.set(Calendar.MINUTE, 0)
            revisionCal.set(Calendar.SECOND, 0)
            nowCal.set(Calendar.SECOND, 0)
            revisionCal.set(Calendar.MILLISECOND, 0)
            nowCal.set(Calendar.MILLISECOND, 0)
            if (revisionCal.before(nowCal)) {
                return R.string.error_revision_date_in_past
            }
        }
        // Validate odometer reading at revision: cannot be negative.
        val revisionOdometer = carData.revisionOdometer
        if (revisionOdometer != null && revisionOdometer < 0) {
            return R.string.error_revision_odometer_positive
        }
        // Validate bollo cost: must be positive.
        val bolloCost = carData.bolloCost
        if (bolloCost != null && bolloCost <= 0) {
            return R.string.error_bollo_cost_positive
        }
        return null
    }

    /**
     * Saves the changes to the car in the database.
     * @param updatedCar The Car object with the updated data to save.
     * suspend fun: The database operation (updateCar) is suspending, so this function must be called
     * from a coroutine. The ViewModel launches a coroutine internally for this.
     */
    fun saveCar(updatedCar: Car) {
        viewModelScope.launch {
            carDao.updateCar(updatedCar)
        }
    }
}
