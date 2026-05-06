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
import java.time.LocalDate
import java.time.LocalTime

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
