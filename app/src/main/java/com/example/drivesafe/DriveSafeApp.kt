package com.example.drivesafe

import android.app.Application
import com.google.firebase.FirebaseApp

class DriveSafeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}