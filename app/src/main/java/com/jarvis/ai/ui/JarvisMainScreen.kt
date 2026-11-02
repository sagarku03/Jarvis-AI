package com.jarvis.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.ui.components.ChatBubble
import com.jarvis.ai.ui.components.ChatMessage
import com.jarvis.ai.ui.components.OrbView
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisGold
import com.jarvis.ai.ui.theme.JarvisPurple
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.ui.theme.JarvisSurfaceLight
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainScreen(
    state: String, // "IDLE", "LISTENING", "THINKING", "SPEAKING"
    messages: List<ChatMessage>,
    apiKey: String,
    isScreenShareActive: Boolean,
    onSaveApiKey: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLaunchForge: (String) -> Unit,
    onToggleScreenShare: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onShowKeyboardPicker: () -> Unit,
    onExecuteTool: (String, String) -> String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("🤖 Assistant", "⚡ The Forge 3D", "⌨️ AI Keyboard", "🛠️ Device Tools", "📱 Screen Share")

    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var tempKey by remember { mutableStateOf(apiKey) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == "LISTENING") 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(top = 28.dp, bottom = 8.dp)
    ) {
        // --- TOP HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (apiKey.isNotBlank()) JarvisCyan else JarvisGold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "JARVIS AI",
                    color = JarvisTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisSurfaceLight)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isScreenShareActive) "LIVE CAST" else "v2.0 PRO",
                        color = if (isScreenShareActive) Color(0xFF00E676) else JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = {
                tempKey = apiKey
                showSettingsDialog = true
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisTextSecondary
                )
            }
        }

        // --- FUTURISTIC TABS ROW ---
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = JarvisBackground,
            contentColor = JarvisCyan,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = JarvisCyan,
                    height = 2.5.dp
                )
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) JarvisCyan else JarvisTextSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- TAB CONTENT ---
        when (selectedTab) {
            0 -> AssistantTabContent(
                state = state,
                messages = messages,
                inputText = inputText,
                onInputChange = { inputText = it },
                pulseScale = pulseScale,
                listState = listState,
                onSendMessage = onSendMessage,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onQuickTabSwitch = { selectedTab = it },
                onExecuteTool = onExecuteTool
            )
            1 -> ForgeTabContent(onLaunchForge = onLaunchForge)
            2 -> KeyboardTabContent(
                onOpenKeyboardSettings = onOpenKeyboardSettings,
                onShowKeyboardPicker = onShowKeyboardPicker
            )
            3 -> DeviceToolsTabContent(onExecuteTool = onExecuteTool)
            4 -> ScreenShareTabContent(
                isScreenShareActive = isScreenShareActive,
                onToggleScreenShare = onToggleScreenShare
            )
        }
    }

    // --- SETTINGS DIALOG ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = JarvisSurface,
            title = {
                Text(
                    text = "Jarvis AI Settings",
                    color = JarvisTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API Key to enable voice reasoning & 18 native phone tools:",
                        color = JarvisTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIzaSy...", color = JarvisTextSecondary.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisSurfaceLight,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveApiKey(tempKey.trim())
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                ) {
                    Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceLight)
                ) {
                    Text("Cancel", color = JarvisTextPrimary)
                }
            }
        )
    }
}

// ====================================================================
// TAB 0: ASSISTANT TAB (Orb + Quick Dock + Chat + Voice Input)
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantTabContent(
    state: String,
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    pulseScale: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSendMessage: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onQuickTabSwitch: (Int) -> Unit,
    onExecuteTool: (String, String) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // --- 3D ORB REACTOR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.95f),
            contentAlignment = Alignment.Center
        ) {
            OrbView(state = state)

            // State Badge overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurface.copy(alpha = 0.88f))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                val stateText = when (state) {
                    "LISTENING" -> "● LISTENING..."
                    "THINKING" -> "✦ ANALYZING..."
                    "SPEAKING" -> "▲ SPEAKING..."
                    else -> "◉ JARVIS ONLINE"
                }
                val stateColor = when (state) {
                    "LISTENING" -> JarvisCyan
                    "THINKING" -> JarvisPurple
                    "SPEAKING" -> Color(0xFF00FFB4)
                    else -> JarvisTextSecondary
                }
                Text(
                    text = stateText,
                    color = stateColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // --- QUICK ACTIONS DOCK (Horizontal Chips) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickChip(label = "⚡ Launch The Forge 3D") { onQuickTabSwitch(1) }
            QuickChip(label = "⌨️ AI Keyboard") { onQuickTabSwitch(2) }
            QuickChip(label = "🔦 Flashlight") { onExecuteTool("flashlight_on", "") }
            QuickChip(label = "🔋 Battery Status") { onExecuteTool("phone_status", "") }
            QuickChip(label = "🎵 Spotify") { onExecuteTool("spotify", "Lo-Fi Beats") }
            QuickChip(label = "📺 YouTube") { onExecuteTool("youtube", "Jarvis AI Assistant") }
            QuickChip(label = "📱 Screen Share") { onQuickTabSwitch(4) }
            QuickChip(label = "📷 Camera") { onExecuteTool("camera", "") }
        }

        // --- CONVERSATION STREAM ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .padding(vertical = 4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }
            }
        }

        // --- ACTION / INPUT CONTROLS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = "Ask Jarvis or give phone command...",
                        color = JarvisTextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = JarvisSurface,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisSurfaceLight,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = {
                        val text = inputText
                        onInputChange("")
                        onSendMessage(text)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Black
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (state == "LISTENING") {
                            onStopListening()
                        } else {
                            onStartListening()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (state == "LISTENING") JarvisCyan else JarvisSurfaceLight)
                ) {
                    Icon(
                        imageVector = if (state == "LISTENING") Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (state == "LISTENING") Color.Black else JarvisCyan
                    )
                }
            }
        }
    }
}

// ====================================================================
// TAB 1: THE FORGE 3D REACTOR (Website Generator)
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgeTabContent(
    onLaunchForge: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("Futuristic Cyberpunk Portfolio Website") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F1E36), Color(0xFF080D1A))
                    )
                )
                .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "THE FORGE 3D ENGINE",
                            color = JarvisCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Three.js WebGL Procedural Website Builder",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Extracted directly from Lia AI via REA. The Forge generates full HTML5/CSS3/JS websites inside an interactive 3D WebGL Reactor with real-time UnrealBloomPass shaders.",
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge("Three.js r160")
                    StatusBadge("UnrealBloom")
                    StatusBadge("HTML5 Compiler")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Prompt Builder
        Text(
            text = "Website Prompt:",
            color = JarvisTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = JarvisSurface,
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisSurfaceLight,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Chips
        Text(text = "Quick Presets:", color = JarvisTextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip("Cyberpunk Portfolio") { prompt = "Cyberpunk Portfolio with Neon Glow & 3D Cards" }
            PresetChip("AI SaaS Platform") { prompt = "AI SaaS Dashboard with Interactive Metrics & Pricing" }
            PresetChip("Coffee Roastery") { prompt = "Dark Minimalist Coffee Roastery & Online Shop" }
            PresetChip("NFT Studio") { prompt = "Web3 NFT Gallery with Holographic Showcase" }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big Launch Button
        Button(
            onClick = { onLaunchForge(prompt) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "🚀 LAUNCH 3D FORGE REACTOR",
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ====================================================================
// TAB 2: JARVIS AI KEYBOARD (IME Setup & Sandbox)
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardTabContent(
    onOpenKeyboardSettings: () -> Unit,
    onShowKeyboardPicker: () -> Unit
) {
    var testInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF16142B), Color(0xFF0C0A1A))
                    )
                )
                .border(1.dp, JarvisPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⌨️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "JARVIS AI KEYBOARD",
                            color = JarvisPurple,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "System Input Method Service (IME)",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Dictate with an interactive glowing Voice Orb, switch between Hindi & English on the fly, and use AI grammar fixes in WhatsApp, Chrome, or any app on your phone.",
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activation Steps
        Text(
            text = "2-Step Activation Guide:",
            color = JarvisTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onOpenKeyboardSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceLight),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("⚙️ Step 1: Enable Jarvis in System Settings", color = JarvisCyan, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onShowKeyboardPicker,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🔄 Step 2: Switch to Jarvis AI Keyboard", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Test Sandbox
        Text(
            text = "🧪 Live Typing Test Sandbox:",
            color = JarvisTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap below to test the Jarvis Voice Orb keyboard right here inside the app:",
            color = JarvisTextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = testInput,
            onValueChange = { testInput = it },
            placeholder = { Text("Tap here to open keyboard and speak...", color = JarvisTextSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = JarvisSurface,
                focusedBorderColor = JarvisPurple,
                unfocusedBorderColor = JarvisSurfaceLight,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ====================================================================
// TAB 3: DEVICE TOOLS (18 Native Phone Tools Dashboard)
// ====================================================================
@Composable
private fun DeviceToolsTabContent(
    onExecuteTool: (String, String) -> String
) {
    var toolResult by remember { mutableStateOf("Tap any native tool below to test on your phone.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "⚡ 18 NATIVE PHONE TOOLS",
            color = JarvisCyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text = "All device features reverse-engineered from Lia AI. Test any tool directly:",
            color = JarvisTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tool Action Cards
        ToolCard(
            title = "🔦 Flashlight Control",
            desc = "Hardware torch via CameraManager",
            actions = listOf(
                "Turn ON" to { toolResult = onExecuteTool("flashlight_on", "") },
                "Turn OFF" to { toolResult = onExecuteTool("flashlight_off", "") }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        ToolCard(
            title = "🔋 Phone Status Inspector",
            desc = "Live battery percentage & volume stream query",
            actions = listOf(
                "Check Status" to { toolResult = onExecuteTool("phone_status", "") }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        ToolCard(
            title = "🔊 Audio Volume Control",
            desc = "Set system media stream volume",
            actions = listOf(
                "Set to 80%" to { toolResult = onExecuteTool("volume_80", "") },
                "Set to 30%" to { toolResult = onExecuteTool("volume_30", "") }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        ToolCard(
            title = "🎵 Spotify & 📺 YouTube",
            desc = "Deep link search & direct playback",
            actions = listOf(
                "Play Lo-Fi Spotify" to { toolResult = onExecuteTool("spotify", "Lo-Fi Beats") },
                "Open YouTube News" to { toolResult = onExecuteTool("youtube", "AI Assistant 2026") }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        ToolCard(
            title = "📷 Camera & 🎨 Sketch Drawing",
            desc = "Direct capture & animated pencil art converter",
            actions = listOf(
                "Launch Camera" to { toolResult = onExecuteTool("camera", "") },
                "Start Sketch" to { toolResult = onExecuteTool("sketch", "") }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        ToolCard(
            title = "⏰ Realtime Clock",
            desc = "Formatted system time, day and date",
            actions = listOf(
                "Check Current Time" to { toolResult = onExecuteTool("datetime", "") }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Output Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(text = "Tool Output:", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = toolResult, color = JarvisTextPrimary, fontSize = 13.sp)
            }
        }
    }
}

// ====================================================================
// TAB 4: SCREEN SHARE (Live MediaProjection Broadcasting)
// ====================================================================
@Composable
private fun ScreenShareTabContent(
    isScreenShareActive: Boolean,
    onToggleScreenShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF072118), Color(0xFF040E0A))
                    )
                )
                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📱", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LIVE SCREEN BROADCASTING",
                            color = Color(0xFF00E676),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Foreground Service MediaProjection",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Streams your phone screen in real-time so Jarvis can analyze apps, documents, and gaming visuals via Gemini Live multimodal reasoning.",
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isScreenShareActive) Color(0xFF00E676) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScreenShareActive) "SERVICE STATUS: ACTIVE (BROADCASTING)" else "SERVICE STATUS: INACTIVE",
                        color = if (isScreenShareActive) Color(0xFF00E676) else JarvisTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onToggleScreenShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScreenShareActive) Color(0xFFFF5252) else Color(0xFF00E676)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isScreenShareActive) "⏹ STOP SCREEN BROADCAST" else "▶ START SCREEN BROADCAST",
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ====================================================================
// REUSABLE UI COMPONENTS
// ====================================================================
@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(JarvisSurfaceLight)
            .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = JarvisCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(JarvisSurfaceLight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisSurfaceLight)
            .border(1.dp, JarvisCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = JarvisTextPrimary, fontSize = 11.sp)
    }
}

@Composable
private fun ToolCard(
    title: String,
    desc: String,
    actions: List<Pair<String, () -> Unit>>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisSurfaceLight, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, color = JarvisTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = JarvisTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (action in actions) {
                    Button(
                        onClick = action.second,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(text = action.first, color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
