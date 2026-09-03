package com.example.data.local

import androidx.room.*
import com.example.data.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

@Suppress("unused")
@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY date ASC, isAllDay DESC, startTime ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events ORDER BY date ASC, isAllDay DESC, startTime ASC")
    suspend fun getAllEventsOnce(): List<CalendarEvent>

    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY isAllDay DESC, startTime ASC")
    fun getEventsByDate(date: String): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE date LIKE :yearMonth || '%' ORDER BY date ASC, isAllDay DESC, startTime ASC")
    fun getEventsByMonth(yearMonth: String): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEvent>)

    @Update
    suspend fun updateEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
}
