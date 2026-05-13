package com.example.mytodo.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.mytodo.desktop.data.Priority
import com.example.mytodo.desktop.data.Scope
import com.example.mytodo.desktop.data.TodoEntity
import com.example.mytodo.desktop.data.TodoRepository
import com.example.mytodo.desktop.ui.components.anchorDateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class TodoFilter { ALL, ACTIVE, DONE }

class TodoState(
    private val repo: TodoRepository,
    private val scope: CoroutineScope,
) {
    private data class CacheKey(val scope: Scope, val anchor: LocalDate)

    var sortByPriority by mutableStateOf(false)
        private set
    var filter by mutableStateOf(TodoFilter.ALL)
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    private val cache: MutableMap<CacheKey, SnapshotStateList<TodoEntity>> = mutableMapOf()
    private val lastRefreshAt: MutableMap<CacheKey, Long> = mutableMapOf()
    private var activeKey: CacheKey? = null
    private var activeJob: Job? = null
    private var visible: Boolean = true

    private val _dayDates = mutableStateOf<Set<LocalDate>>(emptySet())
    val dayDates: Set<LocalDate> by _dayDates

    private val debounceMs = 100L
    private val pollIntervalMs = 15_000L

    fun setActive(scope: Scope) {
        val anchor = scope.anchorDateOf(selectedDate)
        val key = CacheKey(scope, anchor)
        if (activeKey == key && activeJob?.isActive == true) return
        activeKey = key
        cache.getOrPut(key) { mutableStateListOf() }
        if (visible) restartPolling(key)
    }

    fun setVisible(v: Boolean) {
        if (visible == v) return
        visible = v
        val key = activeKey
        if (v && key != null) {
            restartPolling(key)
        } else {
            activeJob?.cancel()
            activeJob = null
        }
    }

    private fun restartPolling(key: CacheKey) {
        activeJob?.cancel()
        activeJob = this.scope.launch {
            delay(debounceMs)
            val sinceLast = System.currentTimeMillis() - (lastRefreshAt[key] ?: 0L)
            if (sinceLast < pollIntervalMs) {
                delay(pollIntervalMs - sinceLast)
            }
            while (isActive) {
                refresh(key)
                lastRefreshAt[key] = System.currentTimeMillis()
                delay(pollIntervalMs)
            }
        }
    }

    fun refreshDayDates() {
        scope.launch {
            try {
                _dayDates.value = repo.fetchDayDates()
            } catch (_: Throwable) {
                // keep stale; calendar dots simply don't refresh this time
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        activeKey = null
        cache.clear()
        lastRefreshAt.clear()
        _dayDates.value = emptySet()
    }

    private suspend fun refresh(key: CacheKey) {
        try {
            val remote = repo.fetchByScopeAndDate(key.scope, key.anchor)
            val list = cache.getOrPut(key) { mutableStateListOf() }
            list.clear()
            list.addAll(remote)
            loadError = null
        } catch (t: Throwable) {
            loadError = t.message
        }
    }

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
        val key = CacheKey(scope, anchor)
        this.scope.launch {
            try {
                val created = repo.add(trimmed, priority, scope, anchor, startTime, endTime)
                if (created != null) {
                    cache.getOrPut(key) { mutableStateListOf() }.add(created)
                    if (scope == Scope.DAY) {
                        _dayDates.value = _dayDates.value + anchor
                    }
                }
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
    }

    fun toggle(id: String, done: Boolean) {
        cache.values.forEach { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(done = done)
        }
        scope.launch {
            try {
                repo.setDone(id, done)
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
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
        cache.values.forEach { list ->
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(
                    text = trimmed,
                    priority = priority,
                    startTime = startTime,
                    endTime = endTime,
                )
            }
        }
        scope.launch {
            try {
                repo.update(id, trimmed, priority, startTime, endTime)
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
    }

    fun delete(id: String) {
        cache.values.forEach { list -> list.removeAll { it.id == id } }
        scope.launch {
            try {
                repo.delete(id)
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
    }

    fun todosFor(scope: Scope): List<TodoEntity> {
        val anchor = scope.anchorDateOf(selectedDate)
        val list = cache.getOrPut(CacheKey(scope, anchor)) { mutableStateListOf() }
        return if (sortByPriority) {
            list.sortedWith(
                compareBy<TodoEntity> { it.done }
                    .thenByDescending { priorityWeight(it.priority) }
                    .thenByDescending { it.createdAt },
            )
        } else {
            list.sortedWith(
                compareBy<TodoEntity> { it.done }
                    .thenByDescending { it.createdAt },
            )
        }
    }

    // ====== Overdue + Weekly ======

    var overdueTodos by mutableStateOf<List<TodoEntity>>(emptyList())
        private set
    var overdueLoading by mutableStateOf(false)
        private set
    var weekStart by mutableStateOf(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
        private set
    var weekTodos by mutableStateOf<Map<LocalDate, List<TodoEntity>>>(emptyMap())
        private set
    var weekLoading by mutableStateOf(false)
        private set

    fun loadOverdue() {
        if (overdueLoading) return
        overdueLoading = true
        scope.launch {
            try {
                overdueTodos = repo.fetchOverdueIncomplete(LocalDate.now())
                loadError = null
            } catch (t: Throwable) {
                loadError = t.message
            } finally {
                overdueLoading = false
            }
        }
    }

    fun loadWeek() {
        if (weekLoading) return
        val start = weekStart
        weekLoading = true
        scope.launch {
            try {
                val list = repo.fetchWeek(start)
                weekTodos = (0..6).associate { i ->
                    val day = start.plusDays(i.toLong())
                    day to list.filter { it.targetDate == day }
                }
                loadError = null
            } catch (t: Throwable) {
                loadError = t.message
            } finally {
                weekLoading = false
            }
        }
    }

    fun previousWeek() {
        weekStart = weekStart.minusWeeks(1)
        loadWeek()
    }

    fun nextWeek() {
        weekStart = weekStart.plusWeeks(1)
        loadWeek()
    }

    fun resetWeek() {
        weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        loadWeek()
    }

    fun markDoneFromOverdue(id: String) {
        overdueTodos = overdueTodos.filter { it.id != id }
        scope.launch {
            try {
                repo.setDone(id, true)
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
    }

    fun reanchorFromOverdue(todo: TodoEntity) {
        overdueTodos = overdueTodos.filter { it.id != todo.id }
        val newAnchor = todo.scope.anchorDateOf(LocalDate.now())
        scope.launch {
            try {
                repo.reanchor(todo.id, newAnchor)
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
    }

    fun toggleInWeek(id: String, done: Boolean) {
        weekTodos = weekTodos.mapValues { (_, list) ->
            list.map { if (it.id == id) it.copy(done = done) else it }
        }
        scope.launch {
            try {
                repo.setDone(id, done)
            } catch (t: Throwable) {
                loadError = "동기화 실패: ${t.message}"
            }
        }
    }

}

private fun priorityWeight(p: Priority): Int = when (p) {
    Priority.HIGH -> 3
    Priority.MEDIUM -> 2
    Priority.LOW -> 1
    Priority.NONE -> 0
}
