package com.jarvis.ai.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class JarvisKeyboardService : InputMethodService() {

    private var isHindi = false
    private var isListening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var statusTextView: TextView
    private lateinit var orbButton: TextView
    private lateinit var langButton: TextView

    // Runnable for repeating backspace
    private var backspaceRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0B10"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Top Toolbar / Mode selector
        val toolbarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }

        val modes = listOf("Dictate", "Fix Grammar", "Translate", "AI Reply")
        val modeScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val modeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (mode in modes) {
            val chip = TextView(this).apply {
                text = mode
                textSize = 12f
                setTextColor(Color.parseColor("#80D8FF"))
                setPadding(dp(12), dp(4), dp(12), dp(4))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#15202B"))
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), Color.parseColor("#263238"))
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(6)
                }
                layoutParams = params
                setOnClickListener {
                    statusTextView.text = "Mode: $mode"
                }
            }
            modeContainer.addView(chip)
        }
        modeScroll.addView(modeContainer)
        toolbarLayout.addView(modeScroll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rootLayout.addView(toolbarLayout)

        // Center Voice Orb / Pulse visualizer
        val centerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(16), dp(10), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10141E"))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#00E5FF").let { Color.argb(40, Color.red(it), Color.green(it), Color.blue(it)) })
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(110)
            )
        }

        orbButton = TextView(this).apply {
            text = "●"
            textSize = 36f
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
            setOnClickListener {
                toggleVoiceDictation()
            }
        }
        centerCard.addView(orbButton)

        statusTextView = TextView(this).apply {
            text = "Tap the orb to speak (English)"
            textSize = 13f
            setTextColor(Color.parseColor("#B0BEC5"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        centerCard.addView(statusTextView)
        rootLayout.addView(centerCard)

        // Bottom Action Row: [🌐 Language] [Space Bar] [⌫ Backspace] [Enter]
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(8)
            }
            layoutParams = params
        }

        // 1. Language Toggle (🌐)
        langButton = createKeyButton("🌐 EN", 1.0f) {
            isHindi = !isHindi
            langButton.text = if (isHindi) "🌐 HI" else "🌐 EN"
            statusTextView.text = if (isHindi) "Tap the orb to speak (Hindi)" else "Tap the orb to speak (English)"
        }
        bottomRow.addView(langButton)

        // 2. Space Bar
        val spaceButton = createKeyButton("Space", 2.6f) {
            currentInputConnection?.commitText(" ", 1)
        }
        bottomRow.addView(spaceButton)

        // 3. Backspace (with long press repeating delete)
        val deleteButton = createKeyButton("⌫", 1.0f) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        deleteButton.setOnLongClickListener {
            backspaceRunnable = object : Runnable {
                override fun run() {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    handler.postDelayed(this, 60)
                }
            }
            handler.post(backspaceRunnable!!)
            true
        }
        deleteButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                backspaceRunnable?.let { handler.removeCallbacks(it) }
                backspaceRunnable = null
            }
            false
        }
        bottomRow.addView(deleteButton)

        // 4. Enter
        val enterButton = createKeyButton("Enter", 1.2f) {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        bottomRow.addView(enterButton)

        rootLayout.addView(bottomRow)
        return rootLayout
    }

    private fun createKeyButton(label: String, weight: Float, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1B2330"))
                cornerRadius = dp(8).toFloat()
            }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            layoutParams = params
            setOnClickListener { onClick() }
        }
    }

    private fun toggleVoiceDictation() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusTextView.text = "Speech recognition unavailable"
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    orbButton.setTextColor(Color.parseColor("#00E676")) // Green listening
                    statusTextView.text = if (isHindi) "Listening (Hindi)..." else "Listening (English)..."
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    statusTextView.text = "Transcribing..."
                }

                override fun onError(error: Int) {
                    stopListening()
                    statusTextView.text = "Tap orb to speak"
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        currentInputConnection?.commitText("$text ", 1)
                        statusTextView.text = "Committed: $text"
                    }
                    stopListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        statusTextView.text = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val locale = if (isHindi) Locale("hi", "IN") else Locale.US
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        orbButton.setTextColor(Color.parseColor("#00E5FF"))
        speechRecognizer?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
