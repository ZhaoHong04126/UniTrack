package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

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
    val category: CourseCategory = CourseCategory.UNSPECIFIED,
    val requirementType: CourseRequirementType = CourseRequirementType.UNSPECIFIED,
    val generalEduSubtype: GeneralEduSubtype = GeneralEduSubtype.NONE,
    val subcategory: String = "", // 自訂子分類 / 領域 / 類別 (如：國語文、向度一、AI通識)
    val customCategory: String = "", // 自訂母體分類名稱 (若為自訂母體分類時使用)
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
    val id: String = UUID.randomUUID().toString(),
    val category: String = "一般", // 一般, 作業, 考試, 公告, 重點
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val week: Int? = null
)

@Entity(tableName = "graduation_plans")
data class GraduationPlan(
    @PrimaryKey
    val id: Long = 1,
    val department: String = "",
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
    val subcategoriesJson: String = "", // JSON map of Category -> List<SubcategoryRule>
    val customCategoriesJson: String = "", // JSON list of CustomParentCategory
    val deletedCategories: String = "" // Comma-separated CourseCategory names, e.g. "GENERAL_EDU,COLLEGE_CORE"
) {
    fun getDeletedCategories(): Set<CourseCategory> {
        if (deletedCategories.isBlank()) return emptySet()
        return deletedCategories.split(",")
            .mapNotNull { name ->
                runCatching { CourseCategory.valueOf(name.trim()) }.getOrNull()
            }.toSet()
    }

    fun isCategoryDeleted(category: CourseCategory): Boolean {
        return getDeletedCategories().contains(category)
    }

    fun getSubcategoryRules(category: CourseCategory): List<SubcategoryRule> {
        if (subcategoriesJson.isBlank()) {
            return if (category == CourseCategory.GENERAL_EDU) {
                listOf(
                    SubcategoryRule(name = "語文：國語文能力", requiredCredits = 4.0, electiveCredits = 0.0),
                    SubcategoryRule(name = "語文：英語文能力", requiredCredits = 6.0, electiveCredits = 0.0),
                    SubcategoryRule(name = "資訊能力課程", requiredCredits = 2.0, electiveCredits = 0.0),
                    SubcategoryRule(name = "跨領域核心：人文藝術領域", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "跨領域核心：社會科學領域", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "跨領域核心：自然科學領域", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "博雅：美學、哲學與文化實踐", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "博雅：公民、社會與全球視野", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "博雅：科技、自然與環境生態", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "博雅：自我、人際與成長調適", requiredCredits = 0.0, electiveCredits = 2.0),
                    SubcategoryRule(name = "體適能：運動與健康", requiredCredits = 2.0, electiveCredits = 0.0)
                )
            } else emptyList()
        }
        return try {
            val json = org.json.JSONObject(subcategoriesJson)
            if (!json.has(category.name)) {
                return if (category == CourseCategory.GENERAL_EDU) {
                    listOf(
                        SubcategoryRule(name = "語文：國語文能力", requiredCredits = 4.0, electiveCredits = 0.0),
                        SubcategoryRule(name = "語文：英語文能力", requiredCredits = 6.0, electiveCredits = 0.0),
                        SubcategoryRule(name = "資訊能力課程", requiredCredits = 2.0, electiveCredits = 0.0),
                        SubcategoryRule(name = "跨領域核心：人文藝術領域", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "跨領域核心：社會科學領域", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "跨領域核心：自然科學領域", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "博雅：美學、哲學與文化實踐", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "博雅：公民、社會與全球視野", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "博雅：科技、自然與環境生態", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "博雅：自我、人際與成長調適", requiredCredits = 0.0, electiveCredits = 2.0),
                        SubcategoryRule(name = "體適能：運動與健康", requiredCredits = 2.0, electiveCredits = 0.0)
                    )
                } else emptyList()
            }
            val arr = json.optJSONArray(category.name) ?: return emptyList()
            val list = mutableListOf<SubcategoryRule>()
            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                if (item is org.json.JSONObject) {
                    val name = item.optString("name", "")
                    if (name.isNotBlank()) {
                        list.add(
                            SubcategoryRule(
                                id = item.optString("id", UUID.randomUUID().toString()),
                                name = name,
                                requiredCredits = item.optDouble("requiredCredits", 0.0),
                                electiveCredits = item.optDouble("electiveCredits", 0.0)
                            )
                        )
                    }
                } else if (item is String && item.isNotBlank()) {
                    list.add(SubcategoryRule(name = item))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getSubcategories(category: CourseCategory): List<String> {
        return getSubcategoryRules(category).map { it.name }
    }

    fun getCustomCategories(): List<CustomParentCategory> {
        if (customCategoriesJson.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(customCategoriesJson)
            val list = mutableListOf<CustomParentCategory>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val subsArr = obj.optJSONArray("subcategories")
                val subs = mutableListOf<SubcategoryRule>()
                if (subsArr != null) {
                    for (j in 0 until subsArr.length()) {
                        val item = subsArr.opt(j)
                        if (item is org.json.JSONObject) {
                            val name = item.optString("name", "")
                            if (name.isNotBlank()) {
                                subs.add(
                                    SubcategoryRule(
                                        id = item.optString("id", UUID.randomUUID().toString()),
                                        name = name,
                                        requiredCredits = item.optDouble("requiredCredits", 0.0),
                                        electiveCredits = item.optDouble("electiveCredits", 0.0)
                                    )
                                )
                            }
                        } else if (item is String && item.isNotBlank()) {
                            subs.add(SubcategoryRule(name = item))
                        }
                    }
                }
                list.add(
                    CustomParentCategory(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        colorHex = obj.optString("colorHex", "#8B5CF6"),
                        requiredCredits = obj.optDouble("requiredCredits", 0.0),
                        electiveCredits = obj.optDouble("electiveCredits", 0.0),
                        subcategories = subs
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodeSubcategoryRules(map: Map<CourseCategory, List<SubcategoryRule>>): String {
            val obj = org.json.JSONObject()
            map.forEach { (cat, list) ->
                val arr = org.json.JSONArray()
                list.filter { it.name.isNotBlank() }.forEach { rule ->
                    val ruleObj = org.json.JSONObject().apply {
                        put("id", rule.id)
                        put("name", rule.name)
                        put("requiredCredits", rule.requiredCredits)
                        put("electiveCredits", rule.electiveCredits)
                    }
                    arr.put(ruleObj)
                }
                obj.put(cat.name, arr)
            }
            return obj.toString()
        }

        fun encodeCustomCategories(list: List<CustomParentCategory>): String {
            val arr = org.json.JSONArray()
            list.forEach { cat ->
                if (cat.name.isNotBlank()) {
                    val obj = org.json.JSONObject()
                    obj.put("id", cat.id)
                    obj.put("name", cat.name)
                    obj.put("colorHex", cat.colorHex)
                    obj.put("requiredCredits", cat.requiredCredits)
                    obj.put("electiveCredits", cat.electiveCredits)
                    val subsArr = org.json.JSONArray()
                    cat.subcategories.filter { it.name.isNotBlank() }.forEach { rule ->
                        val ruleObj = org.json.JSONObject().apply {
                            put("id", rule.id)
                            put("name", rule.name)
                            put("requiredCredits", rule.requiredCredits)
                            put("electiveCredits", rule.electiveCredits)
                        }
                        subsArr.put(ruleObj)
                    }
                    obj.put("subcategories", subsArr)
                    arr.put(obj)
                }
            }
            return arr.toString()
        }
    }
}

data class SubcategoryRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val requiredCredits: Double = 0.0,
    val electiveCredits: Double = 0.0
)

data class CustomParentCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#8B5CF6",
    val requiredCredits: Double = 0.0,
    val electiveCredits: Double = 0.0,
    val subcategories: List<SubcategoryRule> = emptyList()
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

data class NotificationPreferences(
    val masterEnabled: Boolean = true,
    // 1. 課表與課程提醒
    val courseReminderEnabled: Boolean = true,
    val courseReminderMinutesBefore: Int = 15,
    val courseDailySummaryEnabled: Boolean = true,
    val courseDailySummaryTime: String = "07:30",
    val courseChangeNoticeEnabled: Boolean = true,
    val courseOnlyInSession: Boolean = true,
    // 2. 記帳與預算警示
    val expenseAlertEnabled: Boolean = true,
    val expenseAlertThresholdPercent: Int = 75,
    val expenseDailyReminderEnabled: Boolean = false,
    val expenseDailyReminderTime: String = "21:30",
    val expenseMonthlyReportEnabled: Boolean = true,
    val expenseTransactionNoticeEnabled: Boolean = true,
    // 3. 學業與畢業審查
    val graduationAlertEnabled: Boolean = true,
    val graduationCreditThresholdNotice: Boolean = true,
    val graduationGpaSettlementNotice: Boolean = true,
    val graduationAuditAlertNotice: Boolean = true,
    // 4. 系統與備份
    val systemNoticeEnabled: Boolean = true,
    val systemCloudBackupNotice: Boolean = true,
    val systemUpdateNotice: Boolean = true,
    // 5. 提醒方式
    val vibrationEnabled: Boolean = true,
    val badgeEnabled: Boolean = true
)

data class PaymentAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val method: PaymentMethod,
    val initialBalance: Double = 0.0,
    val note: String = "",
    val startYearMonth: String = ""
)

sealed class SemesterScheduleStatus {
    data class NotStarted(val daysUntilStart: Long, val startDate: String) : SemesterScheduleStatus()
    data class InSession(val currentWeek: Int, val totalWeeks: Int) : SemesterScheduleStatus()
    data class Ended(val totalWeeks: Int) : SemesterScheduleStatus()
}
