package com.jarvis.ai

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jarvis.ai.ai.GeminiClient
import com.jarvis.ai.service.DeviceActionsManager
import com.jarvis.ai.service.JarvisVoiceService
import com.jarvis.ai.service.ScreenShareService
import com.jarvis.ai.ui.JarvisMainScreen
import com.jarvis.ai.ui.TheForgeActivity
import com.jarvis.ai.ui.components.ChatMessage
import com.jarvis.ai.ui.theme.JarvisAITheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var actionsManager: DeviceActionsManager
    private lateinit var geminiClient: GeminiClient

    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceService: JarvisVoiceService? = null
    private var isServiceBound = false

    private var jarvisState by mutableStateOf("IDLE")
    private val messages = mutableStateListOf<ChatMessage>()
    private var apiKey by mutableStateOf("")
    private var isScreenShareActive by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as JarvisVoiceService.LocalBinder
            voiceService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isServiceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val recordAudioGranted = perms[Manifest.permission.RECORD_AUDIO] ?: false
        if (!recordAudioGranted) {
            Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved API Key
        val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        apiKey = prefs.getString("gemini_api_key", "") ?: ""

        actionsManager = DeviceActionsManager(this)
        geminiClient = GeminiClient(apiKey, actionsManager)

        // Initial welcome message
        messages.add(
            ChatMessage(
                sender = "jarvis",
                text = "Welcome to Jarvis AI. The Forge 3D Reactor, AI Keyboard, Screen Sharing, and 18 Device Tools are active and ready."
            )
        )

        checkPermissions()
        startVoiceService()
        initSpeechRecognizer()

        setContent {
            JarvisAITheme {
                JarvisMainScreen(
                    state = jarvisState,
                    messages = messages,
                    apiKey = apiKey,
                    isScreenShareActive = isScreenShareActive,
                    onSaveApiKey = { newKey ->
                        apiKey = newKey
                        prefs.edit().putString("gemini_api_key", newKey).apply()
                        geminiClient.updateApiKey(newKey)
                        Toast.makeText(this, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onStartListening = { startListening() },
                    onStopListening = { stopListening() },
                    onSendMessage = { text -> handleUserCommand(text) },
                    onLaunchForge = { prompt -> launchForge(prompt) },
                    onToggleScreenShare = { toggleScreenShare() },
                    onOpenKeyboardSettings = { openKeyboardSettings() },
                    onShowKeyboardPicker = { showKeyboardPicker() },
                    onExecuteTool = { tool, query -> executeDirectTool(tool, query) }
                )
            }
        }
    }

    private fun launchForge(prompt: String) {
        val finalPrompt = prompt.ifBlank { "Futuristic Cyberpunk Portfolio Website" }
        val intent = Intent(this, TheForgeActivity::class.java).apply {
            putExtra("website_prompt", finalPrompt)
        }
        startActivity(intent)
        messages.add(ChatMessage(sender = "jarvis", text = "Launching The Forge 3D Reactor for: '$finalPrompt'", toolNotice = "build_website"))
    }

    private fun toggleScreenShare() {
        val intent = Intent(this, ScreenShareService::class.java)
        if (isScreenShareActive) {
            stopService(intent)
            isScreenShareActive = false
            Toast.makeText(this, "Screen Share Stopped", Toast.LENGTH_SHORT).show()
            messages.add(ChatMessage(sender = "jarvis", text = "Screen sharing stopped.", toolNotice = "ScreenShareService"))
        } else {
            ContextCompat.startForegroundService(this, intent)
            isScreenShareActive = true
            Toast.makeText(this, "Screen Share Broadcasting Started", Toast.LENGTH_SHORT).show()
            messages.add(ChatMessage(sender = "jarvis", text = "Screen sharing broadcast is now LIVE.", toolNotice = "ScreenShareService"))
        }
    }

    private fun openKeyboardSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Toast.makeText(this, "Enable 'Jarvis AI Keyboard' in list", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showKeyboardPicker() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not show picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeDirectTool(tool: String, query: String): String {
        val result = when (tool) {
            "flashlight_on" -> actionsManager.controlDevice("flashlight", "on")
            "flashlight_off" -> actionsManager.controlDevice("flashlight", "off")
            "phone_status" -> actionsManager.getPhoneStatus()
            "volume_80" -> actionsManager.setVolume(80)
            "volume_30" -> actionsManager.setVolume(30)
            "spotify" -> actionsManager.searchSpotify(query.ifBlank { "Top Hits 2026" })
            "youtube" -> actionsManager.searchYouTube(query.ifBlank { "Jarvis AI Assistant" })
            "camera" -> actionsManager.openCamera()
            "sketch" -> actionsManager.startSketchDrawing()
            "datetime" -> actionsManager.getCurrentDateTime()
            "forge" -> {
                launchForge(query)
                "Launching The Forge 3D..."
            }
            else -> "Executed $tool"
        }

        messages.add(ChatMessage(sender = "user", text = "⚡ Quick Action: $tool"))
        messages.add(ChatMessage(sender = "jarvis", text = result, toolNotice = "Native Tool"))
        voiceService?.speak(result) {}
        return result
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    private fun startVoiceService() {
        val intent = Intent(this, JarvisVoiceService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        jarvisState = "LISTENING"
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        jarvisState = "THINKING"
                    }

                    override fun onError(error: Int) {
                        jarvisState = "IDLE"
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            handleUserCommand(recognizedText)
                        } else {
                            jarvisState = "IDLE"
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer?.startListening(intent)
        jarvisState = "LISTENING"
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        jarvisState = "IDLE"
    }

    private fun handleUserCommand(text: String) {
        messages.add(ChatMessage(sender = "user", text = text))
        jarvisState = "THINKING"

        lifecycleScope.launch {
            val (reply, toolNotice) = geminiClient.sendMessage(text)

            messages.add(
                ChatMessage(
                    sender = "jarvis",
                    text = reply,
                    toolNotice = toolNotice
                )
            )

            jarvisState = "SPEAKING"
            voiceService?.speak(reply) {
                jarvisState = "IDLE"
            }

            val estimatedDelay = (reply.length * 70L).coerceIn(2000L, 8000L)
            delay(estimatedDelay)
            if (jarvisState == "SPEAKING") {
                jarvisState = "IDLE"
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }
}
