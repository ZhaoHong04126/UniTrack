package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val apkDownloadUrl: String? = null,
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val OWNER = "ZhaoHong04126"
    private const val REPO = "UniTrack"
    private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 比對目前版本與 GitHub 最新 release。
     * 若有新版回傳 [UpdateInfo]，否則回傳 null。
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "UniTrack-Android-App")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub API returned code: ${response.code}")
                    return@withContext null
                }
                response.body.string().ifBlank { return@withContext null }
            }

            val json = JSONObject(body)
            val rawTagName = json.optString("tag_name", "").trim()
            val tagName = rawTagName.removePrefix("v").trim()
            val htmlUrl = json.optString("html_url", "")
            val releaseNotes = json.optString("body", "").trim()

            // 尋找附帶的 .apk 下載連結
            var apkUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.getJSONObject(i)
                    val assetName = assetObj.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = assetObj.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (tagName.isBlank() || tagName == currentVersion.removePrefix("v").trim()) {
                return@withContext null
            }

            if (isNewerVersion(tagName, currentVersion.removePrefix("v").trim())) {
                UpdateInfo(
                    latestVersion = rawTagName.ifBlank { "v$tagName" },
                    releaseUrl = htmlUrl,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl
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
     * 開啟外部瀏覽器或下載連結
     */
    fun openDownloadPage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open download url: $url", e)
        }
    }

    /**
     * 語意化版本 (SemVer) 比較：把版本字串拆成數字陣列，無法解析則回落字串比對。
     */
    fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteClean = remote.removePrefix("v").trim()
        val localClean = local.removePrefix("v").trim()

        val remoteParts = remoteClean.split(".").mapNotNull { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull()
        }
        val localParts = localClean.split(".").mapNotNull { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull()
        }
        if (remoteParts.isEmpty() || localParts.isEmpty()) return remoteClean > localClean
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
