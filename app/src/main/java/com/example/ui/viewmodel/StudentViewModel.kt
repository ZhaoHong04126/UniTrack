package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultData
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.StudentRepository
import com.example.util.NotificationHelper
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

data class SemesterGpa(
    val semester: String,
    val gpa: Double,
    val averageScore: Double,
    val totalCredits: Double,
    val passedCredits: Double,
    val courseCount: Int
)

data class SubcategoryCreditSummary(
    val label: String, // "必修" 或 "選修"
    val earnedCredits: Double,
    val inProgressCredits: Double,
    val targetCredits: Double,
    val percentage: Float
)

data class CreditCategorySummary(
    val category: CourseCategory,
    val earnedCredits: Double,
    val inProgressCredits: Double,
    val targetCredits: Double,
    val percentage: Float,
    val requiredSummary: SubcategoryCreditSummary? = null,
    val electiveSummary: SubcategoryCreditSummary? = null
)

data class GraduationAuditSummary(
    val plan: GraduationPlan,
    val totalEarnedCredits: Double,
    val totalInProgressCredits: Double,
    val totalTargetCredits: Double,
    val overallPercentage: Float,
    val requiredSummary: CreditCategorySummary,
    val electiveSummary: CreditCategorySummary,
    val generalSummary: CreditCategorySummary,
    val collegeCoreSummary: CreditCategorySummary,
    val basicModuleSummary: CreditCategorySummary,
    val coreModuleSummary: CreditCategorySummary,
    val professionalModuleSummary: CreditCategorySummary,
    val freeSummary: CreditCategorySummary,
    val unspecifiedSummary: CreditCategorySummary = CreditCategorySummary(CourseCategory.UNSPECIFIED, 0.0, 0.0, 0.0, 0f),
    val unspecifiedCourses: List<Course> = emptyList(),
    val peCredits: Double,
    val thresholdsCompletedCount: Int,
    val thresholdsTotalCount: Int,
    val isEligibleToGraduate: Boolean
)

data class ExpenseMonthlySummary(
    val yearMonth: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double,
    val budgetAmount: Double,
    val remainingBudget: Double,
    val budgetUsagePercentage: Float,
    val categoryBreakdown: Map<ExpenseCategory, Double>
)

@Suppress("unused")
class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudentRepository
    private val authRepository: AuthRepository = AuthRepository(application.applicationContext)

    val authState: StateFlow<AuthState> = authRepository.authState
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Persistent App Preferences
    @Suppress("SpellCheckingInspection")
    private val prefs = application.getSharedPreferences("unitrack_prefs", Context.MODE_PRIVATE)

    // Timetable display setting (true: 7 days / 一週, false: 5 days / 平日)
    private val _showWeekend = MutableStateFlow(prefs.getBoolean("pref_show_weekend", false))
    val showWeekend: StateFlow<Boolean> = _showWeekend.asStateFlow()

    fun setShowWeekend(show: Boolean) {
        _showWeekend.value = show
        prefs.edit { putBoolean("pref_show_weekend", show) }
    }

    // Weekly Mode setting (true: 各週模式/左右滑動顯示日期, false: 整學期模式)
    private val _isWeeklyMode = MutableStateFlow(prefs.getBoolean("pref_is_weekly_mode", true))
    val isWeeklyMode: StateFlow<Boolean> = _isWeeklyMode.asStateFlow()

    fun setIsWeeklyMode(isWeekly: Boolean) {
        _isWeeklyMode.value = isWeekly
        prefs.edit { putBoolean("pref_is_weekly_mode", isWeekly) }
    }

    // Timeline display setting (false: 節次 1,2,3... / true: 時間 08:10...)
    private val _showTimeInsteadOfPeriod = MutableStateFlow(prefs.getBoolean("pref_show_timeline_as_time", false))
    val showTimeInsteadOfPeriod: StateFlow<Boolean> = _showTimeInsteadOfPeriod.asStateFlow()

    fun setShowTimeInsteadOfPeriod(showTime: Boolean) {
        _showTimeInsteadOfPeriod.value = showTime
        prefs.edit { putBoolean("pref_show_timeline_as_time", showTime) }
    }

    // Semester Time Settings (開學日 & 總週數)
    private val _semesterTimeConfigVersion = MutableStateFlow(0)
    val semesterTimeConfigVersion: StateFlow<Int> = _semesterTimeConfigVersion.asStateFlow()

    fun getSemesterStartDate(semester: String): String {
        val key = "semester_start_date_$semester"
        val saved = prefs.getString(key, null)
        if (!saved.isNullOrBlank()) return saved
        val semYear = semester.substringBefore("-").filter { it.isDigit() }.toIntOrNull() ?: 114
        val semTerm = semester.substringAfter("-").filter { it.isDigit() }.toIntOrNull() ?: 1
        val westernYear = semYear + 1911
        return try {
            if (semTerm == 1) {
                var d = java.time.LocalDate.of(westernYear, 9, 7)
                while (d.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                    d = d.plusDays(1)
                }
                d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
            } else {
                var d = java.time.LocalDate.of(westernYear + 1, 2, 16)
                while (d.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                    d = d.plusDays(1)
                }
                d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
            }
        } catch (_: Throwable) {
            if (semTerm == 1) "$westernYear.09.07" else "${westernYear + 1}.02.16"
        }
    }

    fun getSemesterEndDate(semester: String): String {
        val key = "semester_end_date_$semester"
        val saved = prefs.getString(key, null)
        if (!saved.isNullOrBlank()) return saved
        val startDateStr = getSemesterStartDate(semester)
        val totalWeeks = getSemesterTotalWeeks(semester)
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val startDate = java.time.LocalDate.parse(startDateStr, formatter)
            val endDate = startDate.plusWeeks(totalWeeks.toLong()).minusDays(1)
            endDate.format(formatter)
        } catch (_: Exception) {
            ""
        }
    }

    fun getSemesterTotalWeeks(semester: String): Int {
        return prefs.getInt("semester_total_weeks_$semester", 18)
    }

    fun getSemesterScheduleStatus(semester: String): SemesterScheduleStatus {
        val startDateStr = getSemesterStartDate(semester)
        val totalWeeks = getSemesterTotalWeeks(semester)
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val startDate = java.time.LocalDate.parse(startDateStr, formatter)
            val today = java.time.LocalDate.now()
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, startDate)
            when {
                daysDiff > 0 -> SemesterScheduleStatus.NotStarted(daysDiff, startDateStr)
                daysDiff == 0L -> SemesterScheduleStatus.InSession(1, totalWeeks)
                else -> {
                    val daysPassed = -daysDiff
                    val weekNum = (daysPassed / 7).toInt() + 1
                    if (weekNum <= totalWeeks) SemesterScheduleStatus.InSession(weekNum, totalWeeks)
                    else SemesterScheduleStatus.Ended(totalWeeks)
                }
            }
        } catch (_: Exception) {
            SemesterScheduleStatus.InSession(1, totalWeeks)
        }
    }

    fun isSemesterInSession(semester: String): Boolean {
        return getSemesterScheduleStatus(semester) is SemesterScheduleStatus.InSession
    }

    fun parseCourseTimeToMinutes(timeStr: String, fallbackPeriod: Int, isStart: Boolean): Int {
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

    fun getCurrentMinutes(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    fun isCourseEndedToday(course: Course, nowMinutes: Int = getCurrentMinutes()): Boolean {
        val endMin = parseCourseTimeToMinutes(course.endTime, course.endPeriod, false)
        return nowMinutes > endMin
    }

    fun isCourseOngoingToday(course: Course, nowMinutes: Int = getCurrentMinutes()): Boolean {
        val startMin = parseCourseTimeToMinutes(course.startTime, course.startPeriod, true)
        val endMin = parseCourseTimeToMinutes(course.endTime, course.endPeriod, false)
        return nowMinutes in startMin..endMin
    }

    fun saveSemesterTimeConfig(semester: String, startDate: String, totalWeeks: Int) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val endDate = try {
            val s = java.time.LocalDate.parse(startDate, formatter)
            s.plusWeeks(totalWeeks.toLong()).minusDays(1).format(formatter)
        } catch (_: Exception) { "" }
        saveSemesterTimeConfig(semester, startDate, endDate, totalWeeks)
    }

    fun saveSemesterTimeConfig(semester: String, startDate: String, endDate: String, totalWeeks: Int) {
        prefs.edit {
            putString("semester_start_date_$semester", startDate)
            putString("semester_end_date_$semester", endDate)
            putInt("semester_total_weeks_$semester", totalWeeks.coerceIn(1, 30))
        }
        _semesterTimeConfigVersion.value += 1
        WidgetUpdateHelper.updateAllWidgets(getApplication())
    }

    fun getCourseAttendance(courseId: Long): Map<Int, String> {
        val key = "course_attendance_$courseId"
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return try {
            val json = org.json.JSONObject(raw)
            val map = mutableMapOf<Int, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val week = k.toIntOrNull()
                if (week != null) {
                    map[week] = json.getString(k)
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveCourseAttendance(courseId: Long, attendance: Map<Int, String>) {
        val key = "course_attendance_$courseId"
        val json = org.json.JSONObject()
        attendance.forEach { (week, status) ->
            json.put(week.toString(), status)
        }
        prefs.edit {
            putString(key, json.toString())
        }
        _semesterTimeConfigVersion.value += 1
    }

    fun getCourseNotes(courseId: Long): List<CourseNote> {
        val key = "course_notes_$courseId"
        val raw = prefs.getString(key, null)
        if (!raw.isNullOrBlank()) {
            return try {
                val arr = org.json.JSONArray(raw)
                val list = mutableListOf<CourseNote>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CourseNote(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            category = obj.optString("category", "一般"),
                            content = obj.optString("content", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            week = if (obj.has("week") && !obj.isNull("week")) obj.getInt("week") else null
                        )
                    )
                }
                list.sortedByDescending { it.timestamp }
            } catch (_: Exception) {
                emptyList()
            }
        }
        return emptyList()
    }

    fun saveCourseNotes(courseId: Long, notes: List<CourseNote>) {
        val key = "course_notes_$courseId"
        val arr = org.json.JSONArray()
        notes.forEach { n ->
            val obj = org.json.JSONObject().apply {
                put("id", n.id)
                put("category", n.category)
                put("content", n.content)
                put("timestamp", n.timestamp)
                if (n.week != null) put("week", n.week) else put("week", org.json.JSONObject.NULL)
            }
            arr.put(obj)
        }
        prefs.edit {
            putString(key, arr.toString())
        }
        _semesterTimeConfigVersion.value += 1
    }

    // Selected semester in timetable
    private val _selectedSemester = MutableStateFlow(DefaultData.getCurrentAcademicSemester())
    val selectedSemester: StateFlow<String> = _selectedSemester.asStateFlow()

    // Selected month in expense tracker
    private val _selectedExpenseMonth = MutableStateFlow(monthFormat.format(Date()))
    val selectedExpenseMonth: StateFlow<String> = _selectedExpenseMonth.asStateFlow()

    // Toast message state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val firestoreSyncRepository: FirestoreSyncRepository

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = StudentRepository(
            application.applicationContext,
            db.courseDao(),
            db.graduationDao(),
            db.expenseDao(),
            db.notificationDao()
        )
        firestoreSyncRepository = FirestoreSyncRepository(
            application.applicationContext,
            db.courseDao(),
            db.graduationDao(),
            db.expenseDao()
        )
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            checkAndGenerateSmartNotifications()
        }
        viewModelScope.launch {
            currentUser.collect { profile ->
                if (profile != null) {
                    // 1. 優先從雲端下載最新檔案（防止本機空資料覆蓋雲端）
                    firestoreSyncRepository.downloadAllFromCloud(profile.uid)

                    // 2. 下載完成後，檢查使用者名稱是否需要填入
                    val nameToSet = profile.displayName?.ifBlank { null }
                        ?: profile.email?.substringBefore("@")
                    if (!nameToSet.isNullOrBlank()) {
                        val currentPlan = repository.getGraduationPlanOnce() ?: repository.getCachedGraduationPlan()
                        if (currentPlan.studentName == "同學" || currentPlan.studentName == "王大明" || currentPlan.studentName == "大學生" || currentPlan.studentName.isBlank()) {
                            val updatedPlan = currentPlan.copy(studentName = nameToSet)
                            repository.updateGraduationPlan(updatedPlan)
                            if (updatedPlan.department.isNotBlank() && updatedPlan.department != "尚未設定系所") {
                                firestoreSyncRepository.uploadAllToCloud(profile.uid)
                            }
                        }
                    }

                    // 3. 首次登入發送歡迎通知（僅發送一次）
                    val welcomeKey = "has_welcomed_user_${profile.uid}"
                    if (!prefs.getBoolean(welcomeKey, false)) {
                        prefs.edit { putBoolean(welcomeKey, true) }
                        sendNotification(
                            title = "歡迎使用 UniTrack+",
                            message = "UniTrack+ 智慧學業助理已準備就緒，隨時為您管理課表、畢業審查與記帳！",
                            type = NotificationType.SYSTEM,
                            actionRoute = "notifications",
                            sendSystemPush = false
                        )
                    }
                }
            }
        }
    }

    // Raw database flows
    val allCourses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customSemesters = MutableStateFlow<Set<String>>(emptySet())

    val allSemesters: StateFlow<List<String>> = combine(
        repository.allSemesters,
        repository.graduationPlan,
        _selectedSemester,
        _customSemesters
    ) { dbSemesters, plan, currentSem, customSemesters ->
        val set = dbSemesters.toMutableSet()
        val rawAdmission = plan?.admissionSemester ?: DefaultData.getCurrentAcademicSemester()
        val startYear = rawAdmission.substringBefore("-").filter { it.isDigit() }.toIntOrNull()
            ?: (Calendar.getInstance().get(Calendar.YEAR) - 1911)
        // Automatically generate semesters from admission year up to 4 years, but ONLY if the semester has started
        val now = Date()
        for (y in startYear until startYear + 4) {
            val sem1 = "$y-1"
            val sem2 = "$y-2"
            if (DefaultData.hasSemesterStarted(sem1, now)) {
                set.add(sem1)
            }
            if (DefaultData.hasSemesterStarted(sem2, now)) {
                set.add(sem2)
            }
        }
        if (currentSem.isNotBlank()) set.add(currentSem)
        set.addAll(customSemesters)
        set.toList().sorted()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        run {
            val now = Date()
            val base = (Calendar.getInstance().get(Calendar.YEAR) - 1911)
            (base until base + 4).flatMap { listOf("$it-1", "$it-2") }
                .filter { DefaultData.hasSemesterStarted(it, now) }
                .ifEmpty { listOf(DefaultData.getCurrentAcademicSemester()) }
        }
    )

    val graduationPlan: StateFlow<GraduationPlan> = repository.graduationPlan
        .map { it ?: repository.getCachedGraduationPlan() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.getCachedGraduationPlan())

    val graduationThresholds: StateFlow<List<GraduationThreshold>> = repository.allThresholds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseRecord>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<MonthlyBudget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications flows
    val allNotifications: StateFlow<List<AppNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    private val _notificationPreferences = MutableStateFlow(repository.getNotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences.asStateFlow()

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        val oldPrefs = _notificationPreferences.value
        _notificationPreferences.value = preferences
        repository.saveNotificationPreferences(preferences)

        // 當使用者開啟預算警示或更改門檻時，若當月支出已超過或等於新門檻，立即發送推播提醒！
        if (preferences.masterEnabled && preferences.expenseAlertEnabled) {
            val thresholdChanged = oldPrefs.expenseAlertThresholdPercent != preferences.expenseAlertThresholdPercent
            val justEnabled = !oldPrefs.expenseAlertEnabled || !oldPrefs.masterEnabled
            if (thresholdChanged || justEnabled) {
                checkExpenseBudgetAlert(_selectedExpenseMonth.value)
            }
        }
    }

    fun sendNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.SYSTEM,
        actionRoute: String? = null,
        sendSystemPush: Boolean = true
    ) {
        val prefs = _notificationPreferences.value
        if (!prefs.masterEnabled) return
        val isTypeEnabled = when (type) {
            NotificationType.COURSE -> prefs.courseReminderEnabled
            NotificationType.EXPENSE -> prefs.expenseAlertEnabled
            NotificationType.GRADUATION -> prefs.graduationAlertEnabled
            NotificationType.SYSTEM -> prefs.systemNoticeEnabled
        }
        if (!isTypeEnabled) return

        viewModelScope.launch {
            repository.insertNotification(
                AppNotification(
                    title = title,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    actionRoute = actionRoute
                )
            )
            if (sendSystemPush) {
                NotificationHelper.sendSystemNotification(
                    context = getApplication(),
                    title = title,
                    message = message,
                    type = type,
                    actionRoute = actionRoute
                )
            }
        }
    }

    fun sendTestSystemNotification() {
        val threshold = _notificationPreferences.value.expenseAlertThresholdPercent
        val testSamples = listOf(
            Triple("今日上課提醒", "下午 14:00 有「線性代數」課程，教室：理學院 302。", NotificationType.COURSE),
            Triple("記帳預算警示", "本月份生活預算已使用達 $threshold%，請留意近期支出。", NotificationType.EXPENSE),
            Triple("學業審查進度更新", "恭喜！您已滿足本系基礎模組必修 24 學分門檻。", NotificationType.GRADUATION),
            Triple("UniTrack+ 系統推播測試", "這是一則測試通知，代表手機系統推播功能運作正常！", NotificationType.SYSTEM)
        )
        val sample = testSamples.random()
        sendNotification(
            title = sample.first,
            message = sample.second,
            type = sample.third,
            actionRoute = when (sample.third) {
                NotificationType.COURSE -> "timetable"
                NotificationType.EXPENSE -> "expense"
                NotificationType.GRADUATION -> "graduation"
                NotificationType.SYSTEM -> "notifications"
            },
            sendSystemPush = true
        )
    }

    fun checkAndGenerateSmartNotifications() {
        // Real notifications are generated on-demand by events (course changes, budget thresholds, etc.)
    }

    // Filtered courses for currently selected semester
    val currentSemesterCourses: StateFlow<List<Course>> = combine(allCourses, _selectedSemester) { courses, sem ->
        courses.filter { it.semester == sem }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's classes for current active semester
    val todayClasses: StateFlow<List<Course>> = combine(allCourses, graduationPlan) { courses, plan ->
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
        courses.filter { it.semester == plan.currentSemester && it.dayOfWeek == dayOfWeekToday }
            .sortedBy { it.startPeriod }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // GPA calculations
    val semesterGpaList: StateFlow<List<SemesterGpa>> = combine(allCourses, graduationPlan, allSemesters) { courses, plan, semList ->
        val result = mutableListOf<SemesterGpa>()

        for (sem in semList) {
            val semCourses = courses.filter { it.semester == sem }
            val gradedCourses = semCourses.filter { it.score != null || it.letterGrade != null }
            if (gradedCourses.isEmpty()) {
                val totalCredits = semCourses.sumOf { it.credits }
                result.add(SemesterGpa(sem, 0.0, 0.0, totalCredits, 0.0, semCourses.size))
                continue
            }

            var totalWeightedScore = 0.0
            var totalGradedCredits = 0.0
            var passedCredits = 0.0

            for (c in gradedCourses) {
                val scoreVal = c.score ?: scoreFromLetterGrade(c.letterGrade) ?: 0.0

                totalWeightedScore += scoreVal * c.credits
                totalGradedCredits += c.credits

                if (c.isCompleted || (c.score != null && c.score >= plan.minPassingScore) || (c.letterGrade != null && c.letterGrade != "F" && c.letterGrade != "E")) {
                    passedCredits += c.credits
                }
            }

            val semAvg = if (totalGradedCredits > 0) totalWeightedScore / totalGradedCredits else 0.0
            val totalCredits = semCourses.sumOf { it.credits }

            result.add(
                SemesterGpa(
                    semester = sem,
                    gpa = round(semAvg * 10.0) / 10.0,
                    averageScore = round(semAvg * 10.0) / 10.0,
                    totalCredits = totalCredits,
                    passedCredits = passedCredits,
                    courseCount = semCourses.size
                )
            )
        }
        result.sortedByDescending { it.semester }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cumulative GPA & Average Score (百分制均分)
    val cumulativeAcademicSummary: StateFlow<Pair<Double, Double>> = combine(allCourses, graduationPlan) { courses, _ ->
        val gradedCourses = courses.filter { it.score != null || it.letterGrade != null }
        if (gradedCourses.isEmpty()) return@combine Pair(0.0, 0.0)

        var totalWeightedScore = 0.0
        var totalGradedCredits = 0.0

        for (c in gradedCourses) {
            val scoreVal = c.score ?: scoreFromLetterGrade(c.letterGrade) ?: 0.0

            totalWeightedScore += scoreVal * c.credits
            totalGradedCredits += c.credits
        }

        val avg = if (totalGradedCredits > 0) totalWeightedScore / totalGradedCredits else 0.0
        val roundedAvg = round(avg * 10.0) / 10.0

        Pair(roundedAvg, roundedAvg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // Graduation Audit Summary
    val graduationAudit: StateFlow<GraduationAuditSummary> = combine(
        allCourses,
        graduationPlan,
        graduationThresholds
    ) { courses, plan, thresholds ->
        var earnedRequired = 0.0
        var inProgressRequired = 0.0

        var earnedElective = 0.0
        var inProgressElective = 0.0

        var earnedGeneral = 0.0
        var inProgressGeneral = 0.0
        var earnedGeneralReq = 0.0
        var inProgressGeneralReq = 0.0
        var earnedGeneralEle = 0.0
        var inProgressGeneralEle = 0.0

        var earnedCollegeCore = 0.0
        var inProgressCollegeCore = 0.0
        var earnedCollegeCoreReq = 0.0
        var inProgressCollegeCoreReq = 0.0
        var earnedCollegeCoreEle = 0.0
        var inProgressCollegeCoreEle = 0.0

        var earnedBasicModule = 0.0
        var inProgressBasicModule = 0.0
        var earnedBasicModuleReq = 0.0
        var inProgressBasicModuleReq = 0.0
        var earnedBasicModuleEle = 0.0
        var inProgressBasicModuleEle = 0.0

        var earnedCoreModule = 0.0
        var inProgressCoreModule = 0.0
        var earnedCoreModuleReq = 0.0
        var inProgressCoreModuleReq = 0.0
        var earnedCoreModuleEle = 0.0
        var inProgressCoreModuleEle = 0.0

        var earnedProfessionalModule = 0.0
        var inProgressProfessionalModule = 0.0
        var earnedProfessionalModuleReq = 0.0
        var inProgressProfessionalModuleReq = 0.0
        var earnedProfessionalModuleEle = 0.0
        var inProgressProfessionalModuleEle = 0.0

        var earnedFree = 0.0
        var inProgressFree = 0.0
        var earnedFreeEle = 0.0
        var inProgressFreeEle = 0.0

        var earnedUnspecified = 0.0
        var inProgressUnspecified = 0.0
        val unspecifiedCourses = mutableListOf<Course>()

        var peCredits = 0.0

        for (c in courses) {
            val isPassed = c.isCompleted || (c.score != null && c.score >= plan.minPassingScore)
            val isInProgress = !isPassed && (c.semester == plan.currentSemester || c.score == null)
            val isReq = c.requirementType == CourseRequirementType.REQUIRED || c.requirementType == CourseRequirementType.REQUIRED_ELECTIVE

            val isUnspecified = c.category == CourseCategory.UNSPECIFIED || c.requirementType == CourseRequirementType.UNSPECIFIED
            if (isUnspecified) {
                unspecifiedCourses.add(c)
            }

            when (c.category) {
                CourseCategory.REQUIRED -> {
                    if (isPassed) earnedRequired += c.credits
                    else if (isInProgress) inProgressRequired += c.credits
                }
                CourseCategory.ELECTIVE -> {
                    if (isPassed) earnedElective += c.credits
                    else if (isInProgress) inProgressElective += c.credits
                }
                CourseCategory.GENERAL_EDU -> {
                    if (isPassed) {
                        earnedGeneral += c.credits
                        if (isReq) earnedGeneralReq += c.credits else earnedGeneralEle += c.credits
                    } else if (isInProgress) {
                        inProgressGeneral += c.credits
                        if (isReq) inProgressGeneralReq += c.credits else inProgressGeneralEle += c.credits
                    }
                }
                CourseCategory.COLLEGE_CORE -> {
                    if (isPassed) {
                        earnedCollegeCore += c.credits
                        if (isReq) earnedCollegeCoreReq += c.credits else earnedCollegeCoreEle += c.credits
                    } else if (isInProgress) {
                        inProgressCollegeCore += c.credits
                        if (isReq) inProgressCollegeCoreReq += c.credits else inProgressCollegeCoreEle += c.credits
                    }
                }
                CourseCategory.BASIC_MODULE -> {
                    if (isPassed) {
                        earnedBasicModule += c.credits
                        if (isReq) earnedBasicModuleReq += c.credits else earnedBasicModuleEle += c.credits
                    } else if (isInProgress) {
                        inProgressBasicModule += c.credits
                        if (isReq) inProgressBasicModuleReq += c.credits else inProgressBasicModuleEle += c.credits
                    }
                }
                CourseCategory.CORE_MODULE -> {
                    if (isPassed) {
                        earnedCoreModule += c.credits
                        if (isReq) earnedCoreModuleReq += c.credits else earnedCoreModuleEle += c.credits
                    } else if (isInProgress) {
                        inProgressCoreModule += c.credits
                        if (isReq) inProgressCoreModuleReq += c.credits else inProgressCoreModuleEle += c.credits
                    }
                }
                CourseCategory.PROFESSIONAL_MODULE -> {
                    if (isPassed) {
                        earnedProfessionalModule += c.credits
                        if (isReq) earnedProfessionalModuleReq += c.credits else earnedProfessionalModuleEle += c.credits
                    } else if (isInProgress) {
                        inProgressProfessionalModule += c.credits
                        if (isReq) inProgressProfessionalModuleReq += c.credits else inProgressProfessionalModuleEle += c.credits
                    }
                }
                CourseCategory.FREE_ELECTIVE -> {
                    if (isPassed) {
                        earnedFree += c.credits
                        earnedFreeEle += c.credits
                    } else if (isInProgress) {
                        inProgressFree += c.credits
                        inProgressFreeEle += c.credits
                    }
                }
                CourseCategory.UNSPECIFIED -> {
                    if (isPassed) earnedUnspecified += c.credits
                    else if (isInProgress) inProgressUnspecified += c.credits
                }
                CourseCategory.PE -> {
                    if (isPassed) peCredits += c.credits
                }
            }
        }

        val totalEarned = earnedRequired + earnedElective + earnedGeneral + earnedCollegeCore + earnedBasicModule + earnedCoreModule + earnedProfessionalModule + earnedFree + earnedUnspecified
        val totalInProgress = inProgressRequired + inProgressElective + inProgressGeneral + inProgressCollegeCore + inProgressBasicModule + inProgressCoreModule + inProgressProfessionalModule + inProgressFree + inProgressUnspecified
        val targetTotal = plan.targetTotalCredits

        val overallPercentage = if (targetTotal > 0.0) ((totalEarned / targetTotal) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

        val reqPercentage = if (plan.targetRequiredCredits > 0.0) ((earnedRequired / plan.targetRequiredCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val elePercentage = if (plan.targetElectiveCredits > 0.0) ((earnedElective / plan.targetElectiveCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val genPercentage = if (plan.targetGeneralCredits > 0.0) ((earnedGeneral / plan.targetGeneralCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val colPercentage = if (plan.targetCollegeCoreCredits > 0.0) ((earnedCollegeCore / plan.targetCollegeCoreCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val basPercentage = if (plan.targetBasicModuleCredits > 0.0) ((earnedBasicModule / plan.targetBasicModuleCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val corPercentage = if (plan.targetCoreModuleCredits > 0.0) ((earnedCoreModule / plan.targetCoreModuleCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val proPercentage = if (plan.targetProfessionalModuleCredits > 0.0) ((earnedProfessionalModule / plan.targetProfessionalModuleCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val freePercentage = if (plan.targetFreeCredits > 0.0) ((earnedFree / plan.targetFreeCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

        fun subSummary(label: String, earned: Double, inProgress: Double, target: Double): SubcategoryCreditSummary {
            val pct = if (target > 0.0) ((earned / target) * 100.0).coerceIn(0.0, 100.0).toFloat() else if (earned > 0.0) 100f else 0f
            return SubcategoryCreditSummary(label, earned, inProgress, target, pct)
        }

        val completedThresholds = thresholds.count { it.isCompleted }
        val totalThresholds = thresholds.size

        val isEligible = totalEarned >= plan.targetTotalCredits &&
                earnedGeneral >= plan.targetGeneralCredits &&
                earnedCollegeCore >= plan.targetCollegeCoreCredits &&
                earnedBasicModule >= plan.targetBasicModuleCredits &&
                earnedCoreModule >= plan.targetCoreModuleCredits &&
                earnedProfessionalModule >= plan.targetProfessionalModuleCredits &&
                (totalThresholds == 0 || completedThresholds == totalThresholds)

        val genReqTarget = if (plan.targetGeneralRequiredCredits == 0.0 && plan.targetGeneralElectiveCredits == 0.0 && plan.targetGeneralCredits > 0.0) plan.targetGeneralCredits else plan.targetGeneralRequiredCredits
        val colReqTarget = if (plan.targetCollegeCoreRequiredCredits > 0.0) plan.targetCollegeCoreRequiredCredits else plan.targetCollegeCoreCredits
        val basReqTarget = if (plan.targetBasicModuleRequiredCredits == 0.0 && plan.targetBasicModuleElectiveCredits == 0.0 && plan.targetBasicModuleCredits > 0.0) plan.targetBasicModuleCredits else plan.targetBasicModuleRequiredCredits
        val corReqTarget = if (plan.targetCoreModuleRequiredCredits == 0.0 && plan.targetCoreModuleElectiveCredits == 0.0 && plan.targetCoreModuleCredits > 0.0) plan.targetCoreModuleCredits else plan.targetCoreModuleRequiredCredits
        val proReqTarget = if (plan.targetProfessionalModuleRequiredCredits == 0.0 && plan.targetProfessionalModuleElectiveCredits == 0.0 && plan.targetProfessionalModuleCredits > 0.0) plan.targetProfessionalModuleCredits else plan.targetProfessionalModuleRequiredCredits
        val freeEleTarget = if (plan.targetFreeElectiveCredits > 0.0) plan.targetFreeElectiveCredits else plan.targetFreeCredits

        GraduationAuditSummary(
            plan = plan,
            totalEarnedCredits = totalEarned,
            totalInProgressCredits = totalInProgress,
            totalTargetCredits = targetTotal,
            overallPercentage = round(overallPercentage * 10f) / 10f,
            requiredSummary = CreditCategorySummary(CourseCategory.REQUIRED, earnedRequired, inProgressRequired, plan.targetRequiredCredits, reqPercentage),
            electiveSummary = CreditCategorySummary(CourseCategory.ELECTIVE, earnedElective, inProgressElective, plan.targetElectiveCredits, elePercentage),
            generalSummary = CreditCategorySummary(
                CourseCategory.GENERAL_EDU, earnedGeneral, inProgressGeneral, plan.targetGeneralCredits, genPercentage,
                requiredSummary = subSummary("必修", earnedGeneralReq, inProgressGeneralReq, genReqTarget),
                electiveSummary = subSummary("選修", earnedGeneralEle, inProgressGeneralEle, plan.targetGeneralElectiveCredits)
            ),
            collegeCoreSummary = CreditCategorySummary(
                CourseCategory.COLLEGE_CORE, earnedCollegeCore, inProgressCollegeCore, plan.targetCollegeCoreCredits, colPercentage,
                requiredSummary = subSummary("必修", earnedCollegeCoreReq, inProgressCollegeCoreReq, colReqTarget),
                electiveSummary = subSummary("選修", earnedCollegeCoreEle, inProgressCollegeCoreEle, plan.targetCollegeCoreElectiveCredits)
            ),
            basicModuleSummary = CreditCategorySummary(
                CourseCategory.BASIC_MODULE, earnedBasicModule, inProgressBasicModule, plan.targetBasicModuleCredits, basPercentage,
                requiredSummary = subSummary("必修", earnedBasicModuleReq, inProgressBasicModuleReq, basReqTarget),
                electiveSummary = subSummary("選修", earnedBasicModuleEle, inProgressBasicModuleEle, plan.targetBasicModuleElectiveCredits)
            ),
            coreModuleSummary = CreditCategorySummary(
                CourseCategory.CORE_MODULE, earnedCoreModule, inProgressCoreModule, plan.targetCoreModuleCredits, corPercentage,
                requiredSummary = subSummary("必修", earnedCoreModuleReq, inProgressCoreModuleReq, corReqTarget),
                electiveSummary = subSummary("選修", earnedCoreModuleEle, inProgressCoreModuleEle, plan.targetCoreModuleElectiveCredits)
            ),
            professionalModuleSummary = CreditCategorySummary(
                CourseCategory.PROFESSIONAL_MODULE, earnedProfessionalModule, inProgressProfessionalModule, plan.targetProfessionalModuleCredits, proPercentage,
                requiredSummary = subSummary("必修", earnedProfessionalModuleReq, inProgressProfessionalModuleReq, proReqTarget),
                electiveSummary = subSummary("選修", earnedProfessionalModuleEle, inProgressProfessionalModuleEle, plan.targetProfessionalModuleElectiveCredits)
            ),
            freeSummary = CreditCategorySummary(
                CourseCategory.FREE_ELECTIVE, earnedFree, inProgressFree, plan.targetFreeCredits, freePercentage,
                requiredSummary = null,
                electiveSummary = subSummary("選修", earnedFreeEle, inProgressFreeEle, freeEleTarget)
            ),
            unspecifiedSummary = CreditCategorySummary(
                CourseCategory.UNSPECIFIED, earnedUnspecified, inProgressUnspecified, 0.0, 0f
            ),
            unspecifiedCourses = unspecifiedCourses,
            peCredits = peCredits,
            thresholdsCompletedCount = completedThresholds,
            thresholdsTotalCount = totalThresholds,
            isEligibleToGraduate = isEligible
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GraduationAuditSummary(
            plan = DefaultData.getDefaultGraduationPlan(),
            totalEarnedCredits = 0.0,
            totalInProgressCredits = 0.0,
            totalTargetCredits = 128.0,
            overallPercentage = 0f,
            requiredSummary = CreditCategorySummary(CourseCategory.REQUIRED, 0.0, 0.0, 58.0, 0f),
            electiveSummary = CreditCategorySummary(CourseCategory.ELECTIVE, 0.0, 0.0, 36.0, 0f),
            generalSummary = CreditCategorySummary(CourseCategory.GENERAL_EDU, 0.0, 0.0, 28.0, 0f),
            collegeCoreSummary = CreditCategorySummary(CourseCategory.COLLEGE_CORE, 0.0, 0.0, 9.0, 0f),
            basicModuleSummary = CreditCategorySummary(CourseCategory.BASIC_MODULE, 0.0, 0.0, 24.0, 0f),
            coreModuleSummary = CreditCategorySummary(CourseCategory.CORE_MODULE, 0.0, 0.0, 24.0, 0f),
            professionalModuleSummary = CreditCategorySummary(CourseCategory.PROFESSIONAL_MODULE, 0.0, 0.0, 23.0, 0f),
            freeSummary = CreditCategorySummary(CourseCategory.FREE_ELECTIVE, 0.0, 0.0, 20.0, 0f),
            peCredits = 0.0,
            thresholdsCompletedCount = 0,
            thresholdsTotalCount = 0,
            isEligibleToGraduate = false
        )
    )

    private fun loadAccounts(): List<PaymentAccount> {
        val defaultList = listOf(
            PaymentAccount(id = "default_cash", name = "現金", method = PaymentMethod.CASH),
            PaymentAccount(id = "default_student_card", name = "學生證", method = PaymentMethod.IC_CARD),
            PaymentAccount(id = "default_post_office", name = "郵局", method = PaymentMethod.TRANSFER)
        )
        val json = prefs.getString("pref_custom_accounts", null)
        if (json.isNullOrBlank()) {
            return defaultList
        }
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<PaymentAccount>()
            val oldDefaultIds = setOf("default_mobile", "default_ic", "default_card", "default_transfer")
            var containsOldDefaults = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                if (id in oldDefaultIds) {
                    containsOldDefaults = true
                }
                val mName = obj.optString("method", PaymentMethod.CASH.name)
                val m = runCatching { PaymentMethod.valueOf(mName) }.getOrDefault(PaymentMethod.CASH)
                list.add(
                    PaymentAccount(
                        id = id,
                        name = obj.optString("name", m.label),
                        method = m,
                        initialBalance = obj.optDouble("initialBalance", 0.0),
                        note = obj.optString("note", ""),
                        startYearMonth = obj.optString("startYearMonth", "2026-08")
                    )
                )
            }
            if (containsOldDefaults) {
                val cashAccount = list.firstOrNull { it.id == "default_cash" } ?: defaultList[0]
                val customAccountsOnly = list.filterNot { it.id.startsWith("default_") }
                val migrated = listOf(
                    cashAccount,
                    defaultList[1],
                    defaultList[2]
                ) + customAccountsOnly

                val outArray = org.json.JSONArray()
                migrated.forEach { acc ->
                    val obj = org.json.JSONObject()
                    obj.put("id", acc.id)
                    obj.put("name", acc.name)
                    obj.put("method", acc.method.name)
                    obj.put("initialBalance", acc.initialBalance)
                    obj.put("note", acc.note)
                    obj.put("startYearMonth", acc.startYearMonth)
                    outArray.put(obj)
                }
                prefs.edit { putString("pref_custom_accounts", outArray.toString()) }
                migrated
            } else if (list.isEmpty()) {
                defaultList
            } else {
                list
            }
        } catch (_: Throwable) {
            defaultList
        }
    }

    private val _customAccounts = MutableStateFlow(loadAccounts())
    val customAccounts: StateFlow<List<PaymentAccount>> = _customAccounts.asStateFlow()

    private fun saveAccounts(accounts: List<PaymentAccount>) {
        _customAccounts.value = accounts
        val array = org.json.JSONArray()
        accounts.forEach { acc ->
            val obj = org.json.JSONObject()
            obj.put("id", acc.id)
            obj.put("name", acc.name)
            obj.put("method", acc.method.name)
            obj.put("initialBalance", acc.initialBalance)
            obj.put("note", acc.note)
            obj.put("startYearMonth", acc.startYearMonth)
            array.put(obj)
        }
        prefs.edit { putString("pref_custom_accounts", array.toString()) }
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                firestoreSyncRepository.uploadAllToCloud(user.uid)
            }
        }
    }

    // Expense Monthly Summary
    val monthlyExpenseSummary: StateFlow<ExpenseMonthlySummary> = combine(
        allExpenses,
        _selectedExpenseMonth,
        allBudgets,
        _customAccounts
    ) { expenses, month, budgets, accounts ->
        val monthExpenses = expenses.filter { it.dateString.startsWith(month) }
        var totalExp = 0.0
        var totalInc = 0.0
        val catMap = mutableMapOf<ExpenseCategory, Double>()

        for (e in monthExpenses) {
            if (e.type == ExpenseType.EXPENSE) {
                totalExp += e.amount
                catMap[e.category] = (catMap[e.category] ?: 0.0) + e.amount
            } else {
                totalInc += e.amount
            }
        }

        val activeAccounts = accounts.filter { it.startYearMonth <= month }
        val totalInitialBalance = activeAccounts.sumOf { it.initialBalance }
        val cumulativeExpenses = expenses.filter { it.dateString.substringBeforeLast("-") <= month }
        val cumExp = cumulativeExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
        val cumInc = cumulativeExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
        val totalAccountBalance = if (activeAccounts.isEmpty() && cumulativeExpenses.isEmpty()) 0.0 else totalInitialBalance + (cumInc - cumExp)

        val budget = budgets.firstOrNull { it.yearMonth == month }?.budgetAmount ?: 10000.0
        val remaining = budget - totalExp
        val usagePercentage = if (budget > 0) ((totalExp / budget) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

        ExpenseMonthlySummary(
            yearMonth = month,
            totalExpense = totalExp,
            totalIncome = totalInc,
            netBalance = totalAccountBalance,
            budgetAmount = budget,
            remainingBudget = remaining,
            budgetUsagePercentage = round(usagePercentage * 10f) / 10f,
            categoryBreakdown = catMap
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ExpenseMonthlySummary(
            yearMonth = monthFormat.format(Date()),
            totalExpense = 0.0,
            totalIncome = 0.0,
            netBalance = 0.0,
            budgetAmount = 10000.0,
            remainingBudget = 10000.0,
            budgetUsagePercentage = 0f,
            categoryBreakdown = emptyMap()
        )
    )

    // User Actions
    fun setSelectedSemester(semester: String) {
        _selectedSemester.value = semester
        _customSemesters.update { it + semester }
    }

    fun setSelectedExpenseMonth(yearMonth: String) {
        _selectedExpenseMonth.value = yearMonth
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showToast(msg: String) {
        _userMessage.value = msg
    }

    fun addCourse(course: Course, sendNotify: Boolean = true) = viewModelScope.launch {
        repository.insertCourse(course)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        WidgetUpdateHelper.updateAllWidgets(getApplication())
        _userMessage.value = "已成功新增課程：${course.name}"

        if (sendNotify) {
            val dayName = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
                .getOrElse(course.dayOfWeek - 1) { "星期一" }
            val timeInfo = if (course.startTime.isNotBlank()) "$dayName ${course.startTime}" else dayName
            val locationInfo = if (course.location.isNotBlank()) "，教室：${course.location}" else ""
            sendNotification(
                title = "📚 新增課程成功：${course.name}",
                message = "已將「${course.name}」(${course.credits.toInt()} 學分) 加入課表，上課時間：$timeInfo$locationInfo。",
                type = NotificationType.COURSE,
                actionRoute = "timetable",
                sendSystemPush = true
            )
        }
    }

    fun addCourses(courses: List<Course>) = viewModelScope.launch {
        if (courses.isEmpty()) return@launch
        repository.insertCourses(courses)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        WidgetUpdateHelper.updateAllWidgets(getApplication())
        val firstCourse = courses.first()
        _userMessage.value = "已成功新增課程：${firstCourse.name}"

        val dayNames = courses.map { c ->
            val dayName = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
                .getOrElse(c.dayOfWeek - 1) { "星期一" }
            if (c.startTime.isNotBlank()) "$dayName ${c.startTime}" else dayName
        }.distinct().joinToString("、")

        val locationInfo = if (firstCourse.location.isNotBlank()) "，教室：${firstCourse.location}" else ""
        sendNotification(
            title = "📚 新增課程成功：${firstCourse.name}",
            message = "已將「${firstCourse.name}」(${firstCourse.credits.toInt()} 學分) 加入課表，上課時間：$dayNames$locationInfo。",
            type = NotificationType.COURSE,
            actionRoute = "timetable",
            sendSystemPush = true
        )
    }

    fun updateCourse(course: Course, sendNotify: Boolean = true) = viewModelScope.launch {
        repository.updateCourse(course)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        WidgetUpdateHelper.updateAllWidgets(getApplication())
        _userMessage.value = "已更新課程：${course.name}"

        if (sendNotify) {
            val dayName = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
                .getOrElse(course.dayOfWeek - 1) { "星期一" }
            val timeInfo = if (course.startTime.isNotBlank()) "$dayName ${course.startTime}" else dayName
            val locationInfo = if (course.location.isNotBlank()) "，教室：${course.location}" else ""
            sendNotification(
                title = "✏️ 課程資訊已更新：${course.name}",
                message = "已更新「${course.name}」課程內容，上課時間：$timeInfo$locationInfo。",
                type = NotificationType.COURSE,
                actionRoute = "timetable",
                sendSystemPush = true
            )
        }
    }

    fun deleteCourse(course: Course) = viewModelScope.launch {
        repository.deleteCourse(course)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        WidgetUpdateHelper.updateAllWidgets(getApplication())
        _userMessage.value = "已刪除課程：${course.name}"
    }

    fun updateGraduationPlan(plan: GraduationPlan, onComplete: (() -> Unit)? = null) = viewModelScope.launch {
        repository.updateGraduationPlan(plan)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        WidgetUpdateHelper.updateAllWidgets(getApplication())
        onComplete?.invoke()
    }

    fun setPrimarySemester(semester: String) = viewModelScope.launch {
        val currentPlan = repository.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
        val updated = currentPlan.copy(currentSemester = semester)
        repository.updateGraduationPlan(updated)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        WidgetUpdateHelper.updateAllWidgets(getApplication())
        _userMessage.value = "已將 $semester 設定為主要學期"
    }

    fun addThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        repository.insertThreshold(threshold)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已新增畢業門檻項目"
    }

    fun toggleThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        val updated = threshold.copy(
            isCompleted = !threshold.isCompleted,
            completedDate = if (!threshold.isCompleted) dateFormat.format(Date()) else ""
        )
        repository.updateThreshold(updated)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
    }

    fun updateThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        repository.updateThreshold(threshold)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已更新門檻：${threshold.title}"
    }

    fun deleteThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        repository.deleteThreshold(threshold)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已刪除門檻項目"
    }

    fun addExpense(expense: ExpenseRecord) = viewModelScope.launch {
        repository.insertExpense(expense)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已記錄${expense.type.label}：$${expense.amount.toInt()}"

        // 記帳後發送推播通知至通知列
        val isExpense = expense.type == ExpenseType.EXPENSE
        val titleText = if (isExpense) "💸 支出記錄成功" else "💰 收入記錄成功"
        val itemTitle = expense.title.ifBlank { expense.category.label }
        val noteInfo = if (expense.note.isNotBlank()) " (${expense.note})" else ""
        sendNotification(
            title = "$titleText：$itemTitle",
            message = "${expense.category.label} ${if (isExpense) "-$" else "+$"}${expense.amount.toInt()} (${expense.paymentMethod.label}) ｜ 日期：${expense.dateString}$noteInfo",
            type = NotificationType.EXPENSE,
            actionRoute = "expense",
            sendSystemPush = _notificationPreferences.value.expenseTransactionNoticeEnabled
        )

        if (expense.type == ExpenseType.EXPENSE) {
            checkExpenseBudgetAlert(expense.dateString)
        }
    }

    fun updateExpense(expense: ExpenseRecord) = viewModelScope.launch {
        repository.updateExpense(expense)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已更新記錄"

        val isExpense = expense.type == ExpenseType.EXPENSE
        val itemTitle = expense.title.ifBlank { expense.category.label }
        val noteInfo = if (expense.note.isNotBlank()) " (${expense.note})" else ""
        sendNotification(
            title = "✏️ 記帳記錄已更新：$itemTitle",
            message = "${expense.category.label} ${if (isExpense) "-$" else "+$"}${expense.amount.toInt()} (${expense.paymentMethod.label}) ｜ 日期：${expense.dateString}$noteInfo",
            type = NotificationType.EXPENSE,
            actionRoute = "expense",
            sendSystemPush = _notificationPreferences.value.expenseTransactionNoticeEnabled
        )

        if (expense.type == ExpenseType.EXPENSE) {
            checkExpenseBudgetAlert(expense.dateString)
        }
    }

    private fun checkExpenseBudgetAlert(dateString: String) = viewModelScope.launch {
        val month = if (dateString.length >= 7) dateString.substring(0, 7) else _selectedExpenseMonth.value
        val expenses = repository.getAllExpensesOnce()
        val monthExpenses = expenses.filter { it.dateString.startsWith(month) }
        val totalExp = monthExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
        val budgets = repository.getAllBudgetsOnce()
        val budget = budgets.firstOrNull { it.yearMonth == month }?.budgetAmount ?: 10000.0
        val prefs = _notificationPreferences.value
        if (budget > 0 && prefs.masterEnabled && prefs.expenseAlertEnabled) {
            val usagePct = ((totalExp / budget) * 100).toInt()
            if (usagePct >= prefs.expenseAlertThresholdPercent) {
                sendNotification(
                    title = "⚠️ 記帳預算警示 ($month)",
                    message = "本月累積支出 $${totalExp.toInt()}，已達設定預算 $${budget.toInt()} 的 ${usagePct}%！",
                    type = NotificationType.EXPENSE,
                    actionRoute = "expense"
                )
            }
        }
    }

    fun deleteExpense(expense: ExpenseRecord) = viewModelScope.launch {
        repository.deleteExpense(expense)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已刪除記錄"

        val isExpense = expense.type == ExpenseType.EXPENSE
        val itemTitle = expense.title.ifBlank { expense.category.label }
        sendNotification(
            title = "🗑️ 記帳記錄已刪除：$itemTitle",
            message = "已刪除 ${expense.category.label} ${if (isExpense) "-$" else "+$"}${expense.amount.toInt()} (${expense.paymentMethod.label}) ｜ 日期：${expense.dateString}",
            type = NotificationType.EXPENSE,
            actionRoute = "expense",
            sendSystemPush = _notificationPreferences.value.expenseTransactionNoticeEnabled
        )
    }

    fun clearAllExpenses() = viewModelScope.launch {
        repository.deleteAllExpenses()
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已清空所有記帳記錄"

        sendNotification(
            title = "🗑️ 記帳本已清空",
            message = "已清空本機與雲端所有的記帳收支明細記錄。",
            type = NotificationType.EXPENSE,
            actionRoute = "expense",
            sendSystemPush = _notificationPreferences.value.expenseTransactionNoticeEnabled
        )
    }

    fun seedMockExpenses() = viewModelScope.launch {
        val month = _selectedExpenseMonth.value
        val mockData = listOf(
            ExpenseRecord(title = "早餐", amount = 65.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-01", paymentMethod = PaymentMethod.CASH, note = "蛋餅大冰奶"),
            ExpenseRecord(title = "午餐", amount = 120.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-01", paymentMethod = PaymentMethod.MOBILE_PAY, note = "排骨便當"),
            ExpenseRecord(title = "捷運", amount = 25.0, category = ExpenseCategory.TRANSPORT, type = ExpenseType.EXPENSE, dateString = "$month-02", paymentMethod = PaymentMethod.IC_CARD, note = "搭捷運到學校"),
            ExpenseRecord(title = "晚餐", amount = 150.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-03", paymentMethod = PaymentMethod.MOBILE_PAY, note = "牛肉麵"),
            ExpenseRecord(title = "專業書籍", amount = 450.0, category = ExpenseCategory.BOOKS_STUDY, type = ExpenseType.EXPENSE, dateString = "$month-04", paymentMethod = PaymentMethod.CARD, note = "買原文書筆記本"),
            ExpenseRecord(title = "飲料", amount = 50.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-05", paymentMethod = PaymentMethod.CASH, note = "珍珠奶茶半糖微冰"),
            ExpenseRecord(title = "家教薪水", amount = 12000.0, category = ExpenseCategory.SALARY_JOB, type = ExpenseType.INCOME, dateString = "$month-05", paymentMethod = PaymentMethod.TRANSFER, note = "家教薪水入帳"),
            ExpenseRecord(title = "電影票", amount = 300.0, category = ExpenseCategory.ENTERTAINMENT, type = ExpenseType.EXPENSE, dateString = "$month-06", paymentMethod = PaymentMethod.CARD, note = "週末威秀影城"),
            ExpenseRecord(title = "聚餐", amount = 180.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-07", paymentMethod = PaymentMethod.MOBILE_PAY, note = "三媽臭臭鍋"),
            ExpenseRecord(title = "生活雜貨", amount = 120.0, category = ExpenseCategory.DAILY, type = ExpenseType.EXPENSE, dateString = "$month-08", paymentMethod = PaymentMethod.MOBILE_PAY, note = "全聯洗衣精與衛生紙"),
            ExpenseRecord(title = "公車", amount = 50.0, category = ExpenseCategory.TRANSPORT, type = ExpenseType.EXPENSE, dateString = "$month-09", paymentMethod = PaymentMethod.IC_CARD, note = "公車與YouBike"),
            ExpenseRecord(title = "便當", amount = 90.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-10", paymentMethod = PaymentMethod.CASH, note = "午餐 燒臘便當"),
            ExpenseRecord(title = "套房房租水電", amount = 3500.0, category = ExpenseCategory.RENT_UTILITY, type = ExpenseType.EXPENSE, dateString = "$month-10", paymentMethod = PaymentMethod.TRANSFER, note = "學生套房水電"),
            ExpenseRecord(title = "獎學金", amount = 5000.0, category = ExpenseCategory.SCHOLARSHIP, type = ExpenseType.INCOME, dateString = "$month-12", paymentMethod = PaymentMethod.TRANSFER, note = "校內學業優良獎學金"),
            ExpenseRecord(title = "拉麵", amount = 110.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-13", paymentMethod = PaymentMethod.MOBILE_PAY, note = "豚骨拉麵"),
            ExpenseRecord(title = "遊戲", amount = 200.0, category = ExpenseCategory.ENTERTAINMENT, type = ExpenseType.EXPENSE, dateString = "$month-15", paymentMethod = PaymentMethod.CARD, note = "Steam夏日特賣遊戲"),
            ExpenseRecord(title = "早午餐", amount = 85.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-16", paymentMethod = PaymentMethod.CASH, note = "漢堡與薯條"),
            ExpenseRecord(title = "晚餐小吃", amount = 60.0, category = ExpenseCategory.FOOD, type = ExpenseType.EXPENSE, dateString = "$month-18", paymentMethod = PaymentMethod.CASH, note = "滷肉飯燙青菜"),
            ExpenseRecord(title = "國道客運", amount = 70.0, category = ExpenseCategory.TRANSPORT, type = ExpenseType.EXPENSE, dateString = "$month-20", paymentMethod = PaymentMethod.IC_CARD, note = "返鄉客運票"),
            ExpenseRecord(title = "文具耗材", amount = 250.0, category = ExpenseCategory.DAILY, type = ExpenseType.EXPENSE, dateString = "$month-22", paymentMethod = PaymentMethod.MOBILE_PAY, note = "九乘九買文具耗材")
        )
        mockData.forEach { record ->
            repository.insertExpense(record)
        }
        checkExpenseBudgetAlert(month)
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已成功為 $month 月匯入 20 筆測試資料！"
    }

    fun setMonthlyBudget(amount: Double) = viewModelScope.launch {
        val month = _selectedExpenseMonth.value
        repository.setBudget(MonthlyBudget(yearMonth = month, budgetAmount = amount))
        currentUser.value?.let { firestoreSyncRepository.uploadAllToCloud(it.uid) }
        _userMessage.value = "已更新 $month 月預算為 $${amount.toInt()}"
        checkExpenseBudgetAlert(month)
    }



    fun addAccount(account: PaymentAccount) {
        val current = _customAccounts.value.toMutableList()
        current.add(account)
        saveAccounts(current)
        _userMessage.value = "已成功新增帳戶：${account.name}"
        sendNotification(
            title = "新增支付帳戶",
            message = "已成功新增「${account.name}」，起始餘額為 $${account.initialBalance.toInt()}。",
            type = NotificationType.EXPENSE,
            actionRoute = "expense",
            sendSystemPush = true
        )
    }

    fun updateAccount(account: PaymentAccount) {
        val oldAccount = _customAccounts.value.find { it.id == account.id }
        val list = _customAccounts.value.map {
            if (it.id == account.id) account else it
        }
        saveAccounts(list)
        _userMessage.value = "已更新帳戶資訊"

        val isNameChanged = oldAccount != null && oldAccount.name != account.name
        val title = if (isNameChanged) "帳戶名稱異動" else "帳戶資訊更新"
        val message = if (isNameChanged) {
            "「${oldAccount.name}」改成「${account.name}」"
        } else if (oldAccount != null && oldAccount.initialBalance != account.initialBalance) {
            "已將「${account.name}」的起始餘額設定為 $${account.initialBalance.toInt()}。"
        } else {
            "已成功更新「${account.name}」的帳戶資訊。"
        }

        sendNotification(
            title = title,
            message = message,
            type = NotificationType.EXPENSE,
            actionRoute = "expense",
            sendSystemPush = _notificationPreferences.value.expenseTransactionNoticeEnabled
        )
    }

    fun onAccountMoved() {
        _userMessage.value = "移動完成"
    }

    fun moveAccount(fromIndex: Int, toIndex: Int) {
        val list = _customAccounts.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            saveAccounts(list)
        }
    }

    fun moveAccountUp(index: Int) {
        val list = _customAccounts.value.toMutableList()
        if (index > 0 && index < list.size) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            saveAccounts(list)
        }
    }

    fun moveAccountDown(index: Int) {
        val list = _customAccounts.value.toMutableList()
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            saveAccounts(list)
        }
    }

    fun deleteAccount(accountId: String) {
        val deletedAccount = _customAccounts.value.firstOrNull { it.id == accountId }
        val current = _customAccounts.value.filterNot { it.id == accountId }
        saveAccounts(current)
        _userMessage.value = "已刪除自訂帳戶"

        deletedAccount?.let { acc ->
            sendNotification(
                title = "🗑️ 已刪除支付帳戶",
                message = "已成功刪除「${acc.name}」支付帳戶。",
                type = NotificationType.EXPENSE,
                actionRoute = "expense",
                sendSystemPush = true
            )
        }
    }

    fun resetToSampleData() = viewModelScope.launch {
        repository.resetToDefaultData()
        _userMessage.value = "已重置並填入範例資料"
    }

    fun clearAllData() = viewModelScope.launch {
        repository.clearAllData()
        _userMessage.value = "已清空所有本機資料"
    }

    suspend fun exportJson(): String {
        return repository.exportDataToJson()
    }

    suspend fun importJson(json: String): Boolean {
        val success = repository.importDataFromJson(json)
        if (success) {
            _userMessage.value = "資料匯入成功！"
        } else {
            _userMessage.value = "資料格式錯誤，匯入失敗"
        }
        return success
    }

    private fun calculateCourseGpa(course: Course): Double {
        if (course.letterGrade != null) {
            return scoreFromLetterGrade(course.letterGrade) ?: 0.0
        }
        if (course.score != null) {
            return course.score
        }
        return 0.0
    }

    private fun scoreFromLetterGrade(letter: String?): Double? {
        if (letter == null) return null
        return when (letter.uppercase()) {
            "A+" -> 95.0
            "A" -> 88.0
            "A-" -> 82.0
            "B+" -> 78.0
            "B" -> 75.0
            "B-" -> 71.0
            "C+" -> 68.0
            "C" -> 65.0
            "C-" -> 61.0
            "D" -> 55.0
            "E", "F" -> 40.0
            else -> null
        }
    }

    // ==========================================
    // 帳號與身分認證 (Authentication Actions)
    // ==========================================

    fun signInWithGoogle(context: Context? = null, webClientId: String = "", onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context, webClientId)
            result.onSuccess { user ->
                // 1. 先下載並同步雲端最新學業與系所檔案
                firestoreSyncRepository.downloadAllFromCloud(user.uid)

                // 2. 檢查使用者姓名是否需要寫入本機
                val nameToSet = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@")
                val currentPlan = repository.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
                if (!nameToSet.isNullOrBlank()) {
                    if (currentPlan.studentName == "同學" || currentPlan.studentName == "王大明" || currentPlan.studentName == "大學生" || currentPlan.studentName.isBlank()) {
                        val updated = currentPlan.copy(studentName = nameToSet)
                        repository.updateGraduationPlan(updated)
                        if (updated.department.isNotBlank() && updated.department != "尚未設定系所") {
                            firestoreSyncRepository.uploadAllToCloud(user.uid)
                        }
                    }
                }
                showToast("歡迎，${user.displayName ?: nameToSet ?: "同學"}！")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast(e.message ?: "Google 登入失敗")
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, pass)
            result.onSuccess { user ->
                // 1. 先下載並同步雲端最新學業與系所檔案
                firestoreSyncRepository.downloadAllFromCloud(user.uid)

                // 2. 檢查使用者姓名是否需要寫入本機
                val nameToSet = user.displayName?.ifBlank { null } ?: email.substringBefore("@")
                val currentPlan = repository.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
                if (nameToSet.isNotBlank()) {
                    if (currentPlan.studentName == "同學" || currentPlan.studentName == "王大明" || currentPlan.studentName == "大學生" || currentPlan.studentName.isBlank()) {
                        val updated = currentPlan.copy(studentName = nameToSet)
                        repository.updateGraduationPlan(updated)
                        if (updated.department.isNotBlank() && updated.department != "尚未設定系所") {
                            firestoreSyncRepository.uploadAllToCloud(user.uid)
                        }
                    }
                }
                showToast("歡迎回來，${user.displayName ?: nameToSet}！")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast(e.message ?: "登入失敗")
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun signUpWithEmail(
        name: String,
        email: String,
        pass: String,
        department: String = "",
        admissionSemester: String = "",
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(name, email, pass)
            result.onSuccess { user ->
                val nameToSet = user.displayName?.ifBlank { null } ?: name.ifBlank { email.substringBefore("@") }
                val currentPlan = repository.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
                val updatedPlan = currentPlan.copy(
                    studentName = nameToSet.ifBlank { currentPlan.studentName },
                    department = department.ifBlank { currentPlan.department },
                    admissionSemester = admissionSemester.ifBlank { currentPlan.admissionSemester }
                )
                repository.updateGraduationPlan(updatedPlan)

                // 立即將包含正確學系與學生的檔案同步上傳至 Firestore 雲端
                firestoreSyncRepository.uploadAllToCloud(user.uid)

                showToast("註冊成功！歡迎加入 UniTrack+，${user.displayName ?: nameToSet}")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast(e.message ?: "註冊失敗")
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess {
                showToast("密碼重設信已寄送至 $email")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast(e.message ?: "寄送重設信失敗")
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            showToast("已成功登出帳號")
        }
    }

    /**
     * 真正刪除帳號：依序清空雲端 Firestore、本機資料庫、並永久刪除 Firebase Auth 帳號
     */
    fun deleteAccount(context: Context? = null, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid.orEmpty()

            // 1. 在帳號仍具有授權時，先徹底清除 Cloud Firestore 所有集合與文件（避免 Auth 刪除後遭遇權限拒絕）
            if (uid.isNotBlank()) {
                firestoreSyncRepository.deleteAllCloudData(uid)
            }

            // 2. 清除手機本機 Room 資料庫（課表、記帳、預算、門檻、學業檔案）
            repository.clearAllData()

            // 3. 永久註銷並刪除 Firebase Auth 帳號與 Google 憑證
            val authDeleteResult = authRepository.deleteAccount(context)
            authDeleteResult.onSuccess {
                showToast("帳號已永久刪除，本機與雲端資料已完全清除")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast(e.message ?: "刪除帳號失敗")
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun clearAuthError() {
        authRepository.clearError()
    }

    // ==================== Cloud Sync (Firestore) ====================

    fun syncWithCloud(silent: Boolean = false, onResult: ((Boolean, String?) -> Unit)? = null) {
        val user = currentUser.value
        if (user == null) {
            if (!silent) showToast("請先登入帳號以同步雲端資料")
            onResult?.invoke(false, "未登入")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val result = firestoreSyncRepository.syncBidirectional(user.uid)
            _isSyncing.value = false
            result.onSuccess {
                _lastSyncTime.value = System.currentTimeMillis()
                if (!silent) {
                    showToast("雲端資料同步完成！")
                }
                onResult?.invoke(true, null)
            }.onFailure { e ->
                if (!silent) {
                    showToast("雲端同步失敗：${e.message}")
                }
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun uploadToCloud(onResult: ((Boolean, String?) -> Unit)? = null) {
        val user = currentUser.value
        if (user == null) {
            showToast("請先登入帳號以備份資料")
            onResult?.invoke(false, "未登入")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val result = firestoreSyncRepository.uploadAllToCloud(user.uid)
            _isSyncing.value = false
            result.onSuccess {
                _lastSyncTime.value = System.currentTimeMillis()
                showToast("本機資料已成功備份至 Firebase 雲端！")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast("雲端備份失敗：${e.message}")
                onResult?.invoke(false, e.message)
            }
        }
    }

    fun downloadFromCloud(silent: Boolean = false, onResult: ((Boolean, String?) -> Unit)? = null) {
        val user = currentUser.value
        if (user == null) {
            if (!silent) showToast("請先登入帳號以還原資料")
            onResult?.invoke(false, "未登入")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val result = firestoreSyncRepository.downloadAllFromCloud(user.uid)
            _isSyncing.value = false
            result.onSuccess {
                _lastSyncTime.value = System.currentTimeMillis()
                if (!silent) showToast("已從 Firebase 雲端成功還原資料！")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                if (!silent) showToast("雲端還原失敗：${e.message}")
                onResult?.invoke(false, e.message)
            }
        }
    }

    private val _avatarUpdateTrigger = MutableStateFlow(System.currentTimeMillis())
    val avatarUpdateTrigger: StateFlow<Long> = _avatarUpdateTrigger.asStateFlow()

    fun getAvatarFile(): java.io.File {
        return java.io.File(getApplication<Application>().filesDir, "user_avatar.jpg")
    }

    fun saveAvatarFromUri(uri: android.net.Uri): Boolean {
        return try {
            val context = getApplication<Application>()
            context.contentResolver.openInputStream(uri)?.use { input ->
                getAvatarFile().outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            _avatarUpdateTrigger.value = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearAvatar(): Boolean {
        val file = getAvatarFile()
        val deleted = if (file.exists()) file.delete() else true
        _avatarUpdateTrigger.value = System.currentTimeMillis()
        return deleted
    }
}
