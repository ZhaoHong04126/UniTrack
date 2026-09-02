package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
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
    @SuppressLint("DiscouragedApi")
    suspend fun signInWithGoogle(callerContext: Context? = null, webClientId: String = ""): Result<UserProfile> = withContext(Dispatchers.IO) {
        val targetContext = callerContext ?: context
        try {
            _authState.value = AuthState.Loading

            val auth = firebaseAuth

            val defaultServerClientId = "742695547810-74oes8t8piajd7uctvobfbcc2ttg93d7.apps.googleusercontent.com"
            // Dynamically resolve default_web_client_id generated from google-services.json if not explicitly provided
            val resolvedClientId = webClientId.ifBlank {
                try {
                    val resId = targetContext.resources.getIdentifier("default_web_client_id", "string", targetContext.packageName)
                    if (resId != 0) targetContext.getString(resId) else defaultServerClientId
                } catch (_: Exception) {
                    defaultServerClientId
                }
            }.ifBlank { defaultServerClientId }

            if (auth == null) {
                // If Firebase is not configured, supply simulated sign-in for preview/testing
                val demoUser = UserProfile(
                    uid = "google_demo_${System.currentTimeMillis()}",
                    displayName = "Google 測試學員",
                    email = "student@gmail.com",
                    photoUrl = null,
                    isAnonymous = false,
                    provider = AuthProvider.GOOGLE,
                    createdAt = getOrSetLocalFirstLoginTime("google_demo_user")
                )
                _currentUser.value = demoUser
                _authState.value = AuthState.Authenticated(demoUser)
                return@withContext Result.success(demoUser)
            }

            val credentialManager = CredentialManager.create(targetContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(resolvedClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(targetContext, request)
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
            val isNewUser = authResult.additionalUserInfo?.isNewUser == true
            val profile = fbUser.toUserProfile(AuthProvider.GOOGLE, isNewUser = isNewUser)
            _currentUser.value = profile
            _authState.value = AuthState.Authenticated(profile)
            Result.success(profile)
        } catch (_: GetCredentialCancellationException) {
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
                    provider = AuthProvider.EMAIL,
                    createdAt = getOrSetLocalFirstLoginTime(email)
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
                    provider = AuthProvider.EMAIL,
                    createdAt = getOrSetLocalFirstLoginTime(email)
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

            val fbCreationTimestamp = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
            val firstLoginTime = getOrSetLocalFirstLoginTime(fbUser.uid, fbCreationTimestamp)
            val profile = UserProfile(
                uid = fbUser.uid,
                displayName = name.ifBlank { fbUser.displayName ?: email.substringBefore("@") },
                email = fbUser.email,
                photoUrl = fbUser.photoUrl?.toString(),
                isAnonymous = false,
                provider = AuthProvider.EMAIL,
                isNewUser = true,
                createdAt = firstLoginTime
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
            val auth = firebaseAuth ?: return@withContext Result.success(Unit)
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseAuthException(e)))
        }
    }

    /**
     * Sign Out
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        try {
            firebaseAuth?.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(tag, "Clear credential state failed: ${e.message}")
        }
    }

    /**
     * 永久刪除 Firebase Auth 帳號 (Permanently delete Firebase Auth Account)
     */
    @SuppressLint("DiscouragedApi")
    suspend fun deleteAccount(callerContext: Context? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val targetContext = callerContext ?: context
        try {
            val auth = firebaseAuth ?: return@withContext Result.failure(IllegalStateException("Firebase 模組未初始化"))
            val currentFirebaseUser = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("目前未登入任何線上帳號"))

            try {
                currentFirebaseUser.delete().await()
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                val isRecentLoginRequired = e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException ||
                    (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode == "ERROR_REQUIRES_RECENT_LOGIN" ||
                    msg.contains("requires-recent-login", ignoreCase = true) ||
                    msg.contains("recent authentication", ignoreCase = true) ||
                    msg.contains("CREDENTIAL_TOO_OLD", ignoreCase = true)

                if (isRecentLoginRequired) {
                    val isGoogle = currentFirebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                    if (isGoogle) {
                        val resolvedClientId = try {
                            val resId = targetContext.resources.getIdentifier("default_web_client_id", "string", targetContext.packageName)
                            if (resId != 0) targetContext.getString(resId) else ""
                        } catch (_: Exception) { "" }

                        if (resolvedClientId.isNotBlank()) {
                            val credentialManager = CredentialManager.create(targetContext)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(resolvedClientId)
                                .setAutoSelectEnabled(false)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(targetContext, request)
                            val credential = result.credential
                            val idToken = when {
                                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                                }
                                else -> credential.data.getString("androidx.credentials.BUNDLE_KEY_ID_TOKEN")
                            } ?: throw IllegalStateException("未能取得 Google 重新驗證憑證")

                            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                            currentFirebaseUser.reauthenticate(authCredential).await()
                            currentFirebaseUser.delete().await()
                        } else {
                            throw Exception("此操作屬於敏感安全性操作，請先重新登入後再執行刪除帳號。")
                        }
                    } else {
                        throw Exception("此操作屬於敏感安全性操作，請先重新登入後再執行刪除帳號。")
                    }
                } else {
                    throw e
                }
            }

            val credentialManager = CredentialManager.create(targetContext)
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
            runCatching {
                auth.signOut()
            }

            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Delete Firebase account failed", e)
            val message = mapFirebaseAuthException(e)
            Result.failure(Exception(message))
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = _currentUser.value?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
        }
    }

    @Suppress("SpellCheckingInspection")
    fun getOrSetLocalFirstLoginTime(keyId: String, preferredTimestamp: Long = 0L): Long {
        val prefs = context.getSharedPreferences("unitrack_prefs", Context.MODE_PRIVATE)
        val safeKey = "first_login_time_${keyId.replace("[^a-zA-Z0-9_]".toRegex(), "_")}"
        val existing = prefs.getLong(safeKey, 0L)
        if (existing > 0L) {
            return existing
        }
        val targetTime = if (preferredTimestamp > 0L) {
            preferredTimestamp
        } else {
            val global = prefs.getLong("pref_first_login_time", 0L)
            if (global > 0L) global else System.currentTimeMillis()
        }
        prefs.edit {
            putLong(safeKey, targetTime)
            val global = prefs.getLong("pref_first_login_time", 0L)
            if (global == 0L) {
                putLong("pref_first_login_time", targetTime)
            }
        }
        return targetTime
    }

    private fun FirebaseUser.toUserProfile(provider: AuthProvider = AuthProvider.EMAIL, isNewUser: Boolean = false): UserProfile {
        val fbCreationTimestamp = this.metadata?.creationTimestamp ?: 0L
        val firstLoginTime = getOrSetLocalFirstLoginTime(this.uid, fbCreationTimestamp)
        return UserProfile(
            uid = this.uid,
            displayName = this.displayName ?: this.email?.substringBefore("@") ?: "同學",
            email = this.email,
            photoUrl = this.photoUrl?.toString(),
            isAnonymous = false,
            provider = provider,
            isNewUser = isNewUser,
            createdAt = firstLoginTime
        )
    }

    private fun mapFirebaseAuthException(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("recent authentication", ignoreCase = true) ||
            msg.contains("requires-recent-login", ignoreCase = true) ||
            msg.contains("CREDENTIAL_TOO_OLD", ignoreCase = true) -> "此操作屬於敏感安全性操作，請先重新登入後再執行刪除帳號"
            msg.contains("invalid-credential", ignoreCase = true) ||
            msg.contains("auth credential is incorrect", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "帳號或密碼錯誤，請重新確認"
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
