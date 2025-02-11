package com.example.language_learning_helper.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "translations",
    indices = [Index(value = ["sourceLanguage", "originalText", "targetLanguage"], unique = true)]
)
data class TranslationEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceLanguage: String,
    val targetLanguage: String,
    val originalText: String,
    val translatedText: String,
    var isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) 

