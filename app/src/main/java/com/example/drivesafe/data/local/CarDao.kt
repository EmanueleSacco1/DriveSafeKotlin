package com.example.drivesafe.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // Import Flow for observable methods.

/**
 * Data Access Object (DAO) for the Car entity.
 * This interface defines the methods for interacting with the 'cars' table in the database.
 * Room will automatically generate an implementation for this interface.
 */
@Dao
interface CarDao {

    /**
     * Inserts a Car entity into the database.
     * @param car The Car entity to insert.
     * @return The row ID of the newly inserted car, or -1 if the insertion was ignored due to conflict.
     * @Insert(onConflict = OnConflictStrategy.IGNORE): If a conflict occurs (e.g., trying to insert a car with an existing ID > 0),
     * the insert operation is ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCar(car: Car): Long

    /**
     * Updates an existing Car entity in the database.
     * Room uses the primary key (id) of the provided Car entity to find the corresponding row to update.
     * @param car The Car entity to update (must have a valid ID that exists in the DB).
     */
    @Update
    suspend fun updateCar(car: Car)

    /**
     * Deletes a Car entity from the database.
     * Room uses the primary key (id) of the provided Car entity to find the corresponding row to delete.
     * @param car The Car entity to delete (must have a valid ID that exists in the DB).
     */
    @Delete
    suspend fun deleteCar(car: Car)

    /**
     * Retrieves all Car entities from the 'cars' table, ordered by ID in ascending order.
     * @Query("SELECT * FROM cars ORDER BY id ASC"): The SQL query to execute.
     * @return A Flow emitting a list of all Car entities. A Flow is a reactive stream;
     * new lists will be emitted whenever the data in the 'cars' table changes.
     * This is not a suspend fun because collecting from the Flow is handled within a coroutine context.
     */
    @Query("SELECT * FROM cars ORDER BY id ASC")
    fun getAllCars(): Flow<List<Car>>

    /**
     * Retrieves a specific Car entity by its ID.
     * @param carId The ID of the Car to retrieve.
     * @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1"): Selects all columns from the 'cars' table
     * where the 'id' matches the provided carId parameter. LIMIT 1 ensures at most one row is returned.
     * @return A Flow emitting a single Car object or null if no car with the specified ID is found.
     * The Flow will also emit a new value if the car with that ID is modified.
     */
    @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1")
    fun getCarById(carId: Long): Flow<Car?>
}
