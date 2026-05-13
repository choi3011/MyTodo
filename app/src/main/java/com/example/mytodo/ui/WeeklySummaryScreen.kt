package com.example.mytodo.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.mytodo.data.local.TodoEntity
import com.example.mytodo.ui.theme.BrandIndigo
import com.example.mytodo.ui.theme.BrandMagenta
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    weekStart: LocalDate,
    weekTodos: Map<LocalDate, List<TodoEntity>>,
    loading: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    val today = LocalDate.now()
    val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val isCurrentWeek = weekStart == currentMonday
    val weekEnd = weekStart.plusDays(6)
    val flatTodos = weekTodos.values.flatten()
    val totalAll = flatTodos.size
    val totalDone = flatTodos.count { it.done }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "주간 요약",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로",
                        )
                    }
                },
                actions = {
                    if (!isCurrentWeek) {
                        TextButton(onClick = onReset) {
                            Text("이번 주", color = BrandIndigo)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "navigator") {
                WeekNavigator(
                    start = weekStart,
                    end = weekEnd,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }
            if (loading && weekTodos.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = BrandIndigo) }
                }
            } else {
                item(key = "summary") {
                    WeekSummaryStats(done = totalDone, total = totalAll)
                }
                items(count = 7, key = { "day-${weekStart.plusDays(it.toLong())}" }) { i ->
                    val day = weekStart.plusDays(i.toLong())
                    DailyCard(
                        day = day,
                        isToday = day == today,
                        todos = weekTodos[day].orEmpty(),
                        onToggle = onToggle,
                    )
                }
            }
            item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun WeekNavigator(
    start: LocalDate,
    end: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "이전 주",
                tint = BrandIndigo,
            )
        }
        Text(
            text = "${start.monthValue}월 ${start.dayOfMonth}일 ~ ${end.monthValue}월 ${end.dayOfMonth}일",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "다음 주",
                tint = BrandIndigo,
            )
        }
    }
}

@Composable
private fun WeekSummaryStats(done: Int, total: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$done",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandIndigo,
                )
                Text(
                    text = " / $total 완료",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (total == 0) "" else "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (progress >= 1f) BrandMagenta else BrandIndigo,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (progress >= 1f) BrandMagenta else BrandIndigo,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun DailyCard(
    day: LocalDate,
    isToday: Boolean,
    todos: List<TodoEntity>,
    onToggle: (String, Boolean) -> Unit,
) {
    val activeCount = todos.count { !it.done }
    val doneCount = todos.size - activeCount
    val dow = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isToday) BrandIndigo.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = if (isToday) {
            Modifier.border(1.dp, BrandIndigo.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        } else Modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = dow,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) BrandIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${day.monthValue}월 ${day.dayOfMonth}일",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isToday) BrandIndigo else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (todos.isNotEmpty()) {
                    Text(
                        text = "$doneCount / ${todos.size}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (activeCount == 0) BrandMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (todos.isEmpty()) {
                Text(
                    text = "등록된 계획 없음",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                todos.forEach { todo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp),
                    ) {
                        Checkbox(
                            checked = todo.done,
                            onCheckedChange = { onToggle(todo.id, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BrandIndigo,
                                uncheckedColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = todo.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                            ),
                            color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
