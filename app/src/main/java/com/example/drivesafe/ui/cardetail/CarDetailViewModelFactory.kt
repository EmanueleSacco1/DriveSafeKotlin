package com.example.drivesafe.ui.cardetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.drivesafe.data.local.CarDao

/**
 * Factory class for creating instances of [CarDetailViewModel].
 * Factories are necessary when a ViewModel has parameters in its constructor
 * that are not saved state (in this case, [carId] and [carDao]).
 * It implements the [ViewModelProvider.Factory] interface.
 */
class CarDetailViewModelFactory(
    /**
     * The ID of the car to be passed to the ViewModel.
     */
    private val carId: Long,
    /**
     * The Data Access Object (DAO) for cars, required by the ViewModel to interact with the database.
     */
    private val carDao: CarDao
) : ViewModelProvider.Factory {

    /**
     * Creates an instance of the specified ViewModel class.
     * This is a core method of the [ViewModelProvider.Factory] interface, called by the system.
     * @param modelClass The [Class] of the ViewModel to create.
     * @return An instance of the specified ViewModel.
     * @throws IllegalArgumentException if the requested ViewModel class is not [CarDetailViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarDetailViewModel(carId, carDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
