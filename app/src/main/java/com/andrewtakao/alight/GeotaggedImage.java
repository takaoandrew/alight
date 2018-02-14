package com.andrewtakao.alight;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/13/18.
 */

public class GeotaggedImage {
    public String imageName;
    public double latitude;
    public double longitude;
    public String localStorageLocation;
    public String audioLocalStorageLocation;
    public String audio;

    public GeotaggedImage(String imageName, double latitude, double longitude) {
        this.imageName = imageName;
        this.latitude = latitude;
        this.longitude = longitude;
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