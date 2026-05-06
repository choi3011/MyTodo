package com.example.mytodo.desktop.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mytodo.desktop.auth.AuthUser
import com.example.mytodo.desktop.data.Priority
import com.example.mytodo.desktop.data.Scope
import com.example.mytodo.desktop.data.TodoEntity
import com.example.mytodo.desktop.ui.components.AddTodoSheet
import com.example.mytodo.desktop.ui.components.CalendarPickerDialog
import com.example.mytodo.desktop.ui.components.EmptyState
import com.example.mytodo.desktop.ui.components.GradientPillButton
import com.example.mytodo.desktop.ui.components.TodoHero
import com.example.mytodo.desktop.ui.components.TodoRow
import com.example.mytodo.desktop.ui.components.accent
import com.example.mytodo.desktop.ui.components.addLabel
import com.example.mytodo.desktop.ui.components.tabLabel
import com.example.mytodo.desktop.theme.BrandCoral
import com.example.mytodo.desktop.theme.BrandIndigo
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodoScreen(
    state: TodoState = remember { TodoState() },
    user: AuthUser? = null,
    onSignOut: () -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { Scope.entries.size })
    val coroutineScope = rememberCoroutineScope()
    val currentScope = Scope.entries[pagerState.currentPage]
    var sheetOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UserMiniFab(user = user, onSignOut = onSignOut)
                    CalendarMiniFab(onClick = { calendarOpen = true })
                    GradientPillButton(
                        onClick = { sheetOpen = true },
                        icon = Icons.Rounded.Add,
                        label = currentScope.addLabel,
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                ScopeTabs(
                    selected = currentScope,
                    onSelect = { scope ->
                        coroutineScope.launch { pagerState.animateScrollToPage(scope.ordinal) }
                    },
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { Scope.entries[it].name },
                ) { page ->
                    val pageScope = Scope.entries[page]
                    val pageTodos = state.todosFor(pageScope)
                    ScopePage(
                        scope = pageScope,
                        selectedDate = state.selectedDate,
                        todos = pageTodos,
                        sortByPriority = state.sortByPriority,
                        filter = state.filter,
                        onFilterClick = { state.chooseFilter(it) },
                        onToggleSort = { state.toggleSort() },
                        onToggle = { id, done -> state.toggle(id, done) },
                        onDelete = { id -> state.delete(id) },
                        onEdit = { editingTodo = it },
                    )
                }
            }
        }

        AddTodoSheet(
            visible = sheetOpen,
            onDismiss = { sheetOpen = false },
            onSubmit = { text, priority, startTime, endTime ->
                state.add(text, priority, currentScope, startTime, endTime)
                sheetOpen = false
            },
        )

        val editing = editingTodo
        AddTodoSheet(
            visible = editing != null,
            onDismiss = { editingTodo = null },
            onSubmit = { text, priority, startTime, endTime ->
                editing?.let { state.update(it.id, text, priority, startTime, endTime) }
                editingTodo = null
            },
            title = "할 일 수정",
            submitLabel = "저장",
            initialText = editing?.text ?: "",
            initialPriority = editing?.priority ?: Priority.NONE,
            initialStartTime = editing?.startTime,
            initialEndTime = editing?.endTime,
        )

        CalendarPickerDialog(
            visible = calendarOpen,
            selectedDate = state.selectedDate,
            dayDates = state.dayDates,
            onDismiss = { calendarOpen = false },
            onSelect = { state.chooseDate(it) },
        )
    }
}

@Composable
private fun UserMiniFab(user: AuthUser?, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val initial = user?.displayName?.firstOrNull()?.uppercase()
        ?: user?.email?.firstOrNull()?.uppercase()
        ?: "?"
    Box {
        Surface(
            onClick = { menuOpen = true },
            shape = CircleShape,
            color = BrandIndigo,
            shadowElevation = 6.dp,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = user?.email ?: "익명",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {},
                enabled = false,
            )
            DropdownMenuItem(
                text = { Text("로그아웃", color = BrandCoral) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = BrandCoral,
                    )
                },
                onClick = {
                    menuOpen = false
                    onSignOut()
                },
            )
        }
    }
}

@Composable
private fun CalendarMiniFab(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = "캘린더",
                tint = BrandIndigo,
            )
        }
    }
}

@Composable
private fun ScopePage(
    scope: Scope,
    selectedDate: LocalDate,
    todos: List<TodoEntity>,
    sortByPriority: Boolean,
    filter: TodoFilter,
    onFilterClick: (TodoFilter) -> Unit,
    onToggleSort: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (TodoEntity) -> Unit,
) {
    val active = todos.count { !it.done }
    val displayed = when (filter) {
        TodoFilter.ALL -> todos
        TodoFilter.ACTIVE -> todos.filter { !it.done }
        TodoFilter.DONE -> todos.filter { it.done }
    }
    if (todos.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TodoHero(
                scope = scope,
                selectedDate = selectedDate,
                activeCount = 0,
                doneCount = 0,
                filter = filter,
                onFilterClick = onFilterClick,
            )
            EmptyState(
                scope = scope,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 160.dp),
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            item(key = "hero-${scope.name}") {
                TodoHero(
                    scope = scope,
                    selectedDate = selectedDate,
                    activeCount = active,
                    doneCount = todos.size - active,
                    filter = filter,
                    onFilterClick = onFilterClick,
                )
            }
            item(key = "sort-${scope.name}") {
                SortToggleRow(
                    scope = scope,
                    active = sortByPriority,
                    onToggle = onToggleSort,
                )
            }
            items(displayed, key = { it.id }) { todo ->
                TodoRow(
                    todo = todo,
                    onToggle = { onToggle(todo.id, it) },
                    onDelete = { onDelete(todo.id) },
                    onEdit = { onEdit(todo) },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun SortToggleRow(
    scope: Scope,
    active: Boolean,
    onToggle: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = active,
            onClick = onToggle,
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = null,
                    modifier = Modifier.padding(0.dp),
                )
            },
            label = { Text(if (active) "우선순위 순" else "최신 순") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = scope.accent.copy(alpha = 0.15f),
                selectedLabelColor = scope.accent,
                selectedLeadingIconColor = scope.accent,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = active,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = scope.accent,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopeTabs(
    selected: Scope,
    onSelect: (Scope) -> Unit,
) {
    val scopes = Scope.entries
    val selectedIndex = scopes.indexOf(selected)
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = selected.accent,
        divider = {},
    ) {
        scopes.forEach { s ->
            val isSel = s == selected
            Tab(
                selected = isSel,
                onClick = { onSelect(s) },
                text = {
                    Text(
                        text = s.tabLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                },
                selectedContentColor = s.accent,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
