package com.example.widget

import android.graphics.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.data.model.Course

object WeeklyOverviewBitmapRenderer {

    /**
     * 繪製 5/7 天每日課表卡片總覽 Bitmap（供 WeeklyOverviewWidget 使用）
     */
    fun renderOverviewCards(
        courses: List<Course>,
        currentDayOfWeek: Int, // 1=Mon..7=Sun
        currentWeek: Int,
        showWeekend: Boolean = false,
        width: Int = 1000,
        height: Int = 600
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // 淺色底
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate 50
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val daysCount = if (showWeekend) 7 else 5
        val dayHeaders = if (showWeekend) {
            listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")
        } else {
            listOf("週一", "週二", "週三", "週四", "週五")
        }

        val weekCourses = courses.filter { TodayScheduleWidget.isCourseInWeek(it, currentWeek) }

        val cardSpacing = 10f
        val padding = 12f
        val totalSpacing = cardSpacing * (daysCount - 1) + padding * 2
        val colWidth = (width - totalSpacing) / daysCount
        val colHeight = height - padding * 2

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val todayCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 247, 255) // Blue 50
            style = Paint.Style.FILL
        }

        val todayCardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Sapphire Primary
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }

        val dayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val todayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Sapphire Primary
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val courseNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val courseTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 18f
        }

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (i in 0 until daysCount) {
            val dayIndex = i + 1
            val isToday = dayIndex == currentDayOfWeek
            val left = padding + i * (colWidth + cardSpacing)
            val right = left + colWidth
            val bottom = padding + colHeight

            val rect = RectF(left, padding, right, bottom)
            val fillPaint = if (isToday) todayCardBgPaint else cardBgPaint
            val strokePaint = if (isToday) todayCardBorderPaint else cardBorderPaint

            canvas.drawRoundRect(rect, 14f, 14f, fillPaint)
            canvas.drawRoundRect(rect, 14f, 14f, strokePaint)

            // Header (週幾)
            val headerCx = left + colWidth / 2
            val hPaint = if (isToday) todayHeaderPaint else dayHeaderPaint
            val hText = if (isToday) "${dayHeaders[i]} (今)" else dayHeaders[i]
            canvas.drawText(hText, headerCx, padding + 36f, hPaint)

            // 分隔線
            canvas.drawLine(left + 8f, padding + 48f, right - 8f, padding + 48f, cardBorderPaint)

            // 條列該天課程
            val dayCourseList = weekCourses
                .filter { it.dayOfWeek == dayIndex }
                .sortedBy { it.startPeriod }

            var currentY = padding + 74f

            if (dayCourseList.isEmpty()) {
                val noClassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(148, 163, 184)
                    textSize = 20f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("無課程", headerCx, padding + colHeight / 2, noClassPaint)
            } else {
                for (c in dayCourseList.take(3)) {
                    val colorInt = try {
                        c.colorHex.toColorInt()
                    } catch (_: Exception) {
                        Color.rgb(37, 99, 235)
                    }
                    pillPaint.color = colorInt

                    // 彩色圓點 / 圓角藥丸
                    canvas.drawRoundRect(
                        left + 10f,
                        currentY - 14f,
                        left + 18f,
                        currentY + 16f,
                        4f,
                        4f,
                        pillPaint
                    )

                    // 節次或時間
                    val timeStr = c.startTime.ifBlank { "第${c.startPeriod}節" }
                    canvas.drawText(timeStr, left + 26f, currentY - 2f, courseTimePaint)

                    // 課名 (截斷)
                    val maxNameLen = if (colWidth > 180) 8 else 6
                    val truncatedName = if (c.name.length > maxNameLen) "${c.name.take(maxNameLen - 1)}…" else c.name
                    canvas.drawText(truncatedName, left + 26f, currentY + 18f, courseNamePaint)

                    currentY += 46f
                }

                if (dayCourseList.size > 3) {
                    val morePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(37, 99, 235)
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("+ 還有 ${dayCourseList.size - 3} 堂", left + 14f, currentY + 6f, morePaint)
                }
            }
        }

        return bitmap
    }
}
