package com.example.drivesafe.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log

import com.example.drivesafe.data.local.Car
import com.example.drivesafe.data.local.CarDao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Garage screen.
 * Holds and manages the list of cars and the operations for adding/deleting cars.
 * It receives the DAO as a dependency in the constructor, provided by the Factory.
 */
class GarageViewModel(private val carDao: CarDao) : ViewModel() {

    /**
     * Public Flow ([carList]) that retrieves all cars from the database.
     * This Flow will emit a new list whenever the cars table changes.
     * It is used by the UI (Fragment) to observe changes in the list of cars.
     */
    val carList: Flow<List<Car>> = carDao.getAllCars()

    /**
     * Adds a new car to the database.
     * This is a suspend function because the database insertion is an operation that may take time
     * and must be executed on a background thread (managed by Room/Coroutines).
     * @param brand The brand of the new car.
     * @param model The model of the new car.
     * @param year The year of the new car.
     * @return The generated ID (Long) for the newly inserted car.
     */
    suspend fun addCar(brand: String, model: String, year: Int): Long {
        val newCar = Car(brand = brand, model = model, year = year)
        Log.d("GarageViewModel", "addCar: Attempting to insert car: $newCar")
        val newId = carDao.insertCar(newCar)
        Log.d("GarageViewModel", "addCar: Car inserted with ID: $newId")
        return newId
    }

    /**
     * Deletes a car from the database.
     * @param car The [Car] object to delete (must have a valid ID that exists in the DB).
     */
    fun deleteCar(car: Car) {
        viewModelScope.launch {
            Log.d("GarageViewModel", "deleteCar: Attempting to delete car with ID: ${car.id}")
            carDao.deleteCar(car)
            Log.d("GarageViewModel", "deleteCar: Car with ID ${car.id} deleted.")
        }
    }
}
