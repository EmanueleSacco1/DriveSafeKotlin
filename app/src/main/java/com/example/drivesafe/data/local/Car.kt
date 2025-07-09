package com.example.drivesafe.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Car entity in the database.
 * This data class is mapped to a table named 'cars'.
 */
@Entity(tableName = "cars")
data class Car(
    /**
     * The unique identifier for the car.
     * @PrimaryKey marks this field as the primary key of the table.
     * autoGenerate = true configures Room to automatically generate a unique value
     * for this field for each new row inserted.
     * Using Long for auto-generated IDs is the recommended standard type in Room.
     * The default value 0L is used before insertion (Room will overwrite it).
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /**
     * The brand of the car (e.g., "Fiat", "Ford").
     * This field is mandatory and cannot be null.
     */
    val brand: String,

    /**
     * The model of the car (e.g., "Panda", "Focus").
     * This field is mandatory and cannot be null.
     */
    val model: String,

    /**
     * The manufacturing or registration year of the car.
     * Stored as an integer. This field is mandatory.
     */
    val year: Int,

    // --- Optional fields for car details ---

    /**
     * Timestamp of the RCA (mandatory car insurance) payment date.
     * Nullable.
     */
    val rcaPaidDate: Long? = null,

    /**
     * Timestamp of the next mandatory technical inspection date.
     * Nullable.
     */
    val nextRevisionDate: Long? = null,

    /**
     * Odometer reading (kilometers) at the time of the last technical inspection.
     * Nullable.
     */
    val revisionOdometer: Int? = null,

    /**
     * Timestamp of the car's initial registration date.
     * Nullable.
     */
    val registrationDate: Long? = null,

    /**
     * Cost of the car tax (bollo auto).
     * Stored as a Double for decimal values. Nullable.
     */
    val bolloCost: Double? = null,

    /**
     * Timestamp of the car tax (bollo auto) expiration date.
     * Nullable.
     */
    val bolloExpirationDate: Long? = null

)
