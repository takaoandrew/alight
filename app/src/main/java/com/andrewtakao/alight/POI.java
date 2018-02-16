package com.andrewtakao.alight;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/13/18.
 */

public class POI {
    public String imageName;
    public double latitude;
    public double longitude;
    public String localStorageLocation;
    public String audioLocalStorageLocation;
    public int order;

    public POI() {

    }

    public POI(String imageName, double latitude, double longitude, int order) {
        this.imageName = imageName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.order = order;
    }

    public double distanceFrom(double latitude, double longitude) {
        return Math.sqrt(Math.pow(this.latitude-latitude,2) + Math.pow(this.longitude-longitude, 2));
    }

    public void setLocalStorageLocation(String localStorageLocation) {
        this.localStorageLocation = localStorageLocation;
    }

    public void setAudioLocalStorageLocation(String audioLocalStorageLocation) {
        this.audioLocalStorageLocation = audioLocalStorageLocation;
    }
}