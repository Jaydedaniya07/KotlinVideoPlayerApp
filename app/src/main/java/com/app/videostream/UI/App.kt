package com.app.videostream.UI

import android.app.Application
import com.app.videostream.utils.NetworkCallback
import com.app.videostream.utils.PreferenceManager

class App : Application(){

    companion object{
        lateinit var preferenceManager: PreferenceManager
        lateinit var sessionId: String
        var isNetworkAvailable: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        sessionId = preferenceManager.accessToken.ifEmpty { "" }
        NetworkCallback.init(this)
    }
}