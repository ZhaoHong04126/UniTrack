package com.example.widget

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.example.data.model.Course

object WeeklyGridBitmapRenderer {

    /**
     * 繪製完整一週網格課表 Bitmap（供 RemoteViews 桌面小工具使用）
     */
    fun renderWeeklyGrid(
        courses: List<Course>,
        currentDayOfWeek: Int, // 1=Mon..7=Sun
        currentWeek: Int,
        showWeekend: Boolean = false,
        width: Int = 1000,
        height: Int = 1200
    ): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // 背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate 50
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val daysCount = if (showWeekend) 7 else 5
        val dayHeaders = if (showWeekend) {
            listOf("一", "二", "三", "四", "五", "六", "日")
        } else {
            listOf("一", "二", "三", "四", "五")
        }

        // 過濾當前週次課程
        val weekCourses = courses.filter { TodayScheduleWidget.isCourseInWeek(it, currentWeek) }
        val maxPeriod = maxOf(8, weekCourses.maxOfOrNull { it.endPeriod } ?: 8)
        val totalPeriods = maxPeriod.coerceIn(8, 14)

        val leftMargin = 70f
        val topMargin = 60f
        val rightMargin = 16f
        val bottomMargin = 16f

        val gridWidth = width - leftMargin - rightMargin
        val gridHeight = height - topMargin - bottomMargin

        val colWidth = gridWidth / daysCount
        val rowHeight = gridHeight / totalPeriods

        val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        val headerDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val todayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Sapphire Primary
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val todayColBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(30, 37, 99, 235) // 淺藍高亮底色
            style = Paint.Style.FILL
        }

        val periodTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // 1. 繪製今日欄位高亮底色
        for (i in 0 until daysCount) {
            val dayIndex = i + 1
            if (dayIndex == currentDayOfWeek) {
                val colLeft = leftMargin + i * colWidth
                canvas.drawRoundRect(
                    colLeft + 2f,
                    topMargin,
                    colLeft + colWidth - 2f,
                    topMargin + gridHeight,
                    8f,
                    8f,
                    todayColBgPaint
                )
            }
        }

        // 2. 繪製頂部星期標題
        for (i in 0 until daysCount) {
            val dayIndex = i + 1
            val isToday = dayIndex == currentDayOfWeek
            val cx = leftMargin + i * colWidth + colWidth / 2
            val paint = if (isToday) todayHeaderPaint else headerDayPaint
            val label = if (isToday) "${dayHeaders[i]} (今)" else dayHeaders[i]
            canvas.drawText(label, cx, topMargin - 18f, paint)
        }

        // 3. 繪製節次左側標籤與水平分隔線
        for (p in 1..totalPeriods) {
            val yTop = topMargin + (p - 1) * rowHeight
            val yBottom = yTop + rowHeight
            val cy = yTop + rowHeight / 2 + 8f

            canvas.drawText(p.toString(), leftMargin / 2, cy, periodTextPaint)
            canvas.drawLine(leftMargin, yTop, leftMargin + gridWidth, yTop, gridLinePaint)

            if (p == totalPeriods) {
                canvas.drawLine(leftMargin, yBottom, leftMargin + gridWidth, yBottom, gridLinePaint)
            }
        }

        // 4. 繪製垂直網格分隔線
        for (i in 0..daysCount) {
            val x = leftMargin + i * colWidth
            canvas.drawLine(x, topMargin, x, topMargin + gridHeight, gridLinePaint)
        }

        // 5. 繪製課程卡片便籤
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val locPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        for (course in weekCourses) {
            val dayIdx = course.dayOfWeek - 1
            if (dayIdx in 0 until daysCount) {
                val startP = course.startPeriod.coerceIn(1, totalPeriods)
                val endP = course.endPeriod.coerceIn(startP, totalPeriods)
                val span = endP - startP + 1

                val cardLeft = leftMargin + dayIdx * colWidth + 4f
                val cardTop = topMargin + (startP - 1) * rowHeight + 4f
                val cardRight = cardLeft + colWidth - 8f
                val cardBottom = cardTop + span * rowHeight - 8f

                val colorInt = try {
                    course.colorHex.toColorInt()
                } catch (_: Exception) {
                    Color.rgb(37, 99, 235)
                }

                cardPaint.color = colorInt
                val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
                canvas.drawRoundRect(cardRect, 10f, 10f, cardPaint)

                // 繪製課程名稱與地點文字
                val cardInnerWidth = (cardRight - cardLeft - 10f).toInt().coerceAtLeast(10)
                val courseName = course.name
                val location = course.location

                canvas.withTranslation(cardLeft + 5f, cardTop + 6f) {
                    val nameLayout = StaticLayout.Builder.obtain(
                        courseName, 0, courseName.length, textPaint, cardInnerWidth
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setMaxLines(if (span > 1) 3 else 2)
                        .setEllipsize(android.text.TextUtils.TruncateAt.END)
                        .build()
                    nameLayout.draw(canvas)

                    if (location.isNotBlank() && span >= 2) {
                        val nameHeight = nameLayout.height
                        val locLayout = StaticLayout.Builder.obtain(
                            "📍 $location", 0, "📍 $location".length, locPaint, cardInnerWidth
                        ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setMaxLines(1)
                            .setEllipsize(android.text.TextUtils.TruncateAt.END)
                            .build()
                        canvas.withTranslation(0f, nameHeight + 4f) {
                            locLayout.draw(canvas)
                        }
                    }
                }
            }
        }

        return bitmap
    }
}
