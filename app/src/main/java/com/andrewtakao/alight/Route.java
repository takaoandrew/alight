package com.andrewtakao.alight;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by andrewtakao on 2/13/18.
 */

@Entity
public class Route {

    public String route;

    public int firebaseCount;

    public int downloadedCount;

    public Route() {

    }

    public Route(String route) {
        this.route = route;
    }

    public Route(String route, int firebaseCount) {
        this.route = route;
        this.firebaseCount = firebaseCount;
    }

    public Route(String route, int firebaseCount, int downloadedCount) {
        this.route = route;
        this.firebaseCount = firebaseCount;
        this.downloadedCount = downloadedCount;
    }
}