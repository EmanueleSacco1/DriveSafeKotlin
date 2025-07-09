package com.example.drivesafe.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a user's profile information stored in Firebase Firestore.
 * This data class holds details about the user, including personal information and driving licenses.
 */
data class User(
    /**
     * The unique identifier for the user document in Firestore.
     * Corresponds to the User ID from Firebase Authentication.
     * @DocumentId automatically populates this field with the document ID.
     */
    @DocumentId val uid: String = "",

    val email: String = "",

    val username: String = "",

    val favoriteCarBrand: String = "",

    val placeOfBirth: String = "",

    val streetAddress: String = "",

    val cityOfResidence: String = "",

    val yearOfBirth: Int? = null,

    /**
     * A list of license subcategories possessed by the user (e.g., ["B", "A1", "BE"]).
     * Stored as a list of strings.
     */
    val licenses: List<String> = emptyList(),

    /**
     * Timestamp when the user profile was initially registered (in milliseconds since epoch).
     * Used for tracking registration date.
     */
    val registrationTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * No-argument constructor required by Firebase Firestore for deserialization.
     * Provides default values for all properties, including the newly added fields.
     */
    constructor() : this("", "", "", "", "", "", "", null, emptyList(), 0L)
}
