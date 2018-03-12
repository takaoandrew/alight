package com.andrewtakao.alight;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by andrewtakao on 2/28/18.
 */

@Database(entities = {POI.class}, version = 1, exportSchema = false)
public abstract class PoiDatabase extends RoomDatabase {
    public abstract POIDao poiDao();
}
