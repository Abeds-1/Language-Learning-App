package com.example.language_learning_helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.mlkit.common.model.DownloadConditions
import com.google.android.gms.tasks.Tasks

object TranslationService {

    private const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "MLKitDownloadChannel"

    fun getTranslation(
        context: Context,
        userInput: List<String>,
        sourceLang: String,
        targetLang: String,
        callback: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val translator = try {
                getTranslator(sourceLang, targetLang)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback("Error initializing translator: ${e.localizedMessage}")
                }
                return@launch
            }

            try {
                val conditions = DownloadConditions.Builder().build()
                showDownloadNotification(context)

                Log.d("TranslationService", "Checking if model download is needed...")
                Tasks.await(translator.downloadModelIfNeeded(conditions))

                removeDownloadNotification(context)

                val translationResult = userInput.map { input ->
                    try {
                        Tasks.await(translator.translate(input))
                    } catch (e: Exception) {
                        Log.e("TranslationService", "Translation failed for input: $input")
                        "Failed to translate: ${e.localizedMessage}"
                    }
                }

                val translation = translationResult.joinToString("\n")
                println("$translation")
                withContext(Dispatchers.Main) {
                    callback(translation)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback("Error during translation: ${e.localizedMessage}")
                }
                Log.e("TranslationService", "Translation error: ${e.localizedMessage}")
            } finally {
                translator.close()
                Log.d("TranslationService", "Translator closed.")
            }
        }
    }

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        Log.d("TranslationService", "Initializing translator for $sourceLang to $targetLang")

        val sourceCode = mapToTranslateLanguage(sourceLang)
        val targetCode = mapToTranslateLanguage(targetLang)

        if (sourceCode == null || targetCode == null) {
            throw IllegalArgumentException("Unsupported language code: $sourceLang or $targetLang")
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
            .setTargetLanguage(targetCode)
            .build()

        return Translation.getClient(options)
    }

    private fun showDownloadNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ML Kit Model Download",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading Language Model")
            .setContentText("ML Kit is downloading the model for offline translation.")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun removeDownloadNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun mapToTranslateLanguage(language: String): String? {
        return when (language.toLowerCase()) {
            "afrikaans" -> TranslateLanguage.AFRIKAANS
            "arabic" -> TranslateLanguage.ARABIC
            "belarusian" -> TranslateLanguage.BELARUSIAN
            "bulgarian" -> TranslateLanguage.BULGARIAN
            "bengali" -> TranslateLanguage.BENGALI
            "catalan" -> TranslateLanguage.CATALAN
            "czech" -> TranslateLanguage.CZECH
            "welsh" -> TranslateLanguage.WELSH
            "danish" -> TranslateLanguage.DANISH
            "german" -> TranslateLanguage.GERMAN
            "greek" -> TranslateLanguage.GREEK
            "english" -> TranslateLanguage.ENGLISH
            "esperanto" -> TranslateLanguage.ESPERANTO
            "spanish" -> TranslateLanguage.SPANISH
            "estonian" -> TranslateLanguage.ESTONIAN
            "persian" -> TranslateLanguage.PERSIAN
            "finnish" -> TranslateLanguage.FINNISH
            "french" -> TranslateLanguage.FRENCH
            "irish" -> TranslateLanguage.IRISH
            "galician" -> TranslateLanguage.GALICIAN
            "gujarati" -> TranslateLanguage.GUJARATI
            "hebrew" -> TranslateLanguage.HEBREW
            "hindi" -> TranslateLanguage.HINDI
            "croatian" -> TranslateLanguage.CROATIAN
            "haitian" -> TranslateLanguage.HAITIAN_CREOLE
            "hungarian" -> TranslateLanguage.HUNGARIAN
            "indonesian" -> TranslateLanguage.INDONESIAN
            "icelandic" -> TranslateLanguage.ICELANDIC
            "italian" -> TranslateLanguage.ITALIAN
            "japanese" -> TranslateLanguage.JAPANESE
            "georgian" -> TranslateLanguage.GEORGIAN
            "kannada" -> TranslateLanguage.KANNADA
            "korean" -> TranslateLanguage.KOREAN
            "lithuanian" -> TranslateLanguage.LITHUANIAN
            "latvian" -> TranslateLanguage.LATVIAN
            "macedonian" -> TranslateLanguage.MACEDONIAN
            "marathi" -> TranslateLanguage.MARATHI
            "malay" -> TranslateLanguage.MALAY
            "maltese" -> TranslateLanguage.MALTESE
            "dutch" -> TranslateLanguage.DUTCH
            "norwegian" -> TranslateLanguage.NORWEGIAN
            "polish" -> TranslateLanguage.POLISH
            "portuguese" -> TranslateLanguage.PORTUGUESE
            "romanian" -> TranslateLanguage.ROMANIAN
            "russian" -> TranslateLanguage.RUSSIAN
            "slovak" -> TranslateLanguage.SLOVAK
            "slovenian" -> TranslateLanguage.SLOVENIAN
            "albanian" -> TranslateLanguage.ALBANIAN
            "swedish" -> TranslateLanguage.SWEDISH
            "swahili" -> TranslateLanguage.SWAHILI
            "tamil" -> TranslateLanguage.TAMIL
            "telugu" -> TranslateLanguage.TELUGU
            "thai" -> TranslateLanguage.THAI
            "tagalog" -> TranslateLanguage.TAGALOG
            "turkish" -> TranslateLanguage.TURKISH
            "ukrainian" -> TranslateLanguage.UKRAINIAN
            "urdu" -> TranslateLanguage.URDU
            "vietnamese" -> TranslateLanguage.VIETNAMESE
            "chinese" -> TranslateLanguage.CHINESE
            else -> null
        }
    }
}
