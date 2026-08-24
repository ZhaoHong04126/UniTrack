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
    private val _showWeekend = MutableStateFlow(prefs.getBoolean("pref_show_weekend", true))
    val showWeekend: StateFlow<Boolean> = _showWeekend.asStateFlow()

    fun setShowWeekend(show: Boolean) {
        _showWeekend.value = show
        prefs.edit { putBoolean("pref_show_weekend", show) }
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

    fun getSemesterTotalWeeks(semester: String): Int {
        return prefs.getInt("semester_total_weeks_$semester", 18)
    }

    fun saveSemesterTimeConfig(semester: String, startDate: String, totalWeeks: Int) {
        prefs.edit {
            putString("semester_start_date_$semester", startDate)
            putInt("semester_total_weeks_$semester", totalWeeks.coerceIn(4, 18))
        }
        _semesterTimeConfigVersion.value += 1
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
        repository = StudentRepository(application.applicationContext, db.courseDao(), db.graduationDao(), db.expenseDao())
        firestoreSyncRepository = FirestoreSyncRepository(
            application.applicationContext,
            db.courseDao(),
            db.graduationDao(),
            db.expenseDao()
        )
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        viewModelScope.launch {
            currentUser.collect { profile ->
                if (profile != null) {
                    val nameToSet = profile.displayName?.ifBlank { null }
                        ?: profile.email?.substringBefore("@")
                    if (!nameToSet.isNullOrBlank()) {
                        val currentPlan = repository.getGraduationPlanOnce() ?: repository.getCachedGraduationPlan()
                        if (currentPlan.studentName == "同學" || currentPlan.studentName == "王大明" || currentPlan.studentName == "大學生" || currentPlan.studentName.isBlank()) {
                            updateGraduationPlan(currentPlan.copy(studentName = nameToSet))
                        }
                    }
                }
                // Automatic background cloud sync on authenticated user login
                if (profile != null) {
                    syncWithCloud(silent = true)
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
    val semesterGpaList: StateFlow<List<SemesterGpa>> = combine(allCourses, graduationPlan) { courses, plan ->
        val semesterGroup = courses.groupBy { it.semester }
        val result = mutableListOf<SemesterGpa>()

        for ((sem, semCourses) in semesterGroup) {
            val gradedCourses = semCourses.filter { it.score != null || it.letterGrade != null }
            if (gradedCourses.isEmpty()) {
                val totalCredits = semCourses.sumOf { it.credits }
                result.add(SemesterGpa(sem, 0.0, 0.0, totalCredits, 0.0, semCourses.size))
                continue
            }

            var totalWeightedGpa = 0.0
            var totalWeightedScore = 0.0
            var totalGradedCredits = 0.0
            var passedCredits = 0.0

            for (c in gradedCourses) {
                val gpaVal = calculateCourseGpa(c, plan.gpaScale)
                val scoreVal = c.score ?: scoreFromLetterGrade(c.letterGrade) ?: (gpaVal * 20.0)

                totalWeightedGpa += gpaVal * c.credits
                totalWeightedScore += scoreVal * c.credits
                totalGradedCredits += c.credits

                if (c.isCompleted || (c.score != null && c.score >= plan.minPassingScore) || (c.letterGrade != null && c.letterGrade != "F" && c.letterGrade != "E")) {
                    passedCredits += c.credits
                }
            }

            val semGpa = if (totalGradedCredits > 0) totalWeightedGpa / totalGradedCredits else 0.0
            val semAvg = if (totalGradedCredits > 0) totalWeightedScore / totalGradedCredits else 0.0
            val totalCredits = semCourses.sumOf { it.credits }

            result.add(
                SemesterGpa(
                    semester = sem,
                    gpa = round(semGpa * 100.0) / 100.0,
                    averageScore = round(semAvg * 10.0) / 10.0,
                    totalCredits = totalCredits,
                    passedCredits = passedCredits,
                    courseCount = semCourses.size
                )
            )
        }
        result.sortedByDescending { it.semester }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cumulative GPA & Average Score
    val cumulativeAcademicSummary: StateFlow<Pair<Double, Double>> = combine(allCourses, graduationPlan) { courses, plan ->
        val gradedCourses = courses.filter { it.score != null || it.letterGrade != null }
        if (gradedCourses.isEmpty()) return@combine Pair(0.0, 0.0)

        var totalWeightedGpa = 0.0
        var totalWeightedScore = 0.0
        var totalGradedCredits = 0.0

        for (c in gradedCourses) {
            val gpaVal = calculateCourseGpa(c, plan.gpaScale)
            val scoreVal = c.score ?: scoreFromLetterGrade(c.letterGrade) ?: (gpaVal * 20.0)

            totalWeightedGpa += gpaVal * c.credits
            totalWeightedScore += scoreVal * c.credits
            totalGradedCredits += c.credits
        }

        val gpa = if (totalGradedCredits > 0) totalWeightedGpa / totalGradedCredits else 0.0
        val avg = if (totalGradedCredits > 0) totalWeightedScore / totalGradedCredits else 0.0

        Pair(round(gpa * 100.0) / 100.0, round(avg * 10.0) / 10.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(3.82, 87.5))

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

        var peCredits = 0.0

        for (c in courses) {
            val isPassed = c.isCompleted || (c.score != null && c.score >= plan.minPassingScore)
            val isInProgress = !isPassed && (c.semester == plan.currentSemester || c.score == null)
            val isReq = c.requirementType == CourseRequirementType.REQUIRED || c.requirementType == CourseRequirementType.REQUIRED_ELECTIVE

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
                        earnedCollegeCoreReq += c.credits
                    } else if (isInProgress) {
                        inProgressCollegeCore += c.credits
                        inProgressCollegeCoreReq += c.credits
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
                CourseCategory.PE -> {
                    if (isPassed) peCredits += c.credits
                }
            }
        }

        val totalEarned = earnedRequired + earnedElective + earnedGeneral + earnedCollegeCore + earnedBasicModule + earnedCoreModule + earnedProfessionalModule + earnedFree
        val totalInProgress = inProgressRequired + inProgressElective + inProgressGeneral + inProgressCollegeCore + inProgressBasicModule + inProgressCoreModule + inProgressProfessionalModule + inProgressFree
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
                electiveSummary = null
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

    // Expense Monthly Summary
    val monthlyExpenseSummary: StateFlow<ExpenseMonthlySummary> = combine(
        allExpenses,
        _selectedExpenseMonth,
        allBudgets
    ) { expenses, month, budgets ->
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

        val budget = budgets.firstOrNull { it.yearMonth == month }?.budgetAmount ?: 10000.0
        val remaining = budget - totalExp
        val usagePercentage = if (budget > 0) ((totalExp / budget) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

        ExpenseMonthlySummary(
            yearMonth = month,
            totalExpense = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
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

    fun addCourse(course: Course) = viewModelScope.launch {
        repository.insertCourse(course)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        _userMessage.value = "已成功新增課程：${course.name}"
    }

    fun updateCourse(course: Course) = viewModelScope.launch {
        repository.updateCourse(course)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        _userMessage.value = "已更新課程：${course.name}"
    }

    fun deleteCourse(course: Course) = viewModelScope.launch {
        repository.deleteCourse(course)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
        _userMessage.value = "已刪除課程：${course.name}"
    }

    fun updateGraduationPlan(plan: GraduationPlan, onComplete: (() -> Unit)? = null) = viewModelScope.launch {
        repository.updateGraduationPlan(plan)
        val user = currentUser.value
        if (user != null) {
            firestoreSyncRepository.uploadAllToCloud(user.uid)
        }
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
        _userMessage.value = "已將 $semester 設定為主要學期"
    }

    fun addThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        repository.insertThreshold(threshold)
        _userMessage.value = "已新增畢業門檻項目"
    }

    fun toggleThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        val updated = threshold.copy(
            isCompleted = !threshold.isCompleted,
            completedDate = if (!threshold.isCompleted) dateFormat.format(Date()) else ""
        )
        repository.updateThreshold(updated)
    }

    fun updateThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        repository.updateThreshold(threshold)
        _userMessage.value = "已更新門檻：${threshold.title}"
    }

    fun deleteThreshold(threshold: GraduationThreshold) = viewModelScope.launch {
        repository.deleteThreshold(threshold)
        _userMessage.value = "已刪除門檻項目"
    }

    fun addExpense(expense: ExpenseRecord) = viewModelScope.launch {
        repository.insertExpense(expense)
        _userMessage.value = "已記錄${expense.type.label}：$${expense.amount.toInt()}"
    }

    fun updateExpense(expense: ExpenseRecord) = viewModelScope.launch {
        repository.updateExpense(expense)
        _userMessage.value = "已更新記錄"
    }

    fun deleteExpense(expense: ExpenseRecord) = viewModelScope.launch {
        repository.deleteExpense(expense)
        _userMessage.value = "已刪除記錄"
    }

    fun clearAllExpenses() = viewModelScope.launch {
        repository.deleteAllExpenses()
        _userMessage.value = "已清空所有記帳記錄"
    }

    fun setMonthlyBudget(amount: Double) = viewModelScope.launch {
        val month = _selectedExpenseMonth.value
        repository.setBudget(MonthlyBudget(yearMonth = month, budgetAmount = amount))
        _userMessage.value = "已更新 $month 月預算為 $${amount.toInt()}"
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

    private fun calculateCourseGpa(course: Course, scale: GpaScale): Double {
        if (course.letterGrade != null) {
            return when (scale) {
                GpaScale.SCALE_4_3 -> when (course.letterGrade.uppercase()) {
                    "A+" -> 4.3
                    "A" -> 4.0
                    "A-" -> 3.7
                    "B+" -> 3.3
                    "B" -> 3.0
                    "B-" -> 2.7
                    "C+" -> 2.3
                    "C" -> 2.0
                    "C-" -> 1.7
                    "D" -> 1.0
                    else -> 0.0
                }
                GpaScale.SCALE_4_0 -> when (course.letterGrade.uppercase()) {
                    "A+", "A" -> 4.0
                    "A-" -> 3.7
                    "B+" -> 3.3
                    "B" -> 3.0
                    "B-" -> 2.7
                    "C+" -> 2.3
                    "C" -> 2.0
                    "C-" -> 1.7
                    "D" -> 1.0
                    else -> 0.0
                }
                GpaScale.PERCENTAGE -> scoreFromLetterGrade(course.letterGrade) ?: 0.0
            }
        }
        if (course.score != null) {
            val score = course.score
            return when (scale) {
                GpaScale.SCALE_4_3 -> when {
                    score >= 90 -> 4.3
                    score >= 85 -> 4.0
                    score >= 80 -> 3.7
                    score >= 77 -> 3.3
                    score >= 73 -> 3.0
                    score >= 70 -> 2.7
                    score >= 67 -> 2.3
                    score >= 63 -> 2.0
                    score >= 60 -> 1.7
                    score >= 50 -> 1.0
                    else -> 0.0
                }
                GpaScale.SCALE_4_0 -> when {
                    score >= 85 -> 4.0
                    score >= 80 -> 3.7
                    score >= 77 -> 3.3
                    score >= 73 -> 3.0
                    score >= 70 -> 2.7
                    score >= 67 -> 2.3
                    score >= 63 -> 2.0
                    score >= 60 -> 1.7
                    score >= 50 -> 1.0
                    else -> 0.0
                }
                GpaScale.PERCENTAGE -> score
            }
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

    fun signInWithGoogle(webClientId: String = "", onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(webClientId)
            result.onSuccess { user ->
                val nameToSet = user.displayName?.ifBlank { null } ?: user.email?.substringBefore("@")
                if (!nameToSet.isNullOrBlank()) {
                    val currentPlan = repository.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
                    if (currentPlan.studentName == "同學" || currentPlan.studentName == "王大明" || currentPlan.studentName == "大學生" || currentPlan.studentName.isBlank()) {
                        updateGraduationPlan(currentPlan.copy(studentName = nameToSet))
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
                val nameToSet = user.displayName?.ifBlank { null } ?: email.substringBefore("@")
                if (nameToSet.isNotBlank()) {
                    val currentPlan = repository.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
                    if (currentPlan.studentName == "同學" || currentPlan.studentName == "王大明" || currentPlan.studentName == "大學生" || currentPlan.studentName.isBlank()) {
                        updateGraduationPlan(currentPlan.copy(studentName = nameToSet))
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
    fun deleteAccount(onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val uid = currentUser.value?.uid.orEmpty()

            // 1. 在帳號仍具有授權時，先徹底清除 Cloud Firestore 所有集合與文件（避免 Auth 刪除後遭遇權限拒絕）
            if (uid.isNotBlank()) {
                firestoreSyncRepository.deleteAllCloudData(uid)
            }

            // 2. 清除手機本機 Room 資料庫（課表、記帳、預算、門檻、學業檔案）
            repository.clearAllData()

            // 3. 永久註銷並刪除 Firebase Auth 帳號與 Google 憑證
            val authDeleteResult = authRepository.deleteAccount()
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

    fun downloadFromCloud(onResult: ((Boolean, String?) -> Unit)? = null) {
        val user = currentUser.value
        if (user == null) {
            showToast("請先登入帳號以還原資料")
            onResult?.invoke(false, "未登入")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val result = firestoreSyncRepository.downloadAllFromCloud(user.uid)
            _isSyncing.value = false
            result.onSuccess {
                _lastSyncTime.value = System.currentTimeMillis()
                showToast("已從 Firebase 雲端成功還原資料！")
                onResult?.invoke(true, null)
            }.onFailure { e ->
                showToast("雲端還原失敗：${e.message}")
                onResult?.invoke(false, e.message)
            }
        }
    }
}
