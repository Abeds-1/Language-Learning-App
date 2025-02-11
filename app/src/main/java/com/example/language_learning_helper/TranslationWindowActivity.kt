package com.example.language_learning_helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.language_learning_helper.database.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collectLatest
import androidx.activity.OnBackPressedCallback

class TranslationWindowActivity : AppCompatActivity() {

    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var translatedTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var saveButton: ImageButton
    private var translatedText: String = ""
    private var translationInput: String = ""
    private var isLoaded = false

    // Initialize ViewModel with repository
    private val viewModel by lazy {
        val db = TranslationDatabase.getDatabase(this) // Fixed incorrect AppDatabase reference
        val repository = TranslationRepository(db)
        ViewModelProvider(this, TranslationViewModelFactory(repository))[TranslationViewModel::class.java]
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.TRANSLATION_COMPLETE_WINDOW_ACTIVITY") {
                translatedText = intent.getStringExtra("translation_output") ?: ""
                saveButton.visibility = View.VISIBLE
                isLoaded = true
                loadingAnimation.visibility = LottieAnimationView.GONE
                loadingAnimation.cancelAnimation()
                translatedTextView.text = translatedText
                translatedTextView.visibility = TextView.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation_window)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        saveButton = findViewById(R.id.saveButton)
        saveButton.visibility = View.GONE

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LanguagePreferences", Context.MODE_PRIVATE)

        // Retrieve saved input and output languages, or use defaults
        val defaultInputLanguage = "English"
        val defaultOutputLanguage = "Arabic"
        val inputLanguage = sharedPreferences.getString("input_language", defaultInputLanguage) ?: defaultInputLanguage
        val outputLanguage = sharedPreferences.getString("output_language", defaultOutputLanguage) ?: defaultOutputLanguage

        // Retrieve the passed data
        translationInput = intent.getStringExtra("TRANSLATION_INPUT") ?: ""

        // Register the broadcast receiver
        registerReceiver(broadcastReceiver, IntentFilter("com.example.language_learning_helper.TRANSLATION_COMPLETE_WINDOW_ACTIVITY"))

        // Initialize views
        val translationsTextView: EditText = findViewById(R.id.translationsTextView)
        val translateButton: Button = findViewById(R.id.translateButton)
        val inputLanguageSpinner: Spinner = findViewById(R.id.inputLanguageSpinner)
        val outputLanguageSpinner: Spinner = findViewById(R.id.outputLanguageSpinner)
        translatedTextView = findViewById(R.id.translatedTextView)
        loadingAnimation = findViewById(R.id.loadingAnimation)

        loadingAnimation.setAnimation("three_dots_loading.json")
        if(translationInput != ""){
            loadingAnimation.playAnimation()
            translatedTextView.visibility = TextView.GONE
            loadingAnimation.visibility = LottieAnimationView.VISIBLE
        }else{        
            loadingAnimation.visibility = LottieAnimationView.GONE
            translatedTextView.visibility = TextView.VISIBLE
        }

        translationsTextView.text = Editable.Factory.getInstance().newEditable(translationInput)
        translatedTextView.text = translatedText

        // Populate spinners with language options
        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        inputLanguageSpinner.adapter = adapter
        outputLanguageSpinner.adapter = adapter

        // Set spinner selections based on SharedPreferences
        inputLanguageSpinner.setSelection(languages.indexOf(inputLanguage))
        outputLanguageSpinner.setSelection(languages.indexOf(outputLanguage))

        // Handle spinner selections
        inputLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedInputLanguage = parent.getItemAtPosition(position).toString()
                saveLanguagePreference("input_language", selectedInputLanguage)
                triggerTranslation(selectedInputLanguage, outputLanguageSpinner.selectedItem.toString(), translationsTextView.text.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        outputLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedOutputLanguage = parent.getItemAtPosition(position).toString()
                saveLanguagePreference("output_language", selectedOutputLanguage)
                triggerTranslation(inputLanguageSpinner.selectedItem.toString(), selectedOutputLanguage, translationsTextView.text.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Watch EditText for changes
        translationsTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                translateButton.visibility = if (s.toString().isNotEmpty()) Button.VISIBLE else Button.GONE
                if(saveButton.visibility == View.VISIBLE){
                    saveButton.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Translate button click listener
        translateButton.setOnClickListener {
            val inputLanguage = inputLanguageSpinner.selectedItem.toString()
            val outputLanguage = outputLanguageSpinner.selectedItem.toString()
            val inputText = translationsTextView.text.toString()

            triggerTranslation(inputLanguage, outputLanguage, inputText)
        }

        // Set up touch listener on overlay to close activity when tapped outside
        val overlayLayout: FrameLayout = findViewById(R.id.overlayLayout) // Transparent overlay
        overlayLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val contentView = findViewById<LinearLayout>(R.id.contentView)
                if (!isPointInsideView(contentView, event.rawX, event.rawY)) {
                    finish()
                    return@setOnTouchListener true
                }
            }
            false
        }

        saveButton.setOnClickListener {
            val text = translationsTextView.text.toString()
            val inputLang = inputLanguageSpinner.selectedItem.toString()
            val outputLang = outputLanguageSpinner.selectedItem.toString()
            viewModel.toggleSaveStatus(inputLang, outputLang, text)
            viewModel.checkIfTranslationSaved(inputLang, outputLang, text)
            lifecycleScope.launch {
                viewModel.isTranslationSaved.collectLatest { isSaved ->
                    updateSaveButton(isSaved)
                }
            }
        }
    }

    private fun saveLanguagePreference(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun isPointInsideView(view: View, x: Float, y: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        return x >= left && x <= right && y >= top && y <= bottom
    }

    private fun triggerTranslation(inputLanguage: String, outputLanguage: String, inputText: String) {
        if (inputText.isBlank()) return

        translatedTextView.visibility = TextView.GONE
        loadingAnimation.visibility = LottieAnimationView.VISIBLE
        loadingAnimation.playAnimation()

        val input = inputText.split("\n").filter { it.isNotBlank() }
        TranslationService.getTranslation(this, input, inputLanguage, outputLanguage) { translatedText ->

            // Create a TranslationEntry to save the translation in the database
            val translationItem = TranslationEntry(
                sourceLanguage = inputLanguage,
                targetLanguage = outputLanguage,
                originalText = inputText,
                translatedText = translatedText,
                isSaved = false
            )

            if(translatedText != inputText){
                // Check if the translation is already saved
                viewModel.checkIfTranslationSaved(inputLanguage, outputLanguage, inputText)
                lifecycleScope.launch {
                    viewModel.isTranslationSaved.collectLatest { isSaved ->
                        updateSaveButton(isSaved)
                    }
                }

                // Show the favorite button
                saveButton.visibility = View.VISIBLE
    
                // Add to recent translations
                viewModel.addRecentTranslation(translationItem)
            }

            val translationCompleteIntent = Intent("com.example.language_learning_helper.TRANSLATION_COMPLETE_WINDOW_ACTIVITY").apply {
                putExtra("translation_output", translatedText)
            }
            sendBroadcast(translationCompleteIntent)
        }
    }

    private fun updateSaveButton(isSaved: Boolean) {
        if (isSaved) {
            saveButton.setImageResource(R.drawable.save) // Filled yellow star
            saveButton.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
        } else {
            saveButton.setImageResource(R.drawable.save) // Unfilled star
            saveButton.setColorFilter(ContextCompat.getColor(this, R.color.save))
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        FloatingIconService.isCapturing = false
    }
}
