package com.example.language_learning_helper

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ServicePermissionManager(
    private val activity: FragmentActivity,
    private val servicePreferencesHelper: ServiceSharedPreferencesHelper
) {
    private var overlayPermissionGranted = false
    private var mediaProjectionPermissionGranted = false

    private lateinit var overlayPermissionContinuation: (Boolean) -> Unit
    private lateinit var mediaProjectionContinuation: (Boolean) -> Unit

    // Launcher for OverlayPermissionBoxActivity
    private val overlayPermissionBoxLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // User pressed "Continue", now request the overlay permission
                launchOverlayPermission()
            } else {
                overlayPermissionContinuation(false) // User canceled, stop
            }
        }

    // Launcher for MediaProjectionPermissionBoxActivity
    private val mediaProjectionBoxLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // User pressed "Continue", now request MediaProjection permission
                launchMediaProjectionPermission()
            } else {
                mediaProjectionContinuation(false) // User canceled, stop
            }
        }

    // Launcher for requesting Overlay Permission
    private val overlayPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            overlayPermissionGranted = Settings.canDrawOverlays(activity.applicationContext)
            overlayPermissionContinuation(overlayPermissionGranted)
        }

    // Launcher for requesting MediaProjection Permission
    private val mediaProjectionPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                FloatingIconService.mediaProjectionData = result.data
                FloatingIconService.resultCode = result.resultCode
                mediaProjectionPermissionGranted = true
            } else {
                Toast.makeText(activity, "MediaProjection permission denied", Toast.LENGTH_SHORT).show()
                mediaProjectionPermissionGranted = false
            }
            mediaProjectionContinuation(mediaProjectionPermissionGranted)
        }

    suspend fun setupPermissions(): Boolean {
        val context = activity.applicationContext

        // Request Notification Permission (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        // Request Overlay Permission and wait for it to complete
        overlayPermissionGranted = requestOverlayPermission()

        // Request MediaProjection Permission and wait for it to complete
        mediaProjectionPermissionGranted = if (!checkSavedMediaProjectionPermission()) {
            requestMediaProjectionPermission()
        } else {
            true
        }

        val permissionsGranted = overlayPermissionGranted && mediaProjectionPermissionGranted
        checkAndStartService()
        return permissionsGranted
    }

    private suspend fun requestOverlayPermission(): Boolean {
        val context = activity.applicationContext
        if (Settings.canDrawOverlays(context)) return true

        return suspendCancellableCoroutine { continuation ->
            overlayPermissionContinuation = { granted ->
                continuation.resume(granted)
            }

            // First, show the OverlayPermissionBoxActivity
            val intent = Intent(activity, OverlayPermissionBoxActivity::class.java)
            overlayPermissionBoxLauncher.launch(intent)
        }
    }

    private fun launchOverlayPermission() {
        overlayPermissionLauncher.launch(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
        )
    }

    private suspend fun requestMediaProjectionPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            mediaProjectionContinuation = { granted ->
                continuation.resume(granted)
            }

            // First, show the MediaProjectionPermissionBoxActivity
            val intent = Intent(activity, MediaProjectionPermissionBoxActivity::class.java)
            mediaProjectionBoxLauncher.launch(intent)
        }
    }

    private fun launchMediaProjectionPermission() {
        val mediaProjectionManager =
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionPermissionLauncher.launch(captureIntent)
    }

    private fun checkSavedMediaProjectionPermission(): Boolean {
        return servicePreferencesHelper.isServiceOn()
    }

    private fun checkAndStartService() {
        if (overlayPermissionGranted && mediaProjectionPermissionGranted) {
            FloatingIconService.startService(activity.applicationContext)

            // **Register a temporary receiver to wait for service startup**
            val serviceStartedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "com.example.language_learning_helper.SERVICE_STARTED") {
                        // Now that the service is fully started, hide the floating icon
                        val hideIntent = Intent("com.example.language_learning_helper.HIDE_FLOATING_ICON")
                        context?.sendBroadcast(hideIntent)

                        // **Unregister the receiver correctly**
                        context?.unregisterReceiver(this)
                    }
                }
            }

            val filter = IntentFilter("com.example.language_learning_helper.SERVICE_STARTED")
            activity.applicationContext.registerReceiver(serviceStartedReceiver, filter)

            // Register lifecycle observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(activity.applicationContext))
        }
        servicePreferencesHelper.setServiceOn(overlayPermissionGranted && mediaProjectionPermissionGranted)
    }
}
