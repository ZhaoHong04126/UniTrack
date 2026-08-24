package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.CourseDao
import com.example.data.local.DefaultData
import com.example.data.local.ExpenseDao
import com.example.data.local.GraduationDao
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val graduationDao: GraduationDao,
    private val expenseDao: ExpenseDao
) {
    private val tag = "FirestoreSync"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                Log.w(tag, "FirebaseApp is not initialized.")
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firestore instance retrieval failed: ${e.message}")
            null
        }
    }

    /**
     * 將本機所有資料（課表、畢業審查、記帳、預算）上傳同步至 Cloud Firestore
     */
    suspend fun uploadAllToCloud(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("用戶 ID 不得為空"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore 尚未初始化"))

        try {
            val userDocRef = db.collection("users").document(userId)
            // 寫入用戶主文檔以確保非虛擬節點
            userDocRef.set(hashMapOf("lastActive" to System.currentTimeMillis()), SetOptions.merge()).await()

            // 1. 上傳 Graduation Plan
            val plan = graduationDao.getGraduationPlanOnce()
            if (plan != null) {
                val planMap = hashMapOf(
                    "department" to plan.department,
                    "studentName" to plan.studentName,
                    "targetTotalCredits" to plan.targetTotalCredits,
                    "targetRequiredCredits" to plan.targetRequiredCredits,
                    "targetElectiveCredits" to plan.targetElectiveCredits,
                    "targetGeneralCredits" to plan.targetGeneralCredits,
                    "targetCollegeCoreCredits" to plan.targetCollegeCoreCredits,
                    "targetBasicModuleCredits" to plan.targetBasicModuleCredits,
                    "targetCoreModuleCredits" to plan.targetCoreModuleCredits,
                    "targetProfessionalModuleCredits" to plan.targetProfessionalModuleCredits,
                    "targetFreeCredits" to plan.targetFreeCredits,
                    "minPassingScore" to plan.minPassingScore,
                    "gpaScale" to plan.gpaScale.name,
                    "admissionSemester" to plan.admissionSemester,
                    "currentSemester" to plan.currentSemester,
                    "lastUpdated" to System.currentTimeMillis()
                )
                userDocRef.collection("profile").document("graduation_plan")
                    .set(planMap, SetOptions.merge()).await()
            }

            // 2. 上傳 Courses
            val courses = courseDao.getAllCoursesOnce()
            val coursesCol = userDocRef.collection("courses")
            val remoteCourses = coursesCol.get().await()
            val localCourseIds = courses.map { it.id.toString() }.toSet()
            for (doc in remoteCourses.documents) {
                if (doc.id !in localCourseIds) {
                    doc.reference.delete().await()
                }
            }
            for (course in courses) {
                val courseMap = hashMapOf(
                    "id" to course.id,
                    "name" to course.name,
                    "code" to course.code,
                    "teacher" to course.teacher,
                    "location" to course.location,
                    "dayOfWeek" to course.dayOfWeek,
                    "startPeriod" to course.startPeriod,
                    "endPeriod" to course.endPeriod,
                    "startTime" to course.startTime,
                    "endTime" to course.endTime,
                    "credits" to course.credits,
                    "category" to course.category.name,
                    "requirementType" to course.requirementType.name,
                    "generalEduSubtype" to course.generalEduSubtype.name,
                    "semester" to course.semester,
                    "score" to course.score,
                    "letterGrade" to course.letterGrade,
                    "isCompleted" to course.isCompleted,
                    "colorHex" to course.colorHex,
                    "notes" to course.notes
                )
                coursesCol.document(course.id.toString()).set(courseMap, SetOptions.merge()).await()
            }

            // 3. 上傳 Graduation Thresholds
            val thresholds = graduationDao.getAllThresholdsOnce()
            val thresholdsCol = userDocRef.collection("thresholds")
            val remoteThresholds = thresholdsCol.get().await()
            val localThresholdIds = thresholds.map { it.id.toString() }.toSet()
            for (doc in remoteThresholds.documents) {
                if (doc.id !in localThresholdIds) {
                    doc.reference.delete().await()
                }
            }
            for (t in thresholds) {
                val tMap = hashMapOf(
                    "id" to t.id,
                    "title" to t.title,
                    "description" to t.description,
                    "isCompleted" to t.isCompleted,
                    "completedDate" to t.completedDate,
                    "proofNote" to t.proofNote
                )
                thresholdsCol.document(t.id.toString()).set(tMap, SetOptions.merge()).await()
            }

            // 4. 上傳 Expenses
            val expenses = expenseDao.getAllExpensesOnce()
            val expensesCol = userDocRef.collection("expenses")
            for (exp in expenses) {
                val expMap = hashMapOf(
                    "id" to exp.id,
                    "title" to exp.title,
                    "amount" to exp.amount,
                    "type" to exp.type.name,
                    "category" to exp.category.name,
                    "paymentMethod" to exp.paymentMethod.name,
                    "timestamp" to exp.timestamp,
                    "dateString" to exp.dateString,
                    "note" to exp.note
                )
                expensesCol.document(exp.id.toString()).set(expMap, SetOptions.merge()).await()
            }

            // 5. 上傳 Budgets
            val budgets = expenseDao.getAllBudgetsOnce()
            val budgetsCol = userDocRef.collection("budgets")
            for (b in budgets) {
                val bMap = hashMapOf(
                    "yearMonth" to b.yearMonth,
                    "budgetAmount" to b.budgetAmount
                )
                budgetsCol.document(b.yearMonth).set(bMap, SetOptions.merge()).await()
            }

            Log.i(tag, "Upload all data to Firestore completed successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Upload all data to Firestore failed", e)
            Result.failure(e)
        }
    }

    /**
     * 從 Cloud Firestore 下載所有雲端資料並合併儲存至本機資料庫
     */
    suspend fun downloadAllFromCloud(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("用戶 ID 不得為空"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore 尚未初始化"))

        try {
            val userDocRef = db.collection("users").document(userId)

            // 1. 下載 Graduation Plan
            val planSnapshot = userDocRef.collection("profile").document("graduation_plan").get().await()
            if (planSnapshot.exists()) {
                val localPlan = graduationDao.getGraduationPlanOnce()
                val remoteDepartment = planSnapshot.getString("department")?.trim()
                val remoteStudentName = planSnapshot.getString("studentName")?.trim()

                // Resolution logic:
                // If remote department is valid (not blank and not "尚未設定系所"), use remote.
                // Else if local plan has a valid department, KEEP local department!
                val resolvedDept = if (!remoteDepartment.isNullOrBlank() && remoteDepartment != "尚未設定系所") {
                    remoteDepartment
                } else if (localPlan != null && localPlan.department.isNotBlank() && localPlan.department != "尚未設定系所") {
                    localPlan.department
                } else {
                    remoteDepartment ?: "尚未設定系所"
                }

                val isDefaultName = { n: String? -> n.isNullOrBlank() || n == "同學" || n == "王大明" || n == "大學生" }
                val resolvedName = if (!isDefaultName(remoteStudentName)) {
                    remoteStudentName!!
                } else if (localPlan != null && !isDefaultName(localPlan.studentName)) {
                    localPlan.studentName
                } else {
                    remoteStudentName ?: localPlan?.studentName ?: "同學"
                }

                val plan = GraduationPlan(
                    id = 1,
                    department = resolvedDept,
                    studentName = resolvedName,
                    targetTotalCredits = planSnapshot.getDouble("targetTotalCredits") ?: (localPlan?.targetTotalCredits ?: 128.0),
                    targetRequiredCredits = planSnapshot.getDouble("targetRequiredCredits") ?: (localPlan?.targetRequiredCredits ?: 58.0),
                    targetElectiveCredits = planSnapshot.getDouble("targetElectiveCredits") ?: (localPlan?.targetElectiveCredits ?: 36.0),
                    targetGeneralCredits = planSnapshot.getDouble("targetGeneralCredits") ?: (localPlan?.targetGeneralCredits ?: 28.0),
                    targetCollegeCoreCredits = planSnapshot.getDouble("targetCollegeCoreCredits") ?: (localPlan?.targetCollegeCoreCredits ?: 9.0),
                    targetBasicModuleCredits = planSnapshot.getDouble("targetBasicModuleCredits") ?: (localPlan?.targetBasicModuleCredits ?: 24.0),
                    targetCoreModuleCredits = planSnapshot.getDouble("targetCoreModuleCredits") ?: (localPlan?.targetCoreModuleCredits ?: 24.0),
                    targetProfessionalModuleCredits = planSnapshot.getDouble("targetProfessionalModuleCredits") ?: (localPlan?.targetProfessionalModuleCredits ?: 23.0),
                    targetFreeCredits = planSnapshot.getDouble("targetFreeCredits") ?: (localPlan?.targetFreeCredits ?: 20.0),
                    minPassingScore = planSnapshot.getDouble("minPassingScore") ?: (localPlan?.minPassingScore ?: 60.0),
                    gpaScale = runCatching {
                        GpaScale.valueOf(planSnapshot.getString("gpaScale") ?: (localPlan?.gpaScale?.name ?: GpaScale.PERCENTAGE.name))
                    }.getOrDefault(localPlan?.gpaScale ?: GpaScale.PERCENTAGE),
                    admissionSemester = planSnapshot.getString("admissionSemester") ?: (localPlan?.admissionSemester ?: DefaultData.getCurrentAcademicSemester()),
                    currentSemester = planSnapshot.getString("currentSemester") ?: (localPlan?.currentSemester ?: DefaultData.getCurrentAcademicSemester())
                )
                graduationDao.insertOrUpdatePlan(plan)

                // If local had a better department/name than cloud, update cloud as well so cloud is in sync!
                if (resolvedDept != remoteDepartment || resolvedName != remoteStudentName) {
                    val updateMap = mutableMapOf<String, Any>(
                        "department" to resolvedDept,
                        "studentName" to resolvedName,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    userDocRef.collection("profile").document("graduation_plan")
                        .set(updateMap, SetOptions.merge()).await()
                }
            } else {
                val localPlan = graduationDao.getGraduationPlanOnce()
                if (localPlan != null) {
                    val planMap = hashMapOf(
                        "department" to localPlan.department,
                        "studentName" to localPlan.studentName,
                        "targetTotalCredits" to localPlan.targetTotalCredits,
                        "targetRequiredCredits" to localPlan.targetRequiredCredits,
                        "targetElectiveCredits" to localPlan.targetElectiveCredits,
                        "targetGeneralCredits" to localPlan.targetGeneralCredits,
                        "targetCollegeCoreCredits" to localPlan.targetCollegeCoreCredits,
                        "targetBasicModuleCredits" to localPlan.targetBasicModuleCredits,
                        "targetCoreModuleCredits" to localPlan.targetCoreModuleCredits,
                        "targetProfessionalModuleCredits" to localPlan.targetProfessionalModuleCredits,
                        "targetFreeCredits" to localPlan.targetFreeCredits,
                        "minPassingScore" to localPlan.minPassingScore,
                        "gpaScale" to localPlan.gpaScale.name,
                        "admissionSemester" to localPlan.admissionSemester,
                        "currentSemester" to localPlan.currentSemester,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    userDocRef.collection("profile").document("graduation_plan")
                        .set(planMap, SetOptions.merge()).await()
                }
            }

            // 2. 下載 Courses
            val coursesSnapshot = userDocRef.collection("courses").get().await()
            if (!coursesSnapshot.isEmpty) {
                val downloadedCourses = mutableListOf<Course>()
                for (doc in coursesSnapshot.documents) {
                    val id = doc.getLong("id") ?: continue
                    val name = doc.getString("name") ?: continue
                    val course = Course(
                        id = id,
                        name = name,
                        code = doc.getString("code") ?: "",
                        teacher = doc.getString("teacher") ?: "",
                        location = doc.getString("location") ?: "",
                        dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: 1,
                        startPeriod = doc.getLong("startPeriod")?.toInt() ?: 1,
                        endPeriod = doc.getLong("endPeriod")?.toInt() ?: 1,
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        credits = doc.getDouble("credits") ?: 3.0,
                        category = runCatching {
                            CourseCategory.valueOf(doc.getString("category") ?: CourseCategory.GENERAL_EDU.name)
                        }.getOrDefault(CourseCategory.GENERAL_EDU),
                        requirementType = runCatching {
                            CourseRequirementType.valueOf(doc.getString("requirementType") ?: CourseRequirementType.REQUIRED.name)
                        }.getOrDefault(CourseRequirementType.REQUIRED),
                        generalEduSubtype = runCatching {
                            GeneralEduSubtype.valueOf(doc.getString("generalEduSubtype") ?: GeneralEduSubtype.NONE.name)
                        }.getOrDefault(GeneralEduSubtype.NONE),
                        semester = doc.getString("semester") ?: DefaultData.getCurrentAcademicSemester(),
                        score = doc.getDouble("score"),
                        letterGrade = doc.getString("letterGrade"),
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        colorHex = doc.getString("colorHex") ?: "#3B82F6",
                        notes = doc.getString("notes") ?: ""
                    )
                    downloadedCourses.add(course)
                }
                if (downloadedCourses.isNotEmpty()) {
                    courseDao.insertCourses(downloadedCourses)
                }
            }

            // 3. 下載 Thresholds
            val thresholdsSnapshot = userDocRef.collection("thresholds").get().await()
            if (!thresholdsSnapshot.isEmpty) {
                val downloadedThresholds = mutableListOf<GraduationThreshold>()
                for (doc in thresholdsSnapshot.documents) {
                    val id = doc.getLong("id") ?: continue
                    val title = doc.getString("title") ?: continue
                    val t = GraduationThreshold(
                        id = id,
                        title = title,
                        description = doc.getString("description") ?: "",
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        completedDate = doc.getString("completedDate") ?: "",
                        proofNote = doc.getString("proofNote") ?: ""
                    )
                    downloadedThresholds.add(t)
                }
                if (downloadedThresholds.isNotEmpty()) {
                    graduationDao.insertThresholds(downloadedThresholds)
                }
            }

            // 4. 下載 Expenses
            val expensesSnapshot = userDocRef.collection("expenses").get().await()
            if (!expensesSnapshot.isEmpty) {
                val downloadedExpenses = mutableListOf<ExpenseRecord>()
                for (doc in expensesSnapshot.documents) {
                    val id = doc.getLong("id") ?: continue
                    val title = doc.getString("title") ?: continue
                    val exp = ExpenseRecord(
                        id = id,
                        title = title,
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = runCatching {
                            ExpenseType.valueOf(doc.getString("type") ?: ExpenseType.EXPENSE.name)
                        }.getOrDefault(ExpenseType.EXPENSE),
                        category = runCatching {
                            ExpenseCategory.valueOf(doc.getString("category") ?: ExpenseCategory.FOOD.name)
                        }.getOrDefault(ExpenseCategory.FOOD),
                        paymentMethod = runCatching {
                            PaymentMethod.valueOf(doc.getString("paymentMethod") ?: PaymentMethod.CASH.name)
                        }.getOrDefault(PaymentMethod.CASH),
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        dateString = doc.getString("dateString") ?: "",
                        note = doc.getString("note") ?: ""
                    )
                    downloadedExpenses.add(exp)
                }
                if (downloadedExpenses.isNotEmpty()) {
                    expenseDao.insertExpenses(downloadedExpenses)
                }
            }

            // 5. 下載 Budgets
            val budgetsSnapshot = userDocRef.collection("budgets").get().await()
            if (!budgetsSnapshot.isEmpty) {
                for (doc in budgetsSnapshot.documents) {
                    val ym = doc.getString("yearMonth") ?: doc.id
                    val amount = doc.getDouble("budgetAmount") ?: 12000.0
                    expenseDao.setBudget(MonthlyBudget(yearMonth = ym, budgetAmount = amount))
                }
            }

            Log.i(tag, "Download all data from Firestore completed successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Download all data from Firestore failed", e)
            Result.failure(e)
        }
    }

    /**
     * 雙向智慧同步：若雲端已有資料則拉取合併；若雲端為空則將本機資料備份上傳至雲端
     */
    suspend fun syncBidirectional(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("用戶 ID 不得為空"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore 尚未初始化"))

        try {
            val userDocRef = db.collection("users").document(userId)
            val coursesSnapshot = userDocRef.collection("courses").limit(1).get().await()
            val planSnapshot = userDocRef.collection("profile").document("graduation_plan").get().await()

            if (!coursesSnapshot.isEmpty || planSnapshot.exists()) {
                // 雲端已有資料，從雲端拉取並智慧合併
                downloadAllFromCloud(userId)
            } else {
                // 雲端為空，將本機資料備份至雲端
                uploadAllToCloud(userId)
            }
        } catch (e: Exception) {
            Log.e(tag, "Bidirectional sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * 徹底刪除該用戶在 Cloud Firestore 上的所有資料（使用者檔案、課程、審查門檻、記帳、預算）
     */
    suspend fun deleteAllCloudData(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.success(Unit)
        val db = firestore ?: return@withContext Result.success(Unit)

        try {
            val userDocRef = db.collection("users").document(userId)

            val subcollections = listOf("profile", "courses", "thresholds", "expenses", "budgets")
            for (sub in subcollections) {
                try {
                    val snapshot = userDocRef.collection(sub).get().await()
                    for (doc in snapshot.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed to delete subcollection $sub for user $userId", e)
                }
            }

            // 刪除 user 主文檔
            try {
                userDocRef.delete().await()
            } catch (e: Exception) {
                Log.w(tag, "Failed to delete user doc for user $userId", e)
            }

            Log.i(tag, "All Cloud Firestore data deleted successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Delete all Cloud Firestore data failed", e)
            Result.failure(e)
        }
    }
}
