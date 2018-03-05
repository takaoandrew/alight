package com.andrewtakao.alight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import com.andrewtakao.alight.databinding.ActivityOrderedTourBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OrderedTourActivity extends AppCompatActivity implements SensorEventListener {

    public static StorageReference mStorageRef;
    public static StorageReference mAudioRef;
    public static StorageReference mImageRef;
    public static HashMap<String, POI> mPOIHashMap;
    private ChildEventListener mImagesListener;
    public static DatabaseReference mImagesDatabaseRef;
    DataSnapshot firstChildSnapshot;

    private ActivityOrderedTourBinding binding;
    private String busRoute;
    private final String TAG = OrderedTourActivity.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    public static String currentKey;

    //RecyclerView
    private POIAdapter mPOIAdapter;

    //GPS
    public static Context mContext;
    LocationManager locationManager;
    LocationListener locationListener;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    //GPS test
    LocationManager mLocationManager;

    //Audio
    public static MediaPlayer mMediaPlayer;

    //Dao Database
    private static AppDatabase db;

    //Smooth Scroller
    LinearLayoutManager layoutManager;
    RecyclerView.SmoothScroller smoothScroller;
    RecyclerView.OnDragListener disabler;

    //Sensors
    private SensorManager mSensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    SensorEventListener sensorEventListener;


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
//        mSensorManager.registerListener(sensorEventListener , Sensor.TYPE_ACCELEROMETER,
//                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
//        mSensorManager.registerListener(this, Sensor.TYPE_MAGNETIC_FIELD,
//                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (Integer.valueOf((event.sensor).toString()) == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (Integer.valueOf((event.sensor).toString()) == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
    }

    //Direction
//    float[] mGravity;
//    float[] mGeomagnetic;
//    float azimut;
//
//    private SensorManager mSensorManager;
//    private Sensor mAccelerometer;
//
//    protected void onResume() {
//        super.onResume();
//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//    }
//
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//    }
//
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            Log.d(TAG, "Accelerometer");
//            mGravity = event.values;
//        }
//
//        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            Log.d(TAG, "Magnetic fields");
//            mGeomagnetic = event.values;
//        }
//
//        if (mGravity != null && mGeomagnetic != null) {
//            Log.d(TAG, "nonnull values");
//            float R[] = new float[9];
//            float I[] = new float[9];
//
//            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {
//
//                // orientation contains azimut, pitch and roll
//                float orientation[] = new float[3];
//                SensorManager.getOrientation(R, orientation);
//
//                azimut = orientation[0];
//                Log.d(TAG, "azimut = " + azimut);
//            }
//        }
//    }
//



    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
//        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
//        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        SensorEventListener _SensorEventListener=   new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
//        mSensorManager.registerListener(_SensorEventListener , mAccelerometer, SensorManager.SENSOR_DELAY_UI);

        //This works, amazing
        Log.d(TAG, "angle should be 180 = " + angleFromCoordinate(43.7007, -71.1058, 42.5157, -71.1345));
        Log.d(TAG, "angle should be 90 = " + angleFromCoordinate(43.7007, -71.1058, 42.5157, -71.1345));


        Log.d(TAG, "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" +
                "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%onCreate onBin");
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_ordered_tour);

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

        //Set smooth scroller

        smoothScroller = new LinearSmoothScroller(mContext) {
            @Override protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };


        //Initialize Adapter
        mPOIAdapter =  new POIAdapter(this, new ArrayList<>(mPOIHashMap.values()));
        layoutManager = new LinearLayoutManager(this) {
//            @Override
//            public boolean canScrollVertically() {
//                return false;
////                return super.canScrollHorizontally();
//            }
        };
//        CustomLinearLayoutManager customLayoutManager = new CustomLinearLayoutManager(mContext,LinearLayoutManager.VERTICAL,false);
        binding.rvPois.setLayoutManager(layoutManager);
        binding.rvPois.setAdapter(mPOIAdapter);

        disabler = new RecyclerViewDisabler();

        binding.rvPois.setOnDragListener(disabler);        // disables scolling
// do stuff while scrolling is disabled

        //Get bus route
        Intent intent = getIntent();
        busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);
        Log.d(TAG, "busRoute = "+busRoute);

        mStorageRef = FirebaseStorage.getInstance().getReference("routes").child(busRoute);

        //run first time only
        if (db == null) {
            Log.d(TAG, "Creating database");
            db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "database-name").allowMainThreadQueries().build();

        }
        Log.d(TAG, "size of database is " + db.poiDao().getAll(busRoute).size());

        if (db.poiDao().getAll(busRoute).size() > 0) {
            Log.d(TAG, "Setting mPOIHashMap from local database!");
            for (POI databasePoi : db.poiDao().getAll(busRoute)) {
                Log.d(TAG, "databasePoi image name is " + databasePoi.imageName);
                mPOIHashMap.put(databasePoi.imageName, databasePoi);
            }
            mPOIAdapter.updateAdapter(new ArrayList<POI>(mPOIHashMap.values()));
            mPOIAdapter.notifyDataSetChanged();
//            Log.d(TAG, "");
        }

        Log.d(TAG, "Creating and setting listener");

        //Listens to firebase database for changes in route content pointers
        mImagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.hasChildren()) {
                    firstChildSnapshot = dataSnapshot.getChildren().iterator().next();
                    if (mPOIHashMap.containsKey(firstChildSnapshot.getKey())) {
                        Log.d(TAG, "key " + firstChildSnapshot.getKey() + " is already in mPOIHashMap");
                        return;
                    }
                    Log.d(TAG, "firstChildSnapshot.getKey() = " + firstChildSnapshot.getKey());
                    POI addedPoi = new POI(
                        firstChildSnapshot.getKey(),
                        Double.valueOf((String) firstChildSnapshot.child("latitude").getValue()),
                        Double.valueOf((String) firstChildSnapshot.child("longitude").getValue()),
                        Integer.valueOf(dataSnapshot.getKey()),
                        busRoute
                    );

                    db.poiDao().insertAll(addedPoi);
                    mPOIHashMap.put(firstChildSnapshot.getKey(), addedPoi);
//                    mPOIAdapter.updateAdapter(new ArrayList<>(mPOIHashMap.values()));
                    mPOIAdapter = new POIAdapter(mContext, new ArrayList<>(mPOIHashMap.values()));
//                    mPOIAdapter.notifyDataSetChanged();

//                        Log.d(TAG, (String) firstChildSnapshot.child("imageName").getValue());
                    try {
                        addImageToTempFile(firstChildSnapshot.getKey()
//                                    , (String) firstChildSnapshot.child("imageName").getValue()
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Store audio location
                    try {
                        addAudioToTempFile(firstChildSnapshot.getKey());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                else {
//                        Log.d(TAG, "onChildAdded-- this snapshot has no children");
                }


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                mPOIHashMap.remove(dataSnapshot.getKey());
                mPOIAdapter.updateAdapter(new ArrayList<POI>(mPOIHashMap.values()));
                mPOIAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mImagesDatabaseRef = MainActivity.routesRef.child(busRoute);
        mImagesDatabaseRef.addChildEventListener(mImagesListener);

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
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (mMediaPlayer!=null && mMediaPlayer.isPlaying())
        mMediaPlayer.stop();
        mImagesDatabaseRef.removeEventListener(mImagesListener);
        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    public class CustomLinearLayoutManager extends LinearLayoutManager {
        public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);

        }

        // it will always pass false to RecyclerView when calling "canScrollVertically()" method.
        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }

    public class RecyclerViewDisabler implements RecyclerView.OnDragListener {

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            return false;
        }
    }

    private void addAudio(String key) {
//        try {
        Log.d(TAG, "key = " + key);
        Log.d(TAG, "readable key = " + readableKey(key));
        Log.d(TAG, "audio key = " + audioKey(readableKey(key)));
        //Get local file

        String fileName;

        fileName = mPOIHashMap.get(key).audioLocalStorageLocation;
        Log.d(TAG, "addAudio-- fileName = " + fileName);

        if (mMediaPlayer!=null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        }

        if (fileName != null) {
            mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
            mMediaPlayer.start();
        }
    }

    private void addAudioToTempFile(final String key) throws IOException {

        Log.d(TAG, "addAudioToTempFile-- key = " + key);
        Log.d(TAG, "addAudioToTempFile-- readable key = " + readableKey(key));
        Log.d(TAG, "addAudioToTempFile-- audio key = " + audioKey(readableKey(key)));
        //Get local file

        mAudioRef = mStorageRef.child(audioKey(readableKey(key)));

        Log.d(TAG, "addAudioToTempFile-- mAudioRef.getPath() = " + mAudioRef.getPath());

        final File localFile = File.createTempFile(audioKey(readableKey(key)), "");
        Log.d(TAG, "addAudioToTempFile-- localFile = " + localFile);

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addAudioToTempFile-- onSuccess");
                if (db == null) {
                    Log.d(TAG, "The db was null!");
                    return;
                }
                mPOIHashMap.get(key).setAudioLocalStorageLocation(localFile.toString());
                db.poiDao().insertAll(mPOIHashMap.get(key));
                mPOIAdapter.updateAdapter(new ArrayList<POI>(mPOIHashMap.values()));
                mPOIAdapter.notifyDataSetChanged();

//                mPOIList.get(mPOIList.indexOf(key)).setImageLocalStorageLocation(localFile.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addAudioToTempFile-- onFailure");
            }
        });
    }

    private void addImageToTempFile(final String key) throws IOException {

        Log.d(TAG, "addImageToTempFile-- key = " + key);
        Log.d(TAG, "addImageToTempFile-- readable key = " + readableKey(key));
        //Get local file

        mImageRef = mStorageRef.child(readableKey(key));

        Log.d(TAG, "addImageToTempFile-- mImageRef.getPath() = " + mImageRef.getPath());

        //TODO there is a / here before the imageName child. It may not be there in the future and cause errors.
        //For now we get rid of it

        String slashlessKey = key.replace("/", "");
        slashlessKey = slashlessKey.replace("*", ".");

        final File localFile = File.createTempFile(slashlessKey, "");
        Log.d(TAG, "addImageToTempFile-- localFile = " + localFile);

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addImageToTempFile-- onSuccess");

                if (db == null) {
                    Log.d(TAG, "The db was null!");
                    return;
                }
                Log.d(TAG, "Setting imageLocalStorageLocation");
                mPOIHashMap.get(key).setImageLocalStorageLocation(localFile.toString());
                db.poiDao().insertAll(mPOIHashMap.get(key));
                mPOIAdapter.updateAdapter(new ArrayList<POI>(mPOIHashMap.values()));
                mPOIAdapter.notifyDataSetChanged();
//                mPOIList.get(mPOIList.indexOf(key)).setImageLocalStorageLocation(localFile.toString());
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addImageToTempFile-- onFailure");
            }
        });
    }

    private String readableKey(String key) {
        return key.replace("*", ".");
    }

    private String audioKey(String key) {
        key = key.replace(".jpeg", ".mp3");
        key = key.replace(".png", ".mp3");
        key = key.replace(".JPG", ".mp3");
        key = key.replace(".PNG", ".mp3");
        return key.replace(".jpg", ".mp3");
    }

    //Location

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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
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

//    public void playAudio(View view) {
//        Log.d(TAG, "playAudio pressed");
//
//        String fileName;
//
//        if (mPOIHashMap.get(currentKey) != null) {
//            fileName = mPOIHashMap.get(currentKey).audioLocalStorageLocation;
//            Log.d(TAG, "playAudio-- fileName = " + fileName);
//        } else {
//            fileName = null;
//        }
//
////        if (mMediaPlayer!=null) {
////            if (mMediaPlayer.isPlaying()) {
////                mMediaPlayer.stop();
////            }
////
////        }
//        if (mMediaPlayer != null ) {
//            if (fileName != null) {
//                if (mMediaPlayer.isPlaying()) {
//                    mMediaPlayer.stop();
//                }
//                else {
//                    mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
//                    mMediaPlayer.start();
//                }
//
//            }
//        } else {
//            mMediaPlayer = new MediaPlayer();
//        }
//    }

    public void makeUseOfNewLocation(Location location) {
        Log.d(TAG, "makeUseOfNewLocation");
        if (location == null) {
            return;
        }
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());

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
        final POI finalPoi = closestPOI;
        if (closestPOI.imageName != null) {
            if (currentKey.equals(closestPOI.imageName)) {
                if (currentKey.equals("")) {
                    return;
                }
                //For debugging purposes. Otherwise, comment this out.
//            addImage(currentKey);
                //Do nothing
            } else {
                Log.d(TAG,"minDistance = "+ minDistance);
                currentKey = closestPOI.imageName;

                //DEBUG ONLY
//                binding.closestPoi.setText(closestPOI.imageName);

//            addImage(currentKey);
                addAudio(currentKey);
                Log.d(TAG, "index is " + mPOIAdapter.poiArrayList.indexOf(closestPOI));
                //TODO don't need finalPoi anymore since not in a method and doesn't need to be final
                smoothScroller.setTargetPosition(mPOIAdapter.poiArrayList.indexOf(finalPoi));
                binding.rvPois.getLayoutManager().startSmoothScroll(smoothScroller);
//                Log.d(TAG, "position is " + binding.rvPois.position)
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        binding.rvPois.scrollToPosition(mPOIAdapter.poiArrayList.indexOf(finalPoi));
//                    }
//                }, 300);
            }
            //DEBUG ONLY
//            binding.location.setText(currentLocation);


            //Uncomment if you want it to always scroll to current position.
//            binding.rvPois.smoothScrollToPosition(mPOIAdapter.poiArrayList.indexOf(closestPOI));
        }

        binding.closestPoiToolbar.setText(userFriendlyName(currentKey));
        binding.directionToolbar.setText((int) minDistance+"m");

    }

    private void checkPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            Log.d(TAG, "onCreate-- getLastLocation(), then locationmanager.requestLocationupdates()");
            binding.location.setText("");
            getLastLocation();

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
}
