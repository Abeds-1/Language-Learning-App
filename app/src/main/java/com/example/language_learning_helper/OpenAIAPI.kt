package com.example.language_learning_helper

import retrofit2.http.Body
import retrofit2.http.POST

// Request body data class
data class OpenAIRequest(
    val messages: List<Map<String, String>>,
    val model: String = "gpt-4o",
    val temperature: Double = 1.0,
    val max_tokens: Int = 4096,
    val top_p: Double = 1.0
)

// Response body data class
data class OpenAIResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class Message(
    val role: String,
    val content: String
)

// Retrofit interface
interface OpenAIApi {
    @POST("chat/completions")
    suspend fun getCompletion(@Body request: OpenAIRequest): OpenAIResponse
}

