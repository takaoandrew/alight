package com.andrewtakao.alight;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

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
//    @ColumnInfo(name = "imageLocalStorageLocation")
    public String imageLocalStorageLocation;
//    @ColumnInfo(name = "audioLocalStorageLocation")
    public String audioLocalStorageLocation;
//    @ColumnInfo(name = "order")
//    public int order;

    public POI() {

    }

    public POI(String imageName, String audio, String audioLocalStorageLocation, String image, String imageLocalStorageLocation,
               String language, String latitude, String longitude, String purpose, String route, ArrayList<String> theme, String transcript) {
        this.imageName = imageName;
        this.audio = audio;
        this.audioLocalStorageLocation = audioLocalStorageLocation;
        this.imageLocalStorageLocation = imageLocalStorageLocation;
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

    public void setImageLocalStorageLocation(String imageLocalStorageLocation) {
        this.imageLocalStorageLocation = imageLocalStorageLocation;
    }

    public void setAudioLocalStorageLocation(String audioLocalStorageLocation) {
        this.audioLocalStorageLocation = audioLocalStorageLocation;
    }
//    public void setEnglishAudioLocalStorageLocation(String audioLocalStorageLocation) {
//        this.englishAudioLocalStorageLocation = audioLocalStorageLocation;
//    }
}