package com.example.drivesafe.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Represents a recorded driving route.
 * This data class is used to store route information in Firebase Firestore.
 */
data class Route(
    /**
     * The unique identifier for the route document in Firestore.
     * @DocumentId automatically populates this field with the document ID.
     */
    @DocumentId val id: String = "",

    /**
     * The unique identifier of the user who recorded this route.
     * Links the route to a specific user profile.
     */
    val userId: String = "",

    /**
     * Timestamp when the route was recorded (in milliseconds since epoch).
     * Used for ordering and temporal information.
     */
    val timestamp: Long = 0L,

    /**
     * A list of geographical points (latitude and longitude) representing the path of the route.
     * Stored as a list of GeoPoint objects.
     */
    val pathPoints: List<GeoPoint> = emptyList(),

    /**
     * Timestamp when the route recording started (in milliseconds since epoch).
     */
    val startTime: Long = 0L,

    /**
     * Timestamp when the route recording ended (in milliseconds since epoch).
     */
    val endTime: Long = 0L,

    /**
     * The total distance covered during the route (in meters).
     */
    val totalDistance: Float = 0f,

    /**
     * The average speed calculated for the route (in km/h).
     */
    val averageSpeed: Float = 0f,

    /**
     * The maximum speed reached during the route (in km/h).
     */
    val maxSpeed: Float = 0f
) {
    /**
     * No-argument constructor required by Firebase Firestore for deserialization.
     * Provides default values for all properties.
     */
    constructor() : this("", "", 0L, emptyList(), 0L, 0L, 0f, 0f, 0f)
}
