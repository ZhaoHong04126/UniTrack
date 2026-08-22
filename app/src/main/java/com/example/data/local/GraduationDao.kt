package com.example.data.local

import androidx.room.*
import com.example.data.model.GraduationPlan
import com.example.data.model.GraduationThreshold
import kotlinx.coroutines.flow.Flow

@Dao
interface GraduationDao {
    @Query("SELECT * FROM graduation_plans WHERE id = 1")
    fun getGraduationPlan(): Flow<GraduationPlan?>

    @Query("SELECT * FROM graduation_plans WHERE id = 1")
    suspend fun getGraduationPlanOnce(): GraduationPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlan(plan: GraduationPlan)

    @Query("SELECT * FROM graduation_thresholds ORDER BY id ASC")
    fun getAllThresholds(): Flow<List<GraduationThreshold>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreshold(threshold: GraduationThreshold): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThresholds(thresholds: List<GraduationThreshold>)

    @Update
    suspend fun updateThreshold(threshold: GraduationThreshold)

    @Delete
    suspend fun deleteThreshold(threshold: GraduationThreshold)

    @Query("DELETE FROM graduation_thresholds WHERE id = :id")
    suspend fun deleteThresholdById(id: Long)

    @Query("DELETE FROM graduation_thresholds")
    suspend fun deleteAllThresholds()
}
