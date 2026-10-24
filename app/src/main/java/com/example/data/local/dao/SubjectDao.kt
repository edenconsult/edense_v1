package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY category ASC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY category ASC, name ASC")
    suspend fun getAllSubjectsDirect(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE isSelected = 1 ORDER BY name ASC")
    fun getSelectedSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isSelected = 1 ORDER BY name ASC")
    suspend fun getSelectedSubjectsDirect(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectById(id: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectByIdDirect(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getSubjectByNameDirect(name: String): SubjectEntity?

    @Query("SELECT COUNT(*) FROM subjects WHERE isSelected = 1")
    fun getSelectedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM subjects WHERE isSelected = 1")
    suspend fun getSelectedCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCatalog(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(subject: SubjectEntity)

    @Query("""
        UPDATE subjects 
        SET isSelected = :isSelected, 
            assignedClasses = :assignedClasses, 
            defaultClassroom = :defaultClassroom, 
            assignedClassrooms = :assignedClassrooms,
            targetPeriodsPerWeek = :targetPeriods,
            updatedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun updateSubjectSelection(
        id: String,
        isSelected: Boolean,
        assignedClasses: String,
        defaultClassroom: String,
        assignedClassrooms: String,
        targetPeriods: Int,
        timestamp: Long = System.currentTimeMillis()
    )
}
