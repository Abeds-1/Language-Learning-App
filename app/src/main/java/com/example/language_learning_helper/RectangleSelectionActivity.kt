package com.example.language_learning_helper

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback

class RectangleSelectionActivity : AppCompatActivity() {
    private lateinit var overlayView: View
    private lateinit var instructionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rectangle_selection)

        overlayView = findViewById(R.id.overlay)
        instructionText = findViewById(R.id.instructionText)  // Make sure to add a TextView with this ID in your XML.

        // Hide overlay and instruction when dragging starts
        val rectangleSelectionView: RectangleSelectionView = findViewById(R.id.rectangleSelectionView)
        rectangleSelectionView.setOnStartDraggingListener {
            overlayView.visibility = View.GONE
            instructionText.visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                FloatingIconService.isCapturing = false
                finish()
            }
        })
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        FloatingIconService.isCapturing = false
        finish()
    }

    // Called if the rectangle is too small
    fun restoreOverlay() {
        overlayView.visibility = View.VISIBLE
        instructionText.visibility = View.VISIBLE
    }
}
