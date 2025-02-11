package com.example.language_learning_helper

import android.Manifest
import android.content.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import androidx.appcompat.app.AppCompatActivity
import com.example.language_learning_helper.database.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collectLatest

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var translatedTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var saveButton: ImageButton
    private lateinit var previousInputLang: String
    private lateinit var previousOutputLang: String

    private var translatedText: String = ""
    private var translationInput: String = ""
    private var isLoaded = false

    // Initialize ViewModel with repository
    private val viewModel by lazy {
        val db = TranslationDatabase.getDatabase(requireContext()) // Fixed incorrect AppDatabase reference
        val repository = TranslationRepository(db)
        ViewModelProvider(this, TranslationViewModelFactory(repository))[TranslationViewModel::class.java]
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.language_learning_helper.TRANSLATION_COMPLETE_MAIN_FRAGMENT") {
                translatedText = intent.getStringExtra("translation_output") ?: ""
                isLoaded = true
                loadingAnimation.visibility = View.GONE
                loadingAnimation.cancelAnimation()
                translatedTextView.text = translatedText
                translatedTextView.visibility = View.VISIBLE
            }
        }
    }

   

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        sharedPreferences = requireContext().getSharedPreferences("LanguagePreferences", Context.MODE_PRIVATE)
        translationInput = requireActivity().intent.getStringExtra("TRANSLATION_INPUT") ?: ""

        // Initialize the views after returning the view
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Register the BroadcastReceiver
        val filter = IntentFilter("com.example.language_learning_helper.TRANSLATION_COMPLETE_MAIN_FRAGMENT")
        requireActivity().registerReceiver(broadcastReceiver, filter)
        
        // Initialize views here after the view has been created
        initViews(view)
    }

    private fun initViews(view: View) {
        val translationsTextView: EditText = view.findViewById(R.id.translationsTextView)
        val translateButton: Button = view.findViewById(R.id.translateButton)
        val inputLanguageSpinner: Spinner = view.findViewById(R.id.inputLanguageSpinner)
        val outputLanguageSpinner: Spinner = view.findViewById(R.id.outputLanguageSpinner)
        translatedTextView = view.findViewById(R.id.translatedTextView)
        loadingAnimation = view.findViewById(R.id.loadingAnimation)
        saveButton = view.findViewById(R.id.saveButton)
        saveButton.visibility = View.GONE

        loadingAnimation.setAnimation("three_dots_loading.json")        
        loadingAnimation.visibility = LottieAnimationView.GONE
        translatedTextView.visibility = TextView.VISIBLE

        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        inputLanguageSpinner.adapter = adapter
        outputLanguageSpinner.adapter = adapter

        val defaultInputLanguage = "English"
        val defaultOutputLanguage = "Arabic"
        val inputLanguage = sharedPreferences.getString("input_language", defaultInputLanguage) ?: defaultInputLanguage
        val outputLanguage = sharedPreferences.getString("output_language", defaultOutputLanguage) ?: defaultOutputLanguage

        previousInputLang = inputLanguage
        previousOutputLang = outputLanguage
        inputLanguageSpinner.setSelection(languages.indexOf(inputLanguage))
        outputLanguageSpinner.setSelection(languages.indexOf(outputLanguage))

        translationsTextView.text = Editable.Factory.getInstance().newEditable(translationInput)
        translatedTextView.text = translatedText

        inputLanguageSpinner.onItemSelectedListener = createLanguageListener(inputLanguageSpinner, outputLanguageSpinner, translationsTextView)
        outputLanguageSpinner.onItemSelectedListener = createLanguageListener(inputLanguageSpinner, outputLanguageSpinner, translationsTextView)

        translationsTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                translateButton.visibility = if (s.toString().isNotEmpty()) View.VISIBLE else View.GONE
                if(saveButton.visibility == View.VISIBLE){
                    saveButton.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        translateButton.setOnClickListener {
            val inputLang = inputLanguageSpinner.selectedItem.toString()
            val outputLang = outputLanguageSpinner.selectedItem.toString()
            val inputText = translationsTextView.text.toString()
            triggerTranslation(inputLang, outputLang, inputText)
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

    

    private fun createLanguageListener(
        inputSpinner: Spinner, outputSpinner: Spinner, inputText: EditText
    ): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val adapter = inputSpinner.adapter as ArrayAdapter<String>
                val newInputLang = inputSpinner.selectedItem.toString()
                val newOutputLang = outputSpinner.selectedItem.toString()

                if (parent === inputSpinner) { // Input language changed
                    if (newInputLang == newOutputLang && previousInputLang != null) {
                        outputSpinner.setSelection(adapter.getPosition(previousInputLang!!))
                    }
                    previousInputLang = newInputLang
                } 
                else if (parent === outputSpinner) { // Output language changed
                    if (newOutputLang == newInputLang && previousOutputLang != null) {
                        inputSpinner.setSelection(adapter.getPosition(previousOutputLang!!))
                    }
                    previousOutputLang = newOutputLang
                }

                saveLanguagePreference("input_language", inputSpinner.selectedItem.toString())
                saveLanguagePreference("output_language", outputSpinner.selectedItem.toString())

                triggerTranslation(inputSpinner.selectedItem.toString(), outputSpinner.selectedItem.toString(), inputText.text.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }




    private fun saveLanguagePreference(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun triggerTranslation(inputLang: String, outputLang: String, inputText: String) {
        if (inputText.isBlank()) return
        translatedTextView.visibility = View.GONE
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()
        val input = inputText.split("\n")
        TranslationService.getTranslation(requireContext(), input, inputLang, outputLang) { translated ->
            // Create a TranslationEntry to save the translation in the database
            val translationItem = TranslationEntry(
                sourceLanguage = inputLang,
                targetLanguage = outputLang,
                originalText = inputText,
                translatedText = translated,
                isSaved = false
            )

           
            if(translated != inputText){
                // Check if the translation is already saved
                viewModel.checkIfTranslationSaved(inputLang, outputLang, inputText)
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

            val intent = Intent("com.example.language_learning_helper.TRANSLATION_COMPLETE_MAIN_FRAGMENT").apply {
                putExtra("translation_output", translated)
            }
            requireContext().sendBroadcast(intent)
        }
    }

    private fun updateSaveButton(isSaved: Boolean) {
        if (isSaved) {
            saveButton.setImageResource(R.drawable.save) // Filled yellow star
            saveButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_color))
        } else {
            saveButton.setImageResource(R.drawable.save) // Unfilled star
            saveButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.save))
        }
    }
    

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }
}