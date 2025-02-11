package com.example.language_learning_helper.database

import androidx.room.*

@Dao
interface TranslationDao {

    // Insert translation and maintain only 10 non-saved recent translations
    @Transaction
    suspend fun insertTranslationWithLimit(entry: TranslationEntry) {
        insertTranslation(entry)
        deleteOldestNonSavedIfExceedsLimit(entry.sourceLanguage)
    }

    // Insert or update a saved word to ensure no duplicates
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTranslation(entry: TranslationEntry)

    // Remove oldest non-saved translations if they exceed 10
    @Query("""
       DELETE FROM translations 
        WHERE id IN (
            SELECT id FROM translations 
            WHERE (sourceLanguage = :sourceLang) 
            AND isSaved = 0 
            ORDER BY timestamp ASC 
            LIMIT -1 OFFSET 10
        )
    """)
    suspend fun deleteOldestNonSavedIfExceedsLimit(sourceLang: String)

    // Get total translated words count for a language
    @Query("SELECT COUNT(*) FROM translations WHERE sourceLanguage = :language")
    suspend fun getTranslatedWordsCount(language: String): Int

    // Get recent translations (limit 10)
    @Query("SELECT * FROM translations WHERE sourceLanguage = :language ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentTranslations(language: String): List<TranslationEntry>

    // Get saved words for a language
    @Query("SELECT * FROM translations WHERE (sourceLanguage = :language) AND isSaved = 1")
    suspend fun getSavedWords(language: String): List<TranslationEntry>

    @Query("UPDATE translations SET timestamp = :newTimestamp WHERE sourceLanguage = :sourceLanguage AND targetLanguage = :targetLanguage AND originalText = :word")
    suspend fun updateTranslationTimestamp(sourceLanguage: String, targetLanguage: String, word: String, newTimestamp: Long)

    @Query("UPDATE translations SET isSaved = :isSaved WHERE sourceLanguage = :sourceLanguage AND targetLanguage = :targetLanguage AND originalText = :word")
    suspend fun updateTranslationSaveStatus(sourceLanguage: String, targetLanguage: String, word: String, isSaved: Boolean)

    @Query("SELECT COUNT(*) FROM translations WHERE sourceLanguage = :sourceLanguage AND targetLanguage = :targetLanguage AND originalText = :word")
    suspend fun doesTranslationExist(sourceLanguage: String, targetLanguage: String, word: String): Int

    @Query("SELECT COUNT(*) FROM translations WHERE sourceLanguage = :sourceLanguage AND targetLanguage = :targetLanguage AND originalText = :word AND isSaved = 1")
    suspend fun isTranslationSaved(sourceLanguage: String, targetLanguage: String, word: String): Int

    // Clear history for a language (excluding saves)
    @Query("DELETE FROM translations WHERE (sourceLanguage = :language)")
    suspend fun clearHistory(language: String)

}
