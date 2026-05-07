package com.example.mytodo.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.mytodo.desktop.auth.AuthRepository
import com.example.mytodo.desktop.data.TodoRepository
import com.example.mytodo.desktop.theme.MyTodoTheme
import com.example.mytodo.desktop.ui.LoginScreen
import com.example.mytodo.desktop.ui.TodoScreen
import com.example.mytodo.desktop.ui.TodoState
import kotlinx.coroutines.launch

fun main() = application {
    val authRepo = remember { AuthRepository() }
    val todoRepo = remember { TodoRepository(authRepo) }
    val windowState = rememberWindowState(width = 480.dp, height = 820.dp)

    var bootstrapped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { authRepo.restoreSession() }
        bootstrapped = true
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MyTodo",
        icon = painterResource("icon.png"),
        state = windowState,
    ) {
        MyTodoTheme(darkTheme = false) {
            val session by authRepo.session.collectAsState()
            val scope = rememberCoroutineScope()
            var signingIn by remember { mutableStateOf(false) }
            var signInError by remember { mutableStateOf<String?>(null) }

            if (!bootstrapped) {
                // brief loading state — leave the window blank during restore
                return@MyTodoTheme
            }

            if (session == null) {
                LoginScreen(
                    onSignInClick = {
                        if (signingIn) return@LoginScreen
                        signInError = null
                        signingIn = true
                        scope.launch {
                            try {
                                authRepo.signInWithGoogle()
                            } catch (t: Throwable) {
                                signInError = t.message
                            } finally {
                                signingIn = false
                            }
                        }
                    },
                    isSigningIn = signingIn,
                    errorMessage = signInError,
                )
            } else {
                val todoState = remember(session?.user?.uid) {
                    TodoState(todoRepo, scope)
                }
                DisposableEffect(todoState) {
                    onDispose { todoState.stop() }
                }
                LaunchedEffect(todoState) {
                    todoState.refreshDayDates()
                }
                val windowInfo = LocalWindowInfo.current
                LaunchedEffect(windowState.isMinimized, windowInfo.isWindowFocused) {
                    todoState.setVisible(!windowState.isMinimized && windowInfo.isWindowFocused)
                }
                TodoScreen(
                    state = todoState,
                    user = session?.user,
                    onSignOut = { scope.launch { authRepo.signOut() } },
                )
            }
        }
    }
}
