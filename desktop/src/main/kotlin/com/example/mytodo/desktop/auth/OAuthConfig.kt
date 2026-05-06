package com.example.mytodo.desktop.auth

object OAuthConfig {
    const val GOOGLE_CLIENT_ID = "REPLACE_WITH_DESKTOP_CLIENT_ID.apps.googleusercontent.com"

    const val FIREBASE_API_KEY = "AIzaSyDUlX59sxOkhKIOawQH-DEIHnA_eqnMFLQ"

    const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"

    const val FIREBASE_SIGN_IN_WITH_IDP_URL =
        "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp"
    const val FIREBASE_SECURE_TOKEN_URL =
        "https://securetoken.googleapis.com/v1/token"

    const val SCOPES = "openid email profile"
}
