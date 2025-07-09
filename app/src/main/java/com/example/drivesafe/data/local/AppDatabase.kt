package com.example.drivesafe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database class for the application using Room persistence library.
 * Defines the database configuration and serves as the main access point
 * to the persisted data.
 */
@Database(
    entities = [Car::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstract method to get the Data Access Object (DAO) for the Car entity.
     * Room will automatically generate an implementation for this method and the DAO.
     * @return The CarDao instance for interacting with the cars table.
     */
    abstract fun carDao(): CarDao

    /**
     * Companion object containing static methods for database access,
     * particularly for creating a singleton instance.
     */
    companion object {
        /**
         * The singleton instance of the database.
         * @Volatile ensures that the value of INSTANCE is always up-to-date and visible
         * to all threads, preventing caching issues.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of the database.
         * If the instance does not exist, it creates one in a synchronized block
         * to ensure that only one thread at a time can create the instance.
         * @param context The application context, used to build the database.
         * Using applicationContext helps prevent memory leaks.
         * @return The singleton instance of AppDatabase.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
