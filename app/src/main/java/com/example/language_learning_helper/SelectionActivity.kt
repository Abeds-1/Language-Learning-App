package com.example.language_learning_helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Point
import android.widget.TextView
import android.content.SharedPreferences
import androidx.activity.OnBackPressedCallback

class SelectionActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedCategory = "Words"
    private lateinit var elementsView: ElementsView
    private var scanningEffectDuration = 1500L
    private var recognitionInProgress = false
    private var triggerHomeButton = true
    private var words = listOf<Word>()
    private var lines = listOf<Line>()
    private var paragraphs = listOf<Paragraph>()
    private val recognitionCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.CAN_RENDER") {
                // Once the CAN_RENDER broadcast is received, initialize the UI and process the bitmap
                renderView()
            }
            if(intent.action == "com.example.language_learning_helper.RESUME_SCANNING"){
                println("hello from selection activity resume scanning")
                if(bitmap != null){
                    processCapturedScreenshot(bitmap)
                }
            }

            if(intent.action == "com.example.language_learning_helper.STOP_SCANNING_BY_FORCE"){
                finish()
            }
        }
    }

    companion object {
        var bitmap: Bitmap? = null
    }

    private var rectStartX = 0f
    private var rectStartY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                println("back button clicked from the selection")
                val broadcastIntent = Intent("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE").apply {
                }
                sendBroadcast(broadcastIntent)
            }
        })

        // Retrieve the passed values for cropping
        val startX = intent.getFloatExtra("startX", 0f)
        val startY = intent.getFloatExtra("startY", 0f)

        val wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getRealSize(size)
    
        val width = size.x
        val height = size.y

        // Calculate StatusBar and NavBar heights explicitly
        val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android").let { resId ->
            if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

        val navBarHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android").let { resId ->
            if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

        rectStartX = startX * width
        rectStartY = startY * (height - statusBarHeight - navBarHeight)


        // Register the receiver with a comprehensive IntentFilter
        val filter = IntentFilter().apply {
            addAction("com.example.language_learning_helper.CAN_RENDER")
            addAction("com.example.language_learning_helper.RESUME_SCANNING")
            addAction("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE")
        }

        try {
            registerReceiver(recognitionCompleteReceiver, filter)
            println("BroadcastReceiver registered successfully in SelectionActivity")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to register BroadcastReceiver: ${e.message}")
        }

        sharedPreferences = getSharedPreferences("LanguagePreferences", Context.MODE_PRIVATE)
        triggerHomeButton = false
    }

    private fun refreshElementsView(){
        if(selectedCategory == "Words" ){
            elementsView.setElements(words as List<TextElement>)
        }         
        if(selectedCategory == "Lines"){
            elementsView.setElements(lines as List<TextElement>)
        }
        if(selectedCategory == "Paragraphs"){
            elementsView.setElements(paragraphs as List<TextElement>)
        }
    }

    // Handle the bitmap and start text recognition
    private fun renderView() {
        triggerHomeButton = true
        try{
        setContentView(R.layout.activity_selection) // Now set the content view only after receiving the broadcast

        // Initialize views and process the bitmap
        elementsView = findViewById(R.id.elementsView)
        val barButton: Button = findViewById(R.id.barButton)
        val tickButton: Button = findViewById(R.id.tickButton) 
        // Initialize views
        val wordsButton: TextView = findViewById(R.id.wordsButton)
        val linesButton: TextView = findViewById(R.id.linesButton)
        val paragraphsButton: TextView = findViewById(R.id.paragraphsButton)

        // Handle button clicks
        val buttons = listOf(wordsButton, linesButton, paragraphsButton)
        buttons.forEach { button ->
            button.setOnClickListener {
                if(button.text.toString() != selectedCategory){
                // Change text color for the selected button
                buttons.forEach { it.setTextColor(getColor(R.color.selection_activity_grey)) } // Reset all to grey
                button.setTextColor(getColor(R.color.white)) // Highlight selected button
                // Update selected category
                selectedCategory = button.text.toString()
                refreshElementsView()
                }     
            }
        }
        
        refreshElementsView() 
        // Set OnClickListener for the bar (✖) button
        barButton.setOnClickListener {
            FloatingIconService.isCapturing = false
            finish() // Close the activity when the button is clicked
        }

        tickButton.setOnClickListener{
            val selectedElements = elementsView.getSelectedElements()
            val translationInput = selectedElements.joinToString(separator = "\n")
            triggerHomeButton = false
            val intent = Intent(this, TranslationWindowActivity::class.java).apply {
                putExtra("TRANSLATION_INPUT", translationInput)
            }
            startActivity(intent)
            FloatingIconService.isCapturing = false
            finish()
            val defaultInputLanguage = "English"
            val defaultOutputLanguage = "Arabic"
            val inputLanguage = sharedPreferences.getString("input_language", defaultInputLanguage) ?: defaultInputLanguage
            val outputLanguage = sharedPreferences.getString("output_language", defaultOutputLanguage) ?: defaultOutputLanguage
            if(translationInput != ""){
                TranslationService.getTranslation(this, selectedElements, inputLanguage, outputLanguage) { translatedText ->
                
                    val translationCompleteIntent = Intent("com.example.language_learning_helper.TRANSLATION_COMPLETE").apply {
                        putExtra("translation_output", translatedText) // Include the translation output
                    }
                    sendBroadcast(translationCompleteIntent)
                    // Start the new activity and pass the translated text
                }
            }
        }

    }catch(e: Exception){
        println("${e.message}")
    }
    }

    private fun processCapturedScreenshot(bitmap: Bitmap?) {
        if (!recognitionInProgress) {
            recognitionInProgress = true
            // Perform text recognition
            if(bitmap != null){
                TextRecognitionUtil.recognizeTextFromImage(bitmap, rectStartX, rectStartY) { words, lines, paragraphs ->
                    runOnUiThread {
                        this.words = words
                        this.lines = lines
                        this.paragraphs = paragraphs
                        recognitionInProgress = false
                        println("hey")
                        // Send a broadcast to notify recognition is done
                        val recognitionCompleteIntent = Intent("com.example.language_learning_helper.RECOGNITION_COMPLETE").apply {}
                        sendBroadcast(recognitionCompleteIntent)
                    }
                }
            }
            }
        }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    override fun onPause(){
        super.onPause()
        if(triggerHomeButton){
            println("home button pressed in selection")
            val broadcastIntent = Intent("com.example.language_learning_helper.STOP_SCANNING_BY_FORCE").apply {
            }
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        println("selection destroy")
        // Unregister the receiver to avoid memory leaks
        unregisterReceiver(recognitionCompleteReceiver)
    }
}
