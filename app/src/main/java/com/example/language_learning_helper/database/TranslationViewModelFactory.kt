package com.example.language_learning_helper.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TranslationViewModelFactory(private val repository: TranslationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranslationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TranslationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
