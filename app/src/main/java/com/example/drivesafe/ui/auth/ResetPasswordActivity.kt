package com.example.drivesafe.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drivesafe.R
import com.example.drivesafe.databinding.ActivityResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * [AppCompatActivity] responsible for managing the user interface and logic
 * to allow users to request a password reset via Firebase Authentication.
 */
class ResetPasswordActivity : AppCompatActivity() {

    /**
     * View binding instance for accessing layout elements defined in `activity_reset_password.xml`.
     */
    private lateinit var binding: ActivityResetPasswordBinding

    /**
     * Firebase Authentication instance, used for sending password reset emails.
     */
    private lateinit var auth: FirebaseAuth

    /**
     * Called when the activity is first created. This is where you should perform
     * all initialization comparable to the setup of an activity's UI and initial data.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     * then this Bundle contains the data it most recently supplied in [onSaveInstanceState].
     * Note: Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)

        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.resetPasswordButton.setOnClickListener {
            val email = binding.emailResetEditText.text.toString().trim()

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, R.string.reset_password_success, Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            getString(
                                R.string.reset_password_failed_format, // Formatted string resource.
                                exception.message // Error message from the exception (e.g., "invalid email format").
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            else {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            }
        }
    }
}