package com.andrewtakao.alight;

import android.arch.persistence.db.SupportSQLiteStatement;
import android.arch.persistence.room.EntityDeletionOrUpdateAdapter;
import android.arch.persistence.room.EntityInsertionAdapter;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.RoomSQLiteQuery;
import android.database.Cursor;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;

public class RouteDao_Impl implements RouteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfRoute;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfRoute;

  public RouteDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRoute = new EntityInsertionAdapter<Route>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `Route`(`route`,`firebaseCount`,`downloadedCount`) VALUES (?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Route value) {
        if (value.route == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.route);
        }
        stmt.bindLong(2, value.firebaseCount);
        stmt.bindLong(3, value.downloadedCount);
      }
    };
    this.__deletionAdapterOfRoute = new EntityDeletionOrUpdateAdapter<Route>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `Route` WHERE `route` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Route value) {
        if (value.route == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.route);
        }
      }
    };
  }

  @Override
  public void insertAll(Route... routes) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfRoute.insert(routes);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(Route route) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfRoute.handle(route);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Route> getAll() {
    final String _sql = "SELECT * FROM route";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfRoute = _cursor.getColumnIndexOrThrow("route");
      final int _cursorIndexOfFirebaseCount = _cursor.getColumnIndexOrThrow("firebaseCount");
      final int _cursorIndexOfDownloadedCount = _cursor.getColumnIndexOrThrow("downloadedCount");
      final List<Route> _result = new ArrayList<Route>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final Route _item;
        _item = new Route();
        _item.route = _cursor.getString(_cursorIndexOfRoute);
        _item.firebaseCount = _cursor.getInt(_cursorIndexOfFirebaseCount);
        _item.downloadedCount = _cursor.getInt(_cursorIndexOfDownloadedCount);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
