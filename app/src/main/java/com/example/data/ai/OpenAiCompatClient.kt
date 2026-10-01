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
 * A model offered by an OpenAI-compatible router (e.g. 9router).
 */
data class AiModelInfo(
    val id: String,
    val ownedBy: String = "",
    val created: Long = 0L
)

/**
 * A single chat message in OpenAI format.
 */
data class ChatMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String
) {
    companion object {
        fun system(content: String) = ChatMessage("system", content)
        fun user(content: String) = ChatMessage("user", content)
        fun assistant(content: String) = ChatMessage("assistant", content)
    }
}

/**
 * Human-readable AI service failure (messages are already localized to Persian
 * so the UI can show them directly).
 */
class AiServiceException(message: String) : Exception(message)

/**
 * Minimal OpenAI-compatible HTTP client used to talk to router services like 9router.
 *
 * Endpoints consumed (relative to the configured base URL, e.g. https://…/v1):
 *  - GET  /models           → {"object":"list","data":[{"id":"gpt-4o-mini", …}, …]}
 *  - POST /chat/completions → {"choices":[{"message":{"role":"assistant","content":"…"}}], …}
 *
 * All JSON parsing lives in pure functions ([parseModelsJson], [parseChatResponseJson],
 * [buildChatRequestBody], [describeHttpError]) so they can be unit-tested without a network.
 */
object OpenAiCompatClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Trims whitespace and any trailing slashes so "{base}/models" never becomes "//models". */
    fun normalizeBaseUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/')

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<AiModelInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "${normalizeBaseUrl(baseUrl)}/models"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer ${apiKey.trim()}")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        throw AiServiceException(describeHttpError(response.code, body))
                    }
                    if (body.isNullOrBlank()) {
                        throw AiServiceException("پاسخ خالی از سرویس دریافت شد")
                    }
                    Result.success(parseModelsJson(body))
                }
            } catch (e: AiServiceException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(
                    AiServiceException("عدم دسترسی به سرویس: ${e.localizedMessage ?: "خطای ناشناخته"}")
                )
            }
        }

    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        temperature: Float = 0.4f
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${normalizeBaseUrl(baseUrl)}/chat/completions"
            val requestBody = buildChatRequestBody(model, messages, temperature)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    throw AiServiceException(describeHttpError(response.code, body))
                }
                if (body.isNullOrBlank()) {
                    throw AiServiceException("پاسخ خالی از سرویس دریافت شد")
                }
                val text = parseChatResponseJson(body)
                    ?: throw AiServiceException("پاسخ سرویس قابل تجزیه نبود (فیلد متن پاسخ یافت نشد)")
                Result.success(text)
            }
        } catch (e: AiServiceException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                AiServiceException("عدم دسترسی به سرویس: ${e.localizedMessage ?: "خطای ناشناخته"}")
            )
        }
    }

    // ------------------------------------------------------------------
    // Pure helpers (unit-testable)
    // ------------------------------------------------------------------

    /**
     * Parses an OpenAI-style model list. Tolerates:
     *  - {"object":"list","data":[…]}   (standard)
     *  - [ … ]                          (bare array, used by some gateways)
     *  - {"id":"…"}                     (single model object)
     */
    fun parseModelsJson(json: String): List<AiModelInfo> {
        val models = mutableListOf<AiModelInfo>()
        try {
            val trimmed = json.trim()
            val dataArray: JSONArray? = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("data")
                        ?: root.optJSONArray("models")
                        ?: if (root.optString("id").isNotBlank()) {
                            JSONArray().apply { put(root) }
                        } else {
                            null
                        }
                }
                else -> null
            }
            dataArray?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").ifBlank { obj.optString("name") }
                    if (id.isNotBlank()) {
                        models.add(
                            AiModelInfo(
                                id = id,
                                ownedBy = obj.optString("owned_by"),
                                created = obj.optLong("created")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Malformed payload → return whatever was collected (possibly nothing).
        }
        return models.distinctBy { it.id }.sortedBy { it.id.lowercase() }
    }

    /**
     * Extracts the assistant text from a /chat/completions response.
     * Returns null when no usable content field exists.
     */
    fun parseChatResponseJson(json: String): String? {
        return try {
            val root = JSONObject(json)
            val firstChoice = root.optJSONArray("choices")?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            message?.optString("content")?.takeIf { it.isNotBlank() }
                ?: message?.optString("text")?.takeIf { it.isNotBlank() }
                ?: firstChoice?.optString("text")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** Builds the JSON request body for a /chat/completions call. */
    fun buildChatRequestBody(model: String, messages: List<ChatMessage>, temperature: Float): String {
        val root = JSONObject()
        root.put("model", model)
        val arr = JSONArray()
        messages.forEach { m ->
            val o = JSONObject()
            o.put("role", m.role)
            o.put("content", m.content)
            arr.put(o)
        }
        root.put("messages", arr)
        root.put("temperature", temperature.toDouble())
        root.put("stream", false)
        return root.toString()
    }

    /** Turns an HTTP error into a readable Persian message, using the server's error body if present. */
    fun describeHttpError(code: Int, body: String?): String {
        val serverMsg = body?.takeIf { it.isNotBlank() }?.let { b ->
            try {
                val root = JSONObject(b)
                root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: root.optString("message").takeIf { it.isNotBlank() }
                    ?: root.optString("detail").takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
        val hint = when (code) {
            401, 403 -> "کلید API نامعتبر یا فاقد مجوز است"
            404 -> "آدرس سرویس یا مدل انتخابی یافت نشد"
            429 -> "سقف درخواست‌های سرویس پر شده است"
            in 500..599 -> "خطای سمت سرورِ سرویس"
            else -> null
        }
        return buildString {
            append("خطای سرویس (کد $code)")
            if (hint != null) append(": $hint")
            if (serverMsg != null) append(" — $serverMsg")
        }
    }
}
