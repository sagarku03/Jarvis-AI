package com.jarvis.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisSurfaceLight
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary

data class ChatMessage(
    val sender: String, // "user" or "jarvis"
    val text: String,
    val toolNotice: String? = null
)

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .background(if (isUser) JarvisSurfaceLight else JarvisSurfaceLight.copy(alpha = 0.6f))
                .padding(12.dp)
        ) {
            Column {
                if (!isUser && message.toolNotice != null) {
                    Text(
                        text = "⚡ ${message.toolNotice}",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = message.text,
                    color = if (isUser) JarvisCyan else JarvisTextPrimary,
                    fontSize = 15.sp
                )
            }
        }
    }
}
