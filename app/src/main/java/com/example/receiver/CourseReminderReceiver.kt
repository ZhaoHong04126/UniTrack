package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.model.NotificationType
import com.example.util.NotificationHelper

class CourseReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_COURSE_LOCATION = "extra_course_location"
        const val EXTRA_COURSE_TIME = "extra_course_time"
        const val EXTRA_COURSE_PERIOD = "extra_course_period"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: return
        val courseLoc = intent.getStringExtra(EXTRA_COURSE_LOCATION).orEmpty()
        val courseTime = intent.getStringExtra(EXTRA_COURSE_TIME).orEmpty()
        val coursePeriod = intent.getIntExtra(EXTRA_COURSE_PERIOD, 1)
        val courseId = intent.getLongExtra(EXTRA_COURSE_ID, 0L)

        val timeStr = courseTime.ifBlank { "第 $coursePeriod 節" }
        val locStr = if (courseLoc.isNotBlank()) "，教室：$courseLoc" else ""

        NotificationHelper.sendSystemNotification(
            context = context,
            title = "⏰ 上課提醒：$courseName",
            message = "即將於 $timeStr 開始上課$locStr，請提早準備前往！",
            type = NotificationType.COURSE,
            actionRoute = "timetable",
            notificationId = (courseId % 100000).toInt() + 10000
        )
    }
}
