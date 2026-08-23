package com.example.data.local

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

object DefaultData {
    private val monthFormat get() = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun getDefaultGraduationPlan(): GraduationPlan {
        return GraduationPlan(
            id = 1,
            department = "資訊工程學系",
            studentName = "大學生",
            targetTotalCredits = 128.0,
            targetRequiredCredits = 58.0,
            targetElectiveCredits = 36.0,
            targetGeneralCredits = 28.0,
            targetCollegeCoreCredits = 9.0,
            targetBasicModuleCredits = 24.0,
            targetCoreModuleCredits = 24.0,
            targetProfessionalModuleCredits = 23.0,
            targetFreeCredits = 20.0,
            minPassingScore = 60.0,
            gpaScale = GpaScale.PERCENTAGE,
            currentSemester = "114-1"
        )
    }

    fun getDefaultThresholds(): List<GraduationThreshold> {
        return emptyList()
    }

    fun getDefaultCourses(): List<Course> {
        return listOf(
            Course(
                name = "數學導論",
                teacher = "何友文",
                location = "SEB304",
                dayOfWeek = 1,
                startPeriod = 8,
                endPeriod = 10,
                credits = 3.0,
                category = CourseCategory.BASIC_MODULE,
                requirementType = CourseRequirementType.REQUIRED,
                semester = "114-1",
                colorHex = "#4F46E5"
            ),
            Course(
                name = "基礎機率與統計",
                teacher = "張永明",
                location = "SEB401",
                dayOfWeek = 2,
                startPeriod = 6,
                endPeriod = 8,
                credits = 3.0,
                category = CourseCategory.BASIC_MODULE,
                requirementType = CourseRequirementType.ELECTIVE,
                semester = "114-1",
                colorHex = "#F59E0B"
            ),
            Course(
                name = "微積分演習(一)",
                teacher = "吳慶堂",
                location = "SEB304",
                dayOfWeek = 2,
                startPeriod = 9,
                endPeriod = 10,
                credits = 1.0,
                category = CourseCategory.BASIC_MODULE,
                requirementType = CourseRequirementType.ELECTIVE,
                semester = "114-1",
                colorHex = "#10B981"
            ),
            Course(
                name = "資料科學概論",
                teacher = "高嘉宏",
                location = "SEB304",
                dayOfWeek = 3,
                startPeriod = 6,
                endPeriod = 8,
                credits = 3.0,
                category = CourseCategory.BASIC_MODULE,
                requirementType = CourseRequirementType.REQUIRED,
                semester = "114-1",
                colorHex = "#8B5CF6"
            ),
            Course(
                name = "微積分(一)",
                teacher = "吳慶堂",
                location = "SEB304",
                dayOfWeek = 4,
                startPeriod = 2,
                endPeriod = 4,
                credits = 3.0,
                category = CourseCategory.COLLEGE_CORE,
                requirementType = CourseRequirementType.REQUIRED,
                semester = "114-1",
                colorHex = "#3B82F6"
            )
        )
    }

    fun getDefaultExpenses(): List<ExpenseRecord> {
        return emptyList()
    }

    fun getDefaultBudget(): MonthlyBudget {
        val currentMonth = monthFormat.format(Date())
        return MonthlyBudget(yearMonth = currentMonth, budgetAmount = 10000.0)
    }
}
