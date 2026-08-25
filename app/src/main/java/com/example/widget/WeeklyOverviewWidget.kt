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
class WeeklyOverviewWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WEEKLY_OVERVIEW = "com.example.widget.ACTION_REFRESH_WEEKLY_OVERVIEW"
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
        if (intent.action == ACTION_REFRESH_WEEKLY_OVERVIEW || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id)
                }
            } else {
                val componentName = android.content.ComponentName(context, WeeklyOverviewWidget::class.java)
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
        val views = RemoteViews(context.packageName, R.layout.widget_weekly_overview)

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
        views.setOnClickPendingIntent(R.id.widget_overview_root, mainPendingIntent)

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
                val dayOfWeekName = TodayScheduleWidget.getDayOfWeekName(dayOfWeekIndex)
                val currentWeek = TodayScheduleWidget.calculateCurrentWeek(startDateStr, totalWeeks)

                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                views.setTextViewText(R.id.tv_overview_date, "${month}月${day}日 $dayOfWeekName")
                views.setTextViewText(R.id.tv_overview_week_badge, "第 $currentWeek 週")

                val allCourses = db.courseDao().getAllCoursesOnce()
                val semesterCourses = allCourses.filter { it.semester == currentSemester }

                // 今日課程與下節課焦點
                val todayCourses = semesterCourses
                    .filter { it.dayOfWeek == dayOfWeekIndex && TodayScheduleWidget.isCourseInWeek(it, currentWeek) }
                    .sortedBy { it.startPeriod }

                val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val nextClass = TodayScheduleWidget.findNextOrOngoingClass(todayCourses, nowMinutes)

                if (nextClass != null) {
                    views.setViewVisibility(R.id.layout_overview_today_focus, View.VISIBLE)
                    views.setTextViewText(
                        R.id.tv_overview_today_badge,
                        if (nextClass.isOngoing) "⚡ 進行中" else "⏱️ 下一堂課"
                    )
                    views.setTextViewText(R.id.tv_overview_today_time, nextClass.timeDisplay)
                    views.setTextViewText(R.id.tv_overview_today_name, nextClass.course.name)
                    val location = nextClass.course.location.ifBlank { "教室未定" }
                    val teacher = if (nextClass.course.teacher.isNotBlank()) " • ${nextClass.course.teacher}" else ""
                    views.setTextViewText(R.id.tv_overview_today_info, "📍 $location$teacher")
                } else if (todayCourses.isNotEmpty()) {
                    views.setViewVisibility(R.id.layout_overview_today_focus, View.VISIBLE)
                    views.setTextViewText(R.id.tv_overview_today_badge, "✨ 今日結束")
                    views.setTextViewText(R.id.tv_overview_today_time, "")
                    views.setTextViewText(R.id.tv_overview_today_name, "今日課程已全數完畢")
                    views.setTextViewText(R.id.tv_overview_today_info, "辛苦了！好好休息吧")
                } else {
                    views.setViewVisibility(R.id.layout_overview_today_focus, View.VISIBLE)
                    views.setTextViewText(R.id.tv_overview_today_badge, "🎉 今日無課")
                    views.setTextViewText(R.id.tv_overview_today_time, "")
                    views.setTextViewText(R.id.tv_overview_today_name, "享受充實的一天")
                    views.setTextViewText(R.id.tv_overview_today_info, "點擊隨時查看完整週課表")
                }

                // 繪製下方一週 5/7 日 mini cards（即使無課程也顯示完整 5 日卡片）
                val overviewBitmap = WeeklyOverviewBitmapRenderer.renderOverviewCards(
                    courses = semesterCourses,
                    currentDayOfWeek = dayOfWeekIndex,
                    currentWeek = currentWeek,
                    showWeekend = showWeekend,
                    width = 1000,
                    height = 600
                )
                views.setImageViewBitmap(R.id.iv_overview_mini_grid, overviewBitmap)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
                // 容錯防護
            }
        }
    }
}
