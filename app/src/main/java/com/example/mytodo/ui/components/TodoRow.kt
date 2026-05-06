package com.example.mytodo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.mytodo.data.local.TodoEntity
import com.example.mytodo.ui.theme.BrandCoral
import com.example.mytodo.ui.theme.BrandIndigo
import java.time.format.DateTimeFormatter

@Composable
fun TodoRow(
    todo: TodoEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val textAlpha by animateFloatAsState(
        targetValue = if (todo.done) 0.45f else 1f,
        label = "textAlpha",
    )
    var menuOpen by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    val rowBackground = if (pressed || menuOpen) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .pointerInput(todo.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onLongPress = { offset ->
                        pressOffset = DpOffset(offset.x.toDp(), 0.dp)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            offset = pressOffset,
        ) {
            DropdownMenuItem(
                text = { Text("수정") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = BrandIndigo) },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(if (todo.done) "미완료로 표시" else "완료로 표시") },
                leadingIcon = {
                    Icon(
                        imageVector = if (todo.done) Icons.Rounded.RadioButtonUnchecked else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                    )
                },
                onClick = {
                    menuOpen = false
                    onToggle(!todo.done)
                },
            )
            DropdownMenuItem(
                text = { Text("삭제", color = BrandCoral) },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = BrandCoral) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(todo.priority.accent),
        )
        Spacer(Modifier.width(14.dp))
        CircleCheckbox(
            checked = todo.done,
            onCheckedChange = onToggle,
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.alpha(textAlpha)) {
            Text(
                text = todo.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
            Text(
                text = formatTimeRange(todo.startTime, todo.endTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val TimeRowFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTimeRange(start: java.time.LocalTime?, end: java.time.LocalTime?): String {
    val s = start?.format(TimeRowFormatter)
    val e = end?.format(TimeRowFormatter)
    return when {
        s != null && e != null -> "$s ~ $e"
        s != null -> s
        e != null -> "~ $e"
        else -> "시간 없음"
    }
}
