package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.catalog.GhanaShsCatalog
import com.example.data.local.dao.AcademicCalendarDao
import com.example.data.local.dao.AcademicTermDao
import com.example.data.local.dao.PeriodDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TeacherProfileDao
import com.example.data.local.dao.TeachingSessionDao
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
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
        AcademicTermEntity::class,
        SubjectEntity::class,
        PeriodEntity::class,
        TeachingSessionEntity::class,
        TaskEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class EdenseDatabase : RoomDatabase() {

    abstract fun teacherProfileDao(): TeacherProfileDao
    abstract fun academicCalendarDao(): AcademicCalendarDao
    abstract fun academicTermDao(): AcademicTermDao
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS academic_terms (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO academic_terms (id, name, startDate, endDate, orderIndex)
                    SELECT id || '_term_1', activeTermName, termStartDate, termEndDate, 0
                    FROM academic_calendar
                    WHERE id = 'active_calendar'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE teaching_sessions ADD COLUMN className TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN className TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO subjects (
                        id, name, category, isSelected, assignedClasses, defaultClassroom,
                        assignedClassrooms, targetPeriodsPerWeek, colorHex, isArchived, updatedAt
                    ) VALUES (
                        'elec_irs', 'Islamic Religious Studies (IRS)', 'ELECTIVE', 0, '', '',
                        '', 0, '#0F766E', 0, 0
                    )
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
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
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
                                database.academicTermDao().replaceAll(
                                    listOf(
                                        AcademicTermEntity(
                                            id = "active_calendar_term_1",
                                            name = "Semester 1",
                                            startDate = "2026-09-01",
                                            endDate = "2026-12-18",
                                            orderIndex = 0
                                        )
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
