package com.example.data.repository

import com.example.data.local.CourseDao
import com.example.data.local.DefaultData
import com.example.data.local.ExpenseDao
import com.example.data.local.GraduationDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StudentRepository(
    private val courseDao: CourseDao,
    private val graduationDao: GraduationDao,
    private val expenseDao: ExpenseDao
) {
    // Courses
    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    val allSemesters: Flow<List<String>> = courseDao.getAllSemesters()

    suspend fun insertCourse(course: Course): Long = withContext(Dispatchers.IO) {
        courseDao.insertCourse(course)
    }

    suspend fun updateCourse(course: Course) = withContext(Dispatchers.IO) {
        courseDao.updateCourse(course)
    }

    suspend fun deleteCourse(course: Course) = withContext(Dispatchers.IO) {
        courseDao.deleteCourse(course)
    }

    // Graduation Plan & Thresholds
    val graduationPlan: Flow<GraduationPlan?> = graduationDao.getGraduationPlan()
    val allThresholds: Flow<List<GraduationThreshold>> = graduationDao.getAllThresholds()

    suspend fun updateGraduationPlan(plan: GraduationPlan) = withContext(Dispatchers.IO) {
        graduationDao.insertOrUpdatePlan(plan)
    }

    suspend fun insertThreshold(threshold: GraduationThreshold): Long = withContext(Dispatchers.IO) {
        graduationDao.insertThreshold(threshold)
    }

    suspend fun updateThreshold(threshold: GraduationThreshold) = withContext(Dispatchers.IO) {
        graduationDao.updateThreshold(threshold)
    }

    suspend fun deleteThreshold(threshold: GraduationThreshold) = withContext(Dispatchers.IO) {
        graduationDao.deleteThreshold(threshold)
    }

    // Expenses & Budgets
    val allExpenses: Flow<List<ExpenseRecord>> = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: ExpenseRecord): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseRecord) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseRecord) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun setBudget(budget: MonthlyBudget) = withContext(Dispatchers.IO) {
        expenseDao.setBudget(budget)
    }

    // Initialize Database with Default Data if empty
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingPlan = graduationDao.getGraduationPlanOnce()
        if (existingPlan == null) {
            graduationDao.insertOrUpdatePlan(DefaultData.getDefaultGraduationPlan())
            graduationDao.insertThresholds(DefaultData.getDefaultThresholds())
            courseDao.insertCourses(DefaultData.getDefaultCourses())
            expenseDao.insertExpenses(DefaultData.getDefaultExpenses())
            expenseDao.setBudget(DefaultData.getDefaultBudget())
        }
    }

    suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        courseDao.deleteAllCourses()
        graduationDao.deleteAllThresholds()
        expenseDao.deleteAllExpenses()

        graduationDao.insertOrUpdatePlan(DefaultData.getDefaultGraduationPlan())
        graduationDao.insertThresholds(DefaultData.getDefaultThresholds())
        courseDao.insertCourses(DefaultData.getDefaultCourses())
        expenseDao.insertExpenses(DefaultData.getDefaultExpenses())
        expenseDao.setBudget(DefaultData.getDefaultBudget())
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        courseDao.deleteAllCourses()
        graduationDao.deleteAllThresholds()
        expenseDao.deleteAllExpenses()
        graduationDao.insertOrUpdatePlan(DefaultData.getDefaultGraduationPlan())
    }

    // Export & Import Offline JSON for full backup
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val courses = allCourses.firstOrNull() ?: emptyList()
        val plan = graduationDao.getGraduationPlanOnce() ?: DefaultData.getDefaultGraduationPlan()
        val thresholds = allThresholds.firstOrNull() ?: emptyList()
        val expenses = allExpenses.firstOrNull() ?: emptyList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val planObj = JSONObject().apply {
            put("department", plan.department)
            put("studentName", plan.studentName)
            put("targetTotalCredits", plan.targetTotalCredits)
            put("targetRequiredCredits", plan.targetRequiredCredits)
            put("targetElectiveCredits", plan.targetElectiveCredits)
            put("targetGeneralCredits", plan.targetGeneralCredits)
            put("targetFreeCredits", plan.targetFreeCredits)
            put("currentSemester", plan.currentSemester)
            put("gpaScale", plan.gpaScale.name)
        }
        root.put("graduationPlan", planObj)

        val coursesArray = JSONArray()
        courses.forEach { c ->
            val cObj = JSONObject().apply {
                put("name", c.name)
                put("code", c.code)
                put("teacher", c.teacher)
                put("location", c.location)
                put("dayOfWeek", c.dayOfWeek)
                put("startPeriod", c.startPeriod)
                put("endPeriod", c.endPeriod)
                put("startTime", c.startTime)
                put("endTime", c.endTime)
                put("credits", c.credits)
                put("category", c.category.name)
                put("generalEduSubtype", c.generalEduSubtype.name)
                put("semester", c.semester)
                put("score", c.score ?: JSONObject.NULL)
                put("letterGrade", c.letterGrade ?: JSONObject.NULL)
                put("isCompleted", c.isCompleted)
                put("colorHex", c.colorHex)
                put("notes", c.notes)
            }
            coursesArray.put(cObj)
        }
        root.put("courses", coursesArray)

        val thresholdsArray = JSONArray()
        thresholds.forEach { t ->
            val tObj = JSONObject().apply {
                put("title", t.title)
                put("description", t.description)
                put("isCompleted", t.isCompleted)
                put("completedDate", t.completedDate)
                put("proofNote", t.proofNote)
            }
            thresholdsArray.put(tObj)
        }
        root.put("thresholds", thresholdsArray)

        val expensesArray = JSONArray()
        expenses.forEach { e ->
            val eObj = JSONObject().apply {
                put("title", e.title)
                put("amount", e.amount)
                put("type", e.type.name)
                put("category", e.category.name)
                put("paymentMethod", e.paymentMethod.name)
                put("timestamp", e.timestamp)
                put("dateString", e.dateString)
                put("note", e.note)
            }
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("graduationPlan")) {
                val planObj = root.getJSONObject("graduationPlan")
                val plan = GraduationPlan(
                    id = 1,
                    department = planObj.optString("department", "資訊工程學系"),
                    studentName = planObj.optString("studentName", "同學"),
                    targetTotalCredits = planObj.optDouble("targetTotalCredits", 128.0),
                    targetRequiredCredits = planObj.optDouble("targetRequiredCredits", 58.0),
                    targetElectiveCredits = planObj.optDouble("targetElectiveCredits", 36.0),
                    targetGeneralCredits = planObj.optDouble("targetGeneralCredits", 28.0),
                    targetFreeCredits = planObj.optDouble("targetFreeCredits", 6.0),
                    currentSemester = planObj.optString("currentSemester", "114-1"),
                    gpaScale = runCatching { GpaScale.valueOf(planObj.optString("gpaScale", "PERCENTAGE")) }.getOrDefault(GpaScale.PERCENTAGE)
                )
                graduationDao.insertOrUpdatePlan(plan)
            }

            if (root.has("courses")) {
                val arr = root.getJSONArray("courses")
                val courseList = mutableListOf<Course>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    courseList.add(
                        Course(
                            name = o.getString("name"),
                            code = o.optString("code", ""),
                            teacher = o.optString("teacher", ""),
                            location = o.optString("location", ""),
                            dayOfWeek = o.optInt("dayOfWeek", 1),
                            startPeriod = o.optInt("startPeriod", 1),
                            endPeriod = o.optInt("endPeriod", 1),
                            startTime = o.optString("startTime", ""),
                            endTime = o.optString("endTime", ""),
                            credits = o.optDouble("credits", 3.0),
                            category = runCatching { CourseCategory.valueOf(o.optString("category", "REQUIRED")) }.getOrDefault(CourseCategory.REQUIRED),
                            generalEduSubtype = runCatching {
                                when (val s = o.optString("generalEduSubtype", "NONE")) {
                                    "HUMANITIES" -> GeneralEduSubtype.CORE_HUMANITIES
                                    "SOCIAL_SCIENCE" -> GeneralEduSubtype.CORE_SOCIAL
                                    "NATURAL_SCIENCE" -> GeneralEduSubtype.CORE_NATURAL
                                    "CORE" -> GeneralEduSubtype.CHINESE
                                    "INTERDISCIPLINARY" -> GeneralEduSubtype.CORE_HUMANITIES
                                    else -> GeneralEduSubtype.valueOf(s)
                                }
                            }.getOrDefault(GeneralEduSubtype.NONE),
                            semester = o.optString("semester", "113-2"),
                            score = if (o.isNull("score")) null else o.optDouble("score"),
                            letterGrade = if (o.isNull("letterGrade")) null else o.optString("letterGrade"),
                            isCompleted = o.optBoolean("isCompleted", false),
                            colorHex = o.optString("colorHex", "#3B82F6"),
                            notes = o.optString("notes", "")
                        )
                    )
                }
                courseDao.deleteAllCourses()
                courseDao.insertCourses(courseList)
            }

            if (root.has("thresholds")) {
                val arr = root.getJSONArray("thresholds")
                val list = mutableListOf<GraduationThreshold>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        GraduationThreshold(
                            title = o.getString("title"),
                            description = o.optString("description", ""),
                            isCompleted = o.optBoolean("isCompleted", false),
                            completedDate = o.optString("completedDate", ""),
                            proofNote = o.optString("proofNote", "")
                        )
                    )
                }
                graduationDao.deleteAllThresholds()
                graduationDao.insertThresholds(list)
            }

            if (root.has("expenses")) {
                val arr = root.getJSONArray("expenses")
                val list = mutableListOf<ExpenseRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        ExpenseRecord(
                            title = o.getString("title"),
                            amount = o.getDouble("amount"),
                            type = runCatching { ExpenseType.valueOf(o.optString("type", "EXPENSE")) }.getOrDefault(ExpenseType.EXPENSE),
                            category = runCatching { ExpenseCategory.valueOf(o.optString("category", "FOOD")) }.getOrDefault(ExpenseCategory.FOOD),
                            paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod", "CASH")) }.getOrDefault(PaymentMethod.CASH),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            dateString = o.optString("dateString", ""),
                            note = o.optString("note", "")
                        )
                    )
                }
                expenseDao.deleteAllExpenses()
                expenseDao.insertExpenses(list)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
