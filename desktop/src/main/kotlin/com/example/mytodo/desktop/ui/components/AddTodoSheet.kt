package com.example.mytodo.desktop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mytodo.desktop.data.Priority
import com.example.mytodo.desktop.theme.BrandIndigo
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val SLIDE_DURATION_MS = 220
private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun AddTodoSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Priority, LocalTime?, LocalTime?) -> Unit,
    title: String = "새 할 일",
    submitLabel: String = "추가",
    initialText: String = "",
    initialPriority: Priority = Priority.NONE,
    initialStartTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
) {
    val transition = updateTransition(targetState = visible, label = "sheet")
    val rendered = transition.currentState || transition.targetState
    if (!rendered) return

    var text by remember(initialText) { mutableStateOf(initialText) }
    var priority by remember(initialPriority) { mutableStateOf(initialPriority) }
    var startInput by remember(initialStartTime) {
        mutableStateOf(initialStartTime?.format(TimeFormatter) ?: "")
    }
    var endInput by remember(initialEndTime) {
        mutableStateOf(initialEndTime?.format(TimeFormatter) ?: "")
    }
    val focusRequester = remember { FocusRequester() }

    fun submit() {
        if (text.isBlank()) return
        onSubmit(text, priority, parseTimeInput(startInput), parseTimeInput(endInput))
        text = ""
        priority = Priority.NONE
        startInput = ""
        endInput = ""
    }

    LaunchedEffect(visible) {
        if (visible) focusRequester.requestFocus()
    }

    val animSpec = tween<Float>(SLIDE_DURATION_MS, easing = FastOutSlowInEasing)

    val scrimAlpha by transition.animateFloat(
        transitionSpec = { animSpec },
        label = "scrim",
    ) { if (it) 0.45f else 0f }

    val slideProgress by transition.animateFloat(
        transitionSpec = { animSpec },
        label = "slide",
    ) { if (it) 1f else 0f }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val offsetY = ((1f - slideProgress) * placeable.height).toInt()
                    layout(placeable.width, placeable.height) {
                        placeable.place(IntOffset(0, offsetY))
                    }
                },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 32.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            submit()
                            true
                        } else {
                            false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("무엇을 해야 하나요?") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandIndigo,
                        cursorColor = BrandIndigo,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        PriorityChip(
                            priority = p,
                            selected = priority == p,
                            onClick = { priority = p },
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeInputField(
                        value = startInput,
                        onValueChange = { startInput = sanitizeTimeInput(it) },
                        label = "시작",
                        imeAction = ImeAction.Next,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "~",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TimeInputField(
                        value = endInput,
                        onValueChange = { endInput = sanitizeTimeInput(it) },
                        label = "종료",
                        imeAction = ImeAction.Done,
                        onImeDone = { submit() },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "취소",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (text.isNotBlank()) {
                        GradientPillButton(
                            onClick = { submit() },
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            label = submitLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onImeDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = { Text("HH:mm") },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandIndigo,
            cursorColor = BrandIndigo,
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeDone?.invoke() }),
    )
}

private fun sanitizeTimeInput(raw: String): String {
    val digitsOnly = raw.filter { it.isDigit() || it == ':' }
    return if (digitsOnly.length > 5) digitsOnly.take(5) else digitsOnly
}

internal fun parseTimeInput(input: String): LocalTime? {
    val s = input.trim()
    if (s.isEmpty()) return null
    val parts = s.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return LocalTime.of(h, m)
}

@Composable
private fun PriorityChip(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = priority.accent
    val containerColor = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
    val labelColor = if (selected) accent else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(percent = 50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = priority.label,
            color = labelColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}
