package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("UniTrack+", appName)
  }

  @Test
  fun testCourseRepeatModeSerialization() {
    val course = com.example.data.model.Course(
      name = "多變量微積分課輔",
      repeatMode = "第 2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18 週",
      repeatWeeks = "2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18",
      customCategory = "數理通識"
    )
    val json = org.json.JSONObject().apply {
      put("name", course.name)
      put("repeatWeeks", course.repeatWeeks)
      put("repeatMode", course.repeatMode)
      put("customCategory", course.customCategory)
    }

    val restored = com.example.data.model.Course(
      name = json.getString("name"),
      repeatWeeks = json.optString("repeatWeeks", "1-18"),
      repeatMode = json.optString("repeatMode", "每週"),
      customCategory = json.optString("customCategory", "")
    )

    assertEquals("多變量微積分課輔", restored.name)
    assertEquals("2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18", restored.repeatWeeks)
    assertEquals("第 2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18 週", restored.repeatMode)
    assertEquals("數理通識", restored.customCategory)
  }
}
