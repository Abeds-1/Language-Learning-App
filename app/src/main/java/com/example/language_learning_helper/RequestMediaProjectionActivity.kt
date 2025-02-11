package com.example.language_learning_helper

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast

class RequestMediaProjectionActivity : Activity() {
    companion object {
        const val REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a MediaProjectionManager to request permission
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Pass the result data to FloatingIconService for screen capture
            FloatingIconService.mediaProjectionData = data
            FloatingIconService.resultCode = resultCode

            // Start the service once permission is granted
            FloatingIconService.startService(this)
            finish()  // Finish the activity once permission is handled
        } else {
            // Handle permission denial and notify the user
            Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
            finish()  // Finish the activity if permission is denied
        }
    }
}
