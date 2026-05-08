package com.example.mytodo.desktop.auth

import java.io.File
import java.util.Properties

object OAuthConfig {
    const val GOOGLE_CLIENT_ID =
        "227833939273-7nrvuega1satca13dk7c1ggaj13bqg09.apps.googleusercontent.com"

    const val FIREBASE_API_KEY = "AIzaSyDUlX59sxOkhKIOawQH-DEIHnA_eqnMFLQ"

    const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"

    const val FIREBASE_SIGN_IN_WITH_IDP_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp"
    const val FIREBASE_SECURE_TOKEN_URL =
        "https://securetoken.googleapis.com/v1/token"

    const val SCOPES = "openid email profile"

    val googleClientSecret: String by lazy {
        loadProperty("google.oauth.client_secret")
            ?: throw IllegalStateException(
                "google.oauth.client_secret missing — set it in either " +
                    "<repo>/local.properties (dev) or " +
                    "~/.mytodo/config.properties (installed)",
            )
    }

    private fun loadProperty(key: String): String? {
        val file = findConfigFile() ?: return null
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.getProperty(key)
    }

    private fun findConfigFile(): File? {
        // Dev: walk up from cwd looking for local.properties
        var dir: File? = File(".").canonicalFile
        while (dir != null) {
            val candidate = File(dir, "local.properties")
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }
        // Installed: ~/.mytodo/config.properties
        val home = System.getProperty("user.home") ?: return null
        val installedConfig = File(home, ".mytodo/config.properties")
        return if (installedConfig.exists()) installedConfig else null
    }
}
