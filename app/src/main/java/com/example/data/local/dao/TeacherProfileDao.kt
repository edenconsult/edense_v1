package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TeacherProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherProfileDao {
    @Query("SELECT * FROM teacher_profile WHERE id = 'local_teacher' LIMIT 1")
    fun getProfile(): Flow<TeacherProfileEntity?>

    @Query("SELECT * FROM teacher_profile WHERE id = 'local_teacher' LIMIT 1")
    suspend fun getProfileDirect(): TeacherProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: TeacherProfileEntity)
}
