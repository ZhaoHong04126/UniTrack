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
            gpaScale = GpaScale.SCALE_4_3,
            currentSemester = "113-2"
        )
    }

    fun getDefaultThresholds(): List<GraduationThreshold> {
        return listOf(
            GraduationThreshold(
                title = "英文畢業門檻 (TOEIC 750分以上 / GEPT中高級)",
                description = "多益金色證書或全民英檢中高級初試通過",
                isCompleted = true,
                completedDate = "2025-11-20",
                proofNote = "多益成績單 835 分"
            ),
            GraduationThreshold(
                title = "畢業專題實作與口試",
                description = "大三下至大四上完成畢業專題審查與論文海報展",
                isCompleted = false,
                completedDate = "",
                proofNote = "題目：校園智慧助手系統"
            ),
            GraduationThreshold(
                title = "校園服務學習 (共 36 小時)",
                description = "完成系所及全校服務學習課程及時數認證",
                isCompleted = true,
                completedDate = "2025-06-15",
                proofNote = "志工服務時數核定通過 40 小時"
            ),
            GraduationThreshold(
                title = "程式設計能力檢定 (CPE / 專業證照)",
                description = "大學程式能力檢定 (CPE) 答對 3 題以上",
                isCompleted = false,
                completedDate = "",
                proofNote = "目前累計答對 2 題"
            ),
            GraduationThreshold(
                title = "常規體育課 4 學期",
                description = "大一至大二體育必修修畢通過",
                isCompleted = true,
                completedDate = "2025-06-20",
                proofNote = "羽球、游泳、桌球、網球修畢"
            )
        )
    }

    fun getDefaultCourses(): List<Course> {
        return listOf(
            // Current Semester: 113-2 (Active timetable)
            Course(
                name = "演算法 (Algorithms)",
                teacher = "陳教授",
                location = "電資大樓 201",
                dayOfWeek = 1, // Mon
                startPeriod = 2,
                endPeriod = 4,
                startTime = "09:10",
                endTime = "12:00",
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                generalEduSubtype = GeneralEduSubtype.NONE,
                semester = "113-2",
                score = null,
                letterGrade = null,
                isCompleted = false,
                colorHex = "#2563EB",
                notes = "每週一有小考，期中考佔 30%"
            ),
            Course(
                name = "作業系統 (Operating Systems)",
                teacher = "李教授",
                location = "工程一館 105",
                dayOfWeek = 2, // Tue
                startPeriod = 3,
                endPeriod = 4,
                startTime = "10:20",
                endTime = "12:00",
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                generalEduSubtype = GeneralEduSubtype.NONE,
                semester = "113-2",
                score = null,
                letterGrade = null,
                isCompleted = false,
                colorHex = "#0D9488",
                notes = "Lab 1 需在下週日前繳交"
            ),
            Course(
                name = "人工智慧導論 (Intro to AI)",
                teacher = "張教授",
                location = "綜合大樓 302",
                dayOfWeek = 3, // Wed
                startPeriod = 6,
                endPeriod = 8,
                startTime = "13:20",
                endTime = "16:10",
                credits = 3.0,
                category = CourseCategory.ELECTIVE,
                generalEduSubtype = GeneralEduSubtype.NONE,
                semester = "113-2",
                score = null,
                letterGrade = null,
                isCompleted = false,
                colorHex = "#7C3AED",
                notes = "期末小組專題報告"
            ),
            Course(
                name = "現代心理學 (Psychology)",
                teacher = "林講師",
                location = "博雅館 101",
                dayOfWeek = 4, // Thu
                startPeriod = 3,
                endPeriod = 4,
                startTime = "10:20",
                endTime = "12:00",
                credits = 2.0,
                category = CourseCategory.GENERAL_EDU,
                generalEduSubtype = GeneralEduSubtype.SOCIAL_SCIENCE,
                semester = "113-2",
                score = null,
                letterGrade = null,
                isCompleted = false,
                colorHex = "#10B981",
                notes = "社會科學通識領域"
            ),
            Course(
                name = "日語初級 (二)",
                teacher = "田中老師",
                location = "文學院 204",
                dayOfWeek = 5, // Fri
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:10",
                endTime = "10:00",
                credits = 2.0,
                category = CourseCategory.FREE_ELECTIVE,
                generalEduSubtype = GeneralEduSubtype.NONE,
                semester = "113-2",
                score = null,
                letterGrade = null,
                isCompleted = false,
                colorHex = "#F59E0B",
                notes = "單字測驗在第五週"
            ),
            Course(
                name = "羽球進階",
                teacher = "王教練",
                location = "綜合體育館 B1",
                dayOfWeek = 2, // Tue
                startPeriod = 6,
                endPeriod = 7,
                startTime = "13:20",
                endTime = "15:10",
                credits = 1.0,
                category = CourseCategory.PE,
                generalEduSubtype = GeneralEduSubtype.NONE,
                semester = "113-2",
                score = null,
                letterGrade = null,
                isCompleted = false,
                colorHex = "#EC4899",
                notes = "需自備球拍與運動鞋"
            ),

            // Completed Semesters for GPA & Credit calculations
            // 113-1
            Course(
                name = "資料結構 (Data Structures)",
                teacher = "陳教授",
                location = "電資 201",
                dayOfWeek = 1,
                startPeriod = 2,
                endPeriod = 4,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "113-1",
                score = 88.0,
                letterGrade = "A",
                isCompleted = true,
                colorHex = "#2563EB"
            ),
            Course(
                name = "離散數學 (Discrete Math)",
                teacher = "郭教授",
                location = "工綜 301",
                dayOfWeek = 2,
                startPeriod = 2,
                endPeriod = 4,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "113-1",
                score = 85.0,
                letterGrade = "A",
                isCompleted = true,
                colorHex = "#0D9488"
            ),
            Course(
                name = "網頁前端程式設計",
                teacher = "周講師",
                location = "電算中心 4",
                dayOfWeek = 3,
                startPeriod = 6,
                endPeriod = 8,
                credits = 3.0,
                category = CourseCategory.ELECTIVE,
                semester = "113-1",
                score = 92.0,
                letterGrade = "A+",
                isCompleted = true,
                colorHex = "#7C3AED"
            ),
            Course(
                name = "哲學與當代社會",
                teacher = "高教授",
                location = "共同教室 102",
                dayOfWeek = 4,
                startPeriod = 3,
                endPeriod = 4,
                credits = 2.0,
                category = CourseCategory.GENERAL_EDU,
                generalEduSubtype = GeneralEduSubtype.HUMANITIES,
                semester = "113-1",
                score = 84.0,
                letterGrade = "A-",
                isCompleted = true,
                colorHex = "#10B981"
            ),
            Course(
                name = "桌球初級",
                teacher = "黃老師",
                location = "體育館",
                dayOfWeek = 5,
                startPeriod = 3,
                endPeriod = 4,
                credits = 1.0,
                category = CourseCategory.PE,
                semester = "113-1",
                score = 90.0,
                letterGrade = "A+",
                isCompleted = true,
                colorHex = "#EC4899"
            ),

            // 112-2
            Course(
                name = "物件導向程式設計 (OOP)",
                teacher = "許教授",
                location = "電資 102",
                dayOfWeek = 1,
                startPeriod = 2,
                endPeriod = 4,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "112-2",
                score = 91.0,
                letterGrade = "A+",
                isCompleted = true,
                colorHex = "#2563EB"
            ),
            Course(
                name = "數位邏輯設計 (Digital Logic)",
                teacher = "賴教授",
                location = "電資 205",
                dayOfWeek = 2,
                startPeriod = 6,
                endPeriod = 8,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "112-2",
                score = 82.0,
                letterGrade = "A-",
                isCompleted = true,
                colorHex = "#0D9488"
            ),
            Course(
                name = "線性代數 (Linear Algebra)",
                teacher = "何教授",
                location = "理學院 101",
                dayOfWeek = 3,
                startPeriod = 2,
                endPeriod = 4,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "112-2",
                score = 78.0,
                letterGrade = "B+",
                isCompleted = true,
                colorHex = "#2563EB"
            ),
            Course(
                name = "全球環境與永續發展",
                teacher = "趙教授",
                location = "綜合 202",
                dayOfWeek = 4,
                startPeriod = 5,
                endPeriod = 6,
                credits = 2.0,
                category = CourseCategory.GENERAL_EDU,
                generalEduSubtype = GeneralEduSubtype.NATURAL_SCIENCE,
                semester = "112-2",
                score = 86.0,
                letterGrade = "A",
                isCompleted = true,
                colorHex = "#10B981"
            ),

            // 112-1
            Course(
                name = "計算機程式設計 (Intro to Programming)",
                teacher = "許教授",
                location = "電資 102",
                dayOfWeek = 1,
                startPeriod = 6,
                endPeriod = 8,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "112-1",
                score = 95.0,
                letterGrade = "A+",
                isCompleted = true,
                colorHex = "#2563EB"
            ),
            Course(
                name = "微積分 (一) (Calculus I)",
                teacher = "廖教授",
                location = "理學院 101",
                dayOfWeek = 3,
                startPeriod = 2,
                endPeriod = 4,
                credits = 4.0,
                category = CourseCategory.REQUIRED,
                semester = "112-1",
                score = 80.0,
                letterGrade = "B+",
                isCompleted = true,
                colorHex = "#2563EB"
            ),
            Course(
                name = "普通物理學 (General Physics)",
                teacher = "彭教授",
                location = "理學院 201",
                dayOfWeek = 4,
                startPeriod = 2,
                endPeriod = 4,
                credits = 3.0,
                category = CourseCategory.REQUIRED,
                semester = "112-1",
                score = 75.0,
                letterGrade = "B",
                isCompleted = true,
                colorHex = "#2563EB"
            ),
            Course(
                name = "大學國文：經典閱讀與思辨",
                teacher = "梁教授",
                location = "文學院 101",
                dayOfWeek = 2,
                startPeriod = 3,
                endPeriod = 4,
                credits = 2.0,
                category = CourseCategory.GENERAL_EDU,
                generalEduSubtype = GeneralEduSubtype.CORE,
                semester = "112-1",
                score = 88.0,
                letterGrade = "A",
                isCompleted = true,
                colorHex = "#10B981"
            )
        )
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
