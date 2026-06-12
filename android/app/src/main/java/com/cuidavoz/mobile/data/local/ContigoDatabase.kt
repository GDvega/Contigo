package com.cuidavoz.mobile.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import java.time.LocalDate
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.MedicationReminderEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.model.SyncQueueEntity

@Database(
    entities = [
        PatientEntity::class,
        MedicationEntity::class,
        BloodPressureEntity::class,
        MedicationLogEntity::class,
        MedicationReminderEntity::class,
        HealthSettingsEntity::class,
        FamilyContactEntity::class,
        SyncQueueEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
@TypeConverters(MedicationConverters::class)
abstract class ContigoDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun medicationDao(): MedicationDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun healthSettingsDao(): HealthSettingsDao
    abstract fun familyContactDao(): FamilyContactDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: ContigoDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val today = MedicationScheduleDefaults.todayIso(LocalDate.now())
                val allDaysJson = MedicationScheduleDefaults.allDaysJson()
                val emptyDatesJson = MedicationScheduleDefaults.emptyDatesJson()
                db.execSQL("ALTER TABLE medications ADD COLUMN scheduleType TEXT NOT NULL DEFAULT 'ALWAYS'")
                db.execSQL("ALTER TABLE medications ADD COLUMN startDate TEXT")
                db.execSQL("ALTER TABLE medications ADD COLUMN endDate TEXT")
                db.execSQL("ALTER TABLE medications ADD COLUMN daysOfWeekJson TEXT NOT NULL DEFAULT '$allDaysJson'")
                db.execSQL("ALTER TABLE medications ADD COLUMN specificDatesJson TEXT NOT NULL DEFAULT '$emptyDatesJson'")
                db.execSQL(
                    """
                    UPDATE medications
                    SET scheduleType = 'ALWAYS',
                        startDate = COALESCE(startDate, '$today'),
                        endDate = NULL,
                        daysOfWeekJson = '$allDaysJson',
                        specificDatesJson = '$emptyDatesJson'
                    """
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS family_contacts (
                        id TEXT NOT NULL PRIMARY KEY,
                        patientId TEXT NOT NULL,
                        fullName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        relationship TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL(
                    """
                    CREATE TABLE medication_reminders_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        patientId TEXT NOT NULL,
                        reminderGroupId TEXT NOT NULL,
                        scheduleTime TEXT NOT NULL,
                        targetDate TEXT NOT NULL,
                        medicationIds TEXT NOT NULL,
                        medicationNames TEXT NOT NULL,
                        alarmRequestCode INTEGER NOT NULL,
                        attemptNumber INTEGER NOT NULL,
                        maxAttempts INTEGER NOT NULL,
                        repeatEveryMinutes INTEGER NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        respondedAt INTEGER,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL(
                    """
                    INSERT INTO medication_reminders_new (
                        id, patientId, reminderGroupId, scheduleTime, targetDate, medicationIds, medicationNames,
                        alarmRequestCode, attemptNumber, maxAttempts, repeatEveryMinutes, scheduledAt, respondedAt,
                        status, createdAt, updatedAt
                    )
                    SELECT
                        id,
                        patientId,
                        patientId || '_' || scheduleTime || '_' || scheduledAt,
                        scheduleTime,
                        '',
                        medicationIds,
                        '',
                        alarmRequestCode,
                        repeatIndex,
                        3,
                        10,
                        scheduledAt,
                        NULL,
                        CASE status WHEN 'SCHEDULED' THEN 'PENDING' ELSE status END,
                        createdAt,
                        createdAt
                    FROM medication_reminders
                    """
                )
                db.execSQL("DROP TABLE medication_reminders")
                db.execSQL("ALTER TABLE medication_reminders_new RENAME TO medication_reminders")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications RENAME COLUMN daysOfWeekJson TO daysOfWeek")
                db.execSQL("ALTER TABLE medications RENAME COLUMN specificDatesJson TO specificDates")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_logs ADD COLUMN skipReason TEXT")
            }
        }

        fun getDatabase(context: Context): ContigoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContigoDatabase::class.java,
                    "cuida_voz.db",
                ).addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
