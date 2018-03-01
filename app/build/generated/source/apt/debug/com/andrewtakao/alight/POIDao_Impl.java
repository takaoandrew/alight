package com.andrewtakao.alight;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityDeletionOrUpdateAdapter;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.arch.persistence.room.util.StringUtil;
import android.database.Cursor;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.List;

public class POIDao_Impl implements POIDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfPOI;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfPOI;

  public POIDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPOI = new EntityInsertionAdapter<POI>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `POI`(`imageName`,`latitude`,`longitude`,`imageLocalStorageLocation`,`audioLocalStorageLocation`,`order`,`busRoute`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, POI value) {
        if (value.imageName == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.imageName);
        }
        stmt.bindDouble(2, value.latitude);
        stmt.bindDouble(3, value.longitude);
        if (value.imageLocalStorageLocation == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.imageLocalStorageLocation);
        }
        if (value.audioLocalStorageLocation == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.audioLocalStorageLocation);
        }
        stmt.bindLong(6, value.order);
        if (value.busRoute == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.busRoute);
        }
      }
    };
    this.__deletionAdapterOfPOI = new EntityDeletionOrUpdateAdapter<POI>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `POI` WHERE `imageName` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, POI value) {
        if (value.imageName == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.imageName);
        }
      }
    };
  }

  @Override
  public void insertAll(POI... pois) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfPOI.insert(pois);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(POI poi) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfPOI.handle(poi);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<POI> getAll() {
    final String _sql = "SELECT * FROM poi";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfImageName = _cursor.getColumnIndexOrThrow("imageName");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfImageLocalStorageLocation = _cursor.getColumnIndexOrThrow("imageLocalStorageLocation");
      final int _cursorIndexOfAudioLocalStorageLocation = _cursor.getColumnIndexOrThrow("audioLocalStorageLocation");
      final int _cursorIndexOfOrder = _cursor.getColumnIndexOrThrow("order");
      final int _cursorIndexOfBusRoute = _cursor.getColumnIndexOrThrow("busRoute");
      final List<POI> _result = new ArrayList<POI>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final POI _item;
        _item = new POI();
        _item.imageName = _cursor.getString(_cursorIndexOfImageName);
        _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _item.imageLocalStorageLocation = _cursor.getString(_cursorIndexOfImageLocalStorageLocation);
        _item.audioLocalStorageLocation = _cursor.getString(_cursorIndexOfAudioLocalStorageLocation);
        _item.order = _cursor.getInt(_cursorIndexOfOrder);
        _item.busRoute = _cursor.getString(_cursorIndexOfBusRoute);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<POI> getAll(String route) {
    final String _sql = "SELECT * FROM poi WHERE busRoute LIKE ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (route == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, route);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfImageName = _cursor.getColumnIndexOrThrow("imageName");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfImageLocalStorageLocation = _cursor.getColumnIndexOrThrow("imageLocalStorageLocation");
      final int _cursorIndexOfAudioLocalStorageLocation = _cursor.getColumnIndexOrThrow("audioLocalStorageLocation");
      final int _cursorIndexOfOrder = _cursor.getColumnIndexOrThrow("order");
      final int _cursorIndexOfBusRoute = _cursor.getColumnIndexOrThrow("busRoute");
      final List<POI> _result = new ArrayList<POI>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final POI _item;
        _item = new POI();
        _item.imageName = _cursor.getString(_cursorIndexOfImageName);
        _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _item.imageLocalStorageLocation = _cursor.getString(_cursorIndexOfImageLocalStorageLocation);
        _item.audioLocalStorageLocation = _cursor.getString(_cursorIndexOfAudioLocalStorageLocation);
        _item.order = _cursor.getInt(_cursorIndexOfOrder);
        _item.busRoute = _cursor.getString(_cursorIndexOfBusRoute);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<POI> loadAllByIds(int[] poiNames) {
    StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM poi WHERE imageName IN (");
    final int _inputSize = poiNames.length;
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int _item : poiNames) {
      _statement.bindLong(_argIndex, _item);
      _argIndex ++;
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfImageName = _cursor.getColumnIndexOrThrow("imageName");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfImageLocalStorageLocation = _cursor.getColumnIndexOrThrow("imageLocalStorageLocation");
      final int _cursorIndexOfAudioLocalStorageLocation = _cursor.getColumnIndexOrThrow("audioLocalStorageLocation");
      final int _cursorIndexOfOrder = _cursor.getColumnIndexOrThrow("order");
      final int _cursorIndexOfBusRoute = _cursor.getColumnIndexOrThrow("busRoute");
      final List<POI> _result = new ArrayList<POI>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final POI _item_1;
        _item_1 = new POI();
        _item_1.imageName = _cursor.getString(_cursorIndexOfImageName);
        _item_1.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item_1.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _item_1.imageLocalStorageLocation = _cursor.getString(_cursorIndexOfImageLocalStorageLocation);
        _item_1.audioLocalStorageLocation = _cursor.getString(_cursorIndexOfAudioLocalStorageLocation);
        _item_1.order = _cursor.getInt(_cursorIndexOfOrder);
        _item_1.busRoute = _cursor.getString(_cursorIndexOfBusRoute);
        _result.add(_item_1);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public POI findByName(String name) {
    final String _sql = "SELECT * FROM poi WHERE imageName LIKE ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (name == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, name);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfImageName = _cursor.getColumnIndexOrThrow("imageName");
      final int _cursorIndexOfLatitude = _cursor.getColumnIndexOrThrow("latitude");
      final int _cursorIndexOfLongitude = _cursor.getColumnIndexOrThrow("longitude");
      final int _cursorIndexOfImageLocalStorageLocation = _cursor.getColumnIndexOrThrow("imageLocalStorageLocation");
      final int _cursorIndexOfAudioLocalStorageLocation = _cursor.getColumnIndexOrThrow("audioLocalStorageLocation");
      final int _cursorIndexOfOrder = _cursor.getColumnIndexOrThrow("order");
      final int _cursorIndexOfBusRoute = _cursor.getColumnIndexOrThrow("busRoute");
      final POI _result;
      if(_cursor.moveToFirst()) {
        _result = new POI();
        _result.imageName = _cursor.getString(_cursorIndexOfImageName);
        _result.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _result.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _result.imageLocalStorageLocation = _cursor.getString(_cursorIndexOfImageLocalStorageLocation);
        _result.audioLocalStorageLocation = _cursor.getString(_cursorIndexOfAudioLocalStorageLocation);
        _result.order = _cursor.getInt(_cursorIndexOfOrder);
        _result.busRoute = _cursor.getString(_cursorIndexOfBusRoute);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
