package com.example.mytodo.desktop.ui.components

import androidx.compose.ui.graphics.Color
import com.example.mytodo.desktop.data.Priority

val Priority.accent: Color
    get() = when (this) {
        Priority.NONE -> Color(0xFF43A047)
        Priority.LOW -> Color(0xFF42A5F5)
        Priority.MEDIUM -> Color(0xFFFF9800)
        Priority.HIGH -> Color(0xFFE53935)
    }

val Priority.label: String
    get() = when (this) {
        Priority.NONE -> "보통"
        Priority.LOW -> "낮음"
        Priority.MEDIUM -> "중간"
        Priority.HIGH -> "높음"
    }
