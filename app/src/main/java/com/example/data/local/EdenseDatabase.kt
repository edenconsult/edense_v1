package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.catalog.GhanaShsCatalog
import com.example.data.local.dao.AcademicCalendarDao
import com.example.data.local.dao.PeriodDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TeacherProfileDao
import com.example.data.local.dao.TeachingSessionDao
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TeacherProfileEntity::class,
        AcademicCalendarEntity::class,
        SubjectEntity::class,
        PeriodEntity::class,
        TeachingSessionEntity::class,
        TaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EdenseDatabase : RoomDatabase() {

    abstract fun teacherProfileDao(): TeacherProfileDao
    abstract fun academicCalendarDao(): AcademicCalendarDao
    abstract fun subjectDao(): SubjectDao
    abstract fun periodDao(): PeriodDao
    abstract fun teachingSessionDao(): TeachingSessionDao
    abstract fun taskDao(): TaskDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE periods ADD COLUMN className TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN assignedClassrooms TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE subjects
                    SET assignedClassrooms = CASE
                        WHEN trim(assignedClasses) = '' THEN ''
                        WHEN instr(assignedClasses, ',') = 0 AND trim(defaultClassroom) != ''
                            THEN trim(defaultClassroom)
                        ELSE trim(assignedClasses)
                    END
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: EdenseDatabase? = null

        fun getDatabase(context: Context): EdenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EdenseDatabase::class.java,
                    "edense_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial catalog, profile and academic year on creation
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.teacherProfileDao().insertOrUpdate(
                                    TeacherProfileEntity(
                                        id = "local_teacher",
                                        name = "Mr. K. Mensah",
                                        email = "teacher@edense.edu.gh",
                                        schoolName = "Achimota Senior High School",
                                        schoolStartTime = "07:00",
                                        schoolEndTime = "15:00",
                                        isOnboardingCompleted = false,
                                        reminderLeadMinutes = 10
                                    )
                                )
                                database.academicCalendarDao().insertOrUpdate(
                                    AcademicCalendarEntity(
                                        id = "active_calendar",
                                        academicYear = "2026/2027",
                                        systemType = "SEMESTER",
                                        activeTermName = "Semester 1",
                                        termStartDate = "2026-09-01",
                                        termEndDate = "2026-12-18",
                                        pauseAlarmsDuringVacation = true,
                                        isManualVacationMode = false
                                    )
                                )
                                database.subjectDao().insertCatalog(GhanaShsCatalog.defaultCatalog)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
