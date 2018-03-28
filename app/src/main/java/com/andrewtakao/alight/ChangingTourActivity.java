package com.andrewtakao.alight;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import com.andrewtakao.alight.databinding.ActivityChangingTourBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.andrewtakao.alight.Utils.audioKey;
import static com.andrewtakao.alight.Utils.readableKey;
import static com.andrewtakao.alight.Utils.userFriendlyName;

public class ChangingTourActivity extends AppCompatActivity implements SensorEventListener{

    //Context
    private Context context;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ActivityChangingTourBinding binding;
    public static DatabaseReference mDatabaseRef;

    private static String busRoute;
    private final String TAG = ChangingTourActivity.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    public static ArrayList<String> currentKeys;
    public static String currentKey;
    public static String displayedKey;
    public static HashMap<String, POI> mPOIHashMap;
    public static HashMap<String, POI> mFillerPOIHashMap;
    public static ArrayList<POI> poiHistory;


    //GPS
    public static Context mContext;
    LocationManager locationManager;
    LocationListener locationListener;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    //GPS test
    LocationManager mLocationManager;

    //Audio
    public static MediaPlayer mMediaPlayer;
    MediaMetadataRetriever mMetaRetriever;
    int mDuration;
    private static final int mSkipTime = 1000;
    private static final int mBarUpdateInterval = 1000;
    Handler handler;
    Runnable runnable;

    //Compass
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    Float azimuth;
    Float oldAzimuth;
    double latitude;
    double longitude;;
    double poiLatitude;;
    double poiLongitude;

    //Animation
    ObjectAnimator scaleDown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = getApplicationContext();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_changing_tour);

        //Get bus route
        Intent intent = getIntent();
        busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);
        Log.d(TAG, "onCreate-- Bus route = " + busRoute);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error- no user!", Toast.LENGTH_LONG).show();
        }

        //Initialize
        mContext = this;
        currentKey = "";
        currentKeys = new ArrayList<>();
        mPOIHashMap = new HashMap<>();
        mFillerPOIHashMap = new HashMap<>();
        poiHistory = new ArrayList<>();

        //Language
        if (RoutePreviewActivity.language.equals("Chinese")) {
            binding.nearby.setText(R.string.nearby_ch);
            binding.playFiller.setText(R.string.skip_ch);
        } else {
            binding.nearby.setText(R.string.nearby);
            binding.playFiller.setText(R.string.skip);
        }

        //Compass
        azimuth = 0f;
        latitude = 0;
        longitude = 0;
        poiLatitude = 0;
        poiLongitude = 0;
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mDatabaseRef = RoutePreviewActivity.routesRef.child(busRoute);
        mDatabaseRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot indexChildSnapshot : dataSnapshot.getChildren()) {
                            if (indexChildSnapshot.getKey().equals("filler")) {
                                for (DataSnapshot poiChildSnapshot : indexChildSnapshot.getChildren()) {
                                    //Set POI
                                    Log.d(TAG, "indexChildSnapshot.getKey() = " + indexChildSnapshot.getKey());
                                    Log.d(TAG, "poiChildSnapshot.getKey() = " + poiChildSnapshot.getKey());
                                    POI addedPoi = new POI(
                                            (String) poiChildSnapshot.getKey(),
                                            (String) poiChildSnapshot.child("audio").getValue(),
                                            (String) poiChildSnapshot.child("coordinates").getValue(),
                                            (String) poiChildSnapshot.child("image").getValue(),
                                            (String) poiChildSnapshot.child("index").getValue(),
                                            (String) poiChildSnapshot.child("language").getValue(),
                                            (String) poiChildSnapshot.child("latitude").getValue(),
                                            (String) poiChildSnapshot.child("longitude").getValue(),
                                            (String) poiChildSnapshot.child("purpose").getValue(),
                                            (String) poiChildSnapshot.child("route").getValue(),
                                            (ArrayList<String>) poiChildSnapshot.child("theme").getValue(),
                                            (String) poiChildSnapshot.child("transcript").getValue()
                                    );
                                    Log.d(TAG, "latitude = " + poiChildSnapshot.child("latitude").getValue());
                                    mFillerPOIHashMap.put(poiChildSnapshot.getKey(), addedPoi);
                                }
                            } else {
                                for (DataSnapshot coordinateChildSnapshot: indexChildSnapshot.getChildren()) {
                                    for (DataSnapshot poiChildSnapshot : coordinateChildSnapshot.getChildren()) {

                                        //Set POI
                                        Log.d(TAG, "indexChildSnapshot.getKey() = " + indexChildSnapshot.getKey());
                                        Log.d(TAG, "poiChildSnapshot.getKey() = " + poiChildSnapshot.getKey());
                                        POI addedPoi = new POI(
                                                (String) poiChildSnapshot.getKey(),
                                                (String) poiChildSnapshot.child("audio").getValue(),
                                                (String) poiChildSnapshot.child("coordinates").getValue(),
                                                (String) poiChildSnapshot.child("image").getValue(),
                                                (String) poiChildSnapshot.child("index").getValue(),
                                                (String) poiChildSnapshot.child("language").getValue(),
                                                (String) poiChildSnapshot.child("latitude").getValue(),
                                                (String) poiChildSnapshot.child("longitude").getValue(),
                                                (String) poiChildSnapshot.child("purpose").getValue(),
                                                (String) poiChildSnapshot.child("route").getValue(),
                                                (ArrayList<String>) poiChildSnapshot.child("theme").getValue(),
                                                (String) poiChildSnapshot.child("transcript").getValue()
                                        );
                                        Log.d(TAG, "latitude = " + poiChildSnapshot.child("latitude").getValue());
                                        mPOIHashMap.put(poiChildSnapshot.getKey(), addedPoi);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });


        //Location
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            
            @Override
            public void onLocationChanged(Location location) {
                //Commented out while using button to debug
//                Log.d(TAG, "onLocationChanged");
                makeUseOfNewLocation(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.d(TAG, "onProviderEnabled");
                getLastLocation();
            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        //TODO toggle this to enable location
//        checkPermission();
//        startGlowing();

        //Only have to do this once
        binding.changingTourBackgroundImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMediaButtons();
                //Make unclickable
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        hideMediaButtons();
                    }
                };
                handler = new Handler();
                handler.postDelayed(runnable, 3000);
            }
        });

//        endTourGlow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Compass
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        checkPermission();
        setPlayPauseButton();
        hideMediaButtons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
            pauseMusic(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
            pauseMusic(null);
        }
        //Compass
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public void playFiller(View view) {
        Random rand = new Random();
        if (mFillerPOIHashMap.size()==0) {
            Toast.makeText(context, "No filler content", Toast.LENGTH_SHORT).show();
            return;
        }
        int n = rand.nextInt(mFillerPOIHashMap.size());
        int count = 0;
        for (POI poi : mFillerPOIHashMap.values()) {
            if (count == n) {
                currentKey = poi.imageName;
                nextFillerPOI();
                break;
            }
            count += 1;
        }
    }

    public void nearby(View view) {
        if (binding.glowView.getVisibility()==View.INVISIBLE) {
            return;
        }
        pauseMusic(null);
        if (mMediaPlayer!=null) {
            mMediaPlayer.seekTo(mMediaPlayer.getDuration());
        }
        getLastLocation();
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
//                azimuth = 180/(float)Math.PI*orientation[0]; // orientation contains: azimut, pitch and roll

                azimuth = Float.valueOf(String.valueOf(-180/(float)Math.PI*orientation[0]+angleFromCoordinate(latitude, longitude, poiLatitude, poiLongitude))); // orientation contains: azimut, pitch and roll
//                azimuth = Float.valueOf(String.valueOf(orientation[0]+Math.PI/180*angleFromCoordinate(latitude, longitude, poiLatitude, poiLongitude))); // orientation contains: azimut, pitch and roll
//                Log.d(TAG, "Azimuth = " + azimuth);
                oldAzimuth = azimuth;
                RotateAnimation rotateAnimation = new RotateAnimation(oldAzimuth, azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setInterpolator(new DecelerateInterpolator());
                rotateAnimation.setRepeatCount(0);
                rotateAnimation.setDuration(100);
                rotateAnimation.setFillAfter(true);
                binding.arrow.startAnimation(rotateAnimation);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startGlowing() {
        binding.glowView.setVisibility(View.VISIBLE);
        binding.nearby.setVisibility(View.VISIBLE);
    }

    private void stopGlowing() {
        binding.glowView.setVisibility(View.INVISIBLE);
        binding.nearby.setVisibility(View.INVISIBLE);
    }

    private void addFillerImage(String key) {
        displayedKey = key;

        //For testing endoftour button
//        if (displayedKey.equals("hacker*jpg") || displayedKey.equals("Ecology*jpg")) {
//            binding.alight.setVisibility(View.VISIBLE);
//        }

        POI poi = mFillerPOIHashMap.get(key);
        Log.d(TAG, "addFillerImage-- Bus route = " + busRoute);
        String fileName = (String) context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language+"/"+busRoute+"/filler/"+
                readableKey(key);
//        Log.d(TAG, "addFillerImage-- filename = " + fileName);
//        Log.d(TAG, "addFillerImage-- check fileexists for " + fileName);
        if (poi == null || !fileExist((String) fileName)) {
            Log.d(TAG, "addFillerImage-- failed for key = " + readableKey(key));
            return;
        }

        binding.changingTourBackgroundImage.setImageDrawable(null);
//            Log.d(TAG, "addImage-- Uri.parse(fileName) = " + Uri.parse(fileName));
        Picasso.with(mContext).load(new File(fileName))
                .resize(binding.changingTourBackgroundImage.getWidth(), binding.changingTourBackgroundImage.getHeight())
                .into(binding.changingTourBackgroundImage);
    }
    
    private void addImage(String key, String route) {
        displayedKey = key;

        if (displayedKey.equals("EndOfTour*jpg")) {
            binding.alight.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "addImage-- Bus route = " + route);

        POI poi = mPOIHashMap.get(key);
        String fileName = (String) context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language+"/"+route+"/"+readableKey(key);
        Log.d(TAG, "addImage-- check fileexists for " + fileName);
        if (poi == null || !fileExist(fileName)) {
            Log.d(TAG, "addImage-- failed for key = " + key);
            return;
        }

        binding.changingTourBackgroundImage.setImageDrawable(null);
        Log.d(TAG, "addImage-- Uri.parse(fileName) = " + Uri.parse(fileName));
        Picasso.with(mContext).load(new File(fileName))
//                    .placeholder(R.drawable.profile_wall_picture)
//                    .resize(binding.changingTourBackgroundImage.getWidth(), binding.changingTourBackgroundImage.getHeight())
                .fit()
                .centerCrop()
                .into(binding.changingTourBackgroundImage);

        //TODO visible invisible for friday testing- make invisible
//        binding.arrow.setVisibility(View.VISIBLE);

//        binding.arrow.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
    }

    private void addFillerAudio(String key) {
        Log.d(TAG, "addFillerAudio");
        Log.d(TAG, "addFillerAudio-- Bus route = " + busRoute);
        POI poi = mFillerPOIHashMap.get(key);
        String fileName = (String) context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language
                +"/"+busRoute+"/filler/"+audioKey(readableKey(key));

        if (poi == null) {
            Log.d(TAG, "addFillerAudio-- poi == null");
            Log.d(TAG, "addFillerAudio-- failed for key = " + key);
            return;
        }

        if (mMediaPlayer!=null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        }

        mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
        try {
            setMediaPlayer(Uri.parse(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        startMusic(null);

    }

    private void addAudio(String key, String route) {
        Log.d(TAG, "addAudio");
        Log.d(TAG, "addAudio-- Bus route = " + route);
        String fileName = (String) context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language
                +"/"+route+"/"+audioKey(readableKey(key));
        POI poi = mPOIHashMap.get(key);
        if (poi == null) {
            Log.d(TAG, "addAudio-- poi == null");
            Log.d(TAG, "addAudio-- failed for key = " + key);
            return;
        }
        if (mMediaPlayer!=null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        }
        mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
        try {
            setMediaPlayer(Uri.parse(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        startMusic(null);
    }

    //Checks whether you have permission, then get's the last known location.
    public void getLastLocation() {
        Log.d(TAG, "getLastLocation");
//        currentKey = "";

        //TODO Choose between GPS and network provider
//        String locationProvider = LocationManager.GPS_PROVIDER;
//        String locationProvider = LocationManager.NETWORK_PROVIDER;

// Or use LocationManager.GPS_PROVIDER
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLastLocation-- you don't have permission to access gps");
            return;
        }
        // Test 2
        Location lastKnownLocation = getLastKnownLocation();

        Log.d(TAG, "lastKnownLocation = " + lastKnownLocation);
        makeUseOfNewLocation(lastKnownLocation);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    return;

                } else {
                    Log.d(TAG, "permission denied");
                }
            }
        }
    }

    //GPS test 2
    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void makeUseOfNewLocation(Location location) {
//        Log.d(TAG, "makeUseOfNewLocation");
        latitude = location.getLatitude();
        longitude = location.getLongitude();
//        double minDistance = 1000;
//        Choosing this as minDistance will show closest POI, as opposed to the closest POI within 1000m
        double minDistance = 100000;
        POI closestPOI = new POI();
        ArrayList<POI> closestPOIs = new ArrayList<>();
        for (POI POI : mPOIHashMap.values()) {
            Log.d(TAG, "POI.imageName = " + POI.imageName);
            if (POI.distanceFrom(latitude, longitude) < minDistance) {
//            if (POI.distanceFromBucket(Double.parseDouble(latitude), Double.parseDouble(longitude)) < minDistance) {
                Log.d(TAG, "New minimum!");
                minDistance = POI.distanceFrom(latitude, longitude);
//                minDistance = POI.distanceFromBucket(Double.parseDouble(latitude), Double.parseDouble(longitude));
                closestPOIs = new ArrayList<>();
                closestPOI = POI;
                closestPOIs.add(closestPOI);
            }
//            else if (POI.distanceFromBucket(Double.parseDouble(latitude), Double.parseDouble(longitude)) == minDistance) {
            else if (POI.distanceFrom(latitude, longitude) == minDistance) {
                Log.d(TAG, "Adding to bucket!");
                closestPOI = POI;
                closestPOIs.add(closestPOI);
            }
        }
        if (closestPOI.latitude == null) {
            Log.d(TAG, "closestPOI is null");
            return;
        }
        poiLatitude = Double.valueOf(closestPOI.latitude);
        poiLongitude = Double.valueOf(closestPOI.longitude);
        binding.directionToolbar.setText((int) minDistance+"m");
        if (closestPOI.imageName == null) {
            Log.d(TAG, "closestPOI.imagename is null");
            return;
        }
        if (currentKey.equals(closestPOI.imageName)) {
            Log.d(TAG, "makeUseOfNewLocation-- closestPoi.imageName = " + closestPOI .imageName);
            if (currentKey==null || currentKey.equals("")) {
                Log.d(TAG, "makeUseOfNewLocation-- currentKey = " + currentKey);
                return;
            }
            else if (binding.glowView.getVisibility()==View.VISIBLE) {
                if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "makeUseOfNewLocation-- waiting for mediaplayer to stop");
                    return;
                }
                //If paused towards the end of the media
                if (mMediaPlayer!=null && mMediaPlayer.getCurrentPosition()>=mMediaPlayer.getDuration()-1000) {
                    nextPOI();
                } else if (mMediaPlayer!=null) {
                    Log.d(TAG, "current position = " + mMediaPlayer.getCurrentPosition() +
                            " and duration = " + mMediaPlayer.getDuration());
                }
            }
        } else {
            Log.d(TAG,"minDistance = "+ minDistance);
            Log.d(TAG, "closestPOI.imageName = " + closestPOI.imageName);
            currentKey = closestPOI.imageName;
            if (currentKey==null || currentKey.equals("")) {
                Log.d(TAG, "makeUseOfNewLocation-- currentKey = " + currentKey);
                return;
            }
            //Will always be visible
            //TODO check if button is not glowing, then make it glow
            if (binding.glowView.getVisibility()==View.INVISIBLE) {
                startGlowing();
            }

            if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                Log.d(TAG, "makeUseOfNewLocation-- waiting for mediaplayer to stop");
                return;
            }
            else {
                nextPOI();
            }
        }
    }

    private void nextPOI() {
        Log.d(TAG, "nextPOI");
        stopGlowing();
        Log.d(TAG, "nextPOI-- bus route = " + busRoute);
        addImage(currentKey, busRoute);
        addAudio(currentKey, busRoute);
        Log.d(TAG, "nextPOI-- currentKey = " + currentKey);
        binding.closestPoiToolbar.setText(userFriendlyName(currentKey));
    }

    private void nextFillerPOI() {
        Log.d(TAG, "nextFillerPOI");
        stopGlowing();
        addFillerImage(currentKey);
        addFillerAudio(currentKey);
        Log.d(TAG, "nextFillerPOI- currentKey = " + currentKey);
        binding.closestPoiToolbar.setText(userFriendlyName(currentKey));
    }

    // Checks if user has enabled permission. If they have, get the last location.
    private void checkPermission() {
        Log.d(TAG, "checkPermission--");
        //No permission has been previously granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkPermission-- permission not granted");


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
        //Permission has been granted before
        else {
            Log.d(TAG, "checkPermission-- Locationmanager.requestLocationupdates()");

            //TODO Choose between GPS and network provider
            //TODO listen to both GPS AND Network, then use timestamps to find most recent

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    public void like(View view) {
        POI poi = mPOIHashMap.get(displayedKey);
        if (poi == null) {
            poi = mFillerPOIHashMap.get(displayedKey);
            Log.d(TAG, "filler! poi.index = " + poi.index);
            if (poi.index == null) {
                Toast.makeText(context, "Not set up to be liked, update database", Toast.LENGTH_SHORT).show();
                return;
            }
            mDatabaseRef.child(poi.index).child(displayedKey).child("likes").child(user.getUid()).setValue("true");
        } else {
            if (poi.index == null) {
                Toast.makeText(context, "poi.index is null!", Toast.LENGTH_LONG).show();
            }
//        if (poi.coordinates.equals("0,0"))
            mDatabaseRef.child(poi.index).child(poi.coordinates).child(displayedKey).child("likes").child(user.getUid()).setValue("true");
        }
    }

    public void dislike(View view) {
        POI poi = mPOIHashMap.get(displayedKey);
        if (poi == null) {
            poi = mFillerPOIHashMap.get(displayedKey);
            if (poi.index == null) {
                Toast.makeText(context, "Not set up to be liked, update database", Toast.LENGTH_SHORT).show();
                return;
            }
            mDatabaseRef.child(poi.index).child(displayedKey).child("likes").child(user.getUid()).setValue("false");
        } else {
            Log.d(TAG, "not filler! poi.index = " + poi.index);
            if (poi.index == null) {
                Toast.makeText(context, "poi.index is null!", Toast.LENGTH_LONG).show();
            }

//        if (poi.coordinates.equals("0,0"))
            mDatabaseRef.child(poi.index).child(poi.coordinates).child(displayedKey).child("likes").child(user.getUid()).setValue("false");
        }
    }


    private double angleFromCoordinate(double lat1, double long1, double lat2,
                                       double long2) {

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    public void setPlayPauseButton() {
        if (mMediaPlayer==null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            binding.ibStart.setImageResource(R.drawable.pause);
            binding.ibStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pauseMusic(view);
                }
            });
        } else {
            binding.ibStart.setImageResource(R.drawable.play);
            binding.ibStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startMusic(view);
                }
            });
        }
    }

    public void startMusic(View v) {
        Log.d(TAG, "startMusic");
        Log.d(TAG, "View v = " + v);
        if (mMediaPlayer!=null&&!mMediaPlayer.isPlaying()){
            mMediaPlayer.start();
        }
        setPlayPauseButton();
    }

    public void pauseMusic(View v) {
        Log.d(TAG, "pauseMusic");
        if (mMediaPlayer!=null&&mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
        }
        setPlayPauseButton();
    }


    public void rewindMusic(View v) {
        if (mMediaPlayer!=null) {
            int position = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.seekTo(position - mSkipTime);
        }
    }

    public void setMediaPlayer(Object object) throws IOException {
        mMediaPlayer = new MediaPlayer();
        mMetaRetriever = new MediaMetadataRetriever();

        if (object instanceof Uri) {
            Log.d(TAG, "Uri object = " + object);
            mMetaRetriever.setDataSource(this, (Uri) object);
            mMediaPlayer.setDataSource(this, (Uri) object);
            mMetaRetriever.setDataSource(this, (Uri) object);
        }
        else {
            Log.d(TAG, "What's the instanceof");
        }
        mMediaPlayer.prepare();
        mDuration = Integer.parseInt(mMetaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        startMusic(null);
    }

    public void showMediaButtons() {
        binding.ibStart.setVisibility(View.VISIBLE);
        binding.ibRewind.setVisibility(View.VISIBLE);
        binding.ibLike.setVisibility(View.VISIBLE);
    }

    public void hideMediaButtons() {
        binding.ibStart.setVisibility(View.GONE);
        binding.ibRewind.setVisibility(View.GONE);
        binding.ibLike.setVisibility(View.GONE);
    }


    public boolean fileExist(String fname){
        File file = new File(fname);
        return file.exists();
    }

    public void endTour(View view) {
        Intent intent = new Intent(context, EndOfTourActivity.class);
        startActivity(intent);
    }

    private void endTourGlow() {
        if (scaleDown == null) {
            scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    binding.alight,
                    PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.1f));
            scaleDown.setDuration(700);

            scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
        }
        scaleDown.start();
    }
}
