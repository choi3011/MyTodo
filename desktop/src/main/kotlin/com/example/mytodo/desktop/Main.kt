package com.example.mytodo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

private val BrandIndigo = Color(0xFF6366F1)
private val BrandMagenta = Color(0xFFEC4899)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MyTodo",
        state = rememberWindowState(width = 480.dp, height = 720.dp),
    ) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(BrandIndigo, BrandMagenta))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "MyTodo Desktop",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
