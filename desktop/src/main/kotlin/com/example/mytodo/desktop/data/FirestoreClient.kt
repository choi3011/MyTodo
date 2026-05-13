package com.example.mytodo.desktop.data

import com.example.mytodo.desktop.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

private const val PROJECT_ID = "mytodo-app-72f46"
private const val FIRESTORE_BASE =
    "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents"

class FirestoreException(message: String) : RuntimeException(message)

class FirestoreClient(
    private val auth: AuthRepository,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    suspend fun runQuery(
        parentDocumentPath: String,
        collectionId: String,
        fieldEquals: List<Pair<String, FirestoreValue>> = emptyList(),
        fieldFilters: List<Triple<String, String, FirestoreValue>> = emptyList(),
    ): List<FirestoreDocument> = withContext(Dispatchers.IO) {
        val token = auth.freshIdToken()
        val equalsFilters = fieldEquals.map { (path, value) ->
            Filter(
                fieldFilter = FieldFilter(
                    field = FieldRef(fieldPath = path),
                    op = "EQUAL",
                    value = value,
                ),
            )
        }
        val otherFilters = fieldFilters.map { (path, op, value) ->
            Filter(
                fieldFilter = FieldFilter(
                    field = FieldRef(fieldPath = path),
                    op = op,
                    value = value,
                ),
            )
        }
        val filters = equalsFilters + otherFilters
        val where = when {
            filters.isEmpty() -> null
            filters.size == 1 -> filters.first()
            else -> Filter(
                compositeFilter = CompositeFilter(op = "AND", filters = filters),
            )
        }
        val request = RunQueryRequest(
            structuredQuery = StructuredQuery(
                from = listOf(CollectionSelector(collectionId = collectionId)),
                where = where,
            ),
        )
        val body = json.encodeToString(RunQueryRequest.serializer(), request)
        val uri = "$FIRESTORE_BASE/$parentDocumentPath:runQuery"
        val req = HttpRequest.newBuilder()
            .uri(URI(uri))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() / 100 != 2) {
            throw FirestoreException("Firestore runQuery failed (${resp.statusCode()}): ${resp.body()}")
        }
        val parsed = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(RunQueryResponseEntry.serializer()),
            resp.body(),
        )
        parsed.mapNotNull { it.document }
    }

    suspend fun listCollection(path: String): List<FirestoreDocument> = withContext(Dispatchers.IO) {
        val token = auth.freshIdToken()
        val all = mutableListOf<FirestoreDocument>()
        var pageToken: String? = null
        do {
            val uri = buildString {
                append("$FIRESTORE_BASE/$path?pageSize=300")
                if (pageToken != null) append("&pageToken=${urlEncode(pageToken!!)}")
            }
            val req = HttpRequest.newBuilder()
                .uri(URI(uri))
                .header("Authorization", "Bearer $token")
                .GET()
                .build()
            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() == 404) return@withContext emptyList()
            if (resp.statusCode() / 100 != 2) {
                throw FirestoreException("Firestore list failed (${resp.statusCode()}): ${resp.body()}")
            }
            val parsed = json.decodeFromString(FirestoreListResponse.serializer(), resp.body())
            all.addAll(parsed.documents.orEmpty())
            pageToken = parsed.nextPageToken
        } while (!pageToken.isNullOrEmpty())
        all
    }

    suspend fun createDocument(
        path: String,
        documentId: String,
        fields: Map<String, FirestoreValue>,
    ): Unit = withContext(Dispatchers.IO) {
        val token = auth.freshIdToken()
        val body = json.encodeToString(
            FirestoreDocument.serializer(),
            FirestoreDocument(fields = fields),
        )
        val uri = "$FIRESTORE_BASE/$path?documentId=${urlEncode(documentId)}"
        val req = HttpRequest.newBuilder()
            .uri(URI(uri))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() / 100 != 2) {
            throw FirestoreException("Firestore create failed (${resp.statusCode()}): ${resp.body()}")
        }
    }

    suspend fun patchDocument(
        documentPath: String,
        fields: Map<String, FirestoreValue>,
    ): Unit = withContext(Dispatchers.IO) {
        val token = auth.freshIdToken()
        val body = json.encodeToString(
            FirestoreDocument.serializer(),
            FirestoreDocument(fields = fields),
        )
        val mask = fields.keys.joinToString("&") { "updateMask.fieldPaths=${urlEncode(it)}" }
        val uri = "$FIRESTORE_BASE/$documentPath?$mask"
        val req = HttpRequest.newBuilder()
            .uri(URI(uri))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() / 100 != 2) {
            throw FirestoreException("Firestore patch failed (${resp.statusCode()}): ${resp.body()}")
        }
    }

    suspend fun deleteDocument(documentPath: String): Unit = withContext(Dispatchers.IO) {
        val token = auth.freshIdToken()
        val req = HttpRequest.newBuilder()
            .uri(URI("$FIRESTORE_BASE/$documentPath"))
            .header("Authorization", "Bearer $token")
            .DELETE()
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() / 100 != 2 && resp.statusCode() != 404) {
            throw FirestoreException("Firestore delete failed (${resp.statusCode()}): ${resp.body()}")
        }
    }

    private fun urlEncode(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8)
}

@Serializable
data class FirestoreDocument(
    val name: String? = null,
    val fields: Map<String, FirestoreValue> = emptyMap(),
    val createTime: String? = null,
    val updateTime: String? = null,
)

@Serializable
data class FirestoreListResponse(
    val documents: List<FirestoreDocument>? = null,
    val nextPageToken: String? = null,
)

@Serializable
data class RunQueryRequest(val structuredQuery: StructuredQuery)

@Serializable
data class StructuredQuery(
    val from: List<CollectionSelector>? = null,
    val where: Filter? = null,
    val limit: Int? = null,
)

@Serializable
data class CollectionSelector(val collectionId: String)

@Serializable
data class Filter(
    val fieldFilter: FieldFilter? = null,
    val compositeFilter: CompositeFilter? = null,
)

@Serializable
data class FieldFilter(val field: FieldRef, val op: String, val value: FirestoreValue)

@Serializable
data class FieldRef(val fieldPath: String)

@Serializable
data class CompositeFilter(val op: String, val filters: List<Filter>)

@Serializable
data class RunQueryResponseEntry(
    val document: FirestoreDocument? = null,
    val readTime: String? = null,
)

@Serializable
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val booleanValue: Boolean? = null,
    val doubleValue: Double? = null,
    @SerialName("nullValue") val nullMarker: String? = null,
)

fun stringV(v: String?): FirestoreValue =
    if (v == null) FirestoreValue(nullMarker = "NULL_VALUE")
    else FirestoreValue(stringValue = v)

fun boolV(v: Boolean): FirestoreValue = FirestoreValue(booleanValue = v)

fun longV(v: Long): FirestoreValue = FirestoreValue(integerValue = v.toString())

fun FirestoreDocument.docId(): String? =
    name?.substringAfterLast('/')

fun FirestoreValue.asString(): String? = stringValue
fun FirestoreValue.asLong(): Long? = integerValue?.toLongOrNull()
fun FirestoreValue.asBool(): Boolean? = booleanValue
