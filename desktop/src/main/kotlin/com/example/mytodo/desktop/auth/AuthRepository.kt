package com.example.mytodo.desktop.auth

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthException(message: String) : RuntimeException(message)

class AuthRepository {
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val random = SecureRandom()

    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private val refreshLock = Mutex()
    private val refreshSkewSec = 60L

    suspend fun freshIdToken(): String {
        val s = _session.value ?: throw AuthException("No active session")
        val now = Instant.now().epochSecond
        if (s.firebaseTokenExpiryEpochSec - refreshSkewSec > now) return s.firebaseIdToken
        return refreshSession().firebaseIdToken
    }

    private suspend fun refreshSession(): AuthSession = refreshLock.withLock {
        val current = _session.value ?: throw AuthException("No active session")
        val now = Instant.now().epochSecond
        if (current.firebaseTokenExpiryEpochSec - refreshSkewSec > now) return current

        val updated = withContext(Dispatchers.IO) {
            val form = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to current.firebaseRefreshToken,
            ).entries.joinToString("&") { (k, v) -> "${urlEncode(k)}=${urlEncode(v)}" }
            val req = HttpRequest.newBuilder()
                .uri(URI("${OAuthConfig.FIREBASE_SECURE_TOKEN_URL}?key=${OAuthConfig.FIREBASE_API_KEY}"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build()
            val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() / 100 != 2) {
                throw AuthException("Token refresh failed (${resp.statusCode()}): ${resp.body()}")
            }
            val parsed = json.decodeFromString(FirebaseRefreshResponse.serializer(), resp.body())
            val newId = parsed.idToken ?: throw AuthException("Refresh missing id_token")
            val newRefresh = parsed.refreshToken ?: current.firebaseRefreshToken
            val expiresIn = parsed.expiresIn?.toLongOrNull() ?: 3600L
            current.copy(
                firebaseIdToken = newId,
                firebaseRefreshToken = newRefresh,
                firebaseTokenExpiryEpochSec = now + expiresIn,
            )
        }
        _session.value = updated
        updated
    }

    suspend fun signInWithGoogle(): AuthSession = withContext(Dispatchers.IO) {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = sha256Base64Url(codeVerifier)
        val state = randomString(24)

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val port = server.address.port
        val redirectUri = "http://127.0.0.1:$port/callback"

        val codeAwait = installCallbackHandler(server, expectedState = state)
        server.executor = null
        server.start()

        try {
            val authUrl = buildAuthUrl(redirectUri, codeChallenge, state)
            openBrowser(authUrl)
            val authCode = codeAwait.await()

            val tokenResp = exchangeCodeForGoogleTokens(authCode, redirectUri, codeVerifier)
            val googleIdToken = tokenResp.idToken
                ?: throw AuthException("Google did not return an id_token")

            val firebaseResp = signInWithIdpToFirebase(googleIdToken)
            val uid = firebaseResp.localId ?: throw AuthException("Firebase response missing localId")
            val firebaseIdToken = firebaseResp.idToken
                ?: throw AuthException("Firebase response missing idToken")
            val firebaseRefresh = firebaseResp.refreshToken
                ?: throw AuthException("Firebase response missing refreshToken")
            val expiresIn = firebaseResp.expiresIn?.toLongOrNull() ?: 3600L

            val sess = AuthSession(
                user = AuthUser(
                    uid = uid,
                    email = firebaseResp.email,
                    displayName = firebaseResp.displayName,
                    photoUrl = firebaseResp.photoUrl,
                ),
                firebaseIdToken = firebaseIdToken,
                firebaseRefreshToken = firebaseRefresh,
                firebaseTokenExpiryEpochSec = Instant.now().epochSecond + expiresIn,
            )
            _session.value = sess
            sess
        } finally {
            server.stop(0)
        }
    }

    fun signOut() {
        _session.value = null
    }

    private fun buildAuthUrl(redirectUri: String, codeChallenge: String, state: String): String {
        val params = mapOf(
            "response_type" to "code",
            "client_id" to OAuthConfig.GOOGLE_CLIENT_ID,
            "redirect_uri" to redirectUri,
            "scope" to OAuthConfig.SCOPES,
            "state" to state,
            "code_challenge" to codeChallenge,
            "code_challenge_method" to "S256",
            "access_type" to "offline",
            "prompt" to "consent",
        )
        return params.entries.joinToString("&", prefix = "${OAuthConfig.GOOGLE_AUTH_URL}?") { (k, v) ->
            "${urlEncode(k)}=${urlEncode(v)}"
        }
    }

    private fun installCallbackHandler(
        server: HttpServer,
        expectedState: String,
    ): Awaitable<String> {
        val deferred = Awaitable<String>()
        server.createContext("/callback") { exchange: HttpExchange ->
            try {
                val params = parseQuery(exchange.requestURI.rawQuery ?: "")
                val error = params["error"]
                if (error != null) {
                    respond(exchange, errorPageHtml("로그인이 취소되었거나 실패했습니다 ($error)"))
                    deferred.completeExceptionally(AuthException("Google sign-in failed: $error"))
                    return@createContext
                }
                if (params["state"] != expectedState) {
                    respond(exchange, errorPageHtml("state 검증 실패"))
                    deferred.completeExceptionally(AuthException("OAuth state mismatch"))
                    return@createContext
                }
                val code = params["code"]
                if (code.isNullOrEmpty()) {
                    respond(exchange, errorPageHtml("code 누락"))
                    deferred.completeExceptionally(AuthException("Missing code in OAuth callback"))
                    return@createContext
                }
                respond(exchange, successPageHtml())
                deferred.complete(code)
            } catch (t: Throwable) {
                deferred.completeExceptionally(t)
            }
        }
        return deferred
    }

    private fun respond(exchange: HttpExchange, html: String) {
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun successPageHtml(): String = """
        <!doctype html><html lang="ko"><head><meta charset="utf-8">
        <title>로그인 완료</title>
        <style>
          body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;
               background:linear-gradient(135deg,#5B4BFF,#FF3D9A);
               color:#fff;height:100vh;margin:0;display:flex;align-items:center;justify-content:center;}
          .card{background:rgba(255,255,255,.12);backdrop-filter:blur(12px);
                padding:48px 64px;border-radius:24px;text-align:center;}
          h1{margin:0 0 12px;font-weight:800;font-size:32px;} p{margin:0;opacity:.85;}
        </style></head><body><div class="card">
          <h1>로그인 완료</h1><p>이 창을 닫고 MyTodo로 돌아가세요.</p>
        </div></body></html>
    """.trimIndent()

    private fun errorPageHtml(message: String): String = """
        <!doctype html><html lang="ko"><head><meta charset="utf-8"><title>로그인 실패</title>
        <style>body{font-family:sans-serif;padding:48px;}</style></head>
        <body><h1>로그인 실패</h1><p>${escape(message)}</p></body></html>
    """.trimIndent()

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun openBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
            ) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (_: Throwable) {
            // fall back to OS-specific command
        }
        val os = System.getProperty("os.name").lowercase()
        val cmd = when {
            os.contains("win") -> arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
            os.contains("mac") -> arrayOf("open", url)
            else -> arrayOf("xdg-open", url)
        }
        Runtime.getRuntime().exec(cmd)
    }

    private fun exchangeCodeForGoogleTokens(
        code: String,
        redirectUri: String,
        codeVerifier: String,
    ): GoogleTokenResponse {
        val form = mapOf(
            "client_id" to OAuthConfig.GOOGLE_CLIENT_ID,
            "client_secret" to OAuthConfig.googleClientSecret,
            "code" to code,
            "redirect_uri" to redirectUri,
            "grant_type" to "authorization_code",
            "code_verifier" to codeVerifier,
        ).entries.joinToString("&") { (k, v) -> "${urlEncode(k)}=${urlEncode(v)}" }

        val req = HttpRequest.newBuilder()
            .uri(URI(OAuthConfig.GOOGLE_TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() / 100 != 2) {
            throw AuthException("Google token exchange failed (${resp.statusCode()}): ${resp.body()}")
        }
        return json.decodeFromString(GoogleTokenResponse.serializer(), resp.body())
    }

    private fun signInWithIdpToFirebase(googleIdToken: String): FirebaseSignInResponse {
        val postBody = "id_token=${urlEncode(googleIdToken)}&providerId=google.com"
        val request = FirebaseSignInRequest(
            postBody = postBody,
            requestUri = "http://127.0.0.1",
        )
        val body = json.encodeToString(FirebaseSignInRequest.serializer(), request)
        val req = HttpRequest.newBuilder()
            .uri(URI("${OAuthConfig.FIREBASE_SIGN_IN_WITH_IDP_URL}?key=${OAuthConfig.FIREBASE_API_KEY}"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() / 100 != 2) {
            throw AuthException("Firebase signInWithIdp failed (${resp.statusCode()}): ${resp.body()}")
        }
        return json.decodeFromString(FirebaseSignInResponse.serializer(), resp.body())
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Base64Url(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun randomString(length: Int): String {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).take(length)
    }

    private fun urlEncode(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8)

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) {
                null
            } else {
                java.net.URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) to
                    java.net.URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
            }
        }.toMap()
}

internal class Awaitable<T> {
    private var result: Result<T>? = null
    private var continuation: kotlin.coroutines.Continuation<T>? = null
    private val lock = Any()

    fun complete(value: T) {
        synchronized(lock) {
            if (result != null) return
            result = Result.success(value)
            continuation?.resume(value)
            continuation = null
        }
    }

    fun completeExceptionally(error: Throwable) {
        synchronized(lock) {
            if (result != null) return
            result = Result.failure(error)
            continuation?.resumeWithException(error)
            continuation = null
        }
    }

    suspend fun await(): T = suspendCancellableCoroutine { cont ->
        synchronized(lock) {
            val r = result
            if (r != null) {
                r.fold(
                    onSuccess = { cont.resume(it) },
                    onFailure = { cont.resumeWithException(it) },
                )
            } else {
                continuation = cont
            }
        }
    }
}
