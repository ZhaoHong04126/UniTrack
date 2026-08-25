package com.example.widget

import com.example.data.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayScheduleWidgetTest {

    @Test
    fun testDayOfWeekName() {
        assertEquals("週一", TodayScheduleWidget.getDayOfWeekName(1))
        assertEquals("週二", TodayScheduleWidget.getDayOfWeekName(2))
        assertEquals("週三", TodayScheduleWidget.getDayOfWeekName(3))
        assertEquals("週四", TodayScheduleWidget.getDayOfWeekName(4))
        assertEquals("週五", TodayScheduleWidget.getDayOfWeekName(5))
        assertEquals("週六", TodayScheduleWidget.getDayOfWeekName(6))
        assertEquals("週日", TodayScheduleWidget.getDayOfWeekName(7))
    }

    @Test
    fun testIsCourseInWeek_modes() {
        val courseEveryWeek = Course(name = "每週課程", repeatMode = "每週")
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseEveryWeek, 1))
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseEveryWeek, 2))

        val courseOddWeek = Course(name = "單週課程", repeatMode = "單週")
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseOddWeek, 1))
        assertFalse(TodayScheduleWidget.isCourseInWeek(courseOddWeek, 2))
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseOddWeek, 3))

        val courseEvenWeek = Course(name = "雙週課程", repeatMode = "雙週")
        assertFalse(TodayScheduleWidget.isCourseInWeek(courseEvenWeek, 1))
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseEvenWeek, 2))
        assertFalse(TodayScheduleWidget.isCourseInWeek(courseEvenWeek, 3))

        val courseCustomWeeks = Course(name = "指定週課程", repeatMode = "自訂", repeatWeeks = "1,3,5,7")
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseCustomWeeks, 1))
        assertFalse(TodayScheduleWidget.isCourseInWeek(courseCustomWeeks, 2))
        assertTrue(TodayScheduleWidget.isCourseInWeek(courseCustomWeeks, 5))
        assertFalse(TodayScheduleWidget.isCourseInWeek(courseCustomWeeks, 6))
    }

    @Test
    fun testGetPeriodTimeRange() {
        val period1to2 = TodayScheduleWidget.getPeriodTimeRange(1, 2)
        assertEquals("08:10 - 10:00", period1to2)

        val period3to4 = TodayScheduleWidget.getPeriodTimeRange(3, 4)
        assertEquals("10:10 - 12:00", period3to4)
    }

    @Test
    fun testFindNextOrOngoingClass() {
        val morningClass = Course(
            name = "計算機組織",
            startPeriod = 2,
            endPeriod = 3,
            startTime = "09:10",
            endTime = "11:00"
        )
        val afternoonClass = Course(
            name = "演算法",
            startPeriod = 5,
            endPeriod = 7,
            startTime = "13:10",
            endTime = "16:00"
        )
        val courses = listOf(morningClass, afternoonClass)

        // 1. 早上 08:30 (before morning class) -> next class is morningClass
        val status830 = TodayScheduleWidget.findNextOrOngoingClass(courses, 8 * 60 + 30)
        assertNotNull(status830)
        assertFalse(status830!!.isOngoing)
        assertEquals("計算機組織", status830.course.name)

        // 2. 早上 10:00 (during morning class) -> ongoing is morningClass
        val status1000 = TodayScheduleWidget.findNextOrOngoingClass(courses, 10 * 60)
        assertNotNull(status1000)
        assertTrue(status1000!!.isOngoing)
        assertEquals("計算機組織", status1000.course.name)

        // 3. 中午 12:00 (between morning and afternoon) -> next is afternoonClass
        val status1200 = TodayScheduleWidget.findNextOrOngoingClass(courses, 12 * 60)
        assertNotNull(status1200)
        assertFalse(status1200!!.isOngoing)
        assertEquals("演算法", status1200.course.name)

        // 4. 下午 17:00 (after all classes) -> null (finished)
        val status1700 = TodayScheduleWidget.findNextOrOngoingClass(courses, 17 * 60)
        assertNull(status1700)
    }

    @Test
    fun testWeeklyGridBitmapRenderer() {
        val course1 = Course(name = "資料結構", dayOfWeek = 1, startPeriod = 2, endPeriod = 4, colorHex = "#2563EB")
        val course2 = Course(name = "離散數學", dayOfWeek = 3, startPeriod = 5, endPeriod = 7, colorHex = "#0D9488")
        val courses = listOf(course1, course2)

        val bitmap = WeeklyGridBitmapRenderer.renderWeeklyGrid(
            courses = courses,
            currentDayOfWeek = 1,
            currentWeek = 1,
            showWeekend = false,
            width = 500,
            height = 600
        )
        assertNotNull(bitmap)
        assertEquals(500, bitmap.width)
        assertEquals(600, bitmap.height)
    }

    @Test
    fun testWeeklyOverviewBitmapRenderer() {
        val course1 = Course(name = "資料結構", dayOfWeek = 1, startPeriod = 2, endPeriod = 4, colorHex = "#2563EB")
        val courses = listOf(course1)

        val bitmap = WeeklyOverviewBitmapRenderer.renderOverviewCards(
            courses = courses,
            currentDayOfWeek = 1,
            currentWeek = 1,
            showWeekend = false,
            width = 500,
            height = 300
        )
        assertNotNull(bitmap)
        assertEquals(500, bitmap.width)
        assertEquals(300, bitmap.height)
    }
}
