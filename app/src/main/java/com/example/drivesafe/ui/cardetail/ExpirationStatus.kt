// File: app/src/main/java/com/example/drivesafe/ui/cardetail/ExpirationStatus.kt
package com.example.drivesafe.ui.cardetail

/**
 * Enum class representing the possible expiration statuses for events
 * such as RCA, Revision, and Bollo (car tax).
 * Used to determine the countdown text and indicator color in the UI.
 */
enum class ExpirationStatus {
    /**
     * Status: The expiration date is in the past.
     */
    EXPIRED,

    /**
     * Status: The expiration date is today.
     */
    TODAY,

    /**
     * Status: The expiration date is in the future but within a certain limit (e.g., <= 30 days).
     */
    NEAR,

    /**
     * Status: The expiration date is in the future and beyond the "near" limit.
     */
    FUTURE,

    /**
     * Status: The expiration date is not defined or is invalid.
     */
    UNKNOWN
}
