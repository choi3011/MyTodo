package com.example.mytodo.desktop.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

@Serializable
data class AuthSession(
    val user: AuthUser,
    val firebaseIdToken: String,
    val firebaseRefreshToken: String,
    val firebaseTokenExpiryEpochSec: Long,
)

@Serializable
internal data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
internal data class FirebaseSignInRequest(
    val postBody: String,
    val requestUri: String,
    val returnIdpCredential: Boolean = true,
    val returnSecureToken: Boolean = true,
)

@Serializable
internal data class FirebaseSignInResponse(
    val localId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: String? = null,
    val error: FirebaseError? = null,
)

@Serializable
internal data class FirebaseError(
    val code: Int,
    val message: String,
)

@Serializable
internal data class FirebaseRefreshResponse(
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: String? = null,
)
