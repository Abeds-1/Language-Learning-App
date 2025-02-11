package com.example.language_learning_helper

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.LinearLayout

class GreetingActivity : AppCompatActivity() {
    private lateinit var servicePermissionManager: ServicePermissionManager
    private lateinit var servicePreferencesHelper: ServiceSharedPreferencesHelper
    private lateinit var btnYes: Button
    private lateinit var btnNo: Button
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        servicePreferencesHelper = ServiceSharedPreferencesHelper(this)
        servicePermissionManager = ServicePermissionManager(this, servicePreferencesHelper)

        // Initialize the views using findViewById
        btnYes = findViewById(R.id.btnYes)
        btnNo = findViewById(R.id.btnNo)
        textView = findViewById(R.id.textView)

        // Listener for the Yes button
        btnYes.setOnClickListener {
            handleYesClick()
        }

        // Listener for the No button
        btnNo.setOnClickListener {
            handleNoClick()
        }

        // Set a touch listener on the root layout
        val rootLayout = findViewById<FrameLayout>(R.id.rootLayout)
        rootLayout.setOnTouchListener { v, event ->
            // If the touch event is outside the box, close the activity
            if (event.action == MotionEvent.ACTION_DOWN) {
                val boxView = findViewById<LinearLayout>(R.id.permissionBox)
                if (!isPointInsideView(event.rawX, event.rawY, boxView)) {
                    finish()
                }
            }
            true
        }
    }

    private fun handleYesClick() {
        lifecycleScope.launch {
            // Hide the current layout and set the background transparent
            window.setBackgroundDrawableResource(android.R.color.transparent)
            val rootLayout = findViewById<FrameLayout>(R.id.rootLayout)
            rootLayout.visibility = View.GONE

            val isPermissible = servicePermissionManager.setupPermissions()
            println("Permission granted: $isPermissible")
            finish()
        }
    }

    private fun handleNoClick() {
        finish() // This closes the activity when No is clicked
    }

    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        return x >= left && x <= right && y >= top && y <= bottom
    }
}
