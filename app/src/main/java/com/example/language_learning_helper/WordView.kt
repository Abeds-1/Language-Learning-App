package com.example.language_learning_helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class WordView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val wordBoxes = mutableListOf<WordBox>() // List of WordBox instances


    // Set the list of Word objects, creating a WordBox for each
    fun setWords(words: List<Word>) {
        wordBoxes.clear()
        words.forEach { word ->
            wordBoxes.add(WordBox(word)) // Create a WordBox for each Word
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the word boxes
        wordBoxes.forEach { box ->
            box.draw(canvas) // Delegate drawing to each WordBox
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Iterate over wordBoxes and ensure no box is null
                    wordBoxes.filterNotNull().forEach { box ->
                        if (box.contains(event.x.toInt(), event.y.toInt())) {
                            box.toggleSelection() // Toggle the selection state of the box
                            return true
                        }
                    }
                    println("before invalidate")
                    invalidate() // Redraw the view to reflect changes
                    println("after invalidate")
                }
            }
        return true
    }


    // Get a list of all selected WordBoxes
    fun getSelectedBoxes(): List<WordBox> {
        return wordBoxes.filter { it.isSelected }
    }

}
