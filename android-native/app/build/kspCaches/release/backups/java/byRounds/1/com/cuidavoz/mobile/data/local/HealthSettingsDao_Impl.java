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
import com.cuidavoz.mobile.data.model.HealthSettingsEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HealthSettingsDao_Impl implements HealthSettingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HealthSettingsEntity> __insertionAdapterOfHealthSettingsEntity;

  private final SharedSQLiteStatement __preparedStmtOfReassignBlankPatientIds;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public HealthSettingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHealthSettingsEntity = new EntityInsertionAdapter<HealthSettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `health_settings` (`id`,`patientId`,`systolicMinNormal`,`systolicMaxNormal`,`diastolicMinNormal`,`diastolicMaxNormal`,`pulseMinNormal`,`pulseMaxNormal`,`doctorRecommendation`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthSettingsEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPatientId());
        statement.bindLong(3, entity.getSystolicMinNormal());
        statement.bindLong(4, entity.getSystolicMaxNormal());
        statement.bindLong(5, entity.getDiastolicMinNormal());
        statement.bindLong(6, entity.getDiastolicMaxNormal());
        statement.bindLong(7, entity.getPulseMinNormal());
        statement.bindLong(8, entity.getPulseMaxNormal());
        if (entity.getDoctorRecommendation() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getDoctorRecommendation());
        }
        statement.bindLong(10, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfReassignBlankPatientIds = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE health_settings\n"
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
        final String _query = "DELETE FROM health_settings";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final HealthSettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHealthSettingsEntity.insert(settings);
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
  public Flow<HealthSettingsEntity> observeSettings(final String patientId) {
    final String _sql = "SELECT * FROM health_settings WHERE patientId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_settings"}, new Callable<HealthSettingsEntity>() {
      @Override
      @Nullable
      public HealthSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfSystolicMinNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "systolicMinNormal");
          final int _cursorIndexOfSystolicMaxNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "systolicMaxNormal");
          final int _cursorIndexOfDiastolicMinNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolicMinNormal");
          final int _cursorIndexOfDiastolicMaxNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolicMaxNormal");
          final int _cursorIndexOfPulseMinNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "pulseMinNormal");
          final int _cursorIndexOfPulseMaxNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "pulseMaxNormal");
          final int _cursorIndexOfDoctorRecommendation = CursorUtil.getColumnIndexOrThrow(_cursor, "doctorRecommendation");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final HealthSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final int _tmpSystolicMinNormal;
            _tmpSystolicMinNormal = _cursor.getInt(_cursorIndexOfSystolicMinNormal);
            final int _tmpSystolicMaxNormal;
            _tmpSystolicMaxNormal = _cursor.getInt(_cursorIndexOfSystolicMaxNormal);
            final int _tmpDiastolicMinNormal;
            _tmpDiastolicMinNormal = _cursor.getInt(_cursorIndexOfDiastolicMinNormal);
            final int _tmpDiastolicMaxNormal;
            _tmpDiastolicMaxNormal = _cursor.getInt(_cursorIndexOfDiastolicMaxNormal);
            final int _tmpPulseMinNormal;
            _tmpPulseMinNormal = _cursor.getInt(_cursorIndexOfPulseMinNormal);
            final int _tmpPulseMaxNormal;
            _tmpPulseMaxNormal = _cursor.getInt(_cursorIndexOfPulseMaxNormal);
            final String _tmpDoctorRecommendation;
            if (_cursor.isNull(_cursorIndexOfDoctorRecommendation)) {
              _tmpDoctorRecommendation = null;
            } else {
              _tmpDoctorRecommendation = _cursor.getString(_cursorIndexOfDoctorRecommendation);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new HealthSettingsEntity(_tmpId,_tmpPatientId,_tmpSystolicMinNormal,_tmpSystolicMaxNormal,_tmpDiastolicMinNormal,_tmpDiastolicMaxNormal,_tmpPulseMinNormal,_tmpPulseMaxNormal,_tmpDoctorRecommendation,_tmpUpdatedAt);
          } else {
            _result = null;
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
  public Object getSettings(final String patientId,
      final Continuation<? super HealthSettingsEntity> $completion) {
    final String _sql = "SELECT * FROM health_settings WHERE patientId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, patientId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HealthSettingsEntity>() {
      @Override
      @Nullable
      public HealthSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPatientId = CursorUtil.getColumnIndexOrThrow(_cursor, "patientId");
          final int _cursorIndexOfSystolicMinNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "systolicMinNormal");
          final int _cursorIndexOfSystolicMaxNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "systolicMaxNormal");
          final int _cursorIndexOfDiastolicMinNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolicMinNormal");
          final int _cursorIndexOfDiastolicMaxNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolicMaxNormal");
          final int _cursorIndexOfPulseMinNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "pulseMinNormal");
          final int _cursorIndexOfPulseMaxNormal = CursorUtil.getColumnIndexOrThrow(_cursor, "pulseMaxNormal");
          final int _cursorIndexOfDoctorRecommendation = CursorUtil.getColumnIndexOrThrow(_cursor, "doctorRecommendation");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final HealthSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPatientId;
            _tmpPatientId = _cursor.getString(_cursorIndexOfPatientId);
            final int _tmpSystolicMinNormal;
            _tmpSystolicMinNormal = _cursor.getInt(_cursorIndexOfSystolicMinNormal);
            final int _tmpSystolicMaxNormal;
            _tmpSystolicMaxNormal = _cursor.getInt(_cursorIndexOfSystolicMaxNormal);
            final int _tmpDiastolicMinNormal;
            _tmpDiastolicMinNormal = _cursor.getInt(_cursorIndexOfDiastolicMinNormal);
            final int _tmpDiastolicMaxNormal;
            _tmpDiastolicMaxNormal = _cursor.getInt(_cursorIndexOfDiastolicMaxNormal);
            final int _tmpPulseMinNormal;
            _tmpPulseMinNormal = _cursor.getInt(_cursorIndexOfPulseMinNormal);
            final int _tmpPulseMaxNormal;
            _tmpPulseMaxNormal = _cursor.getInt(_cursorIndexOfPulseMaxNormal);
            final String _tmpDoctorRecommendation;
            if (_cursor.isNull(_cursorIndexOfDoctorRecommendation)) {
              _tmpDoctorRecommendation = null;
            } else {
              _tmpDoctorRecommendation = _cursor.getString(_cursorIndexOfDoctorRecommendation);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new HealthSettingsEntity(_tmpId,_tmpPatientId,_tmpSystolicMinNormal,_tmpSystolicMaxNormal,_tmpDiastolicMinNormal,_tmpDiastolicMaxNormal,_tmpPulseMinNormal,_tmpPulseMaxNormal,_tmpDoctorRecommendation,_tmpUpdatedAt);
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
