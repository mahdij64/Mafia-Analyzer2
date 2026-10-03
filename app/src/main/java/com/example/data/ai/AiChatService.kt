package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible chat completion client.
 * Works with 9router, OpenAI, and any /v1/chat/completions endpoint.
 */
data class ChatMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

data class AiModel(
    val id: String,
    val name: String = id
)

sealed class ChatResult {
    data class Success(val content: String) : ChatResult()
    data class Error(val message: String) : ChatResult()
    object Loading : ChatResult()
}

object AiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch available models from the API.
     */
    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String
    ): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()
            val models = mutableListOf<AiModel>()
            for (i in 0 until data.length()) {
                val modelObj = data.getJSONObject(i)
                val id = modelObj.optString("id", "")
                if (id.isNotBlank()) {
                    models.add(AiModel(id = id))
                }
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a chat completion request.
     */
    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): ChatResult = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/chat/completions"

            val messagesArray = JSONArray()
            messages.forEach { msg ->
                val msgObj = JSONObject()
                msgObj.put("role", msg.role)
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }

            val root = JSONObject()
            root.put("model", model)
            root.put("messages", messagesArray)
            root.put("temperature", temperature)
            root.put("max_tokens", maxTokens)

            val requestBody = root.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext ChatResult.Error("HTTP ${response.code}: $body")
            }

            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext ChatResult.Error("No choices in response")
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""

            if (content.isBlank()) {
                ChatResult.Error("Empty response content")
            } else {
                ChatResult.Success(content)
            }
        } catch (e: Exception) {
            ChatResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
