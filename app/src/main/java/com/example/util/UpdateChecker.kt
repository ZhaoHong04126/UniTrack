package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val OWNER = "ZhaoHong04126"
    private const val REPO = "UniTrack"
    private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val client = OkHttpClient()

    /**
     * 比對目前版本與 GitHub 最新 release。
     * 若有新版回傳 [UpdateInfo]，否則回傳 null。
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string() ?: return@withContext null
            }

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").removePrefix("v").trim()
            val htmlUrl = json.optString("html_url", "")
            val releaseNotes = json.optString("body", "").trim()

            if (tagName.isBlank() || tagName == currentVersion) return@withContext null

            if (isNewerVersion(tagName, currentVersion)) {
                UpdateInfo(
                    latestVersion = tagName,
                    releaseUrl = htmlUrl,
                    releaseNotes = releaseNotes.lines().take(10).joinToString("\n"),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
            null
        }
    }

    /**
     * Semver 比較：把版本字串拆成數字陣列，無法解析則回落字串比對。
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        if (remoteParts.isEmpty() || localParts.isEmpty()) return remote > local
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
