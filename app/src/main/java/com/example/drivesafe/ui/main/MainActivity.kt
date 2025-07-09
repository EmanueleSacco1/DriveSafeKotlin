package com.example.drivesafe.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.drivesafe.R
import org.osmdroid.config.Configuration

import com.example.drivesafe.databinding.ActivityMainBinding
import com.example.drivesafe.ui.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

/**
 * The main [AppCompatActivity] of the DriveSafe application after successful login.
 * This activity sets up the navigation drawer, manages the app's main UI,
 * handles user session checks, integrates OSMDroid map configuration,
 * and provides options for toggling dark mode.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Configuration for the app bar, defining top-level destinations for the navigation controller.
     * This dictates which destinations do not show an "Up" button and instead show the navigation drawer icon.
     */
    private lateinit var appBarConfiguration: AppBarConfiguration

    /**
     * View binding instance for accessing layout elements defined in `activity_main.xml`.
     */
    private lateinit var binding: ActivityMainBinding

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

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        if (FirebaseAuth.getInstance().currentUser == null) {

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_garage,
                R.id.nav_performance,
                R.id.nav_routes,
                R.id.nav_profile,
                R.id.nav_logout
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val headerView = navView.getHeaderView(0)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val currentUser = FirebaseAuth.getInstance().currentUser

        // If a user is logged in, display their email in the navigation drawer header.
        currentUser?.let {
            val email = currentUser.email
            userEmailTextView.text = email
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val toggleNightModeItem = menu.findItem(R.id.action_toggle_night_mode)
        if (toggleNightModeItem != null) {
            val currentNightMode = AppCompatDelegate.getDefaultNightMode()
            when (currentNightMode) {
                AppCompatDelegate.MODE_NIGHT_YES,
                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                    toggleNightModeItem.setIcon(R.drawable.ic_sun)
                }

                else -> {
                    toggleNightModeItem.setIcon(R.drawable.ic_moon)
                }
            }
        }
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return Return false to allow normal menu processing to proceed, true to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_night_mode -> {
                val currentNightMode = AppCompatDelegate.getDefaultNightMode()
                val newNightMode = when (currentNightMode) {
                    AppCompatDelegate.MODE_NIGHT_YES,
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }

                    else -> {
                        AppCompatDelegate.MODE_NIGHT_YES
                    }
                }
                AppCompatDelegate.setDefaultNightMode(newNightMode)
                recreate()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This method is called whenever the user chooses to navigate Up within your application's
     * task hierarchy from the action bar.
     *
     * @return True if Up navigation was successfully handled by the NavController,
     * false otherwise.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}