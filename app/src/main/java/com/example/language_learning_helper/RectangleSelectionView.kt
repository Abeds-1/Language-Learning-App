package com.example.language_learning_helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs
import android.content.Intent
import android.app.Activity
import android.view.WindowManager
import android.graphics.Point

class RectangleSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var isDragging = false
    private val paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val MIN_RECT_WIDTH = 100f
    private val MIN_RECT_HEIGHT = 100f

    private var startDraggingListener: (() -> Unit)? = null

    // You can set this listener from the activity
    fun setOnStartDraggingListener(listener: () -> Unit) {
        startDraggingListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isDragging = true
                startDraggingListener?.invoke()  // Notify that dragging has started
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    endX = clamp(event.x, 0f, width.toFloat())
                    endY = clamp(event.y, 0f, height.toFloat())
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                endX = clamp(event.x, 0f, width.toFloat())
                endY = clamp(event.y, 0f, height.toFloat())
                invalidate()

                val rectWidth = abs(endX - startX)
                val rectHeight = abs(endY - startY)

                if (rectWidth >= MIN_RECT_WIDTH && rectHeight >= MIN_RECT_HEIGHT) {
                    val rectCoordinates = RectF(startX, startY, endX, endY)
                    println("$rectCoordinates")
                    if (startX > endX) {
                        val temp = startX
                        startX = endX
                        endX = temp
                    }
                    if (startY > endY) {
                        val temp = startY
                        startY = endY
                        endY = temp
                    }
                    val scanningIntent = Intent(context, ScanningEffectActivity::class.java).apply {
                        putExtra("startX", startX / width)
                        putExtra("startY", startY / height)
                        putExtra("endX", endX / width)
                        putExtra("endY", endY / height)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(scanningIntent)
                    (context as? Activity)?.finish()
                } else {
                    // Clear the rectangle if it's too small
                    startX = 0f
                    startY = 0f
                    endX = 0f
                    endY = 0f
                    invalidate()
                    (context as? RectangleSelectionActivity)?.restoreOverlay()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDragging || (startX != endX && startY != endY)) {
            canvas.drawRect(startX, startY, endX, endY, paint)
        }
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}

