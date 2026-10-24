package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.AcademicTermEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicTermDao {
    @Query("SELECT * FROM academic_terms ORDER BY orderIndex ASC")
    fun getTerms(): Flow<List<AcademicTermEntity>>

    @Query("SELECT * FROM academic_terms ORDER BY orderIndex ASC")
    suspend fun getTermsDirect(): List<AcademicTermEntity>

    @Query("DELETE FROM academic_terms")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terms: List<AcademicTermEntity>)

    @Transaction
    suspend fun replaceAll(terms: List<AcademicTermEntity>) {
        deleteAll()
        if (terms.isNotEmpty()) insertAll(terms)
    }
}
