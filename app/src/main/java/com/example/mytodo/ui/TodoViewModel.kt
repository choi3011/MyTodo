package com.example.mytodo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mytodo.MyTodoApplication
import com.example.mytodo.data.AuthRepository
import com.example.mytodo.data.TodoRepository
import com.example.mytodo.data.local.Priority
import com.example.mytodo.data.local.Scope
import com.example.mytodo.data.local.TodoEntity
import com.example.mytodo.ui.components.anchorDateOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class TodoFilter { ALL, ACTIVE, DONE }

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel(
    private val repo: TodoRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {
    private val _sortByPriority = MutableStateFlow(false)
    val sortByPriority: StateFlow<Boolean> = _sortByPriority.asStateFlow()

    private val _filter = MutableStateFlow(TodoFilter.ALL)
    val filter: StateFlow<TodoFilter> = _filter.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = authRepo.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepo.current())

    private val authStateFlow: StateFlow<String?> = authRepo.currentUser
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepo.current()?.uid)

    private val flows: Map<Scope, StateFlow<List<TodoEntity>>> =
        Scope.entries.associateWith { scope ->
            combine(authStateFlow, _selectedDate, _sortByPriority) { uid, date, byPriority ->
                Triple(uid, date, byPriority)
            }
                .flatMapLatest { (uid, date, byPriority) ->
                    if (uid == null) {
                        flowOf(emptyList())
                    } else {
                        val anchor = scope.anchorDateOf(date)
                        repo.observeByScopeAndDate(scope, anchor).map { list ->
                            if (byPriority) {
                                list.sortedWith(
                                    compareBy<TodoEntity> { it.done }
                                        .thenByDescending { priorityWeight(it.priority) }
                                        .thenByDescending { it.createdAt },
                                )
                            } else {
                                list
                            }
                        }
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    val dayDates: StateFlow<Set<LocalDate>> = authStateFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.observeDayDates()
        }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun todosFor(scope: Scope): StateFlow<List<TodoEntity>> = flows.getValue(scope)

    fun toggleSort() {
        _sortByPriority.value = !_sortByPriority.value
    }

    fun setFilter(f: TodoFilter) {
        _filter.value = f
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
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
        val anchor = scope.anchorDateOf(_selectedDate.value)
        viewModelScope.launch { repo.add(trimmed, priority, scope, anchor, startTime, endTime) }
    }

    fun toggle(id: String, done: Boolean) {
        viewModelScope.launch { repo.setDone(id, done) }
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
        viewModelScope.launch { repo.update(id, trimmed, priority, startTime, endTime) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun signOut() {
        viewModelScope.launch { authRepo.signOut() }
    }

    // ====== Overdue + Weekly ======

    private val _overdueTodos = MutableStateFlow<List<TodoEntity>>(emptyList())
    val overdueTodos: StateFlow<List<TodoEntity>> = _overdueTodos.asStateFlow()

    private val _overdueLoading = MutableStateFlow(false)
    val overdueLoading: StateFlow<Boolean> = _overdueLoading.asStateFlow()

    private val _weekStart = MutableStateFlow(currentWeekStart())
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()

    private val _weekTodos = MutableStateFlow<Map<LocalDate, List<TodoEntity>>>(emptyMap())
    val weekTodos: StateFlow<Map<LocalDate, List<TodoEntity>>> = _weekTodos.asStateFlow()

    private val _weekLoading = MutableStateFlow(false)
    val weekLoading: StateFlow<Boolean> = _weekLoading.asStateFlow()

    fun loadOverdue() {
        if (_overdueLoading.value) return
        _overdueLoading.value = true
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val result = repo.fetchOverdueIncomplete(today)
                android.util.Log.d("MyTodoDiag", "loadOverdue: today=$today, count=${result.size}")
                _overdueTodos.value = result
            } catch (e: Throwable) {
                android.util.Log.e("MyTodoDiag", "loadOverdue failed: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                _overdueLoading.value = false
            }
        }
    }

    fun loadWeek() {
        if (_weekLoading.value) return
        val start = _weekStart.value
        _weekLoading.value = true
        viewModelScope.launch {
            try {
                val list = repo.fetchWeek(start)
                android.util.Log.d("MyTodoDiag", "loadWeek: weekStart=$start, count=${list.size}")
                _weekTodos.value = (0..6).associate { i ->
                    val day = start.plusDays(i.toLong())
                    day to list.filter { it.targetDate == day }
                }
            } catch (e: Throwable) {
                android.util.Log.e("MyTodoDiag", "loadWeek failed: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                _weekLoading.value = false
            }
        }
    }

    fun previousWeek() {
        _weekStart.value = _weekStart.value.minusWeeks(1)
        loadWeek()
    }

    fun nextWeek() {
        _weekStart.value = _weekStart.value.plusWeeks(1)
        loadWeek()
    }

    fun resetWeek() {
        _weekStart.value = currentWeekStart()
        loadWeek()
    }

    fun markDoneFromOverdue(id: String) {
        _overdueTodos.value = _overdueTodos.value.filter { it.id != id }
        viewModelScope.launch { repo.setDone(id, true) }
    }

    fun reanchorFromOverdue(todo: TodoEntity) {
        _overdueTodos.value = _overdueTodos.value.filter { it.id != todo.id }
        val newAnchor = todo.scope.anchorDateOf(LocalDate.now())
        viewModelScope.launch { repo.reanchor(todo.id, newAnchor) }
    }

    fun toggleInWeek(id: String, done: Boolean) {
        _weekTodos.value = _weekTodos.value.mapValues { (_, list) ->
            list.map { if (it.id == id) it.copy(done = done) else it }
        }
        viewModelScope.launch { repo.setDone(id, done) }
    }

    private fun currentWeekStart(): LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyTodoApplication
                TodoViewModel(app.repository, app.authRepo)
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
