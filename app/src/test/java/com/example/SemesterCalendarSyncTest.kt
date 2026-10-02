package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DefaultData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SemesterCalendarSyncTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_semester_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testFormatSemesterHeaderLabel() {
        val label = DefaultData.formatSemesterHeaderLabel("114-1", "113-1")
        assertEquals("大二上", label)

        val label2 = DefaultData.formatSemesterHeaderLabel("114-2", "113-1")
        assertEquals("大二下", label2)

        val labelFreshman = DefaultData.formatSemesterHeaderLabel("113-1", "113-1")
        assertEquals("大一上", labelFreshman)
    }

    @Test
    fun testGetSemesterSyncToCalendar_defaultsToTrue() {
        val syncDefault = DefaultData.getSemesterSyncToCalendar(prefs, "114-1")
        assertTrue("Default sync to calendar should be true", syncDefault)

        prefs.edit().putBoolean("semester_sync_calendar_114-1", false).commit()
        val syncDisabled = DefaultData.getSemesterSyncToCalendar(prefs, "114-1")
        assertFalse("Sync to calendar should be false after updating", syncDisabled)
    }

    @Test
    fun testGetSemesterEndDate_calculatedAndSaved() {
        prefs.edit()
            .putString("semester_start_date_114-1", "2026.09.07")
            .putInt("semester_total_weeks_114-1", 18)
            .commit()

        val calculatedEnd = DefaultData.getSemesterEndDate(prefs, "114-1")
        // 2026.09.07 + 18 weeks (126 days) - 1 day = 2027.01.10
        assertEquals("2027.01.10", calculatedEnd)

        // If explicitly saved
        prefs.edit().putString("semester_end_date_114-1", "2027.01.15").commit()
        val savedEnd = DefaultData.getSemesterEndDate(prefs, "114-1")
        assertEquals("2027.01.15", savedEnd)
    }
}
