package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int = 1, // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val startPeriod: Int = 1, // 1..14
    val endPeriod: Int = 1,   // 1..14
    val startTime: String = "", // e.g. "09:00"
    val endTime: String = "",   // e.g. "10:00"
    val credits: Double = 3.0,
    val category: CourseCategory = CourseCategory.REQUIRED,
    val generalEduSubtype: GeneralEduSubtype = GeneralEduSubtype.NONE,
    val semester: String = "113-2", // e.g. 113-1, 113-2
    val score: Double? = null, // 0~100 or null if currently taking
    val letterGrade: String? = null, // "A+", "A", "B+", etc.
    val isCompleted: Boolean = false, // completed and passed
    val colorHex: String = "#3B82F6",
    val notes: String = ""
)

@Entity(tableName = "graduation_plans")
data class GraduationPlan(
    @PrimaryKey
    val id: Long = 1,
    val department: String = "資訊工程學系",
    val studentName: String = "同學",
    val targetTotalCredits: Double = 128.0,
    val targetRequiredCredits: Double = 58.0,
    val targetElectiveCredits: Double = 36.0,
    val targetGeneralCredits: Double = 28.0,
    val targetCollegeCoreCredits: Double = 9.0,
    val targetBasicModuleCredits: Double = 24.0,
    val targetCoreModuleCredits: Double = 24.0,
    val targetProfessionalModuleCredits: Double = 23.0,
    val targetFreeCredits: Double = 20.0,
    val minPassingScore: Double = 60.0,
    val gpaScale: GpaScale = GpaScale.SCALE_4_3,
    val currentSemester: String = "113-2"
)

@Entity(tableName = "graduation_thresholds")
data class GraduationThreshold(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val completedDate: String = "",
    val proofNote: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: ExpenseType = ExpenseType.EXPENSE,
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "", // YYYY-MM-DD
    val note: String = ""
)

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey
    val yearMonth: String, // e.g. "2026-08"
    val budgetAmount: Double = 12000.0
)
