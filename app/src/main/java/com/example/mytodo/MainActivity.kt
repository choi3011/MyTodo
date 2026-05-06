package com.example.mytodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.mytodo.ui.LoginScreen
import com.example.mytodo.ui.TodoScreen
import com.example.mytodo.ui.theme.MyTodoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as MyTodoApplication
        val authRepo = app.authRepo
        setContent {
            MyTodoTheme(darkTheme = false) {
                val user by authRepo.currentUser.collectAsState(initial = authRepo.current())
                var signInError by remember { mutableStateOf<String?>(null) }
                if (user == null) {
                    LoginScreen(
                        onSignInClick = {
                            signInError = null
                            lifecycleScope.launch {
                                val result = authRepo.signInWithGoogle(this@MainActivity)
                                signInError = result.exceptionOrNull()?.message
                            }
                        },
                        errorMessage = signInError,
                    )
                } else {
                    TodoScreen()
                }
            }
        }
    }
}
