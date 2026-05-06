package com.example.mytodo.desktop.data

import com.example.mytodo.desktop.auth.AuthRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class TodoRepository(
    private val auth: AuthRepository,
    private val firestore: FirestoreClient = FirestoreClient(auth),
) {
    private fun userTodosPath(uid: String): String = "users/$uid/todos"

    private fun currentUid(): String? = auth.session.value?.user?.uid

    suspend fun fetchDayDates(): Set<LocalDate> {
        val uid = currentUid() ?: return emptySet()
        val docs = firestore.runQuery(
            parentDocumentPath = "users/$uid",
            collectionId = "todos",
            fieldEquals = listOf("scope" to stringV(Scope.DAY.name)),
        )
        return docs.mapNotNull { doc ->
            doc.fields["targetDate"]?.asString()
                ?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull() }
        }.toSet()
    }

    suspend fun fetchByScopeAndDate(scope: Scope, anchor: LocalDate): List<TodoEntity> {
        val uid = currentUid() ?: return emptyList()
        val docs = firestore.runQuery(
            parentDocumentPath = "users/$uid",
            collectionId = "todos",
            fieldEquals = listOf(
                "scope" to stringV(scope.name),
                "targetDate" to stringV(anchor.toString()),
            ),
        )
        return docs.mapNotNull { it.toTodo() }
    }

    suspend fun add(
        text: String,
        priority: Priority,
        scope: Scope,
        targetDate: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
    ): TodoEntity? {
        val uid = currentUid() ?: return null
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val fields = mapOf(
            "text" to stringV(text),
            "done" to boolV(false),
            "priority" to stringV(priority.name),
            "scope" to stringV(scope.name),
            "targetDate" to stringV(targetDate.toString()),
            "startTime" to stringV(startTime?.toString()),
            "endTime" to stringV(endTime?.toString()),
            "createdAt" to longV(createdAt),
        )
        firestore.createDocument(userTodosPath(uid), id, fields)
        return TodoEntity(
            id = id,
            text = text,
            done = false,
            priority = priority,
            scope = scope,
            targetDate = targetDate,
            startTime = startTime,
            endTime = endTime,
            createdAt = createdAt,
        )
    }

    suspend fun setDone(id: String, done: Boolean) {
        val uid = currentUid() ?: return
        firestore.patchDocument(
            documentPath = "${userTodosPath(uid)}/$id",
            fields = mapOf("done" to boolV(done)),
        )
    }

    suspend fun update(
        id: String,
        text: String,
        priority: Priority,
        startTime: LocalTime?,
        endTime: LocalTime?,
    ) {
        val uid = currentUid() ?: return
        firestore.patchDocument(
            documentPath = "${userTodosPath(uid)}/$id",
            fields = mapOf(
                "text" to stringV(text),
                "priority" to stringV(priority.name),
                "startTime" to stringV(startTime?.toString()),
                "endTime" to stringV(endTime?.toString()),
            ),
        )
    }

    suspend fun delete(id: String) {
        val uid = currentUid() ?: return
        firestore.deleteDocument("${userTodosPath(uid)}/$id")
    }
}

internal fun FirestoreDocument.toTodo(): TodoEntity? {
    val id = docId() ?: return null
    val text = fields["text"]?.asString() ?: return null
    val done = fields["done"]?.asBool() ?: false
    val priority = runCatching {
        Priority.valueOf(fields["priority"]?.asString() ?: Priority.NONE.name)
    }.getOrDefault(Priority.NONE)
    val scope = runCatching {
        Scope.valueOf(fields["scope"]?.asString() ?: Scope.DAY.name)
    }.getOrDefault(Scope.DAY)
    val targetDate = runCatching {
        LocalDate.parse(fields["targetDate"]?.asString() ?: "")
    }.getOrDefault(LocalDate.now())
    val startTime = fields["startTime"]?.asString()
        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val endTime = fields["endTime"]?.asString()
        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val createdAt = fields["createdAt"]?.asLong() ?: System.currentTimeMillis()
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
