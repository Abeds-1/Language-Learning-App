package com.example.language_learning_helper

import android.graphics.Rect
import android.graphics.Point

data class Word(
    val text: String,
    val boundingBox: Rect?, // Use RectF for float-based bounding box
    val cornerPoints: List<Point>? // New field to store corner points
)

