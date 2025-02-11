package com.example.language_learning_helper.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TranslationViewModel(private val repository: TranslationRepository) : ViewModel() {

    private val _translatedCount = MutableStateFlow(0)
    val translatedCount: StateFlow<Int> = _translatedCount

    private val _recentTranslations = MutableStateFlow<List<TranslationEntry>>(emptyList())
    val recentTranslations: StateFlow<List<TranslationEntry>> = _recentTranslations

    private val _saveWords = MutableStateFlow<List<TranslationEntry>>(emptyList())
    val saveWords: StateFlow<List<TranslationEntry>> = _saveWords

    private val _isTranslationSaved = MutableStateFlow(false)
    val isTranslationSaved: StateFlow<Boolean> = _isTranslationSaved
    
    fun loadStats(language: String) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                repository.getTranslatedWordsCount(language)
            }
            _translatedCount.value = count

            val recent = withContext(Dispatchers.IO) {
                repository.getRecentTranslations(language)
            }
            _recentTranslations.value = recent

            val saves = withContext(Dispatchers.IO) {
                repository.getSavedWords(language)
            }
            _saveWords.value = saves
        }
    }


    fun clearHistory(language: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearHistory(language)
            }
            loadStats(language)
        }
    }

    fun addRecentTranslation(entry: TranslationEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertOrUpdateTranslation(entry)
            }
        }
    }

    fun toggleSaveStatus(sourceLanguage: String, targetLanguage: String, word: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.toggleSaveTranslation(sourceLanguage, targetLanguage, word)
                val updatedStatus = repository.isTranslationSaved(sourceLanguage, targetLanguage, word)
                _isTranslationSaved.value = updatedStatus
            }
        }
    }

    fun checkIfTranslationSaved(sourceLanguage: String, targetLanguage: String, word: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.isTranslationSaved(sourceLanguage, targetLanguage, word)
            }
            _isTranslationSaved.value = result
        }
    }
}
