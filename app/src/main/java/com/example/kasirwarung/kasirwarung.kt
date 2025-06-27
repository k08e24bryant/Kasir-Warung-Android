package com.example.kasirwarung

import android.app.Application
import com.google.firebase.FirebaseApp


class KasirWarungApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)



    }
}