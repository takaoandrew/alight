package com.andrewtakao.alight;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by andrewtakao on 2/28/18.
 */

@Dao
public interface RouteDao {
    @Query("SELECT * FROM route")
    List<Route> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Route... routes);

    @Delete
    void delete(Route route);
}
