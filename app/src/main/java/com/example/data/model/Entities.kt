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
    val category: CourseCategory = CourseCategory.GENERAL_EDU,
    val requirementType: CourseRequirementType = CourseRequirementType.REQUIRED,
    val generalEduSubtype: GeneralEduSubtype = GeneralEduSubtype.NONE,
    val subcategory: String = "", // 自訂子分類 / 領域 / 類別 (如：國語文、向度一、AI通識)
    val semester: String = "", // e.g. 113-1, 113-2
    val score: Double? = null, // 0~100 or null if currently taking
    val letterGrade: String? = null, // "A+", "A", "B+", etc.
    val isCompleted: Boolean = false, // completed and passed
    val colorHex: String = "#3B82F6",
    val notes: String = "",
    val repeatWeeks: String = "1-18",
    val repeatMode: String = "每週"
)

data class CourseNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String = "一般", // 一般, 作業, 考試, 公告, 重點
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val week: Int? = null
)

@Entity(tableName = "graduation_plans")
data class GraduationPlan(
    @PrimaryKey
    val id: Long = 1,
    val department: String = "尚未設定系所",
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
    // 子分類 [ 必修 ] 與 [ 選修 ] 學分門檻
    val targetGeneralRequiredCredits: Double = 28.0,
    val targetGeneralElectiveCredits: Double = 0.0,
    val targetCollegeCoreRequiredCredits: Double = 9.0,
    val targetCollegeCoreElectiveCredits: Double = 0.0,
    val targetBasicModuleRequiredCredits: Double = 24.0,
    val targetBasicModuleElectiveCredits: Double = 0.0,
    val targetCoreModuleRequiredCredits: Double = 24.0,
    val targetCoreModuleElectiveCredits: Double = 0.0,
    val targetProfessionalModuleRequiredCredits: Double = 23.0,
    val targetProfessionalModuleElectiveCredits: Double = 0.0,
    val targetFreeElectiveCredits: Double = 20.0,
    val minPassingScore: Double = 60.0,
    val gpaScale: GpaScale = GpaScale.PERCENTAGE,
    val admissionSemester: String = "",
    val currentSemester: String = "",
    val subcategoriesJson: String = "" // JSON map of Category -> List<SubcategoryName>
) {
    fun getSubcategories(category: CourseCategory): List<String> {
        if (subcategoriesJson.isBlank()) {
            return if (category == CourseCategory.GENERAL_EDU) {
                listOf(
                    "語文：國語文能力",
                    "語文：英語文能力",
                    "資訊能力課程",
                    "跨領域核心：人文藝術領域",
                    "跨領域核心：社會科學領域",
                    "跨領域核心：自然科學領域",
                    "博雅：美學、哲學與文化實踐",
                    "博雅：公民、社會與全球視野",
                    "博雅：科技、自然與環境生態",
                    "博雅：自我、人際與成長調適",
                    "體適能：運動與健康"
                )
            } else emptyList()
        }
        return try {
            val json = org.json.JSONObject(subcategoriesJson)
            val arr = json.optJSONArray(category.name) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodeSubcategories(map: Map<CourseCategory, List<String>>): String {
            val obj = org.json.JSONObject()
            map.forEach { (cat, list) ->
                val cleanList = list.filter { it.isNotBlank() }.distinct()
                if (cleanList.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    cleanList.forEach { arr.put(it) }
                    obj.put(cat.name, arr)
                }
            }
            return obj.toString()
        }
    }
}

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

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionRoute: String? = null
)
