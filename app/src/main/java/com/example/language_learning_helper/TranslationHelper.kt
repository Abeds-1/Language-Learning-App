package com.example.language_learning_helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun getTranslation(userInput: String, callback: (String) -> Unit) {
    val request = OpenAIRequest(
        messages = listOf(
            mapOf("role" to "system", "content" to ""),
            mapOf("role" to "user", "content" to userInput)
        )
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.getCompletion(request)
            val translation = response.choices[0].message.content
            withContext(Dispatchers.Main) {
                callback(translation)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback("Error: ${e.message}")
            }
        }
    }
}
