package com.example.data.local

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
object DefaultData {
    private val monthFormat get() = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    /**
     * 台灣學制法定學期起訖日（教育部學期劃分）：
     * - 上學期（第一學期 -1）：每年 08 月 01 日 至 翌年 01 月 31 日
     * - 下學期（第二學期 -2）：每年 02 月 01 日 至 當年 07 月 31 日
     */
    fun getCurrentAcademicSemester(cal: Calendar = Calendar.getInstance()): String {
        val year = cal.get(Calendar.YEAR)
        val taiwanYear = year - 1911
        val month = cal.get(Calendar.MONTH) + 1 // 1~12

        return when (month) {
            // 8月1日 ~ 12月31日：當年度上學期（例如 2026/08 -> 115-1）
            in 8..12 -> "$taiwanYear-1"
            // 1月1日 ~ 1月31日：前一年度上學期（例如 2027/01 -> 115-1）
            1 -> "${taiwanYear - 1}-1"
            // 2月1日 ~ 7月31日：前一年度下學期（例如 2027/02 -> 115-2）
            in 2..7 -> "${taiwanYear - 1}-2"
            else -> "$taiwanYear-1"
        }
    }

    /**
     * 取得特定學期的法定起訖日期區間 (Calendar 起始與結束)
     * 例如 "115-1" -> 2026/08/01 00:00:00 ~ 2027/01/31 23:59:59
     * 例如 "115-2" -> 2027/02/01 00:00:00 ~ 2027/07/31 23:59:59
     */
    fun getSemesterDateRange(semesterCode: String): Pair<Calendar, Calendar>? {
        val taiwanYear = semesterCode.substringBefore("-").filter { it.isDigit() }.toIntOrNull() ?: return null
        val term = semesterCode.substringAfter("-").filter { it.isDigit() }.toIntOrNull() ?: 1
        val westYear = taiwanYear + 1911

        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()

        if (term == 1) {
            // 上學期：westYear/08/01 ~ (westYear+1)/01/31
            startCal.set(westYear, Calendar.AUGUST, 1, 0, 0, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            endCal.set(westYear + 1, Calendar.JANUARY, 31, 23, 59, 59)
            endCal.set(Calendar.MILLISECOND, 999)
        } else {
            // 下學期：(westYear+1)/02/01 ~ (westYear+1)/07/31
            startCal.set(westYear + 1, Calendar.FEBRUARY, 1, 0, 0, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            endCal.set(westYear + 1, Calendar.JULY, 31, 23, 59, 59)
            endCal.set(Calendar.MILLISECOND, 999)
        }
        return Pair(startCal, endCal)
    }

    /**
     * 檢查指定日期是否落於該學期法定期間內
     */
    fun isDateInSemester(date: Date, semesterCode: String): Boolean {
        val (start, end) = getSemesterDateRange(semesterCode) ?: return false
        val time = date.time
        return time >= start.timeInMillis && time <= end.timeInMillis
    }

    /**
     * 檢查指定學期是否已到達法定開始日（上學期 8/1、下學期 2/1）
     */
    fun hasSemesterStarted(semesterCode: String, date: Date = Date()): Boolean {
        val (start, _) = getSemesterDateRange(semesterCode) ?: return true
        return date.time >= start.timeInMillis
    }

    fun getDefaultGraduationPlan(): GraduationPlan {
        val currentAcademicSem = getCurrentAcademicSemester()
        return GraduationPlan(
            id = 1,
            department = "尚未設定系所",
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
            admissionSemester = currentAcademicSem,
            currentSemester = currentAcademicSem
        )
    }

    fun getDefaultThresholds(): List<GraduationThreshold> {
        return emptyList()
    }

    fun getDefaultCourses(): List<Course> {
        return emptyList()
    }

    fun getDefaultExpenses(): List<ExpenseRecord> {
        return emptyList()
    }

    fun getDefaultBudget(): MonthlyBudget {
        val currentMonth = monthFormat.format(Date())
        return MonthlyBudget(yearMonth = currentMonth, budgetAmount = 10000.0)
    }
}
