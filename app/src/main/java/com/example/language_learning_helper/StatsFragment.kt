package com.example.language_learning_helper

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.language_learning_helper.database.*
import androidx.recyclerview.widget.DividerItemDecoration
import android.content.Context
import android.content.SharedPreferences

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private lateinit var spinnerLanguages: Spinner
    private lateinit var tvTranslatedCount: TextView
    private lateinit var recyclerRecent: RecyclerView
    private lateinit var recyclerSaves: RecyclerView
    private lateinit var btnClearHistory: Button
    private lateinit var recentAdapter: TranslationAdapter
    private lateinit var saveAdapter: SaveAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var languages: List<String>

    // Initialize ViewModel with repository
    private val viewModel by lazy {
        val db = TranslationDatabase.getDatabase(requireContext()) // Fixed incorrect AppDatabase reference
        val repository = TranslationRepository(db)
        ViewModelProvider(this, TranslationViewModelFactory(repository))[TranslationViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences("LanguagePreferences", Context.MODE_PRIVATE)

        
        val defaultInputLanguage = "English"
        val inputLanguage = sharedPreferences.getString("input_language", defaultInputLanguage) ?: defaultInputLanguage
        
        // Initialize UI components
        spinnerLanguages = view.findViewById(R.id.spinner_languages)
        tvTranslatedCount = view.findViewById(R.id.tv_translated_count)
        recyclerRecent = view.findViewById(R.id.recycler_recent)
        recyclerSaves = view.findViewById(R.id.recycler_saves)
        btnClearHistory = view.findViewById(R.id.btn_clear_history)

        // Set up RecyclerViews
        recentAdapter = TranslationAdapter(emptyList(), viewModel, ::loadStats)
        recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecent.adapter = recentAdapter
        recyclerRecent.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        saveAdapter = SaveAdapter(emptyList(), viewModel, ::loadStats)
        recyclerSaves.layoutManager = LinearLayoutManager(requireContext())
        recyclerSaves.adapter = saveAdapter
        recyclerSaves.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Populate spinner with language options
        languages = resources.getStringArray(R.array.languages).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguages.adapter = adapter

        spinnerLanguages.setSelection(languages.indexOf(inputLanguage))
        // Load data when language changes
        spinnerLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.loadStats(parent.getItemAtPosition(position).toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val defaultInputLanguage = "English"
            val inputLanguage = sharedPreferences.getString("input_language", defaultInputLanguage) ?: defaultInputLanguage
            spinnerLanguages.setSelection(languages.indexOf(inputLanguage))
                // Fragment is visible
                // Fragment is visible
            viewModel.loadStats(spinnerLanguages.getSelectedItem().toString())

            // Observe data changes
            lifecycleScope.launch {
                viewModel.translatedCount.collectLatest { count ->
                    tvTranslatedCount.text = "Translated Words: $count"
                }
            }

            lifecycleScope.launch {
                viewModel.recentTranslations.collectLatest { translations ->
                    recentAdapter.updateData(translations) // Changed from submitList()
                }
            }

            lifecycleScope.launch {
                viewModel.saveWords.collectLatest { saves ->
                    saveAdapter.updateData(saves) // Changed from submitList()
                }
            }

            // Clear history button
            btnClearHistory.setOnClickListener {
                viewModel.clearHistory(spinnerLanguages.selectedItem.toString())
            }
        } 
    }

    private fun loadStats(){
        viewModel.loadStats(spinnerLanguages.getSelectedItem().toString())

        // Observe data changes
        lifecycleScope.launch {
            viewModel.translatedCount.collectLatest { count ->
                tvTranslatedCount.text = "Translated Words: $count"
            }
        }

        lifecycleScope.launch {
            viewModel.recentTranslations.collectLatest { translations ->
                recentAdapter.updateData(translations) // Changed from submitList()
            }
        }

        lifecycleScope.launch {
            viewModel.saveWords.collectLatest { saves ->
                saveAdapter.updateData(saves) // Changed from submitList()
            }
        }

        // Clear history button
        btnClearHistory.setOnClickListener {
            viewModel.clearHistory(spinnerLanguages.selectedItem.toString())
        }
    }
}
