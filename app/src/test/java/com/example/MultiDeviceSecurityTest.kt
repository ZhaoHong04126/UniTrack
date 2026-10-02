package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DeviceInfo
import com.example.data.model.LoginEvent
import com.example.data.model.NotificationPreferences
import com.example.data.model.getCurrentDeviceName
import com.example.data.model.getOrGenerateDeviceId
import com.example.data.repository.StudentRepository
import com.example.util.LocationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MultiDeviceSecurityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testDeviceModels_getOrGenerateDeviceId_persistence() {
        val deviceId1 = getOrGenerateDeviceId(context)
        assertNotNull(deviceId1)
        assertTrue(deviceId1.startsWith("dev_"))

        // 第二次呼叫應取得相同的 Device ID (持久化在 SharedPreferences)
        val deviceId2 = getOrGenerateDeviceId(context)
        assertEquals(deviceId1, deviceId2)
    }

    @Test
    fun testDeviceModels_getCurrentDeviceName() {
        val deviceName = getCurrentDeviceName()
        assertNotNull(deviceName)
        assertTrue(deviceName.isNotBlank())
    }

    @Test
    fun testDeviceModels_loginEventCreation() {
        val event = LoginEvent(
            userId = "user_test_123",
            deviceId = "dev_abc_456",
            deviceName = "Pixel 7 Pro",
            location = "台灣 台北市信義區",
            action = "LOGIN"
        )
        assertEquals("user_test_123", event.userId)
        assertEquals("dev_abc_456", event.deviceId)
        assertEquals("Pixel 7 Pro", event.deviceName)
        assertEquals("台灣 台北市信義區", event.location)
        assertEquals("LOGIN", event.action)
        assertTrue(event.timestamp > 0L)
    }

    @Test
    fun testNotificationPreferences_multiDeviceLoginAlertDefaultTrue() {
        val defaultPrefs = NotificationPreferences()
        assertTrue("多裝置登入安全警示預設應為開啟", defaultPrefs.multiDeviceLoginAlertEnabled)
    }

    @Test
    fun testLocationHelper_handlesNoPermissionGracefully() = runBlocking {
        // 在未授權環境下，LocationHelper 應優雅回傳說明文字且不應拋出例外
        val locationDesc = LocationHelper.getCurrentLocationDescription(context)
        assertNotNull(locationDesc)
        assertTrue(
            "應包含未授權或未開啟提示",
            locationDesc.contains("未授權") || locationDesc.contains("未知位置") || locationDesc.contains("定位")
        )
    }
}
