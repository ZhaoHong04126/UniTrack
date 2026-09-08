package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Course
import com.example.receiver.CourseReminderReceiver
import com.example.ui.viewmodel.StudentViewModel
import java.util.Calendar

object NotificationScheduler {

    /**
     * 針對今日排課，依據使用者設定的提前分鐘數 (reminderLeadMinutes)，
     * 透過系統 AlarmManager 註冊上課提醒推播廣播。
     */
    fun scheduleCourseReminders(
        context: Context,
        courses: List<Course>,
        reminderLeadMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        courses.forEach { course ->
            val startMin = StudentViewModel.parseCourseTimeToMinutes(course.startTime, course.startPeriod, true)
            val remindMin = (startMin - reminderLeadMinutes).coerceAtLeast(0)

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, remindMin / 60)
                set(Calendar.MINUTE, remindMin % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val triggerMillis = calendar.timeInMillis
            // 僅排定未來尚未到達的時間點
            if (triggerMillis > now) {
                val intent = Intent(context, CourseReminderReceiver::class.java).apply {
                    putExtra(CourseReminderReceiver.EXTRA_COURSE_ID, course.id)
                    putExtra(CourseReminderReceiver.EXTRA_COURSE_NAME, course.name)
                    putExtra(CourseReminderReceiver.EXTRA_COURSE_LOCATION, course.location)
                    putExtra(CourseReminderReceiver.EXTRA_COURSE_TIME, course.startTime)
                    putExtra(CourseReminderReceiver.EXTRA_COURSE_PERIOD, course.startPeriod)
                }

                val requestCode = (course.id % 100000).toInt() + 20000
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                } catch (_: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    /**
     * 取消指定課程的上課提醒廣播。
     */
    @Suppress("unused")
    fun cancelCourseReminders(context: Context, courses: List<Course>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        courses.forEach { course ->
            val intent = Intent(context, CourseReminderReceiver::class.java)
            val requestCode = (course.id % 100000).toInt() + 20000
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
