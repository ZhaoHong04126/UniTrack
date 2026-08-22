package com.example.data.local

import androidx.room.*
import com.example.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY semester DESC, dayOfWeek ASC, startPeriod ASC")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semester = :semester ORDER BY dayOfWeek ASC, startPeriod ASC")
    fun getCoursesBySemester(semester: String): Flow<List<Course>>

    @Query("SELECT DISTINCT semester FROM courses ORDER BY semester DESC")
    fun getAllSemesters(): Flow<List<String>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}
