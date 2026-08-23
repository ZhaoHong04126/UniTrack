package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.AuthProvider
import com.example.data.model.AuthState
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val tag = "AuthRepository"

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                Log.w(tag, "FirebaseApp is not initialized. Running in offline/demo auth mode.")
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase initialization check failed: ${e.message}")
            null
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        // Initial state sync
        firebaseAuth?.let { auth ->
            auth.addAuthStateListener { fbAuth ->
                val user = fbAuth.currentUser
                if (user != null) {
                    val profile = user.toUserProfile()
                    _currentUser.value = profile
                    _authState.value = AuthState.Authenticated(profile)
                } else {
                    _currentUser.value = null
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Google Sign-In via Android Credential Manager
     */
    suspend fun signInWithGoogle(webClientId: String = ""): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading

            val auth = firebaseAuth

            // Dynamically resolve default_web_client_id generated from google-services.json if not explicitly provided
            val resolvedClientId = if (webClientId.isNotBlank()) {
                webClientId
            } else {
                try {
                    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    if (resId != 0) context.getString(resId) else ""
                } catch (e: Exception) {
                    ""
                }
            }

            if (auth == null || resolvedClientId.isBlank()) {
                // If Firebase / Web Client ID is not configured, supply simulated sign-in for preview/testing
                val demoUser = UserProfile(
                    uid = "google_demo_${System.currentTimeMillis()}",
                    displayName = "Google 測試學員",
                    email = "student@gmail.com",
                    photoUrl = null,
                    isAnonymous = false,
                    provider = AuthProvider.GOOGLE
                )
                _currentUser.value = demoUser
                _authState.value = AuthState.Authenticated(demoUser)
                return@withContext Result.success(demoUser)
            }

            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(resolvedClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            val idToken = when {
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                }
                else -> {
                    credential.data.getString("androidx.credentials.BUNDLE_KEY_ID_TOKEN")
                }
            } ?: throw IllegalStateException("未能取得 Google ID Token")

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            val fbUser = authResult.user ?: throw IllegalStateException("Firebase 登入失敗")
            val profile = fbUser.toUserProfile(AuthProvider.GOOGLE)
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: GetCredentialCancellationException) {
            _authState.value = _currentUser.value?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
            Result.failure(Exception("已取消 Google 登入"))
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In failed", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Google 登入失敗")
            Result.failure(e)
        }
    }

    /**
     * Email / Password Login
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val auth = firebaseAuth
            if (auth == null) {
                // Offline fallback
                val demoUser = UserProfile(
                    uid = "email_user_${System.currentTimeMillis()}",
                    displayName = email.substringBefore("@"),
                    email = email,
                    isAnonymous = false,
                    provider = AuthProvider.EMAIL
                )
                _currentUser.value = demoUser
                _authState.value = AuthState.Authenticated(demoUser)
                return@withContext Result.success(demoUser)
            }

            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user ?: throw IllegalStateException("登入失敗")
            val profile = fbUser.toUserProfile(AuthProvider.EMAIL)
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            val message = mapFirebaseAuthException(e)
            _authState.value = AuthState.Error(message)
            Result.failure(Exception(message))
        }
    }

    /**
     * Email / Password Registration
     */
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val auth = firebaseAuth
            if (auth == null) {
                val demoUser = UserProfile(
                    uid = "reg_user_${System.currentTimeMillis()}",
                    displayName = name.ifBlank { email.substringBefore("@") },
                    email = email,
                    isAnonymous = false,
                    provider = AuthProvider.EMAIL
                )
                _currentUser.value = demoUser
                _authState.value = AuthState.Authenticated(demoUser)
                return@withContext Result.success(demoUser)
            }

            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user ?: throw IllegalStateException("註冊失敗")

            if (name.isNotBlank()) {
                val profileUpdates = userProfileChangeRequest {
                    displayName = name.trim()
                }
                fbUser.updateProfile(profileUpdates).await()
            }

            val profile = UserProfile(
                uid = fbUser.uid,
                displayName = name.ifBlank { fbUser.displayName ?: email.substringBefore("@") },
                email = fbUser.email,
                photoUrl = fbUser.photoUrl?.toString(),
                isAnonymous = false,
                provider = AuthProvider.EMAIL
            )
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            val message = mapFirebaseAuthException(e)
            _authState.value = AuthState.Error(message)
            Result.failure(Exception(message))
        }
    }

    /**
     * Password Reset Email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth
            if (auth == null) {
                return@withContext Result.success(Unit)
            }
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseAuthException(e)))
        }
    }

    /**
     * Anonymous / Guest Login
     */
    suspend fun signInAsGuest(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val auth = firebaseAuth
            val profile = if (auth != null) {
                val result = auth.signInAnonymously().await()
                val fbUser = result.user!!
                UserProfile(
                    uid = fbUser.uid,
                    displayName = "訪客同學",
                    email = null,
                    photoUrl = null,
                    isAnonymous = true,
                    provider = AuthProvider.GUEST
                )
            } else {
                UserProfile(
                    uid = "guest_${System.currentTimeMillis()}",
                    displayName = "訪客同學",
                    email = null,
                    photoUrl = null,
                    isAnonymous = true,
                    provider = AuthProvider.GUEST
                )
            }
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "訪客登入失敗")
            Result.failure(e)
        }
    }

    /**
     * Sign Out
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            firebaseAuth?.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(tag, "Clear credential state failed: ${e.message}")
        } finally {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = _currentUser.value?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
        }
    }

    private fun FirebaseUser.toUserProfile(provider: AuthProvider = AuthProvider.EMAIL): UserProfile {
        return UserProfile(
            uid = this.uid,
            displayName = this.displayName ?: this.email?.substringBefore("@") ?: "同學",
            email = this.email,
            photoUrl = this.photoUrl?.toString(),
            isAnonymous = this.isAnonymous,
            provider = if (this.isAnonymous) AuthProvider.GUEST else provider
        )
    }

    private fun mapFirebaseAuthException(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("password", ignoreCase = true) && msg.contains("invalid", ignoreCase = true) -> "密碼錯誤，請重新輸入"
            msg.contains("user-not-found", ignoreCase = true) || msg.contains("no user", ignoreCase = true) -> "此帳號不存在，請先註冊"
            msg.contains("email-already-in-use", ignoreCase = true) -> "此電子郵件已被註冊"
            msg.contains("invalid-email", ignoreCase = true) -> "電子郵件格式不正確"
            msg.contains("weak-password", ignoreCase = true) -> "密碼長度需至少 6 個字元"
            msg.contains("network", ignoreCase = true) -> "網路連線異常，請檢查網路狀態"
            else -> e.localizedMessage ?: "驗證發生錯誤，請稍後再試"
        }
    }
}
