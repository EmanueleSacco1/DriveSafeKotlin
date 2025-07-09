package com.example.drivesafe.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drivesafe.R
import com.example.drivesafe.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * [AppCompatActivity] responsible for managing the user interface and logic
 * to allow new user registration using Firebase Authentication.
 */
class RegisterActivity : AppCompatActivity() {

    /**
     * View binding instance for accessing layout elements defined in `activity_register.xml`.
     */
    private lateinit var binding: ActivityRegisterBinding

    /**
     * Firebase Authentication instance, used for creating new user accounts.
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

        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            // Retrieve email and password input from EditTexts, trimming leading/trailing whitespace.
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Attempt to create a new user with the provided email and password using Firebase Auth.
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
                // Add a listener that is invoked if the user creation operation fails.
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        getString(
                            R.string.registration_failed_format, // Formatted string resource.
                            exception.message // Error message from the exception (e.g., "email already in use").
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}