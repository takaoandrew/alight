package com.andrewtakao.alight;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by andrewtakao on 2/28/18.
 */

@Database(entities = {Route.class}, version = 1, exportSchema = false)
public abstract class RouteDatabase extends RoomDatabase {
    public abstract RouteDao routeDao();
}
