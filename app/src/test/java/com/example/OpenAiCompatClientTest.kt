package com.example

import com.example.data.ai.ChatMessage
import com.example.data.ai.OpenAiCompatClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the pure JSON helpers of [OpenAiCompatClient].
 * Runs under Robolectric because org.json is provided by the Android framework.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenAiCompatClientTest {

    // ---------- parseModelsJson ----------

    @Test
    fun `parses standard OpenAI model list`() {
        val json = """
            {
              "object": "list",
              "data": [
                {"id": "gpt-4o-mini", "object": "model", "created": 1698318179, "owned_by": "openai"},
                {"id": "claude-3-5-sonnet", "object": "model", "created": 1700000000, "owned_by": "anthropic"},
                {"id": "gemini-2.0-flash", "object": "model", "created": 1710000000, "owned_by": "google"}
              ]
            }
        """.trimIndent()

        val models = OpenAiCompatClient.parseModelsJson(json)

        assertEquals(3, models.size)
        // sorted alphabetically by id
        assertEquals("claude-3-5-sonnet", models[0].id)
        assertEquals("anthropic", models[0].ownedBy)
        assertEquals("gemini-2.0-flash", models[1].id)
        assertEquals("gpt-4o-mini", models[2].id)
        assertEquals(1698318179L, models[2].created)
    }

    @Test
    fun `parses bare array model list`() {
        val json = """[{"id": "model-a"}, {"id": "model-b"}]"""
        val models = OpenAiCompatClient.parseModelsJson(json)
        assertEquals(listOf("model-a", "model-b"), models.map { it.id })
    }

    @Test
    fun `parses single model object response`() {
        val json = """{"id": "only-model", "owned_by": "router"}"""
        val models = OpenAiCompatClient.parseModelsJson(json)
        assertEquals(1, models.size)
        assertEquals("only-model", models[0].id)
        assertEquals("router", models[0].ownedBy)
    }

    @Test
    fun `deduplicates models by id`() {
        val json = """{"data": [{"id": "dup"}, {"id": "dup"}, {"id": "other"}]}"""
        val models = OpenAiCompatClient.parseModelsJson(json)
        assertEquals(2, models.size)
    }

    @Test
    fun `malformed json yields empty list instead of crashing`() {
        assertTrue(OpenAiCompatClient.parseModelsJson("not json at all").isEmpty())
        assertTrue(OpenAiCompatClient.parseModelsJson("").isEmpty())
        assertTrue(OpenAiCompatClient.parseModelsJson("{\"data\": 42}").isEmpty())
    }

    // ---------- parseChatResponseJson ----------

    @Test
    fun `parses standard chat completion response`() {
        val json = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "سلام! من آماده‌ام."}, "finish_reason": "stop"}
              ],
              "usage": {"prompt_tokens": 10, "completion_tokens": 5}
            }
        """.trimIndent()

        assertEquals("سلام! من آماده‌ام.", OpenAiCompatClient.parseChatResponseJson(json))
    }

    @Test
    fun `falls back to text fields when content is missing`() {
        val viaMessageText = """{"choices": [{"message": {"role": "assistant", "text": "متن جایگزین"}}]}"""
        assertEquals("متن جایگزین", OpenAiCompatClient.parseChatResponseJson(viaMessageText))

        val viaChoiceText = """{"choices": [{"text": "متن قدیمی"}]}"""
        assertEquals("متن قدیمی", OpenAiCompatClient.parseChatResponseJson(viaChoiceText))
    }

    @Test
    fun `returns null for empty or malformed chat responses`() {
        assertNull(OpenAiCompatClient.parseChatResponseJson("""{"choices": []}"""))
        assertNull(OpenAiCompatClient.parseChatResponseJson("""{"choices": [{"message": {"content": ""}}]}"""))
        assertNull(OpenAiCompatClient.parseChatResponseJson("""{"error": "boom"}"""))
        assertNull(OpenAiCompatClient.parseChatResponseJson("###"))
    }

    // ---------- buildChatRequestBody ----------

    @Test
    fun `builds a valid OpenAI chat request body`() {
        val messages = listOf(
            ChatMessage.system("تو دستیار تحلیل مافیا هستی"),
            ChatMessage.user("وضعیت بازی را تحلیل کن \"با دقت\"")
        )

        val body = OpenAiCompatClient.buildChatRequestBody("gpt-4o-mini", messages, 0.7f)
        val root = JSONObject(body)

        assertEquals("gpt-4o-mini", root.getString("model"))
        assertEquals(false, root.getBoolean("stream"))
        assertEquals(0.7, root.getDouble("temperature"), 0.0001)

        val arr: JSONArray = root.getJSONArray("messages")
        assertEquals(2, arr.length())
        assertEquals("system", arr.getJSONObject(0).getString("role"))
        assertEquals("تو دستیار تحلیل مافیا هستی", arr.getJSONObject(0).getString("content"))
        assertEquals("user", arr.getJSONObject(1).getString("role"))
        // quotes inside content must survive JSON escaping
        assertEquals("وضعیت بازی را تحلیل کن \"با دقت\"", arr.getJSONObject(1).getString("content"))
    }

    // ---------- describeHttpError ----------

    @Test
    fun `http errors include persian hints and server message`() {
        val unauthorized = OpenAiCompatClient.describeHttpError(401, """{"error": {"message": "Invalid API key"}}""")
        assertTrue(unauthorized.contains("401"))
        assertTrue(unauthorized.contains("کلید API نامعتبر"))
        assertTrue(unauthorized.contains("Invalid API key"))

        val rateLimited = OpenAiCompatClient.describeHttpError(429, null)
        assertTrue(rateLimited.contains("سقف درخواست"))

        val serverError = OpenAiCompatClient.describeHttpError(503, """{"message": "upstream down"}""")
        assertTrue(serverError.contains("خطای سمت سرور"))
        assertTrue(serverError.contains("upstream down"))
    }

    // ---------- normalizeBaseUrl ----------

    @Test
    fun `base url normalization strips trailing slashes and spaces`() {
        assertEquals(
            "https://9router-production-e6c9.up.railway.app/v1",
            OpenAiCompatClient.normalizeBaseUrl("  https://9router-production-e6c9.up.railway.app/v1/  ")
        )
        assertEquals(
            "https://example.com/v1",
            OpenAiCompatClient.normalizeBaseUrl("https://example.com/v1///")
        )
    }
}
