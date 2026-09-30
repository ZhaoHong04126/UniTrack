package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdateHelper {

    /**
     * 主動通知所有桌面「今日課表」小工具重新自資料庫載入最新課表並刷新畫面。
     */
    fun updateTodayScheduleWidget(context: Context) {
        sendUpdateBroadcast(context, TodayScheduleWidget::class.java)
    }

    /**
     * 一次性更新所有 UniTrack+ 桌面小工具。
     */
    fun updateAllWidgets(context: Context) {
        updateTodayScheduleWidget(context)
    }

    private fun sendUpdateBroadcast(context: Context, providerClass: Class<*>) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, providerClass)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (_: Exception) {
            // Widget 更新容錯防護
        }
    }
}
