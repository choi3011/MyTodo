package com.example.mytodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mytodo.data.local.Scope
import com.example.mytodo.ui.theme.BrandIndigo
import com.example.mytodo.ui.theme.BrandMagenta

@Composable
fun EmptyState(scope: Scope, modifier: Modifier = Modifier) {
    val (title, body) = when (scope) {
        Scope.DAY -> "오늘은 자유로운 하루" to "할 일을 추가해서 하루를 시작해보세요"
        Scope.WEEK -> "이번 주는 비어 있어요" to "이번 주에 이루고 싶은 일을 적어보세요"
        Scope.MONTH -> "이번 달은 비어 있어요" to "이번 달의 큰 그림을 그려보세요"
        Scope.YEAR -> "올해는 비어 있어요" to "올해 가장 이루고 싶은 것을 적어보세요"
    }
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandMagenta.copy(alpha = 0.55f),
                            BrandIndigo.copy(alpha = 0.35f),
                            BrandIndigo.copy(alpha = 0.0f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
