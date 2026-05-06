package com.example.mytodo.data

import com.example.mytodo.data.local.Priority
import com.example.mytodo.data.local.Scope
import com.example.mytodo.data.local.TodoEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime

class TodoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun userTodos(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection("todos")
    }

    fun observeByScopeAndDate(scope: Scope, anchorDate: LocalDate): Flow<List<TodoEntity>> = callbackFlow {
        val ref = userTodos()
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = ref
            .whereEqualTo("scope", scope.name)
            .whereEqualTo("targetDate", anchorDate.toString())
            .addSnapshotListener { snap, _ ->
                if (snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap.documents.mapNotNull { it.toTodo() }
                    .sortedWith(compareBy<TodoEntity> { it.done }.thenByDescending { it.createdAt })
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeDayDates(): Flow<List<LocalDate>> = callbackFlow {
        val ref = userTodos()
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = ref
            .whereEqualTo("scope", Scope.DAY.name)
            .addSnapshotListener { snap, _ ->
                if (snap == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val dates = snap.documents.mapNotNull {
                    it.getString("targetDate")
                        ?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull() }
                }.distinct()
                trySend(dates)
            }
        awaitClose { reg.remove() }
    }

    suspend fun add(
        text: String,
        priority: Priority = Priority.NONE,
        scope: Scope = Scope.DAY,
        targetDate: LocalDate = LocalDate.now(),
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
    ): String? {
        val ref = userTodos() ?: return null
        val data = mapOf(
            "text" to text,
            "done" to false,
            "priority" to priority.name,
            "scope" to scope.name,
            "targetDate" to targetDate.toString(),
            "startTime" to startTime?.toString(),
            "endTime" to endTime?.toString(),
            "createdAt" to System.currentTimeMillis(),
        )
        val doc = ref.document()
        doc.set(data).await()
        return doc.id
    }

    suspend fun setDone(id: String, done: Boolean) {
        userTodos()?.document(id)?.update("done", done)?.await()
    }

    suspend fun update(
        id: String,
        text: String,
        priority: Priority,
        startTime: LocalTime?,
        endTime: LocalTime?,
    ) {
        userTodos()?.document(id)?.update(
            mapOf(
                "text" to text,
                "priority" to priority.name,
                "startTime" to startTime?.toString(),
                "endTime" to endTime?.toString(),
            ),
        )?.await()
    }

    suspend fun delete(id: String) {
        userTodos()?.document(id)?.delete()?.await()
    }
}

private fun DocumentSnapshot.toTodo(): TodoEntity? {
    val text = getString("text") ?: return null
    val done = getBoolean("done") ?: false
    val priority = runCatching { Priority.valueOf(getString("priority") ?: Priority.NONE.name) }
        .getOrDefault(Priority.NONE)
    val scope = runCatching { Scope.valueOf(getString("scope") ?: Scope.DAY.name) }
        .getOrDefault(Scope.DAY)
    val targetDate = runCatching { LocalDate.parse(getString("targetDate") ?: "") }
        .getOrDefault(LocalDate.now())
    val startTime = getString("startTime")?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val endTime = getString("endTime")?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    return TodoEntity(
        id = id,
        text = text,
        done = done,
        priority = priority,
        scope = scope,
        targetDate = targetDate,
        startTime = startTime,
        endTime = endTime,
        createdAt = createdAt,
    )
}
