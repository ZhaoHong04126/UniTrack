package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale
import com.example.BuildConfig
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.data.model.CourseRequirementType
import com.example.data.model.GeneralEduSubtype
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Suppress("SpellCheckingInspection")
object GeminiTimetableParser {
    private const val TAG = "GeminiTimetableParser"
    private const val MAX_IMAGE_DIMENSION = 1600

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val PRESET_COLORS = listOf(
        "#3B82F6", // Blue
        "#10B981", // Emerald
        "#8B5CF6", // Purple
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#06B6D4", // Cyan
        "#6366F1", // Indigo
        "#14B8A6", // Teal
        "#F97316", // Orange
        "#84CC16"  // Lime
    )

    /**
     * Decode and downscale Uri to Bitmap
     */
    fun loadAndResizeBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val width = originalBitmap.width
            val height = originalBitmap.height

            if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
                return originalBitmap
            }

            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int

            if (width > height) {
                newWidth = MAX_IMAGE_DIMENSION
                newHeight = (MAX_IMAGE_DIMENSION / ratio).toInt()
            } else {
                newHeight = MAX_IMAGE_DIMENSION
                newWidth = (MAX_IMAGE_DIMENSION * ratio).toInt()
            }

            originalBitmap.scale(newWidth, newHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load/resize bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Parse timetable from Bitmap using Gemini Vision REST API
     */
    suspend fun parseTimetableImage(
        bitmap: Bitmap,
        targetSemester: String
    ): Result<List<Course>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY 未設定，請於 .env 檔案中填入 GEMINI_API_KEY。")
            )
        }

        try {
            // 1. Compress bitmap to Base64 JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            // 2. Build Gemini prompt
            val prompt = """
                請分析這張課表照片/圖片，仔細辨識其中的所有課程與上課時間資訊，並以 JSON 陣列格式輸出。
                
                每門課程物件須包含以下欄位：
                - "name": 課程名稱 (字串, 必填)
                - "code": 課程代碼 (字串, 若無則為 "")
                - "teacher": 授課教師姓名 (字串, 若無則為 "")
                - "location": 教室或上課地點 (字串, 若無則為 "")
                - "dayOfWeek": 星期幾 (整數: 1=星期一, 2=星期二, 3=星期三, 4=星期四, 5=星期五, 6=星期六, 7=星期日)
                - "startPeriod": 開始節次 (整數 0..14。例如：第1節/08:10填1，若為第0節/早八前填0，中午或第5節填5等)
                - "endPeriod": 結束節次 (整數 0..14。若為單節課填相同節次，若為2節如3~4節則填4)
                - "startTime": 上課開始時間 (字串格式 "HH:mm" 如 "08:10", "10:10" 等，可根據台灣大學節次推算)
                - "endTime": 上課結束時間 (字串格式 "HH:mm" 如 "10:00", "12:00" 等)
                - "credits": 學分數 (浮點數，若未特別標明則根據節數或預設 2.0 或 3.0)
                - "category": 課程分類 (字串，可為 "REQUIRED", "ELECTIVE", "GENERAL_EDU", "COLLEGE_CORE", "BASIC_MODULE", "CORE_MODULE", "PROFESSIONAL_MODULE", "FREE_ELECTIVE", "PE")
                - "requirementType": 必修或選修 (字串: "REQUIRED" 或 "ELECTIVE")
                - "subcategory": 通識或模組子分類 (字串, 若無則為 "")
                
                特別注意：
                1. 若同一門課在同一天連續上課（例如 3, 4 節），請合併為一筆資料，startPeriod=3, endPeriod=4。
                2. 若同一門課在不同天（例如星期一與星期三），請拆分為多筆不同 dayOfWeek 的課程。
                3. 請只輸出合法的 JSON Array，不要包含任何額外的解釋文字或 Markdown 外框。
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply {
                        put("text", prompt)
                    })
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            // 3. Request Gemini API (Try gemini-3.6-flash, fallback to gemini-3.5-flash)
            val models = listOf("gemini-3.6-flash", "gemini-3.5-flash", "gemini-flash-latest")
            var responseText: String? = null
            var lastError: Exception? = null

            val trimmedKey = apiKey.trim()

            for (model in models) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$trimmedKey"
                    val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("x-goog-api-key", trimmedKey)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val code = response.code
                    val bodyString = response.body.string()

                    if (response.isSuccessful && bodyString.isNotBlank()) {
                        val responseJson = JSONObject(bodyString)
                        val candidates = responseJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                responseText = parts.getJSONObject(0).optString("text")
                                if (!responseText.isNullOrBlank()) {
                                    break // Success!
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Model $model returned error code $code: $bodyString")
                        val friendlyMsg = when (code) {
                            404 ->
                                "模型路徑錯誤 (HTTP 404): $bodyString"
                            403 ->
                                if (bodyString.contains("API_KEY_SERVICE_BLOCKED") || bodyString.contains("blocked")) {
                                    "API 金鑰權限受阻 (HTTP 403)：您的專案尚未在 Google Cloud / AI Studio 啟用「Generative Language API」，或該 API Key 有服務呼叫限制。\n\n請前往 aistudio.google.com 建立專屬 Gemini API Key 並更新 .env 檔案。"
                                } else {
                                    "API 權限不足 (HTTP 403): $bodyString"
                                }
                            400 ->
                                if (bodyString.contains("API_KEY_INVALID")) {
                                    "API 金鑰無效 (HTTP 400)：請確認 .env 中的 GEMINI_API_KEY 是否正確。"
                                } else {
                                    "API 請求無效 (HTTP 400): $bodyString"
                                }
                            429 ->
                                "API 呼叫頻率達上限 (HTTP 429)：請稍候片刻後再試。"
                            else ->
                                "API 請求失敗 (HTTP $code): $bodyString"
                        }
                        lastError = RuntimeException(friendlyMsg)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Request to $model failed: ${e.message}")
                    lastError = e
                }
            }

            if (responseText.isNullOrBlank()) {
                return@withContext Result.failure(lastError ?: RuntimeException("未能從 Gemini 獲取辨識結果，請檢查網路連線或 API Key。"))
            }

            // 4. Parse extracted JSON array to List<Course>
            val courses = parseCoursesFromJson(responseText, targetSemester)
            if (courses.isEmpty()) {
                return@withContext Result.failure(RuntimeException("未在圖片中辨識出任何課程，請嘗試提供更清晰的課表照片。"))
            }

            Result.success(courses)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini timetable recognition failed", e)
            Result.failure(e)
        }
    }

    /**
     * Clean and parse Gemini response into List<Course>
     */
    private fun parseCoursesFromJson(rawJson: String, targetSemester: String): List<Course> {
        var cleanJson = rawJson.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json").trim()
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```").trim()
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```").trim()
        }

        val courseList = mutableListOf<Course>()
        try {
            val jsonArray = if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                obj.optJSONArray("courses") ?: obj.optJSONArray("timetable") ?: JSONArray()
            } else {
                JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val name = item.optString("name", "").trim()
                if (name.isBlank()) continue

                val code = item.optString("code", "").trim()
                val teacher = item.optString("teacher", "").trim()
                val location = item.optString("location", "").trim()
                val dayOfWeek = item.optInt("dayOfWeek", 1).coerceIn(1, 7)
                val startPeriod = item.optInt("startPeriod", 1).coerceIn(0, 14)
                var endPeriod = item.optInt("endPeriod", startPeriod).coerceIn(0, 14)
                if (endPeriod < startPeriod) endPeriod = startPeriod

                val startTime = item.optString("startTime", "").ifBlank {
                    getDefaultStartTimeForPeriod(startPeriod)
                }
                val endTime = item.optString("endTime", "").ifBlank {
                    getDefaultEndTimeForPeriod(endPeriod)
                }

                val calculatedCredits = (endPeriod - startPeriod + 1).toDouble().coerceAtLeast(1.0)
                val credits = item.optDouble("credits", calculatedCredits).let {
                    if (it <= 0.0) calculatedCredits else it
                }

                val categoryStr = item.optString("category", "REQUIRED").uppercase()
                val category = runCatching { CourseCategory.valueOf(categoryStr) }.getOrDefault(CourseCategory.REQUIRED)

                val reqTypeStr = item.optString("requirementType", "REQUIRED").uppercase()
                val reqType = runCatching { CourseRequirementType.valueOf(reqTypeStr) }.getOrDefault(CourseRequirementType.REQUIRED)

                val subcategory = item.optString("subcategory", "")
                val colorHex = PRESET_COLORS[i % PRESET_COLORS.size]

                courseList.add(
                    Course(
                        name = name,
                        code = code,
                        teacher = teacher,
                        location = location,
                        dayOfWeek = dayOfWeek,
                        startPeriod = startPeriod,
                        endPeriod = endPeriod,
                        startTime = startTime,
                        endTime = endTime,
                        credits = credits,
                        category = category,
                        requirementType = reqType,
                        generalEduSubtype = GeneralEduSubtype.NONE,
                        subcategory = subcategory,
                        semester = targetSemester,
                        colorHex = colorHex,
                        repeatWeeks = "1-18",
                        repeatMode = "每週"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing course json: ${e.message}", e)
        }

        return courseList
    }

    private fun getDefaultStartTimeForPeriod(period: Int): String = when (period) {
        0 -> "07:10"
        1 -> "08:10"
        2 -> "09:10"
        3 -> "10:10"
        4 -> "11:10"
        5 -> "13:10"
        6 -> "14:10"
        7 -> "15:10"
        8 -> "16:10"
        9 -> "17:10"
        10 -> "18:20"
        11 -> "19:15"
        12 -> "20:10"
        13 -> "21:05"
        14 -> "22:00"
        else -> String.format(java.util.Locale.US, "%02d:10", (7 + period))
    }

    private fun getDefaultEndTimeForPeriod(period: Int): String = when (period) {
        0 -> "08:00"
        1 -> "09:00"
        2 -> "10:00"
        3 -> "11:00"
        4 -> "12:00"
        5 -> "14:00"
        6 -> "15:00"
        7 -> "16:00"
        8 -> "17:00"
        9 -> "18:00"
        10 -> "19:10"
        11 -> "20:05"
        12 -> "21:00"
        13 -> "21:55"
        14 -> "22:50"
        else -> String.format(java.util.Locale.US, "%02d:00", (8 + period))
    }
}
