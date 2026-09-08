package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.repository.StudentRepository
import com.example.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = StudentRepository(
                    context,
                    db.courseDao(),
                    db.graduationDao(),
                    db.expenseDao(),
                    db.notificationDao(),
                    db.calendarDao()
                )
                val prefs = repository.getNotificationPreferences()
                if (prefs.masterEnabled && prefs.courseReminderEnabled) {
                    val courses = repository.getAllCoursesOnce()
                    val plan = repository.getGraduationPlanOnce()
                    val calendar = Calendar.getInstance()
                    val dayOfWeekToday = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }
                    val todaySemCourses = courses.filter {
                        (it.semester == plan.currentSemester || it.semester.isBlank()) && it.dayOfWeek == dayOfWeekToday
                    }
                    NotificationScheduler.scheduleCourseReminders(
                        context = context,
                        courses = todaySemCourses,
                        reminderLeadMinutes = prefs.courseReminderMinutesBefore.coerceAtLeast(5)
                    )
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
