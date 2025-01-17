package com.example.language_learning_helper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.media.projection.MediaProjectionManager
import android.view.WindowManager
import android.hardware.display.DisplayManager

class MainActivity : AppCompatActivity() {

    private var overlayPermissionGranted = false
    private var mediaProjectionPermissionGranted = false

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            overlayPermissionGranted = Settings.canDrawOverlays(this)
            if (!overlayPermissionGranted) {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
            checkAndStartService()
        }

    private val mediaProjectionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                FloatingIconService.mediaProjectionData = result.data
                FloatingIconService.resultCode = result.resultCode
                mediaProjectionPermissionGranted = true
            } else {
                Toast.makeText(this, "MediaProjection permission denied", Toast.LENGTH_SHORT).show()
            }
            checkAndStartService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText: EditText = findViewById(R.id.inputText)
        val translateButton: Button = findViewById(R.id.translateButton)
        val translationOutput: TextView = findViewById(R.id.translationOutput)

        val screenWidth = getScreenWidth()
        val screenHeight = getScreenHeight()

        println("Screen width: $screenWidth")
        println("Screen height (without status & navigation bars): $screenHeight")

        translateButton.setOnClickListener {
            val input = inputText.text.toString()
            if (input.isNotEmpty()) {
                getTranslation(input) { translatedText ->
                    translationOutput.text = translatedText
                }
            } else {
                translationOutput.text = "Please enter text."
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionGranted = false
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            overlayPermissionGranted = true
        }

        // Check MediaProjection permission
        if (!checkSavedMediaProjectionPermission()) {
            mediaProjectionPermissionGranted = false
            requestMediaProjectionPermission()
        } else {
            mediaProjectionPermissionGranted = true
        }

        checkAndStartService()
    }

    private fun checkSavedMediaProjectionPermission(): Boolean {
        return (FloatingIconService.mediaProjectionData != null &&
                FloatingIconService.resultCode == RESULT_OK)
    }

    private fun requestMediaProjectionPermission() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionPermissionLauncher.launch(captureIntent)
    }

    private fun checkAndStartService() {
        if (overlayPermissionGranted && mediaProjectionPermissionGranted) {
            if (!FloatingIconService.isServiceRunning) {
                FloatingIconService.startService(this)
            }
        }
    }


 // Get screen width without status and navigation bars
 private fun getScreenWidth(): Int {
    val metrics = resources.displayMetrics
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(metrics)
    return metrics.widthPixels
}

// Get screen height without status and navigation bars
private fun getScreenHeight(): Int {
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    val metrics = resources.displayMetrics
    windowManager.defaultDisplay.getMetrics(metrics)

    // Get the status bar height
    val statusBarHeight = getStatusBarHeight()

    // Get the navigation bar height
    val navigationBarHeight = getNavBarHeight()

    // Calculate the height of the screen excluding the bars
    return metrics.heightPixels - statusBarHeight - navigationBarHeight
}

// Get status bar height dynamically
private fun getStatusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}

// Get navigation bar height dynamically
private fun getNavBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}


    override fun onDestroy() {
        super.onDestroy()
    }
}
