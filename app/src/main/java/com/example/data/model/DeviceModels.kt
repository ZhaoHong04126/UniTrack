package com.example.data.model

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import java.util.UUID

/**
 * 裝置資訊資料結構
 */
@Suppress("unused")
data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val model: String = "",
    val osVersion: String = "",
    val appVersion: String = "2.6.0",
    val lastLoginTime: Long = 0L,
    val lastLocation: String = "",
    val isCurrentDevice: Boolean = false
)

/**
 * 跨裝置登入安全事件資料結構
 */
data class LoginEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val location: String = "未知位置",
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = "LOGIN"
)

/**
 * 取得或產生本機固定唯一識別碼
 */
fun getOrGenerateDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences("unitrack_device_prefs", Context.MODE_PRIVATE)
    var deviceId = prefs.getString("local_device_id", null)
    if (deviceId.isNullOrBlank()) {
        deviceId = "dev_" + UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit { putString("local_device_id", deviceId) }
    }
    return deviceId
}

/**
 * 取得可讀之本機硬體型號名稱（例如：Google Pixel 7 或 Samsung SM-S9180）
 */
fun getCurrentDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val model = Build.MODEL.orEmpty()
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model".trim().ifBlank { "Android 裝置" }
    }
}
