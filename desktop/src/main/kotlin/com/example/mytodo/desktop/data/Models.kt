package com.example.mytodo.desktop.data

import java.time.LocalDate
import java.time.LocalTime

enum class Priority { NONE, LOW, MEDIUM, HIGH }

enum class Scope { DAY, WEEK, MONTH, YEAR }

data class TodoEntity(
    val id: String = "",
    val text: String = "",
    val done: Boolean = false,
    val priority: Priority = Priority.NONE,
    val scope: Scope = Scope.DAY,
    val targetDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
