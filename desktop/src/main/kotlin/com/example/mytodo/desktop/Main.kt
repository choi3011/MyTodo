package com.example.mytodo.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.mytodo.desktop.theme.MyTodoTheme
import com.example.mytodo.desktop.ui.TodoScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MyTodo",
        state = rememberWindowState(width = 480.dp, height = 820.dp),
    ) {
        MyTodoTheme(darkTheme = false) {
            TodoScreen()
        }
    }
}
