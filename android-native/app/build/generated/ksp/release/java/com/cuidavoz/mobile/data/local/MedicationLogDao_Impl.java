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
import com.cuidavoz.mobile.data.model.MedicationLogEntity;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MedicationLogDao_Impl implements MedicationLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MedicationLogEntity> __insertionAdapterOfMedicationLogEntity;

  private final SharedSQLiteStatement __preparedStmtOfReassignBlankPatientIds;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public MedicationLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMedicationLogEntity = new EntityInsertionAdapter<MedicationLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `medication_logs` (`id`,`medicationId`,`patientId`,`scheduledFor`,`takenAt`,`status`,`createdAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MedicationLogEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getMedicationId());
        statement.bindString(3, entity.getPatientId());
        statement.bindLong(4, entity.getScheduledFor());
        if (entity.getTakenAt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getTakenAt());
        }
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfReassignBlankPatientIds = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE medication_logs\n"
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
        final String _query = "DELETE FROM medication_logs";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MedicationLogEntity log,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMedicationLogEntity.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<MedicationLogEntity> logs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMedicationLogEntity.insert(logs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
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
  public Flow<List<MedicationLogEntity>> observeLogsForDay(final String patientId,
      final long startOfDay, final long endOfDay) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_logs\n"
            + "        WHERE patientId = ?\n"
            + "          AND scheduledFor >= ?\n"
            + "          AND scheduledFor < ?\n"
            + "        ORDER BY scheduledFor ASC, createdAt ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endOfDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"medication_logs"}, new Callable<List<MedicationLogEntity>>() {
      @Override
      @NonNull
      public List<MedicationLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfScheduledFor = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledFor");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationLogEntity> _result = new ArrayList<MedicationLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMedicationId;
            _tmpMedicationId = _cursor.getString(_cursorIndexOfMedicationId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final long _tmpScheduledFor;
            _tmpScheduledFor = _cursor.getLong(_cursorIndexOfScheduledFor);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationLogEntity(_tmpId,_tmpMedicationId,_tmpPatientId,_tmpScheduledFor,_tmpTakenAt,_tmpStatus,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLogsForDay(final String patientId, final long startOfDay, final long endOfDay,
      final Continuation<? super List<MedicationLogEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_logs\n"
            + "        WHERE patientId = ?\n"
            + "          AND scheduledFor >= ?\n"
            + "          AND scheduledFor < ?\n"
            + "        ORDER BY scheduledFor ASC, createdAt ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endOfDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationLogEntity>>() {
      @Override
      @NonNull
      public List<MedicationLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfScheduledFor = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledFor");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationLogEntity> _result = new ArrayList<MedicationLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMedicationId;
            _tmpMedicationId = _cursor.getString(_cursorIndexOfMedicationId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final long _tmpScheduledFor;
            _tmpScheduledFor = _cursor.getLong(_cursorIndexOfScheduledFor);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationLogEntity(_tmpId,_tmpMedicationId,_tmpPatientId,_tmpScheduledFor,_tmpTakenAt,_tmpStatus,_tmpCreatedAt);
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
  public Flow<List<MedicationLogEntity>> observeLogsForRange(final String patientId,
      final long startAt, final long endAt) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_logs\n"
            + "        WHERE patientId = ?\n"
            + "          AND scheduledFor >= ?\n"
            + "          AND scheduledFor < ?\n"
            + "        ORDER BY scheduledFor DESC, createdAt DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startAt);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endAt);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"medication_logs"}, new Callable<List<MedicationLogEntity>>() {
      @Override
      @NonNull
      public List<MedicationLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfScheduledFor = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledFor");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationLogEntity> _result = new ArrayList<MedicationLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMedicationId;
            _tmpMedicationId = _cursor.getString(_cursorIndexOfMedicationId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final long _tmpScheduledFor;
            _tmpScheduledFor = _cursor.getLong(_cursorIndexOfScheduledFor);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationLogEntity(_tmpId,_tmpMedicationId,_tmpPatientId,_tmpScheduledFor,_tmpTakenAt,_tmpStatus,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLogsForRange(final String patientId, final long startAt, final long endAt,
      final Continuation<? super List<MedicationLogEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_logs\n"
            + "        WHERE patientId = ?\n"
            + "          AND scheduledFor >= ?\n"
            + "          AND scheduledFor < ?\n"
            + "        ORDER BY scheduledFor DESC, createdAt DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startAt);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endAt);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MedicationLogEntity>>() {
      @Override
      @NonNull
      public List<MedicationLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfScheduledFor = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledFor");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MedicationLogEntity> _result = new ArrayList<MedicationLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MedicationLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMedicationId;
            _tmpMedicationId = _cursor.getString(_cursorIndexOfMedicationId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final long _tmpScheduledFor;
            _tmpScheduledFor = _cursor.getLong(_cursorIndexOfScheduledFor);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MedicationLogEntity(_tmpId,_tmpMedicationId,_tmpPatientId,_tmpScheduledFor,_tmpTakenAt,_tmpStatus,_tmpCreatedAt);
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
  public Object getTakenLogForMedication(final String medicationId, final String patientId,
      final long scheduledFor, final Continuation<? super MedicationLogEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM medication_logs\n"
            + "        WHERE medicationId = ?\n"
            + "          AND patientId = ?\n"
            + "          AND scheduledFor = ?\n"
            + "          AND status = 'TAKEN'\n"
            + "        LIMIT 1\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, medicationId);
    _argIndex = 2;
    _statement.bindString(_argIndex, patientId);
    _argIndex = 3;
    _statement.bindLong(_argIndex, scheduledFor);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MedicationLogEntity>() {
      @Override
      @Nullable
      public MedicationLogEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMedicationId = CursorUtil.getColumnIndexOrThrow(_cursor, "medicationId");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfScheduledFor = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledFor");
          final int _cursorIndexOfTakenAt = CursorUtil.getColumnIndexOrThrow(_cursor, "takenAt");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final MedicationLogEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMedicationId;
            _tmpMedicationId = _cursor.getString(_cursorIndexOfMedicationId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final long _tmpScheduledFor;
            _tmpScheduledFor = _cursor.getLong(_cursorIndexOfScheduledFor);
            final Long _tmpTakenAt;
            if (_cursor.isNull(_cursorIndexOfTakenAt)) {
              _tmpTakenAt = null;
            } else {
              _tmpTakenAt = _cursor.getLong(_cursorIndexOfTakenAt);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new MedicationLogEntity(_tmpId,_tmpMedicationId,_tmpPatientId,_tmpScheduledFor,_tmpTakenAt,_tmpStatus,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
