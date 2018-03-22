package com.andrewtakao.alight;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/13/18.
 */

@Entity
public class POI {
//    @PrimaryKey
//    @NonNull
    public String imageName;

//    @ColumnInfo(name = "audio")
    public String audio;
//    @ColumnInfo(name = "image")
    public String image;
//    @ColumnInfo(name = "language")
    public String language;
//    @ColumnInfo(name = "lat")
    public String latitude;
//    @ColumnInfo(name = "longitude")
    public String longitude;
//    @ColumnInfo(name = "purpose")
    public String purpose;
//    @ColumnInfo(name = "route")
    public String route;
//    @ColumnInfo(name = "transcript")
    public String transcript;
//    @ColumnInfo(name = "theme")
    public ArrayList<String> theme;
    public String coordinates;
    public String index;
//    @ColumnInfo(name = "order")
//    public int order;

    public POI() {

    }

    public POI(String imageName, String audio, String coordinates, String image, String index,
               String language, String latitude, String longitude, String purpose, String route, ArrayList<String> theme, String transcript) {
        this.imageName = imageName;
        this.audio = audio;
        this.coordinates = coordinates;
        this.index = index;
        this.image = image;
        this.language = language;
        this.latitude = latitude;
        this.longitude = longitude;
        this.purpose = purpose;
        this.route = route;
        this.theme = theme;
        this.transcript = transcript;
    }

    public double distanceFrom(double latitude, double longitude) {
        double thisLatitude = Double.valueOf(this.latitude);
        double thisLongitude = Double.valueOf(this.longitude);
        // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = latitude * Math.PI / 180 - thisLatitude * Math.PI / 180;
        double dLon = longitude * Math.PI / 180 - thisLongitude * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(thisLatitude * Math.PI / 180) * Math.cos(latitude * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
//        return Math.sqrt(Math.pow(this.latitude-latitude,2) + Math.pow(this.longitude-longitude, 2));
    }
    public double distanceFromBucket(double latitude, double longitude) {
        String thisLongitudeString = this.coordinates.substring(0, this.coordinates.indexOf(",")).replace("*",".");
        String thisLatitudeString = this.coordinates.substring(this.coordinates.indexOf(",")+1).replace("*",".");
        Log.d("test", this.imageName + ", " + thisLongitudeString + ", " + thisLatitudeString);
        double thisLatitude = Double.valueOf(thisLatitudeString);
        double thisLongitude = Double.valueOf(thisLongitudeString);
        // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = latitude * Math.PI / 180 - thisLatitude * Math.PI / 180;
        double dLon = longitude * Math.PI / 180 - thisLongitude * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(thisLatitude * Math.PI / 180) * Math.cos(latitude * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
//        return Math.sqrt(Math.pow(this.latitude-latitude,2) + Math.pow(this.longitude-longitude, 2));
    }
}