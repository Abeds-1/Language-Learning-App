package com.example.language_learning_helper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point

class ElementBox(
    internal val element: TextElement, // The text element object containing the text and corner points
    var isSelected: Boolean = false,
    private val unselectedColor: Int = 0xFFB0BEC5.toInt(), // Light Gray
    private val selectedColor: Int = 0xFF0056D2.toInt(),   // Bright Blue
    
    private val strokeWidth: Float = 2f
) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = this@ElementBox.strokeWidth
    }

    fun draw(canvas: Canvas) {
        val cornerPoints = element.cornerPoints
    
        if (!cornerPoints.isNullOrEmpty()) {
            paint.color = if (isSelected) selectedColor else unselectedColor
    
            // Find the min and max x and y coordinates to determine the bounding box
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
    
            // Find the min/max values for x and y
            for (point in cornerPoints) {
                minX = minOf(minX, point.x.toFloat())
                minY = minOf(minY, point.y.toFloat()) 
                maxX = maxOf(maxX, point.x.toFloat()) 
                maxY = maxOf(maxY, point.y.toFloat())  
            }
            
            
            // Add padding to the bounding box
            val padding = 2f // Adjust the padding value as needed
            minX -= padding
            minY -= padding
            maxX += padding
            maxY += padding
    
            // Create a path for the expanded rectangle
            val path = Path()
            path.moveTo(minX, minY)
            path.lineTo(maxX, minY) // Top-right corner
            path.lineTo(maxX, maxY) // Bottom-right corner
            path.lineTo(minX, maxY) // Bottom-left corner
            path.close() // Close the path back to the top-left corner
    
            // Draw the path (bounding box)
            canvas.drawPath(path, paint)
        }
    }
    

    // Check if the touch is within this bounding box
    fun contains(x: Int, y: Int): Boolean {
        val cornerPoints = element.cornerPoints ?: return false
        return x in cornerPoints.minOf { it.x }..cornerPoints.maxOf { it.x } &&
               y in cornerPoints.minOf { it.y }..cornerPoints.maxOf { it.y }
    }
    

    // Toggle the selection state of the box
    fun toggleSelection() {
        isSelected = !isSelected
    }

    // Get the bounding box's corner points
    fun getCornerPoints(): List<Point>? {
        return element.cornerPoints
    }
}
