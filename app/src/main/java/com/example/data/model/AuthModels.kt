package com.example.data.model

enum class AuthProvider {
    GOOGLE,
    EMAIL
}

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val provider: AuthProvider = AuthProvider.EMAIL,
    val isNewUser: Boolean = false,
    val createdAt: Long = 0L
)

sealed interface AuthState {
    object Initial : AuthState
    object Loading : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
    object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}
