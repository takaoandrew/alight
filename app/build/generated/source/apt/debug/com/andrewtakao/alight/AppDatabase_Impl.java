package com.andrewtakao.alight;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Callback;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomOpenHelper;
import android.arch.persistence.room.RoomOpenHelper.Delegate;
import android.arch.persistence.room.util.TableInfo;
import android.arch.persistence.room.util.TableInfo.Column;
import android.arch.persistence.room.util.TableInfo.ForeignKey;
import android.arch.persistence.room.util.TableInfo.Index;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.HashSet;

public class AppDatabase_Impl extends AppDatabase {
  private volatile POIDao _pOIDao;

  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `POI` (`imageName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `imageLocalStorageLocation` TEXT, `audioLocalStorageLocation` TEXT, `order` INTEGER NOT NULL, `busRoute` TEXT, PRIMARY KEY(`imageName`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"3256b555fcb7aab33d27a8bde674a56c\")");
      }

      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `POI`");
      }

      protected void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      protected void validateMigration(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsPOI = new HashMap<String, TableInfo.Column>(7);
        _columnsPOI.put("imageName", new TableInfo.Column("imageName", "TEXT", true, 1));
        _columnsPOI.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0));
        _columnsPOI.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0));
        _columnsPOI.put("imageLocalStorageLocation", new TableInfo.Column("imageLocalStorageLocation", "TEXT", false, 0));
        _columnsPOI.put("audioLocalStorageLocation", new TableInfo.Column("audioLocalStorageLocation", "TEXT", false, 0));
        _columnsPOI.put("order", new TableInfo.Column("order", "INTEGER", true, 0));
        _columnsPOI.put("busRoute", new TableInfo.Column("busRoute", "TEXT", false, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPOI = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPOI = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPOI = new TableInfo("POI", _columnsPOI, _foreignKeysPOI, _indicesPOI);
        final TableInfo _existingPOI = TableInfo.read(_db, "POI");
        if (! _infoPOI.equals(_existingPOI)) {
          throw new IllegalStateException("Migration didn't properly handle POI(com.andrewtakao.alight.POI).\n"
                  + " Expected:\n" + _infoPOI + "\n"
                  + " Found:\n" + _existingPOI);
        }
      }
    }, "3256b555fcb7aab33d27a8bde674a56c");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "POI");
  }

  @Override
  public POIDao poiDao() {
    if (_pOIDao != null) {
      return _pOIDao;
    } else {
      synchronized(this) {
        if(_pOIDao == null) {
          _pOIDao = new POIDao_Impl(this);
        }
        return _pOIDao;
      }
    }
  }
}
