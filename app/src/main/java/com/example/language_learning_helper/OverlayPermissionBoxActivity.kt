package com.example.language_learning_helper

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

class OverlayPermissionBoxActivity : AppCompatActivity() {
    private lateinit var servicePermissionManager: ServicePermissionManager
    private lateinit var servicePreferencesHelper: ServiceSharedPreferencesHelper
    private lateinit var btnContinue: Button
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        servicePreferencesHelper = ServiceSharedPreferencesHelper(this)
        servicePermissionManager = ServicePermissionManager(this, servicePreferencesHelper)
        
        // Initialize the views using findViewById
        btnContinue = findViewById(R.id.btnContinue)
        textView = findViewById(R.id.textView)

        // Listener for the Continue button
        btnContinue.setOnClickListener {
            handleOkClick()
        }

        // Set the styled text for the TextView
        val fullText = "Please find Owl Assistant app in the next page and turn on Allow display over other apps"
        val boldText = "Owl Assistant"

        // Convert to SpannableString
        val spannableString = SpannableString(fullText)

        // Apply bold style to "Owl Assistant"
        val startIndex = fullText.indexOf(boldText)
        val endIndex = startIndex + boldText.length
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        textView.text = spannableString

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

    private fun handleOkClick() {
        setResult(RESULT_OK) // Indicate that user pressed "Continue"
        finish()
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
