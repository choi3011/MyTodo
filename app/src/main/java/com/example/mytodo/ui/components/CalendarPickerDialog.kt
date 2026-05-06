package com.example.mytodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mytodo.ui.theme.BrandCoral
import com.example.mytodo.ui.theme.BrandIndigo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarPickerDialog(
    visible: Boolean,
    selectedDate: LocalDate,
    dayDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    if (!visible) return
    var displayMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                CalendarHeader(
                    yearMonth = displayMonth,
                    onPrev = { displayMonth = displayMonth.minusMonths(1) },
                    onNext = { displayMonth = displayMonth.plusMonths(1) },
                )
                Spacer(Modifier.height(8.dp))
                WeekdayHeader()
                Spacer(Modifier.height(4.dp))
                CalendarGrid(
                    yearMonth = displayMonth,
                    selectedDate = selectedDate,
                    today = today,
                    dayDates = dayDates,
                    onDateClick = { date ->
                        onSelect(date)
                        onDismiss()
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        onSelect(today)
                        onDismiss()
                    }) {
                        Text("오늘로", color = BrandIndigo, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("닫기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    yearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "이전 달")
        }
        Text(
            text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "다음 달")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { idx, day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = when (idx) {
                    0 -> BrandCoral
                    6 -> BrandIndigo
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    dayDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
) {
    val firstOfMonth = yearMonth.atDay(1)
    val leading = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = ((leading + daysInMonth + 6) / 7) * 7

    Column {
        repeat(totalCells / 7) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - leading + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    ) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayOfMonth)
                            DateCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                hasTodos = date in dayDates,
                                onClick = { onDateClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasTodos: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) BrandIndigo else Color.Transparent
    val textColor = when {
        isSelected -> Color.White
        date.dayOfWeek == DayOfWeek.SUNDAY -> BrandCoral
        date.dayOfWeek == DayOfWeek.SATURDAY -> BrandIndigo
        else -> MaterialTheme.colorScheme.onSurface
    }
    val cellModifier = Modifier
        .fillMaxSize()
        .padding(2.dp)
        .clip(CircleShape)
        .background(containerColor)
        .then(
            if (isToday && !isSelected) {
                Modifier.border(width = 2.dp, color = BrandIndigo, shape = CircleShape)
            } else {
                Modifier
            },
        )
        .clickable(onClick = onClick)

    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${date.dayOfMonth}",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
            )
            if (hasTodos) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else BrandIndigo),
                )
            }
        }
    }
}
