package com.andrewtakao.alight;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.SeekBar;

import com.andrewtakao.alight.databinding.ActivityChangingTourBinding;
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

public class ChangingTourActivity extends AppCompatActivity implements SensorEventListener{

//    public static StorageReference mStorageRef;
//    public static StorageReference mAudioRef;
//    public static StorageReference mImageRef;
    public static HashMap<String, POI> mPOIHashMap;
    public static HashMap<String, POI> mFillerPOIHashMap;
//    private ChildEventListener mImagesListener;
    public static DatabaseReference mDatabaseRef;
//    DataSnapshot firstChildSnapshot;
//    DataSnapshot secondChildSnapshot;

    private ActivityChangingTourBinding binding;
    private String busRoute;
    private final String TAG = ChangingTourActivity.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    public static String currentKey;

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

    //Dao Database
//    private static PoiDatabase db;

    //Animation
    ObjectAnimator scaleDown;

    //Compass
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    Float azimuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //This works, amazing
//        Log.d(TAG, "angle should be 180 = " + angleFromCoordinate(43.7007, -71.1058, 42.5157, -71.1345));
//        Log.d(TAG, "angle should be 90 = " + angleFromCoordinate(43.7007, -71.1058, 42.5157, -71.1345));

        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_changing_tour);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocation(view);
            }
        });

//        // Get the ActionBar here to configure the way it behaves.
        final ActionBar ab = getSupportActionBar();
//        ab.setHomeAsUpIndicator(R.drawable.ic_menu); // set a custom icon for the default home button
        ab.setDisplayShowHomeEnabled(false); // show or hide the default home button
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowCustomEnabled(true); // enable overriding the default toolbar layout
        ab.setDisplayShowTitleEnabled(false); // disable the default title element here (for centered title)

        //Initialize
        mContext = this;
        currentKey = "";
        mPOIHashMap = new HashMap<>();
        mFillerPOIHashMap = new HashMap<>();

        binding.nextPoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPOI();
            }
        });

        //Get bus route
        Intent intent = getIntent();
        busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);

        //Compass
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mDatabaseRef = MainActivity.routesRef.child(busRoute);
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
                                            (String) poiChildSnapshot.child("audioLocalStorageLocation").getValue(),
                                            (String) poiChildSnapshot.child("image").getValue(),
                                            (String) poiChildSnapshot.child("imageLocalStorageLocation").getValue(),
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
                                                (String) poiChildSnapshot.child("audioLocalStorageLocation").getValue(),
                                                (String) poiChildSnapshot.child("image").getValue(),
                                                (String) poiChildSnapshot.child("imageLocalStorageLocation").getValue(),
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
                Log.d(TAG, "onLocationChanged");
                makeUseOfNewLocation(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
                getLastLocation();
                Log.d(TAG, "onProviderEnabled");
            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        //TODO toggle this to enable location
        checkPermission();
//
//        startGlowing();
//        stopGlowing();

//Compass
//        mCustomDrawableView = new CustomDrawableView(this);
//        setContentView(mCustomDrawableView);
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
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (mMediaPlayer!=null && mMediaPlayer.isPlaying())
        mMediaPlayer.stop();
//        mImagesDatabaseRef.removeEventListener(mImagesListener);

        //Compass
        mSensorManager.unregisterListener(this);
    }

    public void playFiller(View view) {
        Random rand = new Random();
        int n = rand.nextInt(mFillerPOIHashMap.size());
        int count = 0;
        for (POI poi : mFillerPOIHashMap.values()) {
            if (count == n) {
                currentKey = poi.imageName;
                nextPOI();
                break;
            }
            count += 1;
        }
    }

    public class CustomDrawableView extends View {
        Paint paint = new Paint();
        public CustomDrawableView(Context context) {
            super(context);
            paint.setColor(0xff00ff00);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
        };

        protected void onDraw(Canvas canvas) {
            int width = getWidth();
            int height = getHeight();
            int centerx = width/2;
            int centery = height/2;
            canvas.drawLine(centerx, 0, centerx, height, paint);
            canvas.drawLine(0, centery, width, centery, paint);
            // Rotate the canvas with the azimut
            if (azimuth != null)
                canvas.rotate(-azimuth*360/(2*3.14159f), centerx, centery);
            paint.setColor(0xff0000ff);
            canvas.drawLine(centerx, -1000, centerx, +1000, paint);
            canvas.drawLine(-1000, centery, 1000, centery, paint);
            canvas.drawText("N", centerx+5, centery-10, paint);
            canvas.drawText("S", centerx-10, centery+15, paint);
            paint.setColor(0xff00ff00);
        }
    }

    CustomDrawableView mCustomDrawableView;

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
                azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
//                Log.d(TAG, "Azimuth = " + azimuth);
            }
        }
//        mCustomDrawableView.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startGlowing() {
        binding.glowView.setVisibility(View.VISIBLE);
        if (scaleDown == null) {
            scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    binding.glowView,
                    PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.1f));
            scaleDown.setDuration(700);

            scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
        }

        scaleDown.start();
    }

    private void stopGlowing() {
        binding.glowView.setVisibility(View.INVISIBLE);
        if (scaleDown != null) {
            scaleDown.end();
        }
    }
    
    private void addImage(String key) {
        POI poi = mPOIHashMap.get(key);
        if (poi == null || poi.imageLocalStorageLocation == null) {
            poi = mFillerPOIHashMap.get(key);
        }
        if (poi == null || poi.imageLocalStorageLocation == null) {
            Log.d(TAG, "addImage-- poi.imageLocalStorageLocation == null");
            Log.d(TAG, "addImage-- failed for key = " + key);
            return;
        }
        final String fileName = poi.imageLocalStorageLocation;
        Log.d(TAG, "addImage-- filename = " + fileName);

        binding.changingTourBackgroundImage.setImageDrawable(null);
        if (fileName != null) {
            Log.d(TAG, "addImage-- Uri.parse(fileName) = " + Uri.parse(fileName));

            Picasso.with(mContext).load(new File(fileName))
//                    .placeholder(R.drawable.profile_wall_picture)
//                    .resize(binding.changingTourBackgroundImage.getWidth(), binding.changingTourBackgroundImage.getHeight())
                    .fit()
                    .centerCrop()
                    .into(binding.changingTourBackgroundImage);
        } else {
            Log.d(TAG, "addImage-- fileName is null, downloading image");
        }


        String location = poi.latitude + ", " + poi.longitude;

        binding.changingTourBackgroundImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMediaButtons();
                //Make unclickable
                binding.changingTourBackgroundImage.setClickable(false);
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        hideMediaButtons();
                        binding.changingTourBackgroundImage.setClickable(true);
                    }
                };
                handler = new Handler();
                handler.postDelayed(runnable, 3000);
            }
        });
    }

    private void addAudio(String key) {
        Log.d(TAG, "addAudio");
        String fileName;
        POI poi = mPOIHashMap.get(key);
        if (poi == null) {
            poi = mFillerPOIHashMap.get(key);
        }
        if (poi == null) {
            Log.d(TAG, "addAudio-- poi == null");
            Log.d(TAG, "addAudio-- failed for key = " + key);
            return;
        }
        fileName = poi.audioLocalStorageLocation;

        if (mMediaPlayer!=null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        }

        if (fileName != null) {
            mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
            try {
                setMediaPlayer(Uri.parse(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaPlayer.start();
            setPlayPauseButton();

            //TODO commenting these lines might break something
//            Location lastKnownLocation = getLastKnownLocation();
//
//            Log.d(TAG, "lastKnownLocation = " + lastKnownLocation);
//            makeUseOfNewLocation(lastKnownLocation);
        }
    }

    //Checks whether you have permission, then get's the last known location.
    public void getLastLocation() {
        Log.d(TAG, "getLastLocation");

        //TODO Choose between GPS and network provider
        String locationProvider = LocationManager.GPS_PROVIDER;
//        String locationProvider = LocationManager.NETWORK_PROVIDER;

// Or use LocationManager.GPS_PROVIDER
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLastLocation-- you don't have permission to access gps");
            return;
        }

//        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

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

    public void getLocation(View view) {
        Log.d(TAG, "getLocation pressed");
        //reset currentKey so it will snap to correct location
        currentKey = "";
        getLastLocation();
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
        Log.d(TAG, "makeUseOfNewLocation");
        if (location == null) {
            Log.d(TAG, "location == null");
            return;
        }
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
//        Log.d(TAG, "reached here");

//        double minDistance = 1000;
//        Choosing this as minDistance will show closest POI, as opposed to the closest POI within 1000m
        double minDistance = 100000;
        POI closestPOI = new POI();
        for (POI POI : mPOIHashMap.values()) {
            if (POI.distanceFrom(Double.parseDouble(latitude), Double.parseDouble(longitude)) < minDistance) {
                minDistance = POI.distanceFrom(Double.parseDouble(latitude), Double.parseDouble(longitude));
                closestPOI = POI;
            }
        }
        binding.directionToolbar.setText((int)minDistance+"m");
//        Log.d(TAG, "and here");
        if (closestPOI.imageName == null) {
            Log.d(TAG, "closestPOI.imagename is null");
            return;
        }
//        Log.d(TAG, "finally here");

        if (currentKey.equals(closestPOI.imageName)) {
            Log.d(TAG, "makeUseOfNewLocation-- currentKey = " + currentKey);
            Log.d(TAG, "makeUseOfNewLocation-- closestPoi.imageName = " + closestPOI .imageName);
            if (currentKey==null || currentKey.equals("")) {
                Log.d(TAG, "makeUseOfNewLocation-- currentKey = " + currentKey);
                return;
            }

            //TODO check if button is glowing

            else if (binding.glowView.getVisibility()==View.VISIBLE) {
                if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "makeUseOfNewLocation-- waiting for mediaplayer to stop");
                    return;
                }
                nextPOI();
            }
            //For debugging purposes. Otherwise, comment this out.
//            addImage(currentKey);
            //Do nothing
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
                //Don't need this if it's an image
//                binding.nextPoi.setText("Next: " + userFriendlyName(closestPOI.imageName));
//                binding.nextPoi.setVisibility(View.VISIBLE);
                startGlowing();
            }

            if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                Log.d(TAG, "makeUseOfNewLocation-- waiting for mediaplayer to stop");
                return;
            }
            else {
                nextPOI();
            }


            //DEBUG ONLY
//                binding.closestPoi.setText(closestPOI.imageName);

        }
        //DEBUG ONLY
//            binding.location.setText(currentLocation);

    }

    private void nextPOI() {
        Log.d(TAG, "nextPOI");
        stopGlowing();
//        binding.nextPoi.setVisibility(View.GONE);
        addImage(currentKey);
        addAudio(currentKey);
        Log.d(TAG, "nextPOI- currentKey = " + currentKey);
        binding.closestPoiToolbar.setText(userFriendlyName(currentKey));
    }

    // Checks if user has enabled permission. If they have, get the last location.
    private void checkPermission() {
        Log.d(TAG, "checkPermission--");
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
        } else {
            Log.d(TAG, "checkPermission-- getLastLocation(), then locationmanager.requestLocationupdates()");
//            getLastLocation();

            //TODO Choose between GPS and network provider
            //TODO listen to both GPS AND Network, then use timestamps to find most recent

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }


    private String userFriendlyName(String name) {
        if (name.indexOf("*") > 0) {

            name = name.substring(0, name.indexOf("*"));

        }
        return name.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
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
                    setPlayPauseButton();
                }
            });
        } else {
            binding.ibStart.setImageResource(R.drawable.play);
            binding.ibStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startMusic(view);
                    setPlayPauseButton();
                }
            });
        }
    }

    public void startMusic(View v) {
        if (mMediaPlayer!=null&&!mMediaPlayer.isPlaying()){
            mMediaPlayer.start();
        }
    }

    public void pauseMusic(View v) {
        if (mMediaPlayer!=null&&mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
        }
    }


    public void rewindMusic(View v) {
        if (mMediaPlayer!=null) {
            int position = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.seekTo(position - mSkipTime);
        }
    }

    public void forwardMusic(View v) {
        if (mMediaPlayer!=null) {
            int position = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.seekTo(position + mSkipTime);
        }
    }


    public void updateBar() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try { while (!isInterrupted()) {
                    Thread.sleep(mBarUpdateInterval);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { binding.sbSong.setProgress(mMediaPlayer.getCurrentPosition());}
                    }); }
                } catch (InterruptedException e) { } }
        };
        t.start();
    }

    public void setMediaPlayer(Object object) throws IOException {
        mMediaPlayer = new MediaPlayer();
        mMetaRetriever = new MediaMetadataRetriever();

        if (object instanceof Uri) {
//            Log.d(TAG, "Uri");
            mMetaRetriever.setDataSource(this, (Uri) object);
            mMediaPlayer.setDataSource(this, (Uri) object);
            mMetaRetriever.setDataSource(this, (Uri) object);
        }
        else {
            Log.d(TAG, "What's the instanceof");
        }

        mMediaPlayer.prepare();

        mDuration = Integer.parseInt(mMetaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//            mStartStopButton.setVisibility(View.VISIBLE);
//            mStopButton.setVisibility(View.VISIBLE);
//        binding.sbSong.setVisibility(View.VISIBLE);
        binding.sbSong.setMax(mDuration);
        binding.tvDuration.setText(convertToMinutesAndSeconds(mDuration));

        binding.sbSong.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.tvTime.setText(convertToMinutesAndSeconds(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pauseMusic(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMediaPlayer.seekTo(seekBar.getProgress());
                startMusic(null);
            }
        });

        updateBar();
        startMusic(null);
    }

    public static String convertToMinutesAndSeconds(int milliseconds) {
        int seconds = milliseconds/1000;
        int minutes = seconds/60;
        int leftOverSeconds = seconds - minutes*60;
        String colon = ":";
        if (leftOverSeconds<10) {
            colon = ":0";
        }
        String returnString = (minutes+colon+leftOverSeconds);
        return returnString;

    }

    public void showMediaButtons() {
        binding.tvTime.setVisibility(View.VISIBLE);
        binding.ibStart.setVisibility(View.VISIBLE);
        binding.ibForward.setVisibility(View.VISIBLE);
        binding.ibRewind.setVisibility(View.VISIBLE);
        binding.tvDuration.setVisibility(View.VISIBLE);
        binding.sbSong.setVisibility(View.VISIBLE);
    }

    public void hideMediaButtons() {
        binding.tvTime.setVisibility(View.GONE);
        binding.ibStart.setVisibility(View.GONE);
        binding.ibForward.setVisibility(View.GONE);
        binding.ibRewind.setVisibility(View.GONE);
        binding.tvDuration.setVisibility(View.GONE);
        binding.sbSong.setVisibility(View.GONE);
    }
}
