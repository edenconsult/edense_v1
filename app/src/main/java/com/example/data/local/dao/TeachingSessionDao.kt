package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TeachingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeachingSessionDao {
    @Query("SELECT * FROM teaching_sessions ORDER BY sessionDate DESC, createdAt DESC")
    fun getAllSessions(): Flow<List<TeachingSessionEntity>>

    @Query("SELECT * FROM teaching_sessions ORDER BY sessionDate DESC, createdAt DESC")
    suspend fun getAllSessionsDirect(): List<TeachingSessionEntity>

    @Query("SELECT * FROM teaching_sessions WHERE subjectId = :subjectId ORDER BY sessionDate DESC, createdAt DESC")
    fun getSessionsForSubject(subjectId: String): Flow<List<TeachingSessionEntity>>

    @Query("SELECT * FROM teaching_sessions WHERE subjectId = :subjectId ORDER BY sessionDate DESC, createdAt DESC LIMIT 1")
    fun getLatestSessionForSubject(subjectId: String): Flow<TeachingSessionEntity?>

    @Query("SELECT * FROM teaching_sessions WHERE subjectId = :subjectId ORDER BY sessionDate DESC, createdAt DESC LIMIT 1")
    suspend fun getLatestSessionForSubjectDirect(subjectId: String): TeachingSessionEntity?

    @Query("SELECT * FROM teaching_sessions WHERE subjectId = :subjectId AND className = :className ORDER BY sessionDate DESC, createdAt DESC LIMIT 1")
    fun getLatestSessionForClass(subjectId: String, className: String): Flow<TeachingSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TeachingSessionEntity)

    @Query("DELETE FROM teaching_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)
}
