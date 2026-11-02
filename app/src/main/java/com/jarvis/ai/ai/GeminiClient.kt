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

        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userText) })
            })
        }
        conversationHistory.put(userTurn)

        val systemPromptText = """
            You are Jarvis, a persistent personal voice assistant living on the user's Android phone - not a generic one-off chatbot. Keep replies short and conversational, like a real spoken conversation, not a written essay.
            
            TOOL USAGE RULES:
            - You can open apps with open_app when the user asks you to.
            - You can call a contact with call_contact, and prefill a message to a contact (SMS or WhatsApp) with message_contact.
            - For the flashlight use device_control (it switches directly). For Wi-Fi and Bluetooth use device_control.
            - To find a song use search_youtube or search_spotify.
            - To pause, resume or skip what is playing use media_control.
            - You can build a complete animated website with build_website when the user asks for one (for example 'ek cafe ki website banao'). It opens The Forge 3D build screen.
            - You can turn a photo into an animated pencil sketch with start_sketch_drawing when the user asks to make a drawing or sketch.
            - You CAN read the user's contacts with search_contacts: use it when they ask who is in their contacts, for names, or how many contacts they have.
            - You do not have a clock of your own: whenever the user asks for the time, today's date, or day, call get_current_datetime.
            - Use set_alarm and set_timer for alarms and timers.
            - set_volume changes the sound volume. navigate_to opens Google Maps with directions. web_search opens the browser. phone_status answers battery, charging, volume. open_camera opens the camera.
            
            SPEED & SPEECH:
            - Spoken ONCE per request: when the user asks you to DO something, say ONE very short acknowledgement of two to three words (e.g. 'Sure thing', 'Right away', 'On it'), then call the tool in that same turn.
            - Stay silent until the whole task is finished, then say ONE short sentence telling the user the outcome.
            - Questions that need no tool: answer right away, short and natural in Hindi, Hinglish, or English depending on user preference.
            - Boundaries: never claim to be a real human or to have a physical body. Never produce explicit content.
        """.trimIndent()

        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", systemPromptText) })
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", conversationHistory)
            put("systemInstruction", systemInstruction)
            put("tools", JarvisToolRegistry.getToolsDeclaration())
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

        // Using gemini-1.5-flash for fastest latency & tool calling accuracy
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

                if (part.has("text")) {
                    assistantReply += part.getString("text")
                }

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
                val appName = args.optString("app_name", "")
                actionsManager.openApp(appName)
            }
            "call_contact" -> {
                val contact = args.optString("contact", "")
                actionsManager.callContact(contact)
            }
            "message_contact" -> {
                val contact = args.optString("contact", "")
                val message = args.optString("message", "")
                val app = args.optString("app", "sms")
                actionsManager.messageContact(contact, message, app)
            }
            "search_contacts" -> {
                val query = args.optString("query", "")
                val limit = args.optInt("limit", 10)
                actionsManager.searchContacts(query, limit)
            }
            "build_website" -> {
                val prompt = args.optString("prompt", "Modern website")
                actionsManager.buildWebsite(prompt)
            }
            "device_control" -> {
                val what = args.optString("what", "flashlight")
                val state = args.optString("state", "on")
                actionsManager.controlDevice(what, state)
            }
            "media_control" -> {
                val action = args.optString("action", "toggle")
                actionsManager.controlMedia(action)
            }
            "search_youtube" -> {
                val query = args.optString("query", "")
                actionsManager.searchYouTube(query)
            }
            "search_spotify" -> {
                val query = args.optString("query", "")
                actionsManager.searchSpotify(query)
            }
            "set_alarm" -> {
                val hour = args.optInt("hour", 7)
                val minute = args.optInt("minute", 0)
                val message = args.optString("message", "Jarvis Alarm")
                actionsManager.setAlarm(hour, minute, message)
            }
            "set_timer" -> {
                val seconds = args.optInt("seconds", 60)
                val message = args.optString("message", "Jarvis Timer")
                actionsManager.setTimer(seconds, message)
            }
            "phone_status" -> {
                actionsManager.getPhoneStatus()
            }
            "set_volume" -> {
                val level = args.optInt("level", 50)
                actionsManager.setVolume(level)
            }
            "get_current_datetime" -> {
                actionsManager.getCurrentDateTime()
            }
            "web_search" -> {
                val query = args.optString("query", "")
                actionsManager.searchWeb(query)
            }
            "navigate_to" -> {
                val dest = args.optString("destination", "")
                actionsManager.navigateTo(dest)
            }
            "open_camera" -> {
                actionsManager.openCamera()
            }
            "start_sketch_drawing" -> {
                actionsManager.startSketchDrawing()
            }
            else -> "Unknown action requested: $name"
        }
    }
}
