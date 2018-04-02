package com.andrewtakao.alight;

import android.arch.persistence.room.Entity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/13/18.
 */

@Entity
public class Stop {

    public String address;
    public String id;
    public String coordinates;

    public Stop() {

    }

    public Stop(String address, String id, String coordinates) {
        this.address = address;
        this.id = id;
        this.coordinates = coordinates;
    }

    public double distanceFrom(double latitude, double longitude) {
        String thisLongitudeString = this.coordinates.substring(0, this.coordinates.indexOf(",")).replace("*",".");
        String thisLatitudeString = this.coordinates.substring(this.coordinates.indexOf(",")+1).replace("*",".");

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

    public String getLatitude() {
        return this.coordinates.substring(this.coordinates.indexOf(",")+1).replace("*",".");
    }

    public String getLongitude() {
        return this.coordinates.substring(0, this.coordinates.indexOf(",")).replace("*",".");
    }

    public double distanceFromBucket(double latitude, double longitude) {
        String thisLongitudeString = this.coordinates.substring(0, this.coordinates.indexOf(",")).replace("*",".");
        String thisLatitudeString = this.coordinates.substring(this.coordinates.indexOf(",")+1).replace("*",".");
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