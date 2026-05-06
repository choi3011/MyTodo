package com.example.mytodo.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.mytodo.desktop.auth.AuthRepository
import com.example.mytodo.desktop.theme.MyTodoTheme
import com.example.mytodo.desktop.ui.LoginScreen
import com.example.mytodo.desktop.ui.TodoScreen
import kotlinx.coroutines.launch

fun main() = application {
    val authRepo = remember { AuthRepository() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MyTodo",
        state = rememberWindowState(width = 480.dp, height = 820.dp),
    ) {
        MyTodoTheme(darkTheme = false) {
            val session by authRepo.session.collectAsState()
            val scope = rememberCoroutineScope()
            var signingIn by remember { mutableStateOf(false) }
            var signInError by remember { mutableStateOf<String?>(null) }

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
                TodoScreen(
                    user = session?.user,
                    onSignOut = { authRepo.signOut() },
                )
            }
        }
    }
}
