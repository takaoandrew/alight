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
public interface POIDao {
    @Query("SELECT * FROM poi")
    List<POI> getAll();

    @Query("SELECT * FROM poi WHERE busRoute LIKE :route")
    List<POI> getAll(String route);

    @Query("SELECT * FROM poi WHERE imageName IN (:poiNames)")
    List<POI> loadAllByIds(int[] poiNames);

    @Query("SELECT * FROM poi WHERE imageName LIKE :name LIMIT 1")
    POI findByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(POI... pois);

    @Delete
    void delete(POI poi);

}
