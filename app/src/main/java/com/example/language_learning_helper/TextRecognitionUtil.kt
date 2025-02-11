package com.example.language_learning_helper

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object TextRecognitionUtil {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeTextFromImage(
        bitmap: Bitmap,
        rectStartX: Float,
        rectStartY: Float,
        onTextExtracted: (List<Word>, List<Line>, List<Paragraph>) -> Unit
    ) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val words = mutableListOf<Word>()
        val lines = mutableListOf<Line>()
        val paragraphs = mutableListOf<Paragraph>()

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {

                    val paragraphCornerPoints = block.cornerPoints?.map{Point(it.x + rectStartX.toInt(), it.y + rectStartY.toInt())}
                    val paragraph = Paragraph(block.text, block.boundingBox, paragraphCornerPoints)
                    paragraphs.add(paragraph)
                    
                    for (line in block.lines) {

                        val lineCornerPoints = line.cornerPoints?.map{Point(it.x + rectStartX.toInt(), it.y + rectStartY.toInt())}
                        val lineObject = Line(line.text, line.boundingBox, lineCornerPoints)
                        lines.add(lineObject)
                        
                        for (element in line.elements) {
                            val cornerPoints = element.cornerPoints?.map { Point(it.x + rectStartX.toInt(), it.y + rectStartY.toInt()) }
                            val word = Word(element.text, element.boundingBox, cornerPoints)
                            words.add(word)
                        }
                    }
                }
                // Callback with the lists of words, lines, and paragraphs
                onTextExtracted(words, lines, paragraphs)
            }
            .addOnFailureListener { e ->
                println("Text Recognition Failed: ${e.message}")
            }
    }
}

open class TextElement(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: List<Point>?
)

data class Word(
    val wordText: String,
    val wordBoundingBox: Rect?,
    val wordCornerPoints: List<Point>?
) : TextElement(wordText, wordBoundingBox, wordCornerPoints)

data class Line(
    val lineText: String,
    val lineBoundingBox: Rect?,
    val lineCornerPoints: List<Point>?
) : TextElement(lineText, lineBoundingBox, lineCornerPoints)

data class Paragraph(
    val paragraphText: String,
    val paragraphBoundingBox: Rect?,
    val paragraphCornerPoints: List<Point>?
) : TextElement(paragraphText, paragraphBoundingBox, paragraphCornerPoints)
