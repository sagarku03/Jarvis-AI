package com.jarvis.ai.ai

import com.jarvis.ai.service.DeviceActionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(
    private var apiKey: String,
    private val actionsManager: DeviceActionsManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val conversationHistory = JSONArray()

    fun updateApiKey(newKey: String) {
        apiKey = newKey.trim()
    }

    suspend fun sendMessage(userText: String): Pair<String, String?> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Pair("Please configure your Google Gemini API Key in Settings to activate Jarvis.", null)
        }

        // Add user turn to conversation history
        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userText) })
            })
        }
        conversationHistory.put(userTurn)

        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "You are Jarvis, a fast, proactive, intelligent voice assistant living on the user's Android phone. " +
                            "Respond concisely in natural, spoken conversational style in Hindi, Hinglish, or English depending on user input. " +
                            "When the user asks to open an app, call someone, set an alarm, or search the web, ALWAYS invoke the corresponding function call tool.")
                })
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", conversationHistory)
            put("systemInstruction", systemInstruction)
            put("tools", JarvisToolRegistry.getToolsDeclaration())
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        // Using gemini-1.5-flash for lowest latency & reliable function calling
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Pair("Jarvis Network Error: HTTP ${response.code}", null)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Pair("Jarvis did not receive a response from the core.", null)
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")

            var assistantReply = ""
            var toolActionTaken: String? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)

                // Check for regular text
                if (part.has("text")) {
                    assistantReply += part.getString("text")
                }

                // Check for Function Call
                if (part.has("functionCall")) {
                    val functionCall = part.getJSONObject("functionCall")
                    val name = functionCall.getString("name")
                    val args = functionCall.optJSONObject("args") ?: JSONObject()

                    val resultMessage = executeFunction(name, args)
                    toolActionTaken = resultMessage
                    if (assistantReply.isBlank()) {
                        assistantReply = resultMessage
                    }
                }
            }

            // Save assistant turn
            val modelTurn = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", assistantReply) })
                })
            }
            conversationHistory.put(modelTurn)

            return@withContext Pair(assistantReply.trim(), toolActionTaken)

        } catch (e: Exception) {
            return@withContext Pair("Error connecting to Jarvis core: ${e.message}", null)
        }
    }

    private fun executeFunction(name: String, args: JSONObject): String {
        return when (name) {
            "open_app" -> {
                val appName = args.optString("app_name", "YouTube")
                actionsManager.openApp(appName)
            }
            "make_call" -> {
                val phone = args.optString("phone_number", "")
                actionsManager.makeCall(phone)
            }
            "set_alarm" -> {
                val hour = args.optInt("hour", 7)
                val minute = args.optInt("minute", 0)
                val message = args.optString("message", "Jarvis Alarm")
                actionsManager.setAlarm(hour, minute, message)
            }
            "search_web" -> {
                val query = args.optString("query", "")
                actionsManager.searchWeb(query)
            }
            "send_whatsapp" -> {
                val phone = args.optString("phone_number", null)
                val msg = args.optString("message", "")
                actionsManager.sendWhatsApp(phone, msg)
            }
            else -> "Unknown action requested: $name"
        }
    }
}
