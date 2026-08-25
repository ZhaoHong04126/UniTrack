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
import com.example.data.model.Course
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@Suppress("SpellCheckingInspection")
class TodayScheduleWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_SCHEDULE = "com.example.widget.ACTION_REFRESH_SCHEDULE"

        fun getDayOfWeekIndex(calendar: Calendar = Calendar.getInstance()): Int {
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }

        fun getDayOfWeekName(dayIndex: Int): String {
            return when (dayIndex) {
                1 -> "週一"
                2 -> "週二"
                3 -> "週三"
                4 -> "週四"
                5 -> "週五"
                6 -> "週六"
                7 -> "週日"
                else -> "週一"
            }
        }

        fun calculateCurrentWeek(startDateStr: String?, totalWeeks: Int): Int {
            if (startDateStr.isNullOrBlank()) return 1
            return try {
                val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                val startDate = LocalDate.parse(startDateStr, formatter)
                val today = LocalDate.now()
                val daysDiff = ChronoUnit.DAYS.between(startDate, today)
                if (daysDiff < 0) {
                    1
                } else {
                    val week = (daysDiff / 7).toInt() + 1
                    week.coerceIn(1, totalWeeks)
                }
            } catch (_: Exception) {
                1
            }
        }

        fun isCourseInWeek(course: Course, week: Int): Boolean {
            if (course.repeatMode == "單週") return week % 2 != 0
            if (course.repeatMode == "雙週") return week % 2 == 0
            if (course.repeatMode == "每週" || course.repeatWeeks == "1-18" || course.repeatWeeks.isBlank()) return true
            val weeks = course.repeatWeeks.split(",").mapNotNull { it.trim().toIntOrNull() }
            return week in weeks
        }

        fun getPeriodTimeRange(startPeriod: Int, endPeriod: Int): String {
            val periodTimes = mapOf(
                1 to ("08:10" to "09:00"),
                2 to ("09:10" to "10:00"),
                3 to ("10:10" to "11:00"),
                4 to ("11:10" to "12:00"),
                5 to ("13:10" to "14:00"),
                6 to ("14:10" to "15:00"),
                7 to ("15:10" to "16:00"),
                8 to ("16:10" to "17:00"),
                9 to ("17:10" to "18:00"),
                10 to ("18:20" to "19:10"),
                11 to ("19:15" to "20:05"),
                12 to ("20:10" to "21:00"),
                13 to ("21:05" to "21:55"),
                14 to ("22:00" to "22:50")
            )
            val start = periodTimes[startPeriod]?.first ?: "第${startPeriod}節"
            val end = periodTimes[endPeriod]?.second ?: "第${endPeriod}節"
            return if (start.contains("節") || end.contains("節")) {
                "第${startPeriod}-${endPeriod}節"
            } else {
                "$start - $end"
            }
        }

        data class NextClassStatus(
            val course: Course,
            val isOngoing: Boolean,
            val timeDisplay: String
        )

        fun findNextOrOngoingClass(courses: List<Course>, nowMinutes: Int): NextClassStatus? {
            fun parseToMinutes(timeStr: String, fallbackPeriod: Int, isStart: Boolean): Int {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    if (h != null && m != null) return h * 60 + m
                }
                val periodMap = mapOf(
                    1 to (8 * 60 + 10 to 9 * 60),
                    2 to (9 * 60 + 10 to 10 * 60),
                    3 to (10 * 60 + 10 to 11 * 60),
                    4 to (11 * 60 + 10 to 12 * 60),
                    5 to (13 * 60 + 10 to 14 * 60),
                    6 to (14 * 60 + 10 to 15 * 60),
                    7 to (15 * 60 + 10 to 16 * 60),
                    8 to (16 * 60 + 10 to 17 * 60),
                    9 to (17 * 60 + 10 to 18 * 60),
                    10 to (18 * 60 + 20 to 19 * 60 + 10),
                    11 to (19 * 60 + 15 to 20 * 60 + 5),
                    12 to (20 * 60 + 10 to 21 * 60),
                    13 to (21 * 60 + 5 to 21 * 60 + 55),
                    14 to (22 * 60 to 22 * 60 + 50)
                )
                val pair = periodMap[fallbackPeriod] ?: (8 * 60 to 9 * 60)
                return if (isStart) pair.first else pair.second
            }

            // 1. 優先檢查是否正在上課中 (Ongoing)
            for (course in courses) {
                val startMin = parseToMinutes(course.startTime, course.startPeriod, true)
                val endMin = parseToMinutes(course.endTime, course.endPeriod, false)
                if (nowMinutes in startMin..endMin) {
                    val timeStr = if (course.startTime.isNotBlank() && course.endTime.isNotBlank()) {
                        "${course.startTime} - ${course.endTime}"
                    } else {
                        getPeriodTimeRange(course.startPeriod, course.endPeriod)
                    }
                    return NextClassStatus(course, isOngoing = true, timeDisplay = timeStr)
                }
            }

            // 2. 檢查今日即將開始的下一堂課 (Upcoming)
            for (course in courses) {
                val startMin = parseToMinutes(course.startTime, course.startPeriod, true)
                if (nowMinutes < startMin) {
                    val timeStr = if (course.startTime.isNotBlank() && course.endTime.isNotBlank()) {
                        "${course.startTime} - ${course.endTime}"
                    } else {
                        getPeriodTimeRange(course.startPeriod, course.endPeriod)
                    }
                    return NextClassStatus(course, isOngoing = false, timeDisplay = timeStr)
                }
            }

            return null
        }
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
        if (intent.action == ACTION_REFRESH_SCHEDULE || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id)
                }
            } else {
                val componentName = android.content.ComponentName(context, TodayScheduleWidget::class.java)
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
        val views = RemoteViews(context.packageName, R.layout.widget_today_schedule)

        // 綁定點擊整個 Widget 跳轉至課表頁面
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
        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

        // 綁定重新整理按鈕
        val refreshIntent = Intent(context, TodayScheduleWidget::class.java).apply {
            action = ACTION_REFRESH_SCHEDULE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val graduationPlan = db.graduationDao().getGraduationPlanOnce()
                val currentSemester = graduationPlan?.currentSemester?.ifBlank { null }
                    ?: DefaultData.getCurrentAcademicSemester()

                val prefs = context.getSharedPreferences("unitrack_prefs", Context.MODE_PRIVATE)
                val startDateStr = prefs.getString("semester_start_date_$currentSemester", null)
                val totalWeeks = prefs.getInt("semester_total_weeks_$currentSemester", 18)

                val calendar = Calendar.getInstance()
                val dayOfWeekIndex = getDayOfWeekIndex(calendar)
                val dayOfWeekName = getDayOfWeekName(dayOfWeekIndex)
                val currentWeek = calculateCurrentWeek(startDateStr, totalWeeks)

                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val dateText = String.format(Locale.getDefault(), "%d月%d日 %s", month, day, dayOfWeekName)

                views.setTextViewText(R.id.tv_widget_date, dateText)
                views.setTextViewText(R.id.tv_widget_week_badge, "第 $currentWeek 週")

                // 查詢課程並過濾今日課程
                val allCourses = db.courseDao().getAllCoursesOnce()
                val todayCourses = allCourses
                    .filter { it.semester == currentSemester && it.dayOfWeek == dayOfWeekIndex && isCourseInWeek(it, currentWeek) }
                    .sortedBy { it.startPeriod }

                val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val nextClass = findNextOrOngoingClass(todayCourses, nowMinutes)

                if (todayCourses.isEmpty()) {
                    // 今日無排課
                    views.setViewVisibility(R.id.layout_next_class_container, View.GONE)
                    views.setViewVisibility(R.id.layout_courses_container, View.GONE)
                    views.setViewVisibility(R.id.layout_empty_state, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.layout_empty_state, View.GONE)
                    views.setViewVisibility(R.id.layout_courses_container, View.VISIBLE)

                    // 設置下一堂課高亮區塊
                    if (nextClass != null) {
                        views.setViewVisibility(R.id.layout_next_class_container, View.VISIBLE)
                        views.setTextViewText(
                            R.id.tv_next_class_badge,
                            if (nextClass.isOngoing) "⚡ 進行中" else "⏱️ 下一堂課"
                        )
                        views.setTextViewText(R.id.tv_next_class_time, nextClass.timeDisplay)
                        views.setTextViewText(R.id.tv_next_class_name, nextClass.course.name)

                        val location = nextClass.course.location.ifBlank { "教室未定" }
                        val teacher = if (nextClass.course.teacher.isNotBlank()) " • ${nextClass.course.teacher}" else ""
                        views.setTextViewText(R.id.tv_next_class_info, "📍 $location$teacher")
                    } else {
                        // 今日課程已全部結束
                        views.setViewVisibility(R.id.layout_next_class_container, View.VISIBLE)
                        views.setTextViewText(R.id.tv_next_class_badge, "✨ 今日結束")
                        views.setTextViewText(R.id.tv_next_class_time, "")
                        views.setTextViewText(R.id.tv_next_class_name, "今日課程已全數完畢")
                        views.setTextViewText(R.id.tv_next_class_info, "辛苦了！好好休息吧")
                    }

                    // 條列今日課表 (最多顯示 4 堂)
                    val itemLayoutIds = listOf(
                        R.id.item_course_1,
                        R.id.item_course_2,
                        R.id.item_course_3,
                        R.id.item_course_4
                    )
                    val periodViewIds = listOf(
                        R.id.tv_course_period_1,
                        R.id.tv_course_period_2,
                        R.id.tv_course_period_3,
                        R.id.tv_course_period_4
                    )
                    val nameViewIds = listOf(
                        R.id.tv_course_name_1,
                        R.id.tv_course_name_2,
                        R.id.tv_course_name_3,
                        R.id.tv_course_name_4
                    )
                    val detailViewIds = listOf(
                        R.id.tv_course_detail_1,
                        R.id.tv_course_detail_2,
                        R.id.tv_course_detail_3,
                        R.id.tv_course_detail_4
                    )

                    for (i in itemLayoutIds.indices) {
                        if (i < todayCourses.size) {
                            val c = todayCourses[i]
                            views.setViewVisibility(itemLayoutIds[i], View.VISIBLE)
                            views.setTextViewText(periodViewIds[i], c.startPeriod.toString())
                            views.setTextViewText(nameViewIds[i], c.name)

                            val timeText = if (c.startTime.isNotBlank() && c.endTime.isNotBlank()) {
                                "${c.startTime}-${c.endTime}"
                            } else {
                                "第${c.startPeriod}-${c.endPeriod}節"
                            }
                            val locText = if (c.location.isNotBlank()) " • ${c.location}" else ""
                            views.setTextViewText(detailViewIds[i], "$timeText$locText")
                        } else {
                            views.setViewVisibility(itemLayoutIds[i], View.GONE)
                        }
                    }

                    if (todayCourses.size > 4) {
                        views.setViewVisibility(R.id.tv_more_courses, View.VISIBLE)
                        views.setTextViewText(
                            R.id.tv_more_courses,
                            "+ 還有 ${todayCourses.size - 4} 堂課，點擊開啟完整課表"
                        )
                    } else {
                        views.setViewVisibility(R.id.tv_more_courses, View.GONE)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
                // 容錯防護
            }
        }
    }
}
