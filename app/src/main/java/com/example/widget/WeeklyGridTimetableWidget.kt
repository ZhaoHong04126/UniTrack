package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultData
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Suppress("SpellCheckingInspection")
class WeeklyGridTimetableWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WEEKLY_GRID = "com.example.widget.ACTION_REFRESH_WEEKLY_GRID"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WEEKLY_GRID || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id)
                }
            } else {
                val componentName = android.content.ComponentName(context, WeeklyGridTimetableWidget::class.java)
                val allIds = appWidgetManager.getAppWidgetIds(componentName)
                if (allIds != null) {
                    for (id in allIds) {
                        updateWidget(context, appWidgetManager, id)
                    }
                }
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_weekly_grid)

        // 點擊整個 Widget 跳轉至課表頁面
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationHelper.EXTRA_NAV_ROUTE, "timetable")
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_grid_root, mainPendingIntent)

        // 重新整理按鈕
        val refreshIntent = Intent(context, WeeklyGridTimetableWidget::class.java).apply {
            action = ACTION_REFRESH_WEEKLY_GRID
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_grid_refresh, refreshPendingIntent)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val graduationPlan = db.graduationDao().getGraduationPlanOnce()
                val currentSemester = graduationPlan?.currentSemester?.ifBlank { null }
                    ?: DefaultData.getCurrentAcademicSemester()

                val prefs = context.getSharedPreferences("unitrack_prefs", Context.MODE_PRIVATE)
                val startDateStr = prefs.getString("semester_start_date_$currentSemester", null)
                val totalWeeks = prefs.getInt("semester_total_weeks_$currentSemester", 18)
                val showWeekend = prefs.getBoolean("pref_show_weekend", false)

                val calendar = Calendar.getInstance()
                val dayOfWeekIndex = TodayScheduleWidget.getDayOfWeekIndex(calendar)
                val currentWeek = TodayScheduleWidget.calculateCurrentWeek(startDateStr, totalWeeks)

                views.setTextViewText(R.id.tv_widget_grid_title, "$currentSemester 週課表")
                views.setTextViewText(R.id.tv_widget_grid_week_badge, "第 $currentWeek 週")

                val allCourses = db.courseDao().getAllCoursesOnce()
                val semesterCourses = allCourses.filter { it.semester == currentSemester }

                if (semesterCourses.isEmpty()) {
                    views.setViewVisibility(R.id.iv_weekly_grid_canvas, View.GONE)
                    views.setViewVisibility(R.id.layout_grid_empty, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.layout_grid_empty, View.GONE)
                    views.setViewVisibility(R.id.iv_weekly_grid_canvas, View.VISIBLE)

                    // 繪製網格 Bitmap
                    val gridBitmap = WeeklyGridBitmapRenderer.renderWeeklyGrid(
                        courses = semesterCourses,
                        currentDayOfWeek = dayOfWeekIndex,
                        currentWeek = currentWeek,
                        showWeekend = showWeekend,
                        width = 1000,
                        height = 1200
                    )
                    views.setImageViewBitmap(R.id.iv_weekly_grid_canvas, gridBitmap)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
                // 容錯防護
            }
        }
    }
}
