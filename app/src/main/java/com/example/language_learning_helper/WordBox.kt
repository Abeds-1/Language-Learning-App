package com.example.language_learning_helper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point

class WordBox(
    private val word: Word, // The Word object containing the text and corner points
    var isSelected: Boolean = false,
    private val unselectedColor: Int = 0xFFFF0000.toInt(), // Red
    private val selectedColor: Int = 0xFF00FF00.toInt(),   // Green
    private val strokeWidth: Float = 5f
) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = this@WordBox.strokeWidth
    }

    // Draw the bounding box using corner points
    fun draw(canvas: Canvas) {
        val cornerPoints = word.cornerPoints

        if (!cornerPoints.isNullOrEmpty()) {
            paint.color = if (isSelected) selectedColor else unselectedColor

            val path = Path()

            // Start from the first corner point
            path.moveTo(cornerPoints[0].x.toFloat(), cornerPoints[0].y.toFloat())

            // Draw lines connecting all corner points
            for (i in 1 until cornerPoints.size) {
                path.lineTo(cornerPoints[i].x.toFloat(), cornerPoints[i].y.toFloat())
            }

            // Close the path (back to the first point)
            path.close()

            // Draw the path
            canvas.drawPath(path, paint)
        }
    }

    // Check if the touch is within this bounding box
    fun contains(x: Int, y: Int): Boolean {
        val cornerPoints = word.cornerPoints ?: return false
        return x in cornerPoints.minOf { it.x }..cornerPoints.maxOf { it.x } &&
               y in cornerPoints.minOf { it.y }..cornerPoints.maxOf { it.y }
    }
    

    // Toggle the selection state of the box
    fun toggleSelection() {
        isSelected = !isSelected
    }

    // Get the bounding box's corner points
    fun getCornerPoints(): List<Point>? {
        return word.cornerPoints
    }
}
