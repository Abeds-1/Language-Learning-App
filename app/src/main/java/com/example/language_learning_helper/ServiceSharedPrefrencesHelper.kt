package com.example.language_learning_helper

import android.content.Context
import android.content.SharedPreferences

class ServiceSharedPreferencesHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "service_prefs"
        private const val KEY_SERVICE_ON = "isServiceOn"
        private const val KEY_SHOW_FLOATING_ICON = "showFloatingIcon"
        private const val KEY_SCANNING_MODE_ON = "isScanningModeOn"
    }

    fun initializeDefaults(serviceOn: Boolean, floatingIcon: Boolean, scanningMode: Boolean) {
        if (!sharedPreferences.contains(KEY_SERVICE_ON)) {
            sharedPreferences.edit()
                .putBoolean(KEY_SERVICE_ON, serviceOn)
                .putBoolean(KEY_SHOW_FLOATING_ICON, floatingIcon)
                .putBoolean(KEY_SCANNING_MODE_ON, scanningMode)
                .apply()
        }
    }

    fun setServiceOn(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SERVICE_ON, value).apply()
    }

    fun toggleServiceOn() {
        setServiceOn(!isServiceOn())
    }

    fun setShowFloatingIcon(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_FLOATING_ICON, value).apply()
    }

    fun toggleShowFloatingIcon() {
        setShowFloatingIcon(!showFloatingIcon())
    }

    fun setScanningModeOn(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SCANNING_MODE_ON, value).apply()
    }

    fun toggleScanningModeOn() {
        setScanningModeOn(!isScanningModeOn())
    }

    fun isServiceOn(): Boolean {
        return sharedPreferences.getBoolean(KEY_SERVICE_ON, false)
    }

    fun showFloatingIcon(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_FLOATING_ICON, false)
    }

    fun isScanningModeOn(): Boolean {
        return sharedPreferences.getBoolean(KEY_SCANNING_MODE_ON, false)
    }
}
