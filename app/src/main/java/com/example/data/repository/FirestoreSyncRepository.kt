package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.data.local.CourseDao
import com.example.data.local.DefaultData
import com.example.data.local.ExpenseDao
import com.example.data.local.GraduationDao
import com.example.data.local.NotificationDao
import com.example.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val graduationDao: GraduationDao,
    private val expenseDao: ExpenseDao,
    private val notificationDao: NotificationDao
) {
    private val tag = "FirestoreSync"
    private val syncMutex = Mutex()

    @Suppress("SpellCheckingInspection")
    private val prefs by lazy {
        context.getSharedPreferences("unitrack_prefs", Context.MODE_PRIVATE)
    }

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
     * 一鍵將本機 Room 所有的資料完整備份至 Firebase 雲端
     * 僅執行 UPSERT（覆寫或新增），絕不因本機暫時無資料而誤刪雲端文檔
     */
    suspend fun uploadAllToCloud(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("用戶 ID 不得為空"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore 尚未初始化"))

        syncMutex.withLock {
            try {
                val userDocRef = db.collection("users").document(userId)
                // 寫入用戶主文檔以確保非虛擬節點
                userDocRef.set(hashMapOf("lastActive" to System.currentTimeMillis()), SetOptions.merge()).await()

                // 1. 上傳 Graduation Plan
                val plan = graduationDao.getGraduationPlanOnce()
                if (plan != null) {
                    val planDocRef = userDocRef.collection("profile").document("graduation_plan")
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
                        "targetGeneralRequiredCredits" to plan.targetGeneralRequiredCredits,
                        "targetGeneralElectiveCredits" to plan.targetGeneralElectiveCredits,
                        "targetCollegeCoreRequiredCredits" to plan.targetCollegeCoreRequiredCredits,
                        "targetCollegeCoreElectiveCredits" to plan.targetCollegeCoreElectiveCredits,
                        "targetBasicModuleRequiredCredits" to plan.targetBasicModuleRequiredCredits,
                        "targetBasicModuleElectiveCredits" to plan.targetBasicModuleElectiveCredits,
                        "targetCoreModuleRequiredCredits" to plan.targetCoreModuleRequiredCredits,
                        "targetCoreModuleElectiveCredits" to plan.targetCoreModuleElectiveCredits,
                        "targetProfessionalModuleRequiredCredits" to plan.targetProfessionalModuleRequiredCredits,
                        "targetProfessionalModuleElectiveCredits" to plan.targetProfessionalModuleElectiveCredits,
                        "targetFreeElectiveCredits" to plan.targetFreeElectiveCredits,
                        "minPassingScore" to plan.minPassingScore,
                        "gpaScale" to plan.gpaScale.name,
                        "admissionSemester" to plan.admissionSemester,
                        "currentSemester" to plan.currentSemester,
                        "subcategoriesJson" to plan.subcategoriesJson,
                        "customCategoriesJson" to plan.customCategoriesJson,
                        "lastUpdated" to System.currentTimeMillis()
                    )
                    planDocRef.set(planMap, SetOptions.merge()).await()
                }

                // 2. 上傳 Courses（安全 UPSERT，不執行全集合盲刪）
                val courses = courseDao.getAllCoursesOnce()
                if (courses.isNotEmpty()) {
                    val coursesCol = userDocRef.collection("courses")
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
                            "subcategory" to course.subcategory,
                            "customCategory" to course.customCategory,
                            "semester" to course.semester,
                            "score" to course.score,
                            "letterGrade" to course.letterGrade,
                            "isCompleted" to course.isCompleted,
                            "colorHex" to course.colorHex,
                            "notes" to course.notes
                        )
                        coursesCol.document(course.id.toString()).set(courseMap, SetOptions.merge()).await()
                    }
                }

                // 3. 上傳 Graduation Thresholds
                val thresholds = graduationDao.getAllThresholdsOnce()
                if (thresholds.isNotEmpty()) {
                    val thresholdsCol = userDocRef.collection("thresholds")
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
                }

                // 4. 上傳 Expenses
                val expenses = expenseDao.getAllExpensesOnce()
                if (expenses.isNotEmpty()) {
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
                }

                // 5. 上傳 Budgets
                val budgets = expenseDao.getAllBudgetsOnce()
                if (budgets.isNotEmpty()) {
                    val budgetsCol = userDocRef.collection("budgets")
                    for (b in budgets) {
                        val bMap = hashMapOf(
                            "yearMonth" to b.yearMonth,
                            "budgetAmount" to b.budgetAmount
                        )
                        budgetsCol.document(b.yearMonth).set(bMap, SetOptions.merge()).await()
                    }
                }

                // 6. 上傳 Custom Accounts (包含排序與初始餘額)
                val accountsJson = prefs.getString("pref_custom_accounts", null)
                if (!accountsJson.isNullOrBlank()) {
                    userDocRef.collection("profile").document("custom_accounts")
                        .set(hashMapOf("accountsJson" to accountsJson, "lastUpdated" to System.currentTimeMillis()), SetOptions.merge()).await()
                }

                // 7. 上傳 Notifications (通知中心歷史記錄)
                val notifications = notificationDao.getAllNotificationsOnce()
                if (notifications.isNotEmpty()) {
                    val notifsCol = userDocRef.collection("notifications")
                    for (notif in notifications) {
                        val notifMap = hashMapOf(
                            "id" to notif.id,
                            "title" to notif.title,
                            "message" to notif.message,
                            "type" to notif.type.name,
                            "timestamp" to notif.timestamp,
                            "isRead" to notif.isRead,
                            "actionRoute" to (notif.actionRoute ?: "")
                        )
                        notifsCol.document(notif.id.toString()).set(notifMap, SetOptions.merge()).await()
                    }
                }

                Log.i(tag, "Upload all data to Firestore completed successfully for user: $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Upload all data to Firestore failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun downloadAllFromCloud(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("用戶 ID 不得為空"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore 尚未初始化"))

        syncMutex.withLock {
            try {
                val userDocRef = db.collection("users").document(userId)

                // 1. 下載 Graduation Plan
                val planSnapshot = userDocRef.collection("profile").document("graduation_plan").get().await()
                val defaultPlan = DefaultData.getDefaultGraduationPlan()
                if (planSnapshot.exists()) {
                    val remoteDepartment = planSnapshot.getString("department")?.trim()
                    val remoteStudentName = planSnapshot.getString("studentName")?.trim()

                    val resolvedDept = remoteDepartment ?: ""

                    val resolvedName = if (!remoteStudentName.isNullOrBlank() && remoteStudentName != "同學" && remoteStudentName != "王大明" && remoteStudentName != "大學生") {
                        remoteStudentName
                    } else {
                        remoteStudentName ?: defaultPlan.studentName
                    }

                    val plan = GraduationPlan(
                        id = 1,
                        department = resolvedDept,
                        studentName = resolvedName,
                        targetTotalCredits = planSnapshot.getDouble("targetTotalCredits") ?: defaultPlan.targetTotalCredits,
                        targetRequiredCredits = planSnapshot.getDouble("targetRequiredCredits") ?: defaultPlan.targetRequiredCredits,
                        targetElectiveCredits = planSnapshot.getDouble("targetElectiveCredits") ?: defaultPlan.targetElectiveCredits,
                        targetGeneralCredits = planSnapshot.getDouble("targetGeneralCredits") ?: defaultPlan.targetGeneralCredits,
                        targetCollegeCoreCredits = planSnapshot.getDouble("targetCollegeCoreCredits") ?: defaultPlan.targetCollegeCoreCredits,
                        targetBasicModuleCredits = planSnapshot.getDouble("targetBasicModuleCredits") ?: defaultPlan.targetBasicModuleCredits,
                        targetCoreModuleCredits = planSnapshot.getDouble("targetCoreModuleCredits") ?: defaultPlan.targetCoreModuleCredits,
                        targetProfessionalModuleCredits = planSnapshot.getDouble("targetProfessionalModuleCredits") ?: defaultPlan.targetProfessionalModuleCredits,
                        targetFreeCredits = planSnapshot.getDouble("targetFreeCredits") ?: defaultPlan.targetFreeCredits,
                        targetGeneralRequiredCredits = planSnapshot.getDouble("targetGeneralRequiredCredits") ?: defaultPlan.targetGeneralRequiredCredits,
                        targetGeneralElectiveCredits = planSnapshot.getDouble("targetGeneralElectiveCredits") ?: defaultPlan.targetGeneralElectiveCredits,
                        targetCollegeCoreRequiredCredits = planSnapshot.getDouble("targetCollegeCoreRequiredCredits") ?: defaultPlan.targetCollegeCoreRequiredCredits,
                        targetCollegeCoreElectiveCredits = planSnapshot.getDouble("targetCollegeCoreElectiveCredits") ?: defaultPlan.targetCollegeCoreElectiveCredits,
                        targetBasicModuleRequiredCredits = planSnapshot.getDouble("targetBasicModuleRequiredCredits") ?: defaultPlan.targetBasicModuleRequiredCredits,
                        targetBasicModuleElectiveCredits = planSnapshot.getDouble("targetBasicModuleElectiveCredits") ?: defaultPlan.targetBasicModuleElectiveCredits,
                        targetCoreModuleRequiredCredits = planSnapshot.getDouble("targetCoreModuleRequiredCredits") ?: defaultPlan.targetCoreModuleRequiredCredits,
                        targetCoreModuleElectiveCredits = planSnapshot.getDouble("targetCoreModuleElectiveCredits") ?: defaultPlan.targetCoreModuleElectiveCredits,
                        targetProfessionalModuleRequiredCredits = planSnapshot.getDouble("targetProfessionalModuleRequiredCredits") ?: defaultPlan.targetProfessionalModuleRequiredCredits,
                        targetProfessionalModuleElectiveCredits = planSnapshot.getDouble("targetProfessionalModuleElectiveCredits") ?: defaultPlan.targetProfessionalModuleElectiveCredits,
                        targetFreeElectiveCredits = planSnapshot.getDouble("targetFreeElectiveCredits") ?: defaultPlan.targetFreeElectiveCredits,
                        minPassingScore = planSnapshot.getDouble("minPassingScore") ?: defaultPlan.minPassingScore,
                        gpaScale = runCatching {
                            GpaScale.valueOf(planSnapshot.getString("gpaScale") ?: defaultPlan.gpaScale.name)
                        }.getOrDefault(defaultPlan.gpaScale),
                        admissionSemester = planSnapshot.getString("admissionSemester") ?: defaultPlan.admissionSemester,
                        currentSemester = planSnapshot.getString("currentSemester") ?: defaultPlan.currentSemester,
                        subcategoriesJson = planSnapshot.getString("subcategoriesJson") ?: "",
                        customCategoriesJson = planSnapshot.getString("customCategoriesJson") ?: ""
                    )
                    graduationDao.insertOrUpdatePlan(plan)
                } else {
                    graduationDao.insertOrUpdatePlan(defaultPlan)
                }

                // 2. 下載 Courses（先清空本機殘留課程，避免帳號切換或未同步刪除導致資料混合）
                val coursesSnapshot = userDocRef.collection("courses").get().await()
                val downloadedCourses = mutableListOf<Course>()
                if (!coursesSnapshot.isEmpty) {
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
                            subcategory = doc.getString("subcategory") ?: "",
                            customCategory = doc.getString("customCategory") ?: "",
                            semester = doc.getString("semester") ?: DefaultData.getCurrentAcademicSemester(),
                            score = doc.getDouble("score"),
                            letterGrade = doc.getString("letterGrade"),
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            colorHex = doc.getString("colorHex") ?: "#3B82F6",
                            notes = doc.getString("notes") ?: "",
                            repeatWeeks = doc.getString("repeatWeeks") ?: "1-18",
                            repeatMode = doc.getString("repeatMode") ?: "每週"
                        )
                        downloadedCourses.add(course)
                    }
                }
                courseDao.deleteAllCourses()
                if (downloadedCourses.isNotEmpty()) {
                    courseDao.insertCourses(downloadedCourses)
                }

                // 3. 下載 Thresholds（先清空本機門檻）
                val thresholdsSnapshot = userDocRef.collection("thresholds").get().await()
                val downloadedThresholds = mutableListOf<GraduationThreshold>()
                if (!thresholdsSnapshot.isEmpty) {
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
                }
                graduationDao.deleteAllThresholds()
                if (downloadedThresholds.isNotEmpty()) {
                    graduationDao.insertThresholds(downloadedThresholds)
                }

                // 4. 下載 Expenses（先清空本機記帳明細）
                val expensesSnapshot = userDocRef.collection("expenses").get().await()
                val downloadedExpenses = mutableListOf<ExpenseRecord>()
                if (!expensesSnapshot.isEmpty) {
                    for (doc in expensesSnapshot.documents) {
                        val id = doc.getLong("id") ?: continue
                        val title = doc.getString("title") ?: ""
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
                }
                expenseDao.deleteAllExpenses()
                if (downloadedExpenses.isNotEmpty()) {
                    expenseDao.insertExpenses(downloadedExpenses)
                }

                // 5. 下載 Budgets（先清空本機預算設定）
                val budgetsSnapshot = userDocRef.collection("budgets").get().await()
                expenseDao.deleteAllBudgets()
                if (!budgetsSnapshot.isEmpty) {
                    for (doc in budgetsSnapshot.documents) {
                        val ym = doc.getString("yearMonth") ?: doc.id
                        val amount = doc.getDouble("budgetAmount") ?: 12000.0
                        expenseDao.setBudget(MonthlyBudget(yearMonth = ym, budgetAmount = amount))
                    }
                }

                // 6. 下載 Custom Accounts (包含排序與初始餘額)
                val accountsSnapshot = userDocRef.collection("profile").document("custom_accounts").get().await()
                if (accountsSnapshot.exists()) {
                    val remoteAccountsJson = accountsSnapshot.getString("accountsJson")
                    if (!remoteAccountsJson.isNullOrBlank()) {
                        prefs.edit { putString("pref_custom_accounts", remoteAccountsJson) }
                    } else {
                        prefs.edit { remove("pref_custom_accounts") }
                    }
                } else {
                    prefs.edit { remove("pref_custom_accounts") }
                }

                // 7. 下載 自訂學期 與 隱藏學期設定
                val semestersSnapshot = userDocRef.collection("profile").document("semesters").get().await()
                if (semestersSnapshot.exists()) {
                    val customSemesters = (semestersSnapshot.get("customSemesters") as? List<*>)?.filterIsInstance<String>()
                    val deletedSemesters = (semestersSnapshot.get("deletedSemesters") as? List<*>)?.filterIsInstance<String>()
                    prefs.edit {
                        if (!customSemesters.isNullOrEmpty()) {
                            putStringSet("pref_custom_semesters", customSemesters.toSet())
                        } else {
                            remove("pref_custom_semesters")
                        }
                        if (!deletedSemesters.isNullOrEmpty()) {
                            putStringSet("pref_deleted_semesters", deletedSemesters.toSet())
                        } else {
                            remove("pref_deleted_semesters")
                        }
                    }
                }

                // 8. 下載 Notifications（通知中心記錄）
                val notifsSnapshot = userDocRef.collection("notifications").get().await()
                val downloadedNotifs = mutableListOf<AppNotification>()
                if (!notifsSnapshot.isEmpty) {
                    for (doc in notifsSnapshot.documents) {
                        val id = doc.getLong("id") ?: continue
                        val title = doc.getString("title") ?: continue
                        val notif = AppNotification(
                            id = id,
                            title = title,
                            message = doc.getString("message") ?: "",
                            type = runCatching {
                                NotificationType.valueOf(doc.getString("type") ?: NotificationType.SYSTEM.name)
                            }.getOrDefault(NotificationType.SYSTEM),
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isRead = doc.getBoolean("isRead") ?: false,
                            actionRoute = doc.getString("actionRoute")?.ifBlank { null }
                        )
                        downloadedNotifs.add(notif)
                    }
                }
                notificationDao.clearAll()
                if (downloadedNotifs.isNotEmpty()) {
                    notificationDao.insertNotifications(downloadedNotifs)
                }

                Log.i(tag, "Download all data from Firestore completed successfully for user: $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Download all data from Firestore failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun deleteCourseFromCloud(userId: String, courseId: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            firestore?.collection("users")?.document(userId)?.collection("courses")?.document(courseId.toString())?.delete()?.await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete course $courseId from cloud", e)
        }
    }

    suspend fun deleteExpenseFromCloud(userId: String, expenseId: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            firestore?.collection("users")?.document(userId)?.collection("expenses")?.document(expenseId.toString())?.delete()?.await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete expense $expenseId from cloud", e)
        }
    }

    suspend fun deleteAllExpensesFromCloud(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val col = firestore?.collection("users")?.document(userId)?.collection("expenses") ?: return@withContext
            val snap = col.get().await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete all expenses from cloud", e)
        }
    }

    suspend fun deleteThresholdFromCloud(userId: String, thresholdId: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            firestore?.collection("users")?.document(userId)?.collection("thresholds")?.document(thresholdId.toString())?.delete()?.await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete threshold $thresholdId from cloud", e)
        }
    }

    suspend fun deleteNotificationFromCloud(userId: String, notificationId: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            firestore?.collection("users")?.document(userId)?.collection("notifications")?.document(notificationId.toString())?.delete()?.await()
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete notification $notificationId from cloud", e)
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

            val subcollections = listOf("profile", "courses", "thresholds", "expenses", "budgets", "notifications")
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
