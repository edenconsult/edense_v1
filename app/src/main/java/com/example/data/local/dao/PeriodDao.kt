package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllPeriods(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllPeriodsDirect(): List<PeriodEntity>

    @Query("SELECT * FROM periods WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getPeriodsForDay(dayOfWeek: Int): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getPeriodsForDayDirect(dayOfWeek: Int): List<PeriodEntity>

    @Query("SELECT * FROM periods WHERE subjectId = :subjectId")
    fun getPeriodsForSubject(subjectId: String): Flow<List<PeriodEntity>>

    @Query("SELECT COUNT(*) FROM periods WHERE subjectId = :subjectId")
    fun getPeriodCountForSubject(subjectId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriods(periods: List<PeriodEntity>)

    @Query("DELETE FROM periods WHERE id = :id")
    suspend fun deletePeriod(id: String)

    @Query("DELETE FROM periods WHERE blockId = :blockId")
    suspend fun deleteBlock(blockId: String)

    @Query("DELETE FROM periods WHERE subjectId = :subjectId")
    suspend fun deletePeriodsForSubject(subjectId: String)

    @Query("DELETE FROM periods")
    suspend fun deleteAll()
}
