package com.example.mytodo

import android.app.Application
import com.example.mytodo.data.AuthRepository
import com.example.mytodo.data.TodoRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class MyTodoApplication : Application() {
    val authRepo: AuthRepository by lazy { AuthRepository(this) }
    val repository: TodoRepository by lazy { TodoRepository() }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
    }
}
