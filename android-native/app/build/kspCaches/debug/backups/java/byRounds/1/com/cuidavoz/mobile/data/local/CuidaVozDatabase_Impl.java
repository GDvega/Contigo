package com.cuidavoz.mobile.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CuidaVozDatabase_Impl extends CuidaVozDatabase {
  private volatile PatientDao _patientDao;

  private volatile MedicationDao _medicationDao;

  private volatile BloodPressureDao _bloodPressureDao;

  private volatile MedicationLogDao _medicationLogDao;

  private volatile MedicationReminderDao _medicationReminderDao;

  private volatile HealthSettingsDao _healthSettingsDao;

  private volatile FamilyContactDao _familyContactDao;

  private volatile SyncQueueDao _syncQueueDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(7) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `patients` (`id` TEXT NOT NULL, `fullName` TEXT NOT NULL, `age` INTEGER, `notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `medications` (`id` TEXT NOT NULL, `patientId` TEXT NOT NULL, `name` TEXT NOT NULL, `dose` TEXT NOT NULL, `color` TEXT, `shape` TEXT, `instructions` TEXT, `scheduleTime` TEXT NOT NULL, `imageUri` TEXT, `isActive` INTEGER NOT NULL, `scheduleType` TEXT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT, `daysOfWeekJson` TEXT NOT NULL, `specificDatesJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `blood_pressure_readings` (`id` TEXT NOT NULL, `patientId` TEXT NOT NULL, `systolic` INTEGER NOT NULL, `diastolic` INTEGER NOT NULL, `pulse` INTEGER, `status` TEXT NOT NULL, `notes` TEXT, `measuredAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `medication_logs` (`id` TEXT NOT NULL, `medicationId` TEXT NOT NULL, `patientId` TEXT NOT NULL, `scheduledFor` INTEGER NOT NULL, `takenAt` INTEGER, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `medication_reminders` (`id` TEXT NOT NULL, `patientId` TEXT NOT NULL, `reminderGroupId` TEXT NOT NULL, `scheduleTime` TEXT NOT NULL, `targetDate` TEXT NOT NULL, `medicationIds` TEXT NOT NULL, `medicationNames` TEXT NOT NULL, `alarmRequestCode` INTEGER NOT NULL, `attemptNumber` INTEGER NOT NULL, `maxAttempts` INTEGER NOT NULL, `repeatEveryMinutes` INTEGER NOT NULL, `scheduledAt` INTEGER NOT NULL, `respondedAt` INTEGER, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `health_settings` (`id` TEXT NOT NULL, `patientId` TEXT NOT NULL, `systolicMinNormal` INTEGER NOT NULL, `systolicMaxNormal` INTEGER NOT NULL, `diastolicMinNormal` INTEGER NOT NULL, `diastolicMaxNormal` INTEGER NOT NULL, `pulseMinNormal` INTEGER NOT NULL, `pulseMaxNormal` INTEGER NOT NULL, `doctorRecommendation` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `family_contacts` (`id` TEXT NOT NULL, `patientId` TEXT NOT NULL, `fullName` TEXT NOT NULL, `phone` TEXT NOT NULL, `relationship` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_queue` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operation` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `status` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastError` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5d1426c721ec09e3e32a481e0dee5acd')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `patients`");
        db.execSQL("DROP TABLE IF EXISTS `medications`");
        db.execSQL("DROP TABLE IF EXISTS `blood_pressure_readings`");
        db.execSQL("DROP TABLE IF EXISTS `medication_logs`");
        db.execSQL("DROP TABLE IF EXISTS `medication_reminders`");
        db.execSQL("DROP TABLE IF EXISTS `health_settings`");
        db.execSQL("DROP TABLE IF EXISTS `family_contacts`");
        db.execSQL("DROP TABLE IF EXISTS `sync_queue`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPatients = new HashMap<String, TableInfo.Column>(6);
        _columnsPatients.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPatients.put("fullName", new TableInfo.Column("fullName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPatients.put("age", new TableInfo.Column("age", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPatients.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPatients.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPatients.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPatients = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPatients = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPatients = new TableInfo("patients", _columnsPatients, _foreignKeysPatients, _indicesPatients);
        final TableInfo _existingPatients = TableInfo.read(db, "patients");
        if (!_infoPatients.equals(_existingPatients)) {
          return new RoomOpenHelper.ValidationResult(false, "patients(com.cuidavoz.mobile.data.model.PatientEntity).\n"
                  + " Expected:\n" + _infoPatients + "\n"
                  + " Found:\n" + _existingPatients);
        }
        final HashMap<String, TableInfo.Column> _columnsMedications = new HashMap<String, TableInfo.Column>(17);
        _columnsMedications.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("patientId", new TableInfo.Column("patientId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("dose", new TableInfo.Column("dose", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("color", new TableInfo.Column("color", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("shape", new TableInfo.Column("shape", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("instructions", new TableInfo.Column("instructions", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("scheduleTime", new TableInfo.Column("scheduleTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("imageUri", new TableInfo.Column("imageUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("scheduleType", new TableInfo.Column("scheduleType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("endDate", new TableInfo.Column("endDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("daysOfWeekJson", new TableInfo.Column("daysOfWeekJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("specificDatesJson", new TableInfo.Column("specificDatesJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedications.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedications = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedications = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedications = new TableInfo("medications", _columnsMedications, _foreignKeysMedications, _indicesMedications);
        final TableInfo _existingMedications = TableInfo.read(db, "medications");
        if (!_infoMedications.equals(_existingMedications)) {
          return new RoomOpenHelper.ValidationResult(false, "medications(com.cuidavoz.mobile.data.model.MedicationEntity).\n"
                  + " Expected:\n" + _infoMedications + "\n"
                  + " Found:\n" + _existingMedications);
        }
        final HashMap<String, TableInfo.Column> _columnsBloodPressureReadings = new HashMap<String, TableInfo.Column>(9);
        _columnsBloodPressureReadings.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("patientId", new TableInfo.Column("patientId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("systolic", new TableInfo.Column("systolic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("diastolic", new TableInfo.Column("diastolic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("pulse", new TableInfo.Column("pulse", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("measuredAt", new TableInfo.Column("measuredAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBloodPressureReadings.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBloodPressureReadings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBloodPressureReadings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBloodPressureReadings = new TableInfo("blood_pressure_readings", _columnsBloodPressureReadings, _foreignKeysBloodPressureReadings, _indicesBloodPressureReadings);
        final TableInfo _existingBloodPressureReadings = TableInfo.read(db, "blood_pressure_readings");
        if (!_infoBloodPressureReadings.equals(_existingBloodPressureReadings)) {
          return new RoomOpenHelper.ValidationResult(false, "blood_pressure_readings(com.cuidavoz.mobile.data.model.BloodPressureEntity).\n"
                  + " Expected:\n" + _infoBloodPressureReadings + "\n"
                  + " Found:\n" + _existingBloodPressureReadings);
        }
        final HashMap<String, TableInfo.Column> _columnsMedicationLogs = new HashMap<String, TableInfo.Column>(7);
        _columnsMedicationLogs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationLogs.put("medicationId", new TableInfo.Column("medicationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationLogs.put("patientId", new TableInfo.Column("patientId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationLogs.put("scheduledFor", new TableInfo.Column("scheduledFor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationLogs.put("takenAt", new TableInfo.Column("takenAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationLogs.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationLogs.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedicationLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedicationLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedicationLogs = new TableInfo("medication_logs", _columnsMedicationLogs, _foreignKeysMedicationLogs, _indicesMedicationLogs);
        final TableInfo _existingMedicationLogs = TableInfo.read(db, "medication_logs");
        if (!_infoMedicationLogs.equals(_existingMedicationLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "medication_logs(com.cuidavoz.mobile.data.model.MedicationLogEntity).\n"
                  + " Expected:\n" + _infoMedicationLogs + "\n"
                  + " Found:\n" + _existingMedicationLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsMedicationReminders = new HashMap<String, TableInfo.Column>(16);
        _columnsMedicationReminders.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("patientId", new TableInfo.Column("patientId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("reminderGroupId", new TableInfo.Column("reminderGroupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("scheduleTime", new TableInfo.Column("scheduleTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("targetDate", new TableInfo.Column("targetDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("medicationIds", new TableInfo.Column("medicationIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("medicationNames", new TableInfo.Column("medicationNames", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("alarmRequestCode", new TableInfo.Column("alarmRequestCode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("attemptNumber", new TableInfo.Column("attemptNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("maxAttempts", new TableInfo.Column("maxAttempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("repeatEveryMinutes", new TableInfo.Column("repeatEveryMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("scheduledAt", new TableInfo.Column("scheduledAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("respondedAt", new TableInfo.Column("respondedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMedicationReminders.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMedicationReminders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMedicationReminders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMedicationReminders = new TableInfo("medication_reminders", _columnsMedicationReminders, _foreignKeysMedicationReminders, _indicesMedicationReminders);
        final TableInfo _existingMedicationReminders = TableInfo.read(db, "medication_reminders");
        if (!_infoMedicationReminders.equals(_existingMedicationReminders)) {
          return new RoomOpenHelper.ValidationResult(false, "medication_reminders(com.cuidavoz.mobile.data.model.MedicationReminderEntity).\n"
                  + " Expected:\n" + _infoMedicationReminders + "\n"
                  + " Found:\n" + _existingMedicationReminders);
        }
        final HashMap<String, TableInfo.Column> _columnsHealthSettings = new HashMap<String, TableInfo.Column>(10);
        _columnsHealthSettings.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("patientId", new TableInfo.Column("patientId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("systolicMinNormal", new TableInfo.Column("systolicMinNormal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("systolicMaxNormal", new TableInfo.Column("systolicMaxNormal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("diastolicMinNormal", new TableInfo.Column("diastolicMinNormal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("diastolicMaxNormal", new TableInfo.Column("diastolicMaxNormal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("pulseMinNormal", new TableInfo.Column("pulseMinNormal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("pulseMaxNormal", new TableInfo.Column("pulseMaxNormal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("doctorRecommendation", new TableInfo.Column("doctorRecommendation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthSettings.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHealthSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHealthSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHealthSettings = new TableInfo("health_settings", _columnsHealthSettings, _foreignKeysHealthSettings, _indicesHealthSettings);
        final TableInfo _existingHealthSettings = TableInfo.read(db, "health_settings");
        if (!_infoHealthSettings.equals(_existingHealthSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "health_settings(com.cuidavoz.mobile.data.model.HealthSettingsEntity).\n"
                  + " Expected:\n" + _infoHealthSettings + "\n"
                  + " Found:\n" + _existingHealthSettings);
        }
        final HashMap<String, TableInfo.Column> _columnsFamilyContacts = new HashMap<String, TableInfo.Column>(7);
        _columnsFamilyContacts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFamilyContacts.put("patientId", new TableInfo.Column("patientId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFamilyContacts.put("fullName", new TableInfo.Column("fullName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFamilyContacts.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFamilyContacts.put("relationship", new TableInfo.Column("relationship", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFamilyContacts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFamilyContacts.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFamilyContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFamilyContacts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFamilyContacts = new TableInfo("family_contacts", _columnsFamilyContacts, _foreignKeysFamilyContacts, _indicesFamilyContacts);
        final TableInfo _existingFamilyContacts = TableInfo.read(db, "family_contacts");
        if (!_infoFamilyContacts.equals(_existingFamilyContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "family_contacts(com.cuidavoz.mobile.data.model.FamilyContactEntity).\n"
                  + " Expected:\n" + _infoFamilyContacts + "\n"
                  + " Found:\n" + _existingFamilyContacts);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncQueue = new HashMap<String, TableInfo.Column>(10);
        _columnsSyncQueue.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("entityType", new TableInfo.Column("entityType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("entityId", new TableInfo.Column("entityId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("operation", new TableInfo.Column("operation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("payloadJson", new TableInfo.Column("payloadJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("lastError", new TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncQueue = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncQueue = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncQueue = new TableInfo("sync_queue", _columnsSyncQueue, _foreignKeysSyncQueue, _indicesSyncQueue);
        final TableInfo _existingSyncQueue = TableInfo.read(db, "sync_queue");
        if (!_infoSyncQueue.equals(_existingSyncQueue)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_queue(com.cuidavoz.mobile.data.model.SyncQueueEntity).\n"
                  + " Expected:\n" + _infoSyncQueue + "\n"
                  + " Found:\n" + _existingSyncQueue);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "5d1426c721ec09e3e32a481e0dee5acd", "1add258a330085fc1a789496d4b23e1b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "patients","medications","blood_pressure_readings","medication_logs","medication_reminders","health_settings","family_contacts","sync_queue");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `patients`");
      _db.execSQL("DELETE FROM `medications`");
      _db.execSQL("DELETE FROM `blood_pressure_readings`");
      _db.execSQL("DELETE FROM `medication_logs`");
      _db.execSQL("DELETE FROM `medication_reminders`");
      _db.execSQL("DELETE FROM `health_settings`");
      _db.execSQL("DELETE FROM `family_contacts`");
      _db.execSQL("DELETE FROM `sync_queue`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PatientDao.class, PatientDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MedicationDao.class, MedicationDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BloodPressureDao.class, BloodPressureDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MedicationLogDao.class, MedicationLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MedicationReminderDao.class, MedicationReminderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HealthSettingsDao.class, HealthSettingsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FamilyContactDao.class, FamilyContactDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncQueueDao.class, SyncQueueDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PatientDao patientDao() {
    if (_patientDao != null) {
      return _patientDao;
    } else {
      synchronized(this) {
        if(_patientDao == null) {
          _patientDao = new PatientDao_Impl(this);
        }
        return _patientDao;
      }
    }
  }

  @Override
  public MedicationDao medicationDao() {
    if (_medicationDao != null) {
      return _medicationDao;
    } else {
      synchronized(this) {
        if(_medicationDao == null) {
          _medicationDao = new MedicationDao_Impl(this);
        }
        return _medicationDao;
      }
    }
  }

  @Override
  public BloodPressureDao bloodPressureDao() {
    if (_bloodPressureDao != null) {
      return _bloodPressureDao;
    } else {
      synchronized(this) {
        if(_bloodPressureDao == null) {
          _bloodPressureDao = new BloodPressureDao_Impl(this);
        }
        return _bloodPressureDao;
      }
    }
  }

  @Override
  public MedicationLogDao medicationLogDao() {
    if (_medicationLogDao != null) {
      return _medicationLogDao;
    } else {
      synchronized(this) {
        if(_medicationLogDao == null) {
          _medicationLogDao = new MedicationLogDao_Impl(this);
        }
        return _medicationLogDao;
      }
    }
  }

  @Override
  public MedicationReminderDao medicationReminderDao() {
    if (_medicationReminderDao != null) {
      return _medicationReminderDao;
    } else {
      synchronized(this) {
        if(_medicationReminderDao == null) {
          _medicationReminderDao = new MedicationReminderDao_Impl(this);
        }
        return _medicationReminderDao;
      }
    }
  }

  @Override
  public HealthSettingsDao healthSettingsDao() {
    if (_healthSettingsDao != null) {
      return _healthSettingsDao;
    } else {
      synchronized(this) {
        if(_healthSettingsDao == null) {
          _healthSettingsDao = new HealthSettingsDao_Impl(this);
        }
        return _healthSettingsDao;
      }
    }
  }

  @Override
  public FamilyContactDao familyContactDao() {
    if (_familyContactDao != null) {
      return _familyContactDao;
    } else {
      synchronized(this) {
        if(_familyContactDao == null) {
          _familyContactDao = new FamilyContactDao_Impl(this);
        }
        return _familyContactDao;
      }
    }
  }

  @Override
  public SyncQueueDao syncQueueDao() {
    if (_syncQueueDao != null) {
      return _syncQueueDao;
    } else {
      synchronized(this) {
        if(_syncQueueDao == null) {
          _syncQueueDao = new SyncQueueDao_Impl(this);
        }
        return _syncQueueDao;
      }
    }
  }
}
