package com.example.language_learning_helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ElementsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val elementsBoxes = mutableListOf<ElementBox>() // List of ElementBox instances
    

    private var xDown = 0f
    private var yDown = 0f
    private var isDragging = false
    private var longTapThreshold = 300L // Threshold for detecting a long tap (milliseconds)
    private var downTime = 0L // Time when the touch started

    // Set the list of Element objects, creating a ElementBox for each
    fun setElements(elements: List<TextElement>) {
        elementsBoxes.clear()
        elements.forEach { element ->
            elementsBoxes.add(ElementBox(element)) // Create a ElementBox for each Element
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the element boxes
        elementsBoxes.forEach { box ->
            box.draw(canvas) // Delegate drawing to each ElementBox
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                xDown = event.x
                yDown = event.y
                downTime = System.currentTimeMillis()
                isDragging = false
                // Start a delayed runnable to check if it's a long tap
            }
            MotionEvent.ACTION_MOVE -> {
                checkForLongTap()
                if (isDragging) {
                    // Handle the drag and select boxes during drag
                    handleTapOrDrag(event.x, event.y, isDrag = true)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    handleTapOrDrag(event.x, event.y, isDrag = false) // Handle tap action
                }
            }
        }
        return true
    }

    private fun checkForLongTap() {
        // If the user has held their finger for a longer period, start dragging
        val currentTime = System.currentTimeMillis()
        if (currentTime - downTime >= longTapThreshold) {
            isDragging = true
        }
    }

    private fun handleTapOrDrag(x: Float, y: Float, isDrag: Boolean) {
        // Find the box under the touch point
        val touchedBox = elementsBoxes.find { it.contains(x.toInt(), y.toInt()) }

        if (touchedBox != null) {
            if (isDrag) {
                // During drag, only select boxes that are not already selected
                if (!touchedBox.isSelected) {
                    touchedBox.isSelected = true // Select the box
                    invalidate() // Redraw the view
                }
            } else {
                // On tap, toggle the selection state
                touchedBox.isSelected = !touchedBox.isSelected
                invalidate() // Redraw the view
            }
        }
    }

    // Get a list of all selected ElementsBoxes
    fun getSelectedElements(): List<String> {
        return elementsBoxes.filter { it.isSelected }.map { it.element.text }
    }
    
}
