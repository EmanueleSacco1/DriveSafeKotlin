package com.example.drivesafe.ui.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.drivesafe.R
import com.example.drivesafe.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

/**
 * Instrumentation test for [LoginActivity].
 * These tests verify the UI behavior and navigation within the login activity.
 */
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    // Rule to launch the activity before each test.
    // Sets the `launchActivity` flag to false to manually control launching.
    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    // Mock of FirebaseAuth to control authentication behavior.
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Before
    fun setup() {
        // Initializes Espresso Intents to monitor intentions (navigation between activities).
        Intents.init()

        // Mocks FirebaseAuth.
        mockFirebaseAuth = mock(FirebaseAuth::class.java)

        // Replaces the FirebaseAuth instance with the mock.
        // This is an advanced trick and might require changes to the source code
        // to allow dependency injection of FirebaseAuth.
        // For this example, we assume that FirebaseAuth.getInstance() can be "mocked"
        // or that there is a way to inject a mock instance.
        // In a real application, you would use a dependency injection framework (e.g., Hilt/Dagger)
        // to provide mockable instances during testing.
        // Since we cannot modify the source code here, this part is conceptual.
        // For real UI tests, Firebase Test Lab or an emulator with test accounts would be more appropriate.

        // For demonstration purposes, we will simulate a success/failure without a real mock of getInstance.
        // Tests will rely on the UI and Toasts, which are easier to test without
        // complex dependency injection for FirebaseAuth.
    }

    @After
    fun tearDown() {
        // Releases Espresso Intents after each test.
        Intents.release()
    }

    @Test
    fun testLoginSuccessNavigatesToMainActivity() {
        // Simulates a successful login (this is conceptual without a real Firebase.getInstance() mock)
        // In a real test, you would configure the FirebaseAuth mock to return a successful Task.
        // Since we cannot statically mock getInstance(), this test relies on the assumption
        // that if the fields are filled correctly, the app attempts login.

        // Types email and password
        onView(withId(R.id.emailEditText)).perform(typeText("test@esempio.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(typeText("password123prova"), closeSoftKeyboard())

        // Clicks the login button
        onView(withId(R.id.loginButton)).perform(click())

        // Verifies that the intent to launch MainActivity was sent.
        // This test will assume that login was successful and the app navigates.
        // In a real application, you should mock Firebase to simulate success.
        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun testLoginFailureShowsToast() {
        // Types incorrect credentials
        onView(withId(R.id.emailEditText)).perform(typeText("sbagliato@esempio.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(typeText("passwordsbagliata"), closeSoftKeyboard())

        // Clicks the login button
        onView(withId(R.id.loginButton)).perform(click())

        // Verifies that a Toast with an error message is shown.
        // Note: Toasts are difficult to test directly with Espresso.
        // An indirect approach is often used, such as verifying that the activity has not finished
        // or that an internal UI error message is visible.
        // For this example, we assume the Toast is visible for a short period.
        // A more robust approach would be to check an error TextView within the layout.
        activityRule.scenario.onActivity { activity ->
            onView(withText(R.string.login_failed_format))
                .inRoot(withDecorView(not(activity.window.decorView)))
                .check(matches(isDisplayed()))
        }
    }
    @Test
    fun testRegisterTextNavigatesToRegisterActivity() {
        // Clicks the "Don't have an account? Register" text
        onView(withId(R.id.registerText)).perform(click())

        // Verifies that the intent to launch RegisterActivity was sent.
        intended(hasComponent(RegisterActivity::class.java.name))
    }

    @Test
    fun testForgotPasswordTextNavigatesToResetPasswordActivity() {
        // Clicks the "Forgot password?" text
        onView(withId(R.id.forgotPasswordText)).perform(click())

        // Verifies that the intent to launch ResetPasswordActivity was sent.
        intended(hasComponent(ResetPasswordActivity::class.java.name))
    }
}
