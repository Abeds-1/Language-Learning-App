package com.example.language_learning_helper.database


class TranslationRepository(private val db: TranslationDatabase) {

    suspend fun insertOrUpdateTranslation(entry: TranslationEntry) {
        val existingCount = db.translationDao().doesTranslationExist(entry.sourceLanguage, entry.targetLanguage, entry.originalText)
        if (existingCount > 0) {
            db.translationDao().updateTranslationTimestamp(entry.sourceLanguage, entry.targetLanguage, entry.originalText, System.currentTimeMillis())
        } else {
            db.translationDao().insertTranslationWithLimit(entry)
        }
    }

    suspend fun toggleSaveTranslation(sourceLanguage: String, targetLanguage: String, word: String) {
        val isSaved = db.translationDao().isTranslationSaved(sourceLanguage, targetLanguage, word) > 0
        db.translationDao().updateTranslationSaveStatus(sourceLanguage, targetLanguage, word, !isSaved)
    }

    suspend fun isTranslationSaved(sourceLanguage: String, targetLanguage: String, word: String): Boolean {
        return db.translationDao().isTranslationSaved(sourceLanguage, targetLanguage, word) > 0
    }

    suspend fun getTranslatedWordsCount(language: String): Int {
        return db.translationDao().getTranslatedWordsCount(language)
    }

    suspend fun getRecentTranslations(language: String): List<TranslationEntry> {
        return db.translationDao().getRecentTranslations(language)
    }

    suspend fun getSavedWords(language: String): List<TranslationEntry> {
        return db.translationDao().getSavedWords(language)
    }

    suspend fun clearHistory(language: String) {
        db.translationDao().clearHistory(language)
    }

    
}
