package com.example.mytodo.ui.components

import androidx.compose.ui.graphics.Color
import com.example.mytodo.data.local.Priority

val Priority.accent: Color
    get() = when (this) {
        Priority.NONE -> Color(0xFF43A047)      // green
        Priority.LOW -> Color(0xFF42A5F5)      // blue
        Priority.MEDIUM -> Color(0xFFFF9800)   // orange
        Priority.HIGH -> Color(0xFFE53935)     // red
    }

val Priority.label: String
    get() = when (this) {
        Priority.NONE -> "보통"
        Priority.LOW -> "낮음"
        Priority.MEDIUM -> "중간"
        Priority.HIGH -> "높음"
    }
