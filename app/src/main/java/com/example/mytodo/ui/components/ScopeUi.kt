package com.example.mytodo.ui.components

import androidx.compose.ui.graphics.Color
import com.example.mytodo.data.local.Scope
import com.example.mytodo.ui.theme.BrandAmber
import com.example.mytodo.ui.theme.BrandCoral
import com.example.mytodo.ui.theme.BrandIndigo
import com.example.mytodo.ui.theme.BrandMagenta
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

val Scope.tabLabel: String
    get() = when (this) {
        Scope.DAY -> "Today"
        Scope.WEEK -> "Week"
        Scope.MONTH -> "Month"
        Scope.YEAR -> "Year"
    }

fun Scope.heroLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val anchor = anchorDateOf(date)
    val todayAnchor = anchorDateOf(today)
    return when (this) {
        Scope.DAY -> when (anchor) {
            todayAnchor -> "오늘"
            todayAnchor.plusDays(1) -> "내일"
            todayAnchor.minusDays(1) -> "어제"
            else -> anchor.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
        }
        Scope.WEEK -> when (anchor) {
            todayAnchor -> "이번 주"
            todayAnchor.plusWeeks(1) -> "다음 주"
            todayAnchor.minusWeeks(1) -> "지난 주"
            else -> {
                val weekFields = WeekFields.of(Locale.KOREAN)
                "${anchor.monthValue}월 ${anchor.get(weekFields.weekOfMonth())}주차"
            }
        }
        Scope.MONTH -> when (anchor) {
            todayAnchor -> "이번 달"
            todayAnchor.plusMonths(1) -> "다음 달"
            todayAnchor.minusMonths(1) -> "지난 달"
            else -> "${anchor.monthValue}월"
        }
        Scope.YEAR -> when (anchor) {
            todayAnchor -> "올해"
            todayAnchor.plusYears(1) -> "내년"
            todayAnchor.minusYears(1) -> "작년"
            else -> "${anchor.year}년"
        }
    }
}

val Scope.addLabel: String
    get() = when (this) {
        Scope.DAY -> "할 일 추가"
        Scope.WEEK -> "주 계획 추가"
        Scope.MONTH -> "월 계획 추가"
        Scope.YEAR -> "연 계획 추가"
    }

val Scope.accent: Color
    get() = when (this) {
        Scope.DAY -> BrandIndigo
        Scope.WEEK -> BrandMagenta
        Scope.MONTH -> BrandAmber
        Scope.YEAR -> BrandCoral
    }

fun Scope.anchorDateOf(date: LocalDate): LocalDate = when (this) {
    Scope.DAY -> date
    Scope.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    Scope.MONTH -> date.withDayOfMonth(1)
    Scope.YEAR -> date.withDayOfYear(1)
}

fun Scope.formatDate(date: LocalDate): String = when (this) {
    Scope.DAY -> date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN))
    Scope.WEEK -> {
        val weekFields = WeekFields.of(Locale.KOREAN)
        val anchor = anchorDateOf(date)
        "${anchor.year}년 ${anchor.monthValue}월 ${anchor.get(weekFields.weekOfMonth())}주차"
    }
    Scope.MONTH -> "${date.year}년 ${date.monthValue}월"
    Scope.YEAR -> "${date.year}년"
}
