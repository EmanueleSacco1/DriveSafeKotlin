package com.example.drivesafe.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.drivesafe.data.local.CarDao

/**
 * Factory class for creating instances of [GarageViewModel].
 * Factories are necessary when a ViewModel has parameters in its constructor
 * (in this case, the [carDao] parameter) and cannot be created directly by the system.
 * It implements the [ViewModelProvider.Factory] interface.
 */
class GarageViewModelFactory(private val carDao: CarDao) :
    ViewModelProvider.Factory {

    /**
     * Creates an instance of the specified ViewModel class.
     * This is a core method of the [ViewModelProvider.Factory] interface, called by the system.
     * @param modelClass The [Class] of the ViewModel to create.
     * @return An instance of the specified ViewModel.
     * @throws IllegalArgumentException if the requested ViewModel class is not [GarageViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GarageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GarageViewModel(carDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
