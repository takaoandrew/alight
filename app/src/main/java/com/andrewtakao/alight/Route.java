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
    @PrimaryKey
    @NonNull
    public String route;

    public Route() {

    }
    public Route(String route) {
        this.route = route;
    }
}