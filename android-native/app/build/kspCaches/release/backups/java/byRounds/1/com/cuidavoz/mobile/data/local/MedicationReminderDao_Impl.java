package com.cuidavoz.mobile.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.cuidavoz.mobile.data.model.MedicationReminderEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MedicationReminderDao_Impl implements MedicationReminderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MedicationReminderEntity> __insertionAdapterOfMedicationReminderEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateReminderStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateGroupStatus;

  private final SharedSQLiteStatement __preparedStmtOfCancelAllReminders;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldReminders;

  private final SharedSQLiteStatement __preparedStmtOfReassignBlankPatientIds;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public MedicationReminderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedicationReminderEntity = new EntityInsertionAdapter<MedicationReminderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `medication_reminders` (`id`,`patientId`,`reminderGroupId`,`scheduleTime`,`targetDate`,`medicationIds`,`medicationNames`,`alarmRequestCode`,`attemptNumber`,`maxAttempts`,`repeatEveryMinutes`,`scheduledAt`,`respondedAt`,`status`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MedicationReminderEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPatientId());
        statement.bindString(3, entity.getReminderGroupId());
        statement.bindString(4, entity.getScheduleTime());
        statement.bindString(5, entity.getTargetDate());
        statement.bindString(6, entity.getMedicationIds());
        statement.bindString(7, entity.getMedicationNames());
        statement.bindLong(8, entity.getAlarmRequestCode());
        statement.bindLong(9, entity.getAttemptNumber());
        statement.bindLong(10, entity.getMaxAttempts());
        statement.bindLong(11, entity.getRepeatEveryMinutes());
        statement.bindLong(12, entity.getScheduledAt());
        if (entity.getRespondedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getRespondedAt());
        }
        statement.bindString(14, entity.getStatus());
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfUpdateReminderStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE medication_reminders\n"
                + "        SET status = ?,\n"
                + "            respondedAt = ?,\n"
                + "            updatedAt = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateGroupStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE medication_reminders\n"
                + "        SET status = ?,\n"
                + "            respondedAt = COALESCE(respondedAt, ?),\n"
                + "            updatedAt = ?\n"
                + "        WHERE reminderGroupId = ?\n"
                + "          AND status = 'PENDING'\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfCancelAllReminders = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE medication_reminders\n"
                + "        SET status = 'CANCELLED',\n"
                + "            updatedAt = ?\n"
                + "        WHERE patientId = ?\n"
                + "          AND status = 'PENDING'\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOldReminders = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        DELETE FROM medication_reminders\n"
                + "        WHERE scheduledAt < ?\n"
                + "          AND status != 'SCHEDULED'\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfReassignBlankPatientIds = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE medication_reminders\n"
                + "        SET patientId = ?\n"
                + "        WHERE TRIM(patientId) = ''\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM medication_reminders";
        return _query;
      }
    };
  }

  @Override
  public Object insertReminder(final MedicationReminderEntity reminder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMedicationReminderEntity.insert(reminder);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateReminderStatus(final String id, final String status, final Long respondedAt,
      final long updatedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateReminderStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (respondedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, respondedAt);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 4;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateReminderStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateGroupStatus(final String reminderGroupId, final String status,
      final Long respondedAt, final long updatedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateGroupStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (respondedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, respondedAt);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 4;
        _stmt.bindString(_argIndex, reminderGroupId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateGroupStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object cancelAllReminders(final String patientId, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCancelAllReminders.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 2;
        _stmt.bindString(_argIndex, patientId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfCancelAllReminders.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldReminders(final long cutoffTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldReminders.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOldReminders.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object reassignBlankPatientIds(final String patientId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfReassignBlankPatientIds.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, patientId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfReassignBlankPatientIds.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getScheduledReminders(final String patientId,
      final Continuation<? super List<MedicationReminderEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_reminders\n"
            + "        WHERE patientId = ?\n"
            + "          AND status = 'PENDING'\n"
            + "        ORDER BY scheduledAt ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationReminderEntity>>() {
      @Override
      @NonNull
      public List<MedicationReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfReminderGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderGroupId");
          final int _cursorIndexOfScheduleTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTime");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfMedicationIds = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationIds");
          final int _cursorIndexOfMedicationNames = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationNames");
          final int _cursorIndexOfAlarmRequestCode = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmRequestCode");
          final int _cursorIndexOfAttemptNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptNumber");
          final int _cursorIndexOfMaxAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "maxAttempts");
          final int _cursorIndexOfRepeatEveryMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatEveryMinutes");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfRespondedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "respondedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MedicationReminderEntity> _result = new ArrayList<MedicationReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationReminderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final String _tmpReminderGroupId;
            _tmpReminderGroupId = _cursor.getString(_cursorIndexOfReminderGroupId);
            final String _tmpScheduleTime;
            _tmpScheduleTime = _cursor.getString(_cursorIndexOfScheduleTime);
            final String _tmpTargetDate;
            _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            final String _tmpMedicationIds;
            _tmpMedicationIds = _cursor.getString(_cursorIndexOfMedicationIds);
            final String _tmpMedicationNames;
            _tmpMedicationNames = _cursor.getString(_cursorIndexOfMedicationNames);
            final int _tmpAlarmRequestCode;
            _tmpAlarmRequestCode = _cursor.getInt(_cursorIndexOfAlarmRequestCode);
            final int _tmpAttemptNumber;
            _tmpAttemptNumber = _cursor.getInt(_cursorIndexOfAttemptNumber);
            final int _tmpMaxAttempts;
            _tmpMaxAttempts = _cursor.getInt(_cursorIndexOfMaxAttempts);
            final int _tmpRepeatEveryMinutes;
            _tmpRepeatEveryMinutes = _cursor.getInt(_cursorIndexOfRepeatEveryMinutes);
            final long _tmpScheduledAt;
            _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt);
            final Long _tmpRespondedAt;
            if (_cursor.isNull(_cursorIndexOfRespondedAt)) {
              _tmpRespondedAt = null;
            } else {
              _tmpRespondedAt = _cursor.getLong(_cursorIndexOfRespondedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MedicationReminderEntity(_tmpId,_tmpPatientId,_tmpReminderGroupId,_tmpScheduleTime,_tmpTargetDate,_tmpMedicationIds,_tmpMedicationNames,_tmpAlarmRequestCode,_tmpAttemptNumber,_tmpMaxAttempts,_tmpRepeatEveryMinutes,_tmpScheduledAt,_tmpRespondedAt,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRemindersByGroupId(final String reminderGroupId,
      final Continuation<? super List<MedicationReminderEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_reminders\n"
            + "        WHERE reminderGroupId = ?\n"
            + "        ORDER BY attemptNumber ASC, scheduledAt ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, reminderGroupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationReminderEntity>>() {
      @Override
      @NonNull
      public List<MedicationReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfReminderGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderGroupId");
          final int _cursorIndexOfScheduleTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTime");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfMedicationIds = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationIds");
          final int _cursorIndexOfMedicationNames = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationNames");
          final int _cursorIndexOfAlarmRequestCode = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmRequestCode");
          final int _cursorIndexOfAttemptNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptNumber");
          final int _cursorIndexOfMaxAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "maxAttempts");
          final int _cursorIndexOfRepeatEveryMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatEveryMinutes");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfRespondedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "respondedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MedicationReminderEntity> _result = new ArrayList<MedicationReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationReminderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final String _tmpReminderGroupId;
            _tmpReminderGroupId = _cursor.getString(_cursorIndexOfReminderGroupId);
            final String _tmpScheduleTime;
            _tmpScheduleTime = _cursor.getString(_cursorIndexOfScheduleTime);
            final String _tmpTargetDate;
            _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            final String _tmpMedicationIds;
            _tmpMedicationIds = _cursor.getString(_cursorIndexOfMedicationIds);
            final String _tmpMedicationNames;
            _tmpMedicationNames = _cursor.getString(_cursorIndexOfMedicationNames);
            final int _tmpAlarmRequestCode;
            _tmpAlarmRequestCode = _cursor.getInt(_cursorIndexOfAlarmRequestCode);
            final int _tmpAttemptNumber;
            _tmpAttemptNumber = _cursor.getInt(_cursorIndexOfAttemptNumber);
            final int _tmpMaxAttempts;
            _tmpMaxAttempts = _cursor.getInt(_cursorIndexOfMaxAttempts);
            final int _tmpRepeatEveryMinutes;
            _tmpRepeatEveryMinutes = _cursor.getInt(_cursorIndexOfRepeatEveryMinutes);
            final long _tmpScheduledAt;
            _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt);
            final Long _tmpRespondedAt;
            if (_cursor.isNull(_cursorIndexOfRespondedAt)) {
              _tmpRespondedAt = null;
            } else {
              _tmpRespondedAt = _cursor.getLong(_cursorIndexOfRespondedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MedicationReminderEntity(_tmpId,_tmpPatientId,_tmpReminderGroupId,_tmpScheduleTime,_tmpTargetDate,_tmpMedicationIds,_tmpMedicationNames,_tmpAlarmRequestCode,_tmpAttemptNumber,_tmpMaxAttempts,_tmpRepeatEveryMinutes,_tmpScheduledAt,_tmpRespondedAt,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getReminder(final String reminderGroupId, final int attemptNumber,
      final Continuation<? super MedicationReminderEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_reminders\n"
            + "        WHERE reminderGroupId = ?\n"
            + "          AND attemptNumber = ?\n"
            + "        LIMIT 1\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, reminderGroupId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, attemptNumber);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MedicationReminderEntity>() {
      @Override
      @Nullable
      public MedicationReminderEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfReminderGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderGroupId");
          final int _cursorIndexOfScheduleTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTime");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfMedicationIds = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationIds");
          final int _cursorIndexOfMedicationNames = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationNames");
          final int _cursorIndexOfAlarmRequestCode = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmRequestCode");
          final int _cursorIndexOfAttemptNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptNumber");
          final int _cursorIndexOfMaxAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "maxAttempts");
          final int _cursorIndexOfRepeatEveryMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatEveryMinutes");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfRespondedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "respondedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final MedicationReminderEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final String _tmpReminderGroupId;
            _tmpReminderGroupId = _cursor.getString(_cursorIndexOfReminderGroupId);
            final String _tmpScheduleTime;
            _tmpScheduleTime = _cursor.getString(_cursorIndexOfScheduleTime);
            final String _tmpTargetDate;
            _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            final String _tmpMedicationIds;
            _tmpMedicationIds = _cursor.getString(_cursorIndexOfMedicationIds);
            final String _tmpMedicationNames;
            _tmpMedicationNames = _cursor.getString(_cursorIndexOfMedicationNames);
            final int _tmpAlarmRequestCode;
            _tmpAlarmRequestCode = _cursor.getInt(_cursorIndexOfAlarmRequestCode);
            final int _tmpAttemptNumber;
            _tmpAttemptNumber = _cursor.getInt(_cursorIndexOfAttemptNumber);
            final int _tmpMaxAttempts;
            _tmpMaxAttempts = _cursor.getInt(_cursorIndexOfMaxAttempts);
            final int _tmpRepeatEveryMinutes;
            _tmpRepeatEveryMinutes = _cursor.getInt(_cursorIndexOfRepeatEveryMinutes);
            final long _tmpScheduledAt;
            _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt);
            final Long _tmpRespondedAt;
            if (_cursor.isNull(_cursorIndexOfRespondedAt)) {
              _tmpRespondedAt = null;
            } else {
              _tmpRespondedAt = _cursor.getLong(_cursorIndexOfRespondedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new MedicationReminderEntity(_tmpId,_tmpPatientId,_tmpReminderGroupId,_tmpScheduleTime,_tmpTargetDate,_tmpMedicationIds,_tmpMedicationNames,_tmpAlarmRequestCode,_tmpAttemptNumber,_tmpMaxAttempts,_tmpRepeatEveryMinutes,_tmpScheduledAt,_tmpRespondedAt,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getReminderById(final String id,
      final Continuation<? super MedicationReminderEntity> $completion) {
    final String _sql = "SELECT * FROM medication_reminders WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MedicationReminderEntity>() {
      @Override
      @Nullable
      public MedicationReminderEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfReminderGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderGroupId");
          final int _cursorIndexOfScheduleTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTime");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfMedicationIds = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationIds");
          final int _cursorIndexOfMedicationNames = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationNames");
          final int _cursorIndexOfAlarmRequestCode = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmRequestCode");
          final int _cursorIndexOfAttemptNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptNumber");
          final int _cursorIndexOfMaxAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "maxAttempts");
          final int _cursorIndexOfRepeatEveryMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatEveryMinutes");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfRespondedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "respondedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final MedicationReminderEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final String _tmpReminderGroupId;
            _tmpReminderGroupId = _cursor.getString(_cursorIndexOfReminderGroupId);
            final String _tmpScheduleTime;
            _tmpScheduleTime = _cursor.getString(_cursorIndexOfScheduleTime);
            final String _tmpTargetDate;
            _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            final String _tmpMedicationIds;
            _tmpMedicationIds = _cursor.getString(_cursorIndexOfMedicationIds);
            final String _tmpMedicationNames;
            _tmpMedicationNames = _cursor.getString(_cursorIndexOfMedicationNames);
            final int _tmpAlarmRequestCode;
            _tmpAlarmRequestCode = _cursor.getInt(_cursorIndexOfAlarmRequestCode);
            final int _tmpAttemptNumber;
            _tmpAttemptNumber = _cursor.getInt(_cursorIndexOfAttemptNumber);
            final int _tmpMaxAttempts;
            _tmpMaxAttempts = _cursor.getInt(_cursorIndexOfMaxAttempts);
            final int _tmpRepeatEveryMinutes;
            _tmpRepeatEveryMinutes = _cursor.getInt(_cursorIndexOfRepeatEveryMinutes);
            final long _tmpScheduledAt;
            _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt);
            final Long _tmpRespondedAt;
            if (_cursor.isNull(_cursorIndexOfRespondedAt)) {
              _tmpRespondedAt = null;
            } else {
              _tmpRespondedAt = _cursor.getLong(_cursorIndexOfRespondedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new MedicationReminderEntity(_tmpId,_tmpPatientId,_tmpReminderGroupId,_tmpScheduleTime,_tmpTargetDate,_tmpMedicationIds,_tmpMedicationNames,_tmpAlarmRequestCode,_tmpAttemptNumber,_tmpMaxAttempts,_tmpRepeatEveryMinutes,_tmpScheduledAt,_tmpRespondedAt,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRemindersForSchedule(final String patientId, final String scheduleTime,
      final String targetDate,
      final Continuation<? super List<MedicationReminderEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_reminders\n"
            + "        WHERE patientId = ?\n"
            + "          AND scheduleTime = ?\n"
            + "          AND targetDate = ?\n"
            + "        ORDER BY attemptNumber ASC, scheduledAt ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    _argIndex = 2;
    _statement.bindString(_argIndex, scheduleTime);
    _argIndex = 3;
    _statement.bindString(_argIndex, targetDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationReminderEntity>>() {
      @Override
      @NonNull
      public List<MedicationReminderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfReminderGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderGroupId");
          final int _cursorIndexOfScheduleTime = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTime");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfMedicationIds = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationIds");
          final int _cursorIndexOfMedicationNames = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationNames");
          final int _cursorIndexOfAlarmRequestCode = CursorUtil.getColumnIndexOrThrow(_cursor, "alarmRequestCode");
          final int _cursorIndexOfAttemptNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptNumber");
          final int _cursorIndexOfMaxAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "maxAttempts");
          final int _cursorIndexOfRepeatEveryMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatEveryMinutes");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfRespondedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "respondedAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MedicationReminderEntity> _result = new ArrayList<MedicationReminderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationReminderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final String _tmpReminderGroupId;
            _tmpReminderGroupId = _cursor.getString(_cursorIndexOfReminderGroupId);
            final String _tmpScheduleTime;
            _tmpScheduleTime = _cursor.getString(_cursorIndexOfScheduleTime);
            final String _tmpTargetDate;
            _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            final String _tmpMedicationIds;
            _tmpMedicationIds = _cursor.getString(_cursorIndexOfMedicationIds);
            final String _tmpMedicationNames;
            _tmpMedicationNames = _cursor.getString(_cursorIndexOfMedicationNames);
            final int _tmpAlarmRequestCode;
            _tmpAlarmRequestCode = _cursor.getInt(_cursorIndexOfAlarmRequestCode);
            final int _tmpAttemptNumber;
            _tmpAttemptNumber = _cursor.getInt(_cursorIndexOfAttemptNumber);
            final int _tmpMaxAttempts;
            _tmpMaxAttempts = _cursor.getInt(_cursorIndexOfMaxAttempts);
            final int _tmpRepeatEveryMinutes;
            _tmpRepeatEveryMinutes = _cursor.getInt(_cursorIndexOfRepeatEveryMinutes);
            final long _tmpScheduledAt;
            _tmpScheduledAt = _cursor.getLong(_cursorIndexOfScheduledAt);
            final Long _tmpRespondedAt;
            if (_cursor.isNull(_cursorIndexOfRespondedAt)) {
              _tmpRespondedAt = null;
            } else {
              _tmpRespondedAt = _cursor.getLong(_cursorIndexOfRespondedAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MedicationReminderEntity(_tmpId,_tmpPatientId,_tmpReminderGroupId,_tmpScheduleTime,_tmpTargetDate,_tmpMedicationIds,_tmpMedicationNames,_tmpAlarmRequestCode,_tmpAttemptNumber,_tmpMaxAttempts,_tmpRepeatEveryMinutes,_tmpScheduledAt,_tmpRespondedAt,_tmpStatus,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
