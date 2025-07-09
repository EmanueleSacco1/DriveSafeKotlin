package com.example.drivesafe.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drivesafe.ui.main.MainActivity
import com.example.drivesafe.R
import com.example.drivesafe.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * [AppCompatActivity] responsible for managing the user interface and logic
 * to allow users to log in to the application using Firebase Authentication.
 */
class LoginActivity : AppCompatActivity() {

    /**
     * View binding instance for accessing layout elements defined in `activity_login.xml`.
     */
    private lateinit var binding: ActivityLoginBinding

    /**
     * Firebase Authentication instance, providing access to all Firebase Auth functionalities.
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

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            // Retrieve email and password input from EditTexts, trimming leading/trailing whitespace.
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Attempt to sign in the user with the provided email and password using Firebase Auth.
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                // Add a listener that is invoked if the sign-in operation fails.
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this, // Context for the Toast.
                        getString(
                            R.string.login_failed_format, // Formatted string resource.
                            exception.message // Error message from the exception.
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        // Set an OnClickListener for the "Don't have an account? Register" TextView.
        binding.registerText.setOnClickListener {

            val intent = Intent(this, RegisterActivity::class.java)

            startActivity(intent)

        }

        // Set an OnClickListener for the "Forgot password?" TextView.
        binding.forgotPasswordText.setOnClickListener {
            // Create an Intent to navigate to `ResetPasswordActivity`.
            val intent = Intent(this, ResetPasswordActivity::class.java)

            startActivity(intent)

        }

    }
}