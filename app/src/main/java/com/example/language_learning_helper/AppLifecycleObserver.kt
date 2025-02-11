package com.example.language_learning_helper

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(private val serviceContext: Context) : DefaultLifecycleObserver {
    private val servicePreferencesHelper = ServiceSharedPreferencesHelper(serviceContext)

    override fun onStart(owner: LifecycleOwner) {
        if (servicePreferencesHelper.showFloatingIcon()) {
            val intent = Intent("com.example.language_learning_helper.HIDE_FLOATING_ICON")
            serviceContext.sendBroadcast(intent)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (servicePreferencesHelper.showFloatingIcon()) {
            val intent = Intent("com.example.language_learning_helper.SHOW_FLOATING_ICON")
            serviceContext.sendBroadcast(intent)
        }
    }
}