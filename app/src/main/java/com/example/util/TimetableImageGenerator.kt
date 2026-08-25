package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.example.data.model.Course
import java.io.File
import java.io.FileOutputStream

@Suppress("SpellCheckingInspection")
object TimetableImageGenerator {

    fun shareTimetableImage(
        context: Context,
        semesterLabel: String,
        courses: List<Course>,
        showWeekend: Boolean
    ) {
        val bitmap = generateTimetableBitmap(semesterLabel, courses, showWeekend)
        val imageUri = saveBitmapToCache(context, bitmap) ?: return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            clipData = ClipData.newRawUri("Timetable", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, null).apply {
            clipData = ClipData.newRawUri("Timetable", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun generateTimetableBitmap(
        semesterLabel: String,
        courses: List<Course>,
        showWeekend: Boolean
    ): Bitmap {
        val width = 1200
        val height = 1800
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.WHITE)

        val totalCredits = courses.sumOf { it.credits }
        val daysCount = if (showWeekend) 7 else 5
        val dayHeaders = if (showWeekend) {
            listOf("一", "二", "三", "四", "五", "六", "日")
        } else {
            listOf("一", "二", "三", "四", "五")
        }

        val maxPeriod = maxOf(8, courses.maxOfOrNull { it.endPeriod } ?: 8)
        val minPeriod = 8
        val totalPeriods = (maxPeriod - minPeriod + 1).coerceAtLeast(8)

        // Paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Sapphire Primary #2563EB
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 32f
        }

        val headerDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val periodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            strokeWidth = 2f
        }

        val headerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 3f
        }

        // Draw Header
        val paddingLeft = 60f
        val paddingRight = width - 60f
        val headerTop = 90f

        canvas.drawText("📅 課表  $semesterLabel", paddingLeft, headerTop, titlePaint)
        canvas.drawText("unitrack+", paddingRight, headerTop, brandPaint)
        canvas.drawText("總學分：$totalCredits 學分 · 共 ${courses.size} 門課程", paddingLeft, headerTop + 50f, subtitlePaint)

        // Grid Coordinates
        val gridTop = headerTop + 130f
        val gridBottom = height - 100f
        val periodColWidth = 80f
        val tableLeft = paddingLeft + periodColWidth
        val tableWidth = paddingRight - tableLeft
        val colWidth = tableWidth / daysCount

        val headerRowHeight = 60f
        val contentTop = gridTop + headerRowHeight
        val contentHeight = gridBottom - contentTop
        val rowHeight = contentHeight / totalPeriods

        // Draw Day Headers
        for (i in 0 until daysCount) {
            val centerX = tableLeft + i * colWidth + colWidth / 2
            canvas.drawText(dayHeaders[i], centerX, gridTop + 40f, headerDayPaint)
        }

        // Horizontal line under day headers
        canvas.drawLine(paddingLeft, contentTop, paddingRight, contentTop, headerLinePaint)

        // Draw Period Rows and Horizontal Grid lines
        for (p in 0 until totalPeriods) {
            val currentPeriod = minPeriod + p
            val y = contentTop + p * rowHeight

            // Period number on left
            canvas.drawText("$currentPeriod", paddingLeft + periodColWidth / 2, y + rowHeight / 2 + 10f, periodPaint)

            // Grid Line
            if (p > 0) {
                canvas.drawLine(tableLeft, y, paddingRight, y, gridLinePaint)
            }
        }

        // Draw Vertical Grid lines
        for (i in 1 until daysCount) {
            val x = tableLeft + i * colWidth
            canvas.drawLine(x, contentTop, x, gridBottom, gridLinePaint)
        }

        // Draw Courses
        courses.forEach { course ->
            val dayIndex = course.dayOfWeek - 1
            if (dayIndex in 0 until daysCount) {
                val startP = (course.startPeriod - minPeriod).coerceAtLeast(0)
                val endP = (course.endPeriod - minPeriod).coerceAtMost(totalPeriods - 1)

                if (startP <= endP && startP < totalPeriods) {
                    val cardLeft = tableLeft + dayIndex * colWidth + 4f
                    val cardRight = tableLeft + (dayIndex + 1) * colWidth - 4f
                    val cardTop = contentTop + startP * rowHeight + 4f
                    val cardBottom = contentTop + (endP + 1) * rowHeight - 4f

                    val cardColor = runCatching {
                        course.colorHex.toColorInt()
                    }.getOrDefault(Color.rgb(99, 102, 241))

                    // Draw soft rounded card background
                    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = cardColor
                    }
                    val rect = RectF(cardLeft, cardTop, cardRight, cardBottom)
                    canvas.drawRoundRect(rect, 16f, 16f, cardPaint)

                    // Draw Course Text inside card
                    val cardWidth = (cardRight - cardLeft).toInt() - 16
                    if (cardWidth > 20) {
                        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            textSize = 28f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }

                        val courseText = buildString {
                            if (course.location.isNotBlank()) appendLine(course.location)
                            appendLine(course.name)
                            if (course.teacher.isNotBlank()) append(course.teacher)
                        }.trim()

                        val staticLayout = StaticLayout.Builder.obtain(
                            courseText,
                            0,
                            courseText.length,
                            textPaint,
                            cardWidth
                        )
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setIncludePad(false)
                            .setMaxLines(6)
                            .build()

                        canvas.withTranslation(cardLeft + 10f, cardTop + 12f) {
                            staticLayout.draw(this)
                        }
                    }
                }
            }
        }

        // Footer Brand
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("UniTrack+ 智慧學業助理", width / 2f, height - 40f, footerPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): android.net.Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cacheDir, "timetable_share.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (_: Exception) {
            null
        }
    }
}
