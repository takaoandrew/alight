package com.andrewtakao.alight;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by andrewtakao on 2/13/18.
 */

@Entity
public class POI {
    @PrimaryKey
    @NonNull
    public String imageName;

    @ColumnInfo(name = "latitude")
    public double latitude;
    @ColumnInfo(name = "longitude")
    public double longitude;
    @ColumnInfo(name = "imageLocalStorageLocation")
    public String imageLocalStorageLocation;
    @ColumnInfo(name = "audioLocalStorageLocation")
    public String audioLocalStorageLocation;
    @ColumnInfo(name = "order")
    public int order;
    @ColumnInfo(name = "busRoute")
    public String busRoute;

    public POI() {

    }


    public POI(String imageName, double latitude, double longitude, int order, String busRoute) {
        this.imageName = imageName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.order = order;
        this.busRoute = busRoute;
    }

    public double distanceFrom(double latitude, double longitude) {
        // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = latitude * Math.PI / 180 - this.latitude * Math.PI / 180;
        double dLon = longitude * Math.PI / 180 - this.longitude * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(this.latitude * Math.PI / 180) * Math.cos(latitude * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
//        return Math.sqrt(Math.pow(this.latitude-latitude,2) + Math.pow(this.longitude-longitude, 2));
    }

    public void setImageLocalStorageLocation(String imageLocalStorageLocation) {
        this.imageLocalStorageLocation = imageLocalStorageLocation;
    }

    public void setAudioLocalStorageLocation(String audioLocalStorageLocation) {
        this.audioLocalStorageLocation = audioLocalStorageLocation;
    }
}