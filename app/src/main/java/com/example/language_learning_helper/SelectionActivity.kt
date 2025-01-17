package com.example.language_learning_helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SelectionActivity : AppCompatActivity() {

    private lateinit var wordView: WordView
    private var scanningEffectDuration = 1500L
    private var recognitionInProgress = false
    private var words = listOf<Word>()
    private val recognitionCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.CAN_RENDER") {
                // Once the CAN_RENDER broadcast is received, initialize the UI and process the bitmap
                renderView()
            }
            if(intent.action == "com.example.language_learning_helper.RESUME_SCANNING"){
                println("hello from selection activity resume scanning")
                if(bitmap != null){
                    processCapturedScreenshot(bitmap)
                }
            }
        }
    }

    companion object {
        var bitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the receiver with a comprehensive IntentFilter
        val filter = IntentFilter().apply {
            addAction("com.example.language_learning_helper.CAN_RENDER")
            addAction("com.example.language_learning_helper.RESUME_SCANNING")
        }

        try {
            registerReceiver(recognitionCompleteReceiver, filter)
            println("BroadcastReceiver registered successfully in SelectionActivity")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to register BroadcastReceiver: ${e.message}")
        }
    }

    // Handle the bitmap and start text recognition
    private fun renderView() {
        setContentView(R.layout.activity_selection) // Now set the content view only after receiving the broadcast

        // Initialize views and process the bitmap
        wordView = findViewById(R.id.wordView)
        val barButton: Button = findViewById(R.id.barButton)

        wordView.setWords(words) // Update the WordView with recognized words
        // Set OnClickListener for the bar (✖) button
        barButton.setOnClickListener {
            finish() // Close the activity when the button is clicked
        }

        processCapturedScreenshot(bitmap)
    }

    private fun processCapturedScreenshot(bitmap: Bitmap?) {
        if (!recognitionInProgress) {
            recognitionInProgress = true

            // Perform text recognition
            if (bitmap != null) {
                TextRecognitionUtil.recognizeTextFromImage(bitmap) { words ->
                    runOnUiThread {
                        this.words = words
                        recognitionInProgress = false
                        // Send a broadcast to notify recognition is done
                        val recognitionCompleteIntent = Intent("com.example.language_learning_helper.RECOGNITION_COMPLETE").apply {
                        }
                        sendBroadcast(recognitionCompleteIntent)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to avoid memory leaks
        try {
            unregisterReceiver(recognitionCompleteReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to unregister BroadcastReceiver: ${e.message}")
        }
    }
}
