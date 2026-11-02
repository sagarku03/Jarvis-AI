package com.jarvis.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    onSaveApiKey: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendMessage: (String) -> Unit
) {
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
            .padding(top = 28.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        // --- TOP HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "JARVIS CORE",
                    color = JarvisTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
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

        // --- 3D ORB REACTOR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            contentAlignment = Alignment.Center
        ) {
            OrbView(state = state)

            // Current State Badge overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurface.copy(alpha = 0.85f))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                val stateText = when (state) {
                    "LISTENING" -> "● LISTENING..."
                    "THINKING" -> "✦ ANALYZING..."
                    "SPEAKING" -> "▲ SPEAKING..."
                    else -> "◉ SYSTEM READY"
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // --- CONVERSATION STREAM ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .padding(vertical = 8.dp)
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
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Command Jarvis...",
                        color = JarvisTextSecondary.copy(alpha = 0.6f)
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

            // If text is entered, show Send button; otherwise show Mic button
            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = {
                        val text = inputText
                        inputText = ""
                        onSendMessage(text)
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = JarvisBackground
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
                        .size(52.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (state == "LISTENING") JarvisCyan else JarvisSurface)
                        .border(
                            2.dp,
                            if (state == "LISTENING") Color.White else JarvisCyan,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (state == "LISTENING") Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Mic",
                        tint = if (state == "LISTENING") JarvisBackground else JarvisCyan
                    )
                }
            }
        }
    }

    // --- API KEY SETTINGS MODAL ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Jarvis Settings",
                    color = JarvisTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API Key from Google AI Studio (aistudio.google.com):",
                        color = JarvisTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveApiKey(tempKey)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                ) {
                    Text("Save Key", color = JarvisBackground)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            },
            containerColor = JarvisSurface
        )
    }
}
