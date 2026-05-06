package com.example.mytodo.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.example.mytodo.desktop.data.Priority
import com.example.mytodo.desktop.data.Scope
import com.example.mytodo.desktop.data.TodoEntity
import com.example.mytodo.desktop.ui.components.anchorDateOf
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class TodoFilter { ALL, ACTIVE, DONE }

class TodoState {
    val todos: SnapshotStateList<TodoEntity> = mutableListOf<TodoEntity>(
        TodoEntity(
            id = UUID.randomUUID().toString(),
            text = "Compose Desktop 구조 잡기",
            priority = Priority.HIGH,
            scope = Scope.DAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(11, 30),
        ),
        TodoEntity(
            id = UUID.randomUUID().toString(),
            text = "Firestore REST 인증 흐름 설계",
            priority = Priority.MEDIUM,
            scope = Scope.DAY,
            done = true,
        ),
        TodoEntity(
            id = UUID.randomUUID().toString(),
            text = "이번 주 회고 작성",
            priority = Priority.LOW,
            scope = Scope.WEEK,
        ),
    ).toMutableStateList()

    var sortByPriority by mutableStateOf(false)
        private set
    var filter by mutableStateOf(TodoFilter.ALL)
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    fun toggleSort() {
        sortByPriority = !sortByPriority
    }

    fun chooseFilter(f: TodoFilter) {
        filter = if (filter == f) TodoFilter.ALL else f
    }

    fun chooseDate(date: LocalDate) {
        selectedDate = date
    }

    fun add(
        text: String,
        priority: Priority,
        scope: Scope,
        startTime: LocalTime?,
        endTime: LocalTime?,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val anchor = scope.anchorDateOf(selectedDate)
        todos.add(
            TodoEntity(
                id = UUID.randomUUID().toString(),
                text = trimmed,
                priority = priority,
                scope = scope,
                targetDate = anchor,
                startTime = startTime,
                endTime = endTime,
            ),
        )
    }

    fun toggle(id: String, done: Boolean) {
        val idx = todos.indexOfFirst { it.id == id }
        if (idx >= 0) todos[idx] = todos[idx].copy(done = done)
    }

    fun update(
        id: String,
        text: String,
        priority: Priority,
        startTime: LocalTime?,
        endTime: LocalTime?,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val idx = todos.indexOfFirst { it.id == id }
        if (idx >= 0) {
            todos[idx] = todos[idx].copy(
                text = trimmed,
                priority = priority,
                startTime = startTime,
                endTime = endTime,
            )
        }
    }

    fun delete(id: String) {
        todos.removeAll { it.id == id }
    }

    fun todosFor(scope: Scope): List<TodoEntity> {
        val anchor = scope.anchorDateOf(selectedDate)
        val list = todos.filter { it.scope == scope && it.targetDate == anchor }
        return if (sortByPriority) {
            list.sortedWith(
                compareBy<TodoEntity> { it.done }
                    .thenByDescending { priorityWeight(it.priority) }
                    .thenByDescending { it.createdAt },
            )
        } else {
            list
        }
    }

    val dayDates: Set<LocalDate>
        get() = todos.filter { it.scope == Scope.DAY }.map { it.targetDate }.toSet()
}

private fun priorityWeight(p: Priority): Int = when (p) {
    Priority.HIGH -> 3
    Priority.MEDIUM -> 2
    Priority.LOW -> 1
    Priority.NONE -> 0
}
