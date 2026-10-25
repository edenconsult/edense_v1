package com.example.data.export

import androidx.room.withTransaction
import com.example.data.local.EdenseDatabase
import com.example.data.local.entity.AcademicCalendarEntity
import com.example.data.local.entity.AcademicTermEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TeacherProfileEntity
import com.example.data.local.entity.TeachingSessionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class EdenseDataExport(
    val schemaVersion: Int = 1,
    val exportedAtUtc: String,
    val profile: TeacherProfileEntity?,
    val academicCalendar: AcademicCalendarEntity?,
    val academicTerms: List<AcademicTermEntity>,
    val subjects: List<SubjectEntity>,
    val periods: List<PeriodEntity>,
    val teachingSessions: List<TeachingSessionEntity>,
    val tasks: List<TaskEntity>
)

object EdenseDataExporter {
    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(EdenseDataExport::class.java)
        .indent("  ")

    suspend fun export(database: EdenseDatabase): String = withContext(Dispatchers.IO) {
        val export = database.withTransaction {
            EdenseDataExport(
                exportedAtUtc = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.ROOT
                ).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date()),
                profile = database.teacherProfileDao().getProfileDirect(),
                academicCalendar = database.academicCalendarDao().getCalendarDirect(),
                academicTerms = database.academicTermDao().getTermsDirect(),
                subjects = database.subjectDao().getAllSubjectsDirect(),
                periods = database.periodDao().getAllPeriodsDirect(),
                teachingSessions = database.teachingSessionDao().getAllSessionsDirect(),
                tasks = database.taskDao().getAllTasksDirect()
            )
        }
        adapter.toJson(export)
    }

    fun toJson(export: EdenseDataExport): String = adapter.toJson(export)
}
