package com.andrewtakao.alight;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/13/18.
 */

public class GeotaggedImage {
    public String imageName;
    public double latitude;
    public double longitude;

    public GeotaggedImage(String imageName, double latitude, double longitude) {
        this.imageName = imageName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}