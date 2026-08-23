package com.example.data.local

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

object DefaultData {
    private val dateFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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
        return emptyList()
    }

    fun getDefaultExpenses(): List<ExpenseRecord> {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val todayStr = dateFormat.format(Date(now))
        val yestStr = dateFormat.format(Date(now - dayMillis))
        val twoDaysAgoStr = dateFormat.format(Date(now - 2 * dayMillis))
        val threeDaysAgoStr = dateFormat.format(Date(now - 3 * dayMillis))

        return listOf(
            ExpenseRecord(
                title = "學餐排骨飯套餐",
                amount = 95.0,
                type = ExpenseType.EXPENSE,
                category = ExpenseCategory.FOOD,
                paymentMethod = PaymentMethod.IC_CARD,
                timestamp = now - 2 * 3600 * 1000L,
                dateString = todayStr,
                note = "一餐學生優惠九折"
            ),
            ExpenseRecord(
                title = "冰美式咖啡",
                amount = 45.0,
                type = ExpenseType.EXPENSE,
                category = ExpenseCategory.FOOD,
                paymentMethod = PaymentMethod.MOBILE_PAY,
                timestamp = now - 5 * 3600 * 1000L,
                dateString = todayStr,
                note = "圖書館讀書提神"
            ),
            ExpenseRecord(
                title = "演算法原文書教科書 (二手)",
                amount = 650.0,
                type = ExpenseType.EXPENSE,
                category = ExpenseCategory.BOOKS_STUDY,
                paymentMethod = PaymentMethod.TRANSFER,
                timestamp = now - dayMillis,
                dateString = yestStr,
                note = "跟大三學長購買 CLRS 演算法"
            ),
            ExpenseRecord(
                title = "TPASS 通勤月票 (基北北桃)",
                amount = 1200.0,
                type = ExpenseType.EXPENSE,
                category = ExpenseCategory.TRANSPORT,
                paymentMethod = PaymentMethod.IC_CARD,
                timestamp = now - 2 * dayMillis,
                dateString = twoDaysAgoStr,
                note = "公車、捷運、台鐵通勤"
            ),
            ExpenseRecord(
                title = "高中國文家教時薪收入",
                amount = 3000.0,
                type = ExpenseType.INCOME,
                category = ExpenseCategory.SALARY_JOB,
                paymentMethod = PaymentMethod.TRANSFER,
                timestamp = now - 3 * dayMillis,
                dateString = threeDaysAgoStr,
                note = "6小時家教費"
            ),
            ExpenseRecord(
                title = "校內學術優良獎學金",
                amount = 5000.0,
                type = ExpenseType.INCOME,
                category = ExpenseCategory.SCHOLARSHIP,
                paymentMethod = PaymentMethod.TRANSFER,
                timestamp = now - 5 * dayMillis,
                dateString = dateFormat.format(Date(now - 5 * dayMillis)),
                note = "上學期書卷獎第三名"
            ),
            ExpenseRecord(
                title = "社團聚餐火鍋",
                amount = 380.0,
                type = ExpenseType.EXPENSE,
                category = ExpenseCategory.ENTERTAINMENT,
                paymentMethod = PaymentMethod.MOBILE_PAY,
                timestamp = now - 6 * dayMillis,
                dateString = dateFormat.format(Date(now - 6 * dayMillis)),
                note = "程式設計社期初迎新"
            )
        )
    }

    fun getDefaultBudget(): MonthlyBudget {
        val currentMonth = monthFormat.format(Date())
        return MonthlyBudget(yearMonth = currentMonth, budgetAmount = 10000.0)
    }
}
