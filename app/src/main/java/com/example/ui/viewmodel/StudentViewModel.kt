package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultData
import com.example.data.model.*
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

data class CreditCategorySummary(
    val category: CourseCategory,
    val earnedCredits: Double,
    val inProgressCredits: Double,
    val targetCredits: Double,
    val percentage: Float
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

    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Selected semester in timetable
    private val _selectedSemester = MutableStateFlow("114-1")
    val selectedSemester: StateFlow<String> = _selectedSemester.asStateFlow()

    // Selected month in expense tracker
    private val _selectedExpenseMonth = MutableStateFlow(monthFormat.format(Date()))
    val selectedExpenseMonth: StateFlow<String> = _selectedExpenseMonth.asStateFlow()

    // Snackbar / Toast message state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = StudentRepository(db.courseDao(), db.graduationDao(), db.expenseDao())
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Raw database flows
    val allCourses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customSemesters = MutableStateFlow<Set<String>>(emptySet())

    val allSemesters: StateFlow<List<String>> = combine(
        repository.allSemesters,
        _selectedSemester,
        _customSemesters
    ) { dbSemesters, currentSem, customSems ->
        val set = dbSemesters.toMutableSet()
        set.add("114-1")
        set.add("114-2")
        if (currentSem.isNotBlank()) set.add(currentSem)
        set.addAll(customSems)
        set.toList().sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("114-2", "114-1"))

    val graduationPlan: StateFlow<GraduationPlan> = repository.graduationPlan
        .map { it ?: DefaultData.getDefaultGraduationPlan() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultData.getDefaultGraduationPlan())

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

        var earnedCollegeCore = 0.0
        var inProgressCollegeCore = 0.0

        var earnedBasicModule = 0.0
        var inProgressBasicModule = 0.0

        var earnedCoreModule = 0.0
        var inProgressCoreModule = 0.0

        var earnedProfessionalModule = 0.0
        var inProgressProfessionalModule = 0.0

        var earnedFree = 0.0
        var inProgressFree = 0.0

        var peCredits = 0.0

        for (c in courses) {
            val isPassed = c.isCompleted || (c.score != null && c.score >= plan.minPassingScore)
            val isInProgress = !isPassed && (c.semester == plan.currentSemester || c.score == null)

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
                    if (isPassed) earnedGeneral += c.credits
                    else if (isInProgress) inProgressGeneral += c.credits
                }
                CourseCategory.COLLEGE_CORE -> {
                    if (isPassed) earnedCollegeCore += c.credits
                    else if (isInProgress) inProgressCollegeCore += c.credits
                }
                CourseCategory.BASIC_MODULE -> {
                    if (isPassed) earnedBasicModule += c.credits
                    else if (isInProgress) inProgressBasicModule += c.credits
                }
                CourseCategory.CORE_MODULE -> {
                    if (isPassed) earnedCoreModule += c.credits
                    else if (isInProgress) inProgressCoreModule += c.credits
                }
                CourseCategory.PROFESSIONAL_MODULE -> {
                    if (isPassed) earnedProfessionalModule += c.credits
                    else if (isInProgress) inProgressProfessionalModule += c.credits
                }
                CourseCategory.FREE_ELECTIVE -> {
                    if (isPassed) earnedFree += c.credits
                    else if (isInProgress) inProgressFree += c.credits
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

        val completedThresholds = thresholds.count { it.isCompleted }
        val totalThresholds = thresholds.size

        val isEligible = totalEarned >= plan.targetTotalCredits &&
                earnedGeneral >= plan.targetGeneralCredits &&
                earnedCollegeCore >= plan.targetCollegeCoreCredits &&
                earnedBasicModule >= plan.targetBasicModuleCredits &&
                earnedCoreModule >= plan.targetCoreModuleCredits &&
                earnedProfessionalModule >= plan.targetProfessionalModuleCredits &&
                (totalThresholds == 0 || completedThresholds == totalThresholds)

        GraduationAuditSummary(
            plan = plan,
            totalEarnedCredits = totalEarned,
            totalInProgressCredits = totalInProgress,
            totalTargetCredits = targetTotal,
            overallPercentage = round(overallPercentage * 10f) / 10f,
            requiredSummary = CreditCategorySummary(CourseCategory.REQUIRED, earnedRequired, inProgressRequired, plan.targetRequiredCredits, reqPercentage),
            electiveSummary = CreditCategorySummary(CourseCategory.ELECTIVE, earnedElective, inProgressElective, plan.targetElectiveCredits, elePercentage),
            generalSummary = CreditCategorySummary(CourseCategory.GENERAL_EDU, earnedGeneral, inProgressGeneral, plan.targetGeneralCredits, genPercentage),
            collegeCoreSummary = CreditCategorySummary(CourseCategory.COLLEGE_CORE, earnedCollegeCore, inProgressCollegeCore, plan.targetCollegeCoreCredits, colPercentage),
            basicModuleSummary = CreditCategorySummary(CourseCategory.BASIC_MODULE, earnedBasicModule, inProgressBasicModule, plan.targetBasicModuleCredits, basPercentage),
            coreModuleSummary = CreditCategorySummary(CourseCategory.CORE_MODULE, earnedCoreModule, inProgressCoreModule, plan.targetCoreModuleCredits, corPercentage),
            professionalModuleSummary = CreditCategorySummary(CourseCategory.PROFESSIONAL_MODULE, earnedProfessionalModule, inProgressProfessionalModule, plan.targetProfessionalModuleCredits, proPercentage),
            freeSummary = CreditCategorySummary(CourseCategory.FREE_ELECTIVE, earnedFree, inProgressFree, plan.targetFreeCredits, freePercentage),
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

    fun addCourse(course: Course) = viewModelScope.launch {
        repository.insertCourse(course)
        _userMessage.value = "已成功新增課程：${course.name}"
    }

    fun updateCourse(course: Course) = viewModelScope.launch {
        repository.updateCourse(course)
        _userMessage.value = "已更新課程：${course.name}"
    }

    fun deleteCourse(course: Course) = viewModelScope.launch {
        repository.deleteCourse(course)
        _userMessage.value = "已刪除課程：${course.name}"
    }

    fun updateGraduationPlan(plan: GraduationPlan) = viewModelScope.launch {
        repository.updateGraduationPlan(plan)
        _userMessage.value = "已儲存畢業審查標準"
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
}
