package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AcademicCalendarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicCalendarDao {
    @Query("SELECT * FROM academic_calendar WHERE id = 'active_calendar' LIMIT 1")
    fun getCalendar(): Flow<AcademicCalendarEntity?>

    @Query("SELECT * FROM academic_calendar WHERE id = 'active_calendar' LIMIT 1")
    suspend fun getCalendarDirect(): AcademicCalendarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(calendar: AcademicCalendarEntity)

    @Query("DELETE FROM academic_calendar")
    suspend fun deleteAll()
}
