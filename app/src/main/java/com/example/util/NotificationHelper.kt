package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.NotificationType

object NotificationHelper {

    const val CHANNEL_ID_COURSES = "unitrack_courses_channel"
    const val CHANNEL_ID_EXPENSES = "unitrack_expenses_channel"
    const val CHANNEL_ID_GRADUATION = "unitrack_graduation_channel"
    const val CHANNEL_ID_SYSTEM = "unitrack_system_channel"

    const val EXTRA_NAV_ROUTE = "extra_nav_route"

    fun createNotificationChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

        val courseChannel = NotificationChannel(
            CHANNEL_ID_COURSES,
            "課表與上課提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "提供每日課表、上課前提醒與課程異動通知"
            enableVibration(true)
            setShowBadge(true)
        }

        val expenseChannel = NotificationChannel(
            CHANNEL_ID_EXPENSES,
            "記帳與預算警示",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "提供每月預算使用超標警示與記帳提醒"
            enableVibration(true)
            setShowBadge(true)
        }

        val graduationChannel = NotificationChannel(
            CHANNEL_ID_GRADUATION,
            "學業與畢業審查",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "提供學分門檻達成、GPA 成績結算與學業提醒"
            enableVibration(true)
            setShowBadge(true)
        }

        val systemChannel = NotificationChannel(
            CHANNEL_ID_SYSTEM,
            "系統與公告通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "UniTrack+ 系統公告、歡迎與備份提醒"
            enableVibration(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannels(
            listOf(courseChannel, expenseChannel, graduationChannel, systemChannel)
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!areEnabled) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun sendSystemNotification(
        context: Context,
        title: String,
        message: String,
        type: NotificationType = NotificationType.SYSTEM,
        actionRoute: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val channelId = when (type) {
            NotificationType.COURSE -> CHANNEL_ID_COURSES
            NotificationType.EXPENSE -> CHANNEL_ID_EXPENSES
            NotificationType.GRADUATION -> CHANNEL_ID_GRADUATION
            NotificationType.SYSTEM -> CHANNEL_ID_SYSTEM
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_ROUTE, actionRoute ?: "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = R.drawable.ic_notification

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setColor(0xFF2563EB.toInt()) // SapphirePrimary
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission might have been revoked dynamically
        }
    }

    @Suppress("unused")
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {
        }
    }

    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {
        }
    }
}
