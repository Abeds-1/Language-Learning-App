package com.example.language_learning_helper

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.activity.OnBackPressedCallback

class ScanningEffectActivity : AppCompatActivity() {

    private lateinit var lottieAnimationView: LottieAnimationView
    private var triggerHomeButton = true
    private var hasSentBroadcast = false
    private var isPaused = false
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper()) // Declare Handler instance

    private var startX = 0f
    private var startY = 0f
    private var endX = 1f
    private var endY = 1f


    private val recognitionCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.RECOGNITION_COMPLETE") {
                //println("hey")
                // Calculate the elapsed time
                val elapsedTime = System.currentTimeMillis() - startTime

                if (elapsedTime >= 1500) {

                    // If 1.5 seconds have already passed, stop the effect immediately
                    stopEffect()

                    // Notify the WORDVIEW that it can render 
                    val broadcastIntent = Intent("com.example.language_learning_helper.CAN_RENDER").apply {}
                    sendBroadcast(broadcastIntent)

                } else {
                    // If less than 1.5 seconds have passed, delay the stop
                    handler.postDelayed({
                        stopEffect()

                        // Notify the WORDVIEW that it can render 
                        val broadcastIntent = Intent("com.example.language_learning_helper.CAN_RENDER").apply {}
                        sendBroadcast(broadcastIntent)

                    }, 1500 - elapsedTime)
                }
            }
            if (intent.action == "com.example.language_learning_helper.PAUSE_SCANNING") {
                hideAnimation()
                pauseAnimation()
                println("hello from pause scanning")
                // Notify the service that rendering is complete
                triggerHomeButton = false
                val broadcastIntent = Intent("com.example.language_learning_helper.TAKE_SCREENSHOT").apply {}
                sendBroadcast(broadcastIntent)
            }
            if (intent.action == "com.example.language_learning_helper.RESUME_SCANNING") {
                println("hello from scanning activity resume scanning")
                triggerHomeButton = true
                showAnimation()
                resumeAnimation()
            }
            if(intent.action == "com.example.language_learning_helper.STOP_SCANNING_BY_FORCE"){
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_effect)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                println("back button pressed in effect")
                val broadcastIntent = Intent("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE").apply {
                }
                sendBroadcast(broadcastIntent)
            }
        })

        // Get rectangle coordinates from the Intent
        startX = intent.getFloatExtra("startX", 0f)
        startY = intent.getFloatExtra("startY", 0f)
        endX = intent.getFloatExtra("endX", 1f)
        endY = intent.getFloatExtra("endY", 1f)
        println("from scanning effect\n startX${startX} startY${startY} endX${endX} endY${endY}")
        // Register the receiver with a comprehensive IntentFilter
        val filter = IntentFilter().apply {
            addAction("com.example.language_learning_helper.RECOGNITION_COMPLETE")
            addAction("com.example.language_learning_helper.PAUSE_SCANNING")
            addAction("com.example.language_learning_helper.RESUME_SCANNING")
            addAction("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE")
        }

        try {
            registerReceiver(recognitionCompleteReceiver, filter)
            println("BroadcastReceiver registered successfully in ScanningEffectActivity")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to register BroadcastReceiver: ${e.message}")
        }


        // Initialize the Lottie animation view
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        lottieAnimationView.setAnimation("scanning_animation.json")
        lottieAnimationView.playAnimation()

        // Record the start time
        startTime = System.currentTimeMillis()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasSentBroadcast = true // Ensure it's sent only once

        val broadcastIntent = Intent("com.example.language_learning_helper.RENDER_COMPLETE").apply {
            putExtra("startX", startX)
            putExtra("startY", startY)
            putExtra("endX", endX)
            putExtra("endY", endY)
        }
        sendBroadcast(broadcastIntent)
    }

    // Show the animation
    fun showAnimation() {
        lottieAnimationView.visibility = View.VISIBLE
        if (!isPaused) {
            lottieAnimationView.playAnimation()
        }
    }

    // Hide the animation
    fun hideAnimation() {
        lottieAnimationView.cancelAnimation()
        lottieAnimationView.visibility = View.GONE
    }

    // Pause the animation
    fun pauseAnimation() {
        lottieAnimationView.pauseAnimation()
        isPaused = true
    }

    // Resume the animation
    fun resumeAnimation() {
        lottieAnimationView.resumeAnimation()
        isPaused = false
    }

    // Stop the Lottie animation
    private fun stopEffect() {
        lottieAnimationView.cancelAnimation()
        println("stop effect is triggered")
        finish() // Close the activity
    }

    override fun onUserLeaveHint(){
        super.onUserLeaveHint()
    }

    override fun onPause(){
        super.onPause()
        if(triggerHomeButton){
            println("home button pressed in effect")
            val broadcastIntent = Intent("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE").apply {
            }
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        println("i am destroyed")
        // Unregister the receiver to avoid memory leaks
        unregisterReceiver(recognitionCompleteReceiver)
        handler.removeCallbacksAndMessages(null) // Clean up any pending callbacks
    }
}
