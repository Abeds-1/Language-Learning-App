package com.example.language_learning_helper

import android.graphics.Bitmap
import android.graphics.Point
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.common.InputImage

object TextRecognitionUtil {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeTextFromImage(bitmap: Bitmap, onWordsExtracted: (List<Word>) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val words = mutableListOf<Word>()
        println("from text recognition")
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val cornerPoints = element.cornerPoints?.map { Point(it.x, it.y) }
                            words.add(Word(element.text, element.boundingBox, cornerPoints))
                        }
                    }
                }
                // Callback with the list of words
                onWordsExtracted(words)
            }
            .addOnFailureListener { e ->
                println("Text Recognition Failed: ${e.message}")
            }
    }
}
