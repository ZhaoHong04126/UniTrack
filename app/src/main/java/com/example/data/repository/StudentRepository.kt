package com.example.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.data.local.CourseDao
import com.example.data.local.DefaultData
import com.example.data.local.ExpenseDao
import com.example.data.local.GraduationDao
import com.example.data.local.NotificationDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Suppress("unused")
class StudentRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val graduationDao: GraduationDao,
    private val expenseDao: ExpenseDao,
    private val notificationDao: NotificationDao
) {
    private val prefs by lazy {
        context.getSharedPreferences("unitrack_profile_prefs", Context.MODE_PRIVATE)
    }

    fun getCachedGraduationPlan(): GraduationPlan {
        val defaultPlan = DefaultData.getDefaultGraduationPlan()
        val dept = prefs.getString("department", null)
        val name = prefs.getString("studentName", null)
        val adm = prefs.getString("admissionSemester", null)
        val cur = prefs.getString("currentSemester", null)
        return defaultPlan.copy(
            department = if (!dept.isNullOrBlank()) dept else defaultPlan.department,
            studentName = if (!name.isNullOrBlank()) name else defaultPlan.studentName,
            admissionSemester = if (!adm.isNullOrBlank()) adm else defaultPlan.admissionSemester,
            currentSemester = if (!cur.isNullOrBlank()) cur else defaultPlan.currentSemester
        )
    }

    // Courses
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    val allSemesters: Flow<List<String>> = courseDao.getAllSemesters()

    suspend fun insertCourse(course: Course): Long = withContext(Dispatchers.IO) {
        courseDao.insertCourse(course)
    }

    suspend fun insertCourses(courses: List<Course>) = withContext(Dispatchers.IO) {
        courseDao.insertCourses(courses)
    }

    suspend fun updateCourse(course: Course) = withContext(Dispatchers.IO) {
        courseDao.updateCourse(course)
    }

    suspend fun deleteCourse(course: Course) = withContext(Dispatchers.IO) {
        if (course.id != 0L) {
            courseDao.deleteCourseById(course.id)
        } else {
            courseDao.deleteCourse(course)
        }
    }

    suspend fun deleteCoursesBySemester(semester: String) = withContext(Dispatchers.IO) {
        courseDao.deleteCoursesBySemester(semester)
    }

    // Graduation Plan & Thresholds
    val graduationPlan: Flow<GraduationPlan?> = graduationDao.getGraduationPlan()
    val allThresholds: Flow<List<GraduationThreshold>> = graduationDao.getAllThresholds()

    suspend fun getGraduationPlanOnce(): GraduationPlan? = withContext(Dispatchers.IO) {
        val plan = graduationDao.getGraduationPlanOnce()
        if (plan != null) {
            if (plan.department.isNotBlank() && plan.department != "尚未設定系所") {
                prefs.edit {
                    putString("department", plan.department)
                    putString("studentName", plan.studentName)
                    putString("admissionSemester", plan.admissionSemester)
                    putString("currentSemester", plan.currentSemester)
                }
            }
            plan
        } else {
            val cached = getCachedGraduationPlan()
            if (cached.department.isNotBlank() && cached.department != "尚未設定系所") {
                graduationDao.insertOrUpdatePlan(cached)
                cached
            } else {
                null
            }
        }
    }

    suspend fun updateGraduationPlan(plan: GraduationPlan) = withContext(Dispatchers.IO) {
        if (plan.department.isNotBlank() && plan.department != "尚未設定系所") {
            prefs.edit {
                putString("department", plan.department)
                putString("studentName", plan.studentName)
                putString("admissionSemester", plan.admissionSemester)
                putString("currentSemester", plan.currentSemester)
            }
        }
        graduationDao.insertOrUpdatePlan(plan)
    }

    suspend fun insertThreshold(threshold: GraduationThreshold): Long = withContext(Dispatchers.IO) {
        graduationDao.insertThreshold(threshold)
    }

    suspend fun updateThreshold(threshold: GraduationThreshold) = withContext(Dispatchers.IO) {
        graduationDao.updateThreshold(threshold)
    }

    suspend fun deleteThreshold(threshold: GraduationThreshold) = withContext(Dispatchers.IO) {
        graduationDao.deleteThreshold(threshold)
    }

    // Expenses & Budgets
    val allExpenses: Flow<List<ExpenseRecord>> = expenseDao.getAllExpenses()
    val allBudgets: Flow<List<MonthlyBudget>> = expenseDao.getAllBudgets()

    suspend fun getAllExpensesOnce(): List<ExpenseRecord> = withContext(Dispatchers.IO) {
        expenseDao.getAllExpensesOnce()
    }

    suspend fun getAllBudgetsOnce(): List<MonthlyBudget> = withContext(Dispatchers.IO) {
        expenseDao.getAllBudgetsOnce()
    }

    suspend fun insertExpense(expense: ExpenseRecord): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseRecord) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseRecord) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteAllExpenses() = withContext(Dispatchers.IO) {
        expenseDao.deleteAllExpenses()
    }

    suspend fun setBudget(budget: MonthlyBudget) = withContext(Dispatchers.IO) {
        expenseDao.setBudget(budget)
    }

    // Initialize Database with Default Data if empty
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingPlan = graduationDao.getGraduationPlanOnce()
        if (existingPlan == null) {
            graduationDao.insertOrUpdatePlan(DefaultData.getDefaultGraduationPlan())
            graduationDao.insertThresholds(DefaultData.getDefaultThresholds())
            expenseDao.setBudget(DefaultData.getDefaultBudget())
        }
    }

    suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        courseDao.deleteAllCourses()
        graduationDao.deleteAllThresholds()
        expenseDao.deleteAllExpenses()

        graduationDao.insertOrUpdatePlan(DefaultData.getDefaultGraduationPlan())
        graduationDao.insertThresholds(DefaultData.getDefaultThresholds())
        courseDao.insertCourses(DefaultData.getDefaultCourses())
        expenseDao.insertExpenses(DefaultData.getDefaultExpenses())
        expenseDao.setBudget(DefaultData.getDefaultBudget())
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        courseDao.deleteAllCourses()
        graduationDao.deleteAllThresholds()
        expenseDao.deleteAllExpenses()
        expenseDao.deleteAllBudgets()
        graduationDao.insertOrUpdatePlan(DefaultData.getDefaultGraduationPlan())
    }

    // Export & Import Offline JSON for full backup
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val courses = allCourses.firstOrNull() ?: emptyList()
        val plan = graduationDao.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
        val thresholds = allThresholds.firstOrNull() ?: emptyList()
        val expenses = allExpenses.firstOrNull() ?: emptyList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val planObj = JSONObject().apply {
            put("department", plan.department)
            put("studentName", plan.studentName)
            put("targetTotalCredits", plan.targetTotalCredits)
            put("targetRequiredCredits", plan.targetRequiredCredits)
            put("targetElectiveCredits", plan.targetElectiveCredits)
            put("targetGeneralCredits", plan.targetGeneralCredits)
            put("targetFreeCredits", plan.targetFreeCredits)
            put("admissionSemester", plan.admissionSemester)
            put("currentSemester", plan.currentSemester)
            put("gpaScale", plan.gpaScale.name)
        }
        root.put("graduationPlan", planObj)

        val coursesArray = JSONArray()
        courses.forEach { c ->
            val cObj = JSONObject().apply {
                put("name", c.name)
                put("code", c.code)
                put("teacher", c.teacher)
                put("location", c.location)
                put("dayOfWeek", c.dayOfWeek)
                put("startPeriod", c.startPeriod)
                put("endPeriod", c.endPeriod)
                put("startTime", c.startTime)
                put("endTime", c.endTime)
                put("credits", c.credits)
                put("category", c.category.name)
                put("requirementType", c.requirementType.name)
                put("generalEduSubtype", c.generalEduSubtype.name)
                put("subcategory", c.subcategory)
                put("semester", c.semester)
                put("score", c.score ?: JSONObject.NULL)
                put("letterGrade", c.letterGrade ?: JSONObject.NULL)
                put("isCompleted", c.isCompleted)
                put("colorHex", c.colorHex)
                put("notes", c.notes)
            }
            coursesArray.put(cObj)
        }
        root.put("courses", coursesArray)

        val thresholdsArray = JSONArray()
        thresholds.forEach { t ->
            val tObj = JSONObject().apply {
                put("title", t.title)
                put("description", t.description)
                put("isCompleted", t.isCompleted)
                put("completedDate", t.completedDate)
                put("proofNote", t.proofNote)
            }
            thresholdsArray.put(tObj)
        }
        root.put("thresholds", thresholdsArray)

        val expensesArray = JSONArray()
        expenses.forEach { e ->
            val eObj = JSONObject().apply {
                put("title", e.title)
                put("amount", e.amount)
                put("type", e.type.name)
                put("category", e.category.name)
                put("paymentMethod", e.paymentMethod.name)
                put("timestamp", e.timestamp)
                put("dateString", e.dateString)
                put("note", e.note)
            }
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("graduationPlan")) {
                val planObj = root.getJSONObject("graduationPlan")
                val plan = GraduationPlan(
                    id = 1,
                    department = planObj.optString("department", "尚未設定系所"),
                    studentName = planObj.optString("studentName", "同學"),
                    targetTotalCredits = planObj.optDouble("targetTotalCredits", 128.0),
                    targetRequiredCredits = planObj.optDouble("targetRequiredCredits", 58.0),
                    targetElectiveCredits = planObj.optDouble("targetElectiveCredits", 36.0),
                    targetGeneralCredits = planObj.optDouble("targetGeneralCredits", 28.0),
                    targetFreeCredits = planObj.optDouble("targetFreeCredits", 6.0),
                    admissionSemester = planObj.optString("admissionSemester", DefaultData.getCurrentAcademicSemester()),
                    currentSemester = planObj.optString("currentSemester", DefaultData.getCurrentAcademicSemester()),
                    gpaScale = runCatching { GpaScale.valueOf(planObj.optString("gpaScale", "PERCENTAGE")) }.getOrDefault(GpaScale.PERCENTAGE)
                )
                graduationDao.insertOrUpdatePlan(plan)
            }

            if (root.has("courses")) {
                val arr = root.getJSONArray("courses")
                val courseList = mutableListOf<Course>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    courseList.add(
                        Course(
                            name = o.getString("name"),
                            code = o.optString("code", ""),
                            teacher = o.optString("teacher", ""),
                            location = o.optString("location", ""),
                            dayOfWeek = o.optInt("dayOfWeek", 1),
                            startPeriod = o.optInt("startPeriod", 1),
                            endPeriod = o.optInt("endPeriod", 1),
                            startTime = o.optString("startTime", ""),
                            endTime = o.optString("endTime", ""),
                            credits = o.optDouble("credits", 3.0),
                            category = runCatching { CourseCategory.valueOf(o.optString("category", "REQUIRED")) }.getOrDefault(CourseCategory.REQUIRED),
                            requirementType = runCatching { CourseRequirementType.valueOf(o.optString("requirementType", "REQUIRED")) }.getOrDefault(CourseRequirementType.REQUIRED),
                            generalEduSubtype = runCatching {
                                when (val s = o.optString("generalEduSubtype", "NONE")) {
                                    "HUMANITIES" -> GeneralEduSubtype.CORE_HUMANITIES
                                    "SOCIAL_SCIENCE" -> GeneralEduSubtype.CORE_SOCIAL
                                    "NATURAL_SCIENCE" -> GeneralEduSubtype.CORE_NATURAL
                                    "CORE" -> GeneralEduSubtype.CHINESE
                                    "INTERDISCIPLINARY" -> GeneralEduSubtype.CORE_HUMANITIES
                                    else -> GeneralEduSubtype.valueOf(s)
                                }
                            }.getOrDefault(GeneralEduSubtype.NONE),
                            subcategory = o.optString("subcategory", ""),
                            semester = o.optString("semester", "113-2"),
                            score = if (o.isNull("score")) null else o.optDouble("score"),
                            letterGrade = if (o.isNull("letterGrade")) null else o.optString("letterGrade"),
                            isCompleted = o.optBoolean("isCompleted", false),
                            colorHex = o.optString("colorHex", "#3B82F6"),
                            notes = o.optString("notes", "")
                        )
                    )
                }
                courseDao.deleteAllCourses()
                courseDao.insertCourses(courseList)
            }

            if (root.has("thresholds")) {
                val arr = root.getJSONArray("thresholds")
                val list = mutableListOf<GraduationThreshold>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        GraduationThreshold(
                            title = o.getString("title"),
                            description = o.optString("description", ""),
                            isCompleted = o.optBoolean("isCompleted", false),
                            completedDate = o.optString("completedDate", ""),
                            proofNote = o.optString("proofNote", "")
                        )
                    )
                }
                graduationDao.deleteAllThresholds()
                graduationDao.insertThresholds(list)
            }

            if (root.has("expenses")) {
                val arr = root.getJSONArray("expenses")
                val list = mutableListOf<ExpenseRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        ExpenseRecord(
                            title = o.getString("title"),
                            amount = o.getDouble("amount"),
                            type = runCatching { ExpenseType.valueOf(o.optString("type", "EXPENSE")) }.getOrDefault(ExpenseType.EXPENSE),
                            category = runCatching { ExpenseCategory.valueOf(o.optString("category", "FOOD")) }.getOrDefault(ExpenseCategory.FOOD),
                            paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod", "CASH")) }.getOrDefault(PaymentMethod.CASH),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            dateString = o.optString("dateString", ""),
                            note = o.optString("note", "")
                        )
                    )
                }
                expenseDao.deleteAllExpenses()
                expenseDao.insertExpenses(list)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Notifications
    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications()
    val unreadNotificationCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun insertNotification(notification: AppNotification): Long = withContext(Dispatchers.IO) {
        notificationDao.insertNotification(notification)
    }

    suspend fun insertNotifications(notifications: List<AppNotification>) = withContext(Dispatchers.IO) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAll()
    }

    suspend fun getAllNotificationsOnce(): List<AppNotification> = withContext(Dispatchers.IO) {
        notificationDao.getAllNotificationsOnce()
    }

    fun getNotificationPreferences(): NotificationPreferences {
        return NotificationPreferences(
            masterEnabled = prefs.getBoolean("notif_master_enabled", true),
            // 1. 課表與課程提醒
            courseReminderEnabled = prefs.getBoolean("notif_course_enabled", true),
            courseReminderMinutesBefore = prefs.getInt("notif_course_minutes", 15),
            courseDailySummaryEnabled = prefs.getBoolean("notif_course_daily_summary", true),
            courseDailySummaryTime = prefs.getString("notif_course_daily_summary_time", "07:30") ?: "07:30",
            courseChangeNoticeEnabled = prefs.getBoolean("notif_course_change_notice", true),
            courseOnlyInSession = prefs.getBoolean("notif_course_only_in_session", true),
            // 2. 記帳與預算警示
            expenseAlertEnabled = prefs.getBoolean("notif_expense_enabled", true),
            expenseAlertThresholdPercent = prefs.getInt("notif_expense_threshold", 75),
            expenseDailyReminderEnabled = prefs.getBoolean("notif_expense_daily_reminder", false),
            expenseDailyReminderTime = prefs.getString("notif_expense_daily_reminder_time", "21:30") ?: "21:30",
            expenseMonthlyReportEnabled = prefs.getBoolean("notif_expense_monthly_report", true),
            expenseTransactionNoticeEnabled = prefs.getBoolean("notif_expense_transaction_notice", true),
            // 3. 學業與畢業審查
            graduationAlertEnabled = prefs.getBoolean("notif_graduation_enabled", true),
            graduationCreditThresholdNotice = prefs.getBoolean("notif_graduation_credit_threshold", true),
            graduationGpaSettlementNotice = prefs.getBoolean("notif_graduation_gpa_settlement", true),
            graduationAuditAlertNotice = prefs.getBoolean("notif_graduation_audit_alert", true),
            // 4. 系統與備份
            systemNoticeEnabled = prefs.getBoolean("notif_system_enabled", true),
            systemCloudBackupNotice = prefs.getBoolean("notif_system_cloud_backup", true),
            systemUpdateNotice = prefs.getBoolean("notif_system_update", true),
            // 5. 提醒方式
            vibrationEnabled = prefs.getBoolean("notif_vibration_enabled", true),
            badgeEnabled = prefs.getBoolean("notif_badge_enabled", true)
        )
    }

    fun saveNotificationPreferences(preferences: NotificationPreferences) {
        prefs.edit {
            putBoolean("notif_master_enabled", preferences.masterEnabled)
            // 1. 課表與課程提醒
            putBoolean("notif_course_enabled", preferences.courseReminderEnabled)
            putInt("notif_course_minutes", preferences.courseReminderMinutesBefore)
            putBoolean("notif_course_daily_summary", preferences.courseDailySummaryEnabled)
            putString("notif_course_daily_summary_time", preferences.courseDailySummaryTime)
            putBoolean("notif_course_change_notice", preferences.courseChangeNoticeEnabled)
            putBoolean("notif_course_only_in_session", preferences.courseOnlyInSession)
            // 2. 記帳與預算警示
            putBoolean("notif_expense_enabled", preferences.expenseAlertEnabled)
            putInt("notif_expense_threshold", preferences.expenseAlertThresholdPercent)
            putBoolean("notif_expense_daily_reminder", preferences.expenseDailyReminderEnabled)
            putString("notif_expense_daily_reminder_time", preferences.expenseDailyReminderTime)
            putBoolean("notif_expense_monthly_report", preferences.expenseMonthlyReportEnabled)
            putBoolean("notif_expense_transaction_notice", preferences.expenseTransactionNoticeEnabled)
            // 3. 學業與畢業審查
            putBoolean("notif_graduation_enabled", preferences.graduationAlertEnabled)
            putBoolean("notif_graduation_credit_threshold", preferences.graduationCreditThresholdNotice)
            putBoolean("notif_graduation_gpa_settlement", preferences.graduationGpaSettlementNotice)
            putBoolean("notif_graduation_audit_alert", preferences.graduationAuditAlertNotice)
            // 4. 系統與備份
            putBoolean("notif_system_enabled", preferences.systemNoticeEnabled)
            putBoolean("notif_system_cloud_backup", preferences.systemCloudBackupNotice)
            putBoolean("notif_system_update", preferences.systemUpdateNotice)
            // 5. 提醒方式
            putBoolean("notif_vibration_enabled", preferences.vibrationEnabled)
            putBoolean("notif_badge_enabled", preferences.badgeEnabled)
        }
    }
}
