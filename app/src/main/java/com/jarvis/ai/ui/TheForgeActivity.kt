package com.jarvis.ai.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TheForgeActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    initForgeEngine()
                }
            }
            addJavascriptInterface(ForgeBridge(), "ForgeBridge")
        }

        root.addView(webView)
        setContentView(root)

        val prompt = intent.getStringExtra("website_prompt") ?: "Modern portfolio website"
        webView.loadUrl("file:///android_asset/forge/forge.html")

        // Simulate / Drive Forge 3D pipeline stages
        lifecycleScope.launch {
            delay(1500)
            driveForgeStages(prompt)
        }
    }

    private fun initForgeEngine() {
        val js = """
            if (window.Forge) {
                window.Forge.init({
                    theme: {
                        top: '#051329',
                        bg: '#02050B',
                        accentA: '#00E5FF',
                        accentB: '#7C4DFF',
                        coreTint: '#00B0FF'
                    },
                    pixelRatioCap: 2,
                    lowRam: false,
                    reducedMotion: false
                });
            }
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private suspend fun driveForgeStages(prompt: String) {
        val stages = listOf(
            "PARSING_PROMPT" to "Analyzing requirements...",
            "SCAFFOLD" to "Building HTML5 semantic layout...",
            "STYLING" to "Applying responsive CSS3 & cyber theme...",
            "IMAGERY" to "Generating visuals & icons...",
            "MOTION" to "Injecting interactive JavaScript animations...",
            "FINALIZING" to "Compiling production bundle..."
        )

        for ((index, stage) in stages.withIndex()) {
            val progress = (index + 1).toFloat() / stages.size.toFloat()
            val js = """
                if (window.Forge) {
                    window.Forge.setStage('${stage.first}');
                    window.Forge.setProgress($progress);
                    window.Forge.addSection('Section $index');
                    window.Forge.pushLog('${stage.second}');
                    window.Forge.onChunk(120, ${(index + 1) * 350}, 45.5);
                }
            """.trimIndent()
            webView.evaluateJavascript(js, null)
            delay(1200)
        }

        delay(800)
        webView.evaluateJavascript("if (window.Forge) { window.Forge.complete(); }", null)
    }

    inner class ForgeBridge {
        @JavascriptInterface
        fun onReady() {}

        @JavascriptInterface
        fun onRevealFinished() {}

        @JavascriptInterface
        fun onWebGLFailed() {}
    }

    override fun onDestroy() {
        webView.evaluateJavascript("if (window.Forge) { window.Forge.dispose(); }", null)
        super.onDestroy()
    }
}
