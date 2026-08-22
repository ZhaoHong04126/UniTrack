package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class CourseCategory(val label: String, val shortLabel: String, val badgeColor: Color) {
    REQUIRED("必修課程", "必修", Color(0xFFEF4444)),
    ELECTIVE("選修課程", "選修", Color(0xFFF59E0B)),
    GENERAL_EDU("通識教育課程", "通識", Color(0xFF10B981)),
    COLLEGE_CORE("院共同課程", "院核", Color(0xFF3B82F6)),
    BASIC_MODULE("基礎模組", "基礎", Color(0xFF6366F1)),
    CORE_MODULE("核心模組", "核心", Color(0xFF8B5CF6)),
    PROFESSIONAL_MODULE("專業模組", "專業", Color(0xFFEF4444)),
    FREE_ELECTIVE("自由選修", "自選", Color(0xFFF59E0B)),
    PE("體育課程", "體育", Color(0xFFEC4899))
}

enum class GeneralEduSubtype(val label: String) {
    NONE("無 / 核心必修"),
    HUMANITIES("人文藝術"),
    SOCIAL_SCIENCE("社會科學"),
    NATURAL_SCIENCE("自然科學"),
    CORE("核心通識"),
    INTERDISCIPLINARY("跨領域 / 融合通識")
}

enum class ExpenseType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入")
}

enum class ExpenseCategory(val label: String, val iconName: String) {
    FOOD("餐飲美食", "restaurant"),
    BOOKS_STUDY("書籍學業", "menu_book"),
    TRANSPORT("交通出行", "directions_bus"),
    RENT_UTILITY("住宿水電", "home"),
    ENTERTAINMENT("休閒娛樂", "sports_esports"),
    DAILY("生活用品", "shopping_bag"),
    SALARY_JOB("打工家教", "work"),
    SCHOLARSHIP("獎助學金", "school"),
    OTHER("其他收支", "more_horiz")
}

enum class PaymentMethod(val label: String) {
    CASH("現金"),
    MOBILE_PAY("行動支付 (LinePay/街口)"),
    IC_CARD("悠遊卡 / 一卡通"),
    CARD("信用卡 / 簽帳卡"),
    TRANSFER("銀行轉帳")
}

enum class GpaScale(val label: String, val maxGpa: Double) {
    SCALE_4_3("4.3 制 (A+=4.3, A=4.0...)", 4.3),
    SCALE_4_0("4.0 制 (A=4.0, B=3.0...)", 4.0),
    PERCENTAGE("百分制 (100分制)", 100.0)
}
