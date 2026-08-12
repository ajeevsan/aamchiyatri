package com.amchiyatri.rider

import android.app.Application
import com.amchiyatri.rider.util.ApiKeys
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AmchiYatriApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, ApiKeys.mapsApiKey(this))
        }
    }
}
