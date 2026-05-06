package com.example.mytodo.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytodo.desktop.data.Scope
import com.example.mytodo.desktop.theme.BrandAmber
import com.example.mytodo.desktop.ui.TodoFilter
import java.time.LocalDate

@Composable
fun TodoHero(
    scope: Scope,
    selectedDate: LocalDate,
    activeCount: Int,
    doneCount: Int,
    filter: TodoFilter,
    onFilterClick: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
        Text(
            text = scope.formatDate(selectedDate),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = scope.heroLabel(selectedDate),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 29.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(
                label = "할일",
                count = activeCount,
                accent = scope.accent,
                selected = filter == TodoFilter.ACTIVE,
                onClick = { onFilterClick(TodoFilter.ACTIVE) },
            )
            StatChip(
                label = "완료",
                count = doneCount,
                accent = BrandAmber,
                selected = filter == TodoFilter.DONE,
                onClick = { onFilterClick(TodoFilter.DONE) },
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) accent else accent.copy(alpha = 0.14f)
    val content = if (selected) Color.White else accent
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}
