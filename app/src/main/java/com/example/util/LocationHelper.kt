package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object LocationHelper {

    private const val TAG = "LocationHelper"

    /**
     * 檢查是否已具備精確或概略位置權限
     */
    fun hasLocationPermission(context: Context): Boolean {
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return finePermission || coarsePermission
    }

    /**
     * 檢查系統定位服務 (GPS 或 Network) 是否已啟用
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        val isGpsEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val isNetworkEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        return isGpsEnabled || isNetworkEnabled
    }

    /**
     * 開啟系統定位設定頁面
     */
    @Suppress("unused")
    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open location settings: ${e.message}")
        }
    }

    /**
     * 獲取當前登入裝置的地理位置描述字串 (例如「台灣 台北市信義區」)
     * 僅在登入或觸發安全事件時呼叫一次，不常駐追蹤。
     */
    suspend fun getCurrentLocationDescription(context: Context): String = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext "未授權定位 (未知位置)"
        }

        if (!isLocationServiceEnabled(context)) {
            return@withContext "定位服務未開啟 (未知位置)"
        }

        val location = withTimeoutOrNull(1500.milliseconds) {
            getDeviceLocation(context)
        } ?: run {
            getLastKnownLocation(context)
        }

        if (location == null) {
            return@withContext "未知位置 (無法定位)"
        }

        // 使用 Geocoder 轉換經緯度為縣市/地區名稱
        val geocoded = reverseGeocode(context, location.latitude, location.longitude)
        geocoded.ifBlank {
            String.format(Locale.US, "經緯度: %.3f, %.3f", location.latitude, location.longitude)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }

                val provider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> LocationManager.PASSIVE_PROVIDER
                }

                try {
                    locationManager.getCurrentLocation(
                        provider,
                        signal,
                        ContextCompat.getMainExecutor(context)
                    ) { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "getCurrentLocation failed: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        } else {
            return getLastKnownLocation(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        var bestLocation: Location? = null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "getLastKnownLocation error for $provider: ${e.message}")
            }
        }
        return bestLocation
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull() ?: return ""

            val country = address.countryName.orEmpty()
            val adminArea = address.adminArea.orEmpty() // e.g. 台北市 or 台灣省
            val subAdminArea = address.subAdminArea.orEmpty()
            val locality = address.locality.orEmpty() // e.g. 信義區 or 板橋區
            val subLocality = address.subLocality.orEmpty()

            val parts = linkedSetOf<String>()
            if (country.isNotBlank()) parts.add(country)
            if (adminArea.isNotBlank() && adminArea != country) parts.add(adminArea)
            if (subAdminArea.isNotBlank() && !parts.contains(subAdminArea)) parts.add(subAdminArea)
            if (locality.isNotBlank() && !parts.contains(locality)) parts.add(locality)
            if (subLocality.isNotBlank() && !parts.contains(subLocality)) parts.add(subLocality)

            if (parts.isNotEmpty()) {
                parts.joinToString(" ")
            } else {
                address.getAddressLine(0) ?: ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder reverseGeocode error: ${e.message}")
            ""
        }
    }
}
