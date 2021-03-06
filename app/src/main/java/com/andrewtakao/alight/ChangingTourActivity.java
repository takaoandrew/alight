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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.andrewtakao.alight.databinding.ActivityChangingTourBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static com.andrewtakao.alight.Utils.angleFromCoordinate;
import static com.andrewtakao.alight.Utils.audioKey;
import static com.andrewtakao.alight.Utils.fileExist;
import static com.andrewtakao.alight.Utils.readableKey;
import static com.andrewtakao.alight.Utils.userFriendlyName;

public class ChangingTourActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback {

    //Context
    private Context context;

    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ActivityChangingTourBinding binding;
    public static DatabaseReference mDatabaseRef;

    private String language = "English";
    private static String busRoute = "home";
    private final String TAG = ChangingTourActivity.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    public static HashMap<String, POI> mPOIHashMap;
    public static HashMap<String, POI> mFillerPOIHashMap;
    public static ArrayList<POI> poiHistory;
    //    private final double mMinDistance = 5750.22644;
    private final double mMinDistance = 50;
    private int previouslyFirstCompletelyVisibleItemPosition = -1;

    //GPS
    LocationManager locationManager;
    LocationListener locationListener;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    POITourAdapter mAdapter;

    //Audio
    public static MediaPlayer mMediaPlayer;
    MediaMetadataRetriever mMetaRetriever;
    int mDuration;
    private static final int mSkipTime = 5000;
    MediaPlayer.OnCompletionListener onCompletionListener;
    Handler handler;
    Runnable runnable;

    //Map
    SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private PolylineOptions polylineOptions;
    private MarkerOptions lastMarkerOptions;
    private Marker lastMarker;

    //Compass
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    Float azimuth;
    Float trueNorthAzimuth;
    Float oldAzimuth;
    Float oldTrueNorthAzimuth;
    double latitude;
    double longitude;
//    double poiLatitude;
//    double poiLongitude;

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

        if (intent.hasExtra(BUS_ROUTE_EXTRA)) {
            busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);
        }
        if (intent.hasExtra(LANGUAGE_EXTRA)) {
            language = intent.getStringExtra(LANGUAGE_EXTRA);
        }
        Log.d(TAG, "onCreate-- Bus route = " + busRoute);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error- no user!", Toast.LENGTH_LONG).show();
        }

        //Map
        polylineOptions = new PolylineOptions();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Initialize
        mPOIHashMap = new HashMap<>();
        mFillerPOIHashMap = new HashMap<>();
        poiHistory = new ArrayList<>();

        mAdapter = new POITourAdapter(context, poiHistory);
        binding.rvTourPois.setAdapter(mAdapter);
        binding.rvTourPois.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(binding.rvTourPois);

        binding.rvTourPois.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
//                Log.d(TAG, "onScrolled");
                recyclerViewScrolled();
                showLocation();
            }
        });

        onCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.d(TAG, "onCompletion");
                LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
                int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                if (firstCompletelyVisibleItemPosition < poiHistory.size() - 1) {
                    binding.rvTourPois.smoothScrollToPosition(poiHistory.size() - 1);
                }
            }
        };

        //Compass
        azimuth = 0f;
        trueNorthAzimuth = 0f;
        oldAzimuth = 1000f;
        oldTrueNorthAzimuth = 1000f;
        latitude = 0;
        longitude = 0;
//        poiLatitude = 0;
//        poiLongitude = 0;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        database = Utils.getDatabase();
        mDatabaseRef = database.getReference(language + "/routes").child(busRoute);
        mDatabaseRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot indexChildSnapshot : dataSnapshot.getChildren()) {
                            if (indexChildSnapshot.getKey().equals("stops")) {

                            }
                            else if (indexChildSnapshot.getKey().equals("filler")) {
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
//                                    Log.d(TAG, "key = " + poiChildSnapshot.getKey());
                                    Log.d(TAG, "index = " + poiChildSnapshot.child("index").getValue());
                                    mFillerPOIHashMap.put(poiChildSnapshot.getKey(), addedPoi);
                                }
                            } else {
                                for (DataSnapshot coordinateChildSnapshot : indexChildSnapshot.getChildren()) {
                                    String thisLongitudeString = coordinateChildSnapshot.getKey()
                                            .substring(0, coordinateChildSnapshot.getKey().indexOf(",")).replace("*", ".");
                                    String thisLatitudeString = coordinateChildSnapshot.getKey()
                                            .substring(coordinateChildSnapshot.getKey().indexOf(",") + 1).replace("*", ".");
                                    LatLng latLng = new LatLng(Double.valueOf(thisLatitudeString), Double.valueOf(thisLongitudeString));
                                    polylineOptions.add(latLng);
                                    for (DataSnapshot poiChildSnapshot : coordinateChildSnapshot.getChildren()) {

                                        //Set POI
//                                        Log.d(TAG, "indexChildSnapshot.getKey() = " + indexChildSnapshot.getKey());
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
                                        Log.d(TAG, "purpose = " + poiChildSnapshot.child("purpose").getValue());
                                        mPOIHashMap.put(poiChildSnapshot.getKey(), addedPoi);
                                    }
                                }
                            }
                        }
                        setMap(polylineOptions);
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
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };

        //TODO toggle this to enable location
//        checkPermission();
//        startGlowing();
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            pauseMusic(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
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
                LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
                int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                if (firstCompletelyVisibleItemPosition == -1) {
                    return;
                }
                if (poiHistory.size()<=firstCompletelyVisibleItemPosition) {
                    return;
                }
                POI poi = poiHistory.get(firstCompletelyVisibleItemPosition);
                Double poiLatitude = Double.valueOf(poi.latitude);
                Double poiLongitude = Double.valueOf(poi.longitude);

                azimuth = Float.valueOf(String.valueOf(-180 / (float) Math.PI * orientation[0] + angleFromCoordinate(latitude, longitude, poiLatitude, poiLongitude))); // orientation contains: azimut, pitch and roll
                trueNorthAzimuth = Float.valueOf(String.valueOf(-180 / (float) Math.PI * orientation[0])); // orientation contains: azimut, pitch and roll
//                azimuth = Float.valueOf(String.valueOf(orientation[0]+Math.PI/180*angleFromCoordinate(latitude, longitude, poiLatitude, poiLongitude))); // orientation contains: azimut, pitch and roll
//                Log.d(TAG, "Azimuth = " + azimuth);
                //round to nearest 1/16 of 360 degrees
                azimuth = 22.5f*(Math.round(azimuth/22.5f));
                trueNorthAzimuth = 22.5f*(Math.round(trueNorthAzimuth/22.5f));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

    public void recyclerViewScrolled() {
//        Log.d(TAG, "recyclerViewScrolled");
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (firstCompletelyVisibleItemPosition == previouslyFirstCompletelyVisibleItemPosition) {
//            Log.d(TAG, "recyclerViewScrolled-- wasn't changed to a new POI");
            return;
        }
        if (firstCompletelyVisibleItemPosition == -1) {
//            Log.d(TAG, "firstCompletelyVisibleItemPosition = " + firstCompletelyVisibleItemPosition);
            return;
        }
        previouslyFirstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPosition;

        showNewTitle(firstCompletelyVisibleItemPosition);

        setMapImage();
        //Should show current position?
        shouldShowCurrentButton();

        addMarkerToMap(firstCompletelyVisibleItemPosition);

        isLiked(firstCompletelyVisibleItemPosition);

        playAudio(firstCompletelyVisibleItemPosition);

        isAlighting(firstCompletelyVisibleItemPosition);
//        Log.d(TAG, "current position = " + firstCompletelyVisibleItemPosition);
//        Log.d(TAG, "previouslyFirstCompletelyVisibleItemPosition = " + previouslyFirstCompletelyVisibleItemPosition);

        //Check if swiped to last
        if (firstCompletelyVisibleItemPosition == poiHistory.size() - 1) {
            addPOIOnDeck();
        }
    }

    private void addMarkerToMap(int position) {
        if (mMap == null) {
            return;
        }
        POI currentPoi = poiHistory.get(position);
        if (lastMarker != null) {
            lastMarker.remove();
        }
        lastMarkerOptions = new MarkerOptions()
                .position(new LatLng(Double.valueOf(currentPoi.latitude), Double.valueOf(currentPoi.longitude)));

        lastMarker = mMap.addMarker(lastMarkerOptions);
    }

    private void updateCompass() {
        if (azimuth.equals(oldAzimuth)) {
            return;
        }
        oldAzimuth = azimuth;
        oldTrueNorthAzimuth = trueNorthAzimuth;
        RotateAnimation rotateAnimation = new RotateAnimation(oldAzimuth, azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new DecelerateInterpolator());
        rotateAnimation.setRepeatCount(0);
        rotateAnimation.setDuration(1000);
        rotateAnimation.setFillAfter(true);
        RotateAnimation trueNorthRotateAnimation = new RotateAnimation(oldTrueNorthAzimuth, trueNorthAzimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        trueNorthRotateAnimation.setInterpolator(new DecelerateInterpolator());
        trueNorthRotateAnimation.setRepeatCount(0);
        trueNorthRotateAnimation.setDuration(1000);
        trueNorthRotateAnimation.setFillAfter(true);
        binding.arrow.startAnimation(rotateAnimation);
        binding.trueNorthArrow.startAnimation(trueNorthRotateAnimation);
    }

    private void isAlighting(int position) {
        POI currentPoi = poiHistory.get(position);
        Log.d(TAG, "purpose = " + currentPoi.purpose);
        if (currentPoi.purpose.equals("alighting")) {
            binding.ivAlight.setVisibility(View.VISIBLE);
            makeGlow();
        } else {
            binding.ivAlight.setVisibility(View.INVISIBLE);
        }
    }

    public void playAudio(int position) {
        Log.d(TAG, "Position, poiHistory.get(position) = " + position + ", " + poiHistory.get(position));
        POI currentPoi = poiHistory.get(position);
        String fileName = context.getFilesDir().getPath() + "/" + language
                + "/" + busRoute + "/" + audioKey(readableKey(currentPoi.imageName));
        if (!fileExist(fileName)) {
            fileName = context.getFilesDir().getPath() + "/" + language
                    + "/" + busRoute + "/filler/" + audioKey(readableKey(currentPoi.imageName));
            if (!fileExist(fileName)) {
                Toast.makeText(context, "Can't find audio", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        }
        mMediaPlayer = MediaPlayer.create(context, Uri.parse(fileName));
        try {
            setMediaPlayer(Uri.parse(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        startMusic(null);
    }

    private void showNewTitle(int position) {
        String title = userFriendlyName(poiHistory.get(position).imageName);
        binding.closestPoiToolbar.setText(title);
    }

    private void isLiked(int position) {
        //TODO once database is updated
//        if (poiHistory.get(position).likes)
    }

    private void shouldShowCurrentButton() {
        Log.d(TAG, "shouldShowCurrentButton-- ?");
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstCompletelyVisibleItemPosition == -1) {
            Log.d(TAG, "shouldShowCurrentButton-- do nothing, still scrolling");
            return;
        }
        if (firstCompletelyVisibleItemPosition < poiHistory.size() - 1) {
            Log.d(TAG, "shouldShowCurrentButton-- position is less than where we would scroll to");
            if (poiHistory.get(poiHistory.size() - 1).index == null) {
                Log.d(TAG, "shouldShowCurrentButton-- where we would scroll to, index is null. Do nothing");
                return;
            } else if (poiHistory.get(poiHistory.size() - 1).index.equals("filler")) {
                Log.d(TAG, "shouldShowCurrentButton-- Where we would scroll to, index is filler. Make Invisible!");
                binding.btCurrentPoi.setVisibility(View.GONE);
            } else {
                Log.d(TAG, "shouldShowCurrentButton-- Place to scroll to should be closest POI. Make Visible!");
                binding.btCurrentPoi.setVisibility(View.VISIBLE);
            }
        } else {
            Log.d(TAG, "shouldShowCurrentButton-- position is already at or past where we would scroll to. Make Invisible.");
            binding.btCurrentPoi.setVisibility(View.GONE);
        }
    }

    private POI fillerPoi() {
        Random rand = new Random();
        if (mFillerPOIHashMap.size() == 0) {
            Toast.makeText(context, "No filler content", Toast.LENGTH_SHORT).show();
            return null;
        }
        int n = rand.nextInt(mFillerPOIHashMap.size());
        int count = 0;
        for (POI poi : mFillerPOIHashMap.values()) {
            if (count == n) {
                mFillerPOIHashMap.remove(poi.imageName);
                return poi;
            }
            count += 1;
        }
        return null;
    }

    public void replacePOIOnDeck() {
        if (poiHistory.size() == 0) {
            Log.d(TAG, "replacePOIOnDeck-- Nothing to replace");
            return;
        }
        POI poiOnDeck = poiHistory.get(poiHistory.size() - 1);
        double minDistance = mMinDistance;
        POI closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (!poiHistory.contains(poi) && poi.distanceFrom(latitude, longitude) < minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            if (poiOnDeck.index != null && poiOnDeck.index.equals("filler")) {
                Log.d(TAG, "replacePOIOnDeck-- removing filler to add nearby POI");
                //Fillers were not appearing at all when they were put on deck (removed from fillerpoihashmap), then removed from history
                mFillerPOIHashMap.put(poiOnDeck.imageName, poiOnDeck);
                Log.d(TAG, "replacePOIOnDeck-- (fillerReplace) before changing poiHistory, poiHistory.size() = " + poiHistory.size());
                poiHistory.remove(poiOnDeck);
                poiHistory.add(closestPoi);
                Log.d(TAG, "replacePOIOnDeck-- (fillerReplace) after changing poiHistory, poiHistory.size() = " + poiHistory.size());
                mAdapter.notifyDataSetChanged();
            } else if (poiOnDeck.distanceFrom(latitude, longitude) > closestPoi.distanceFrom(latitude, longitude)) {
//                Log.d(TAG, "replacePOIOnDeck-- poiOnDeck is no longer closest");
                Log.d(TAG, "replacePOIOnDeck-- (nearbyReplace) before changing poiHistory, poiHistory.size() = " + poiHistory.size());
                poiHistory.remove(poiOnDeck);
                poiHistory.add(closestPoi);
                Log.d(TAG, "replacePOIOnDeck-- (nearbyReplace) after changing poiHistory, poiHistory.size() = " + poiHistory.size());
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void addPOIOnDeck() {
        double minDistance = mMinDistance;
        POI closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (!poiHistory.contains(poi) && poi.distanceFrom(latitude, longitude) < minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            Log.d(TAG, "adding " + closestPoi.imageName + ", distance = " + minDistance);
            Log.d(TAG, "addPOIOnDeck-- (nearby) before changing poiHistory, poiHistory.size() = " + poiHistory.size());
            poiHistory.add(closestPoi);
            Log.d(TAG, "addPOIOnDeck-- (nearby) after changing poiHistory, poiHistory.size() = " + poiHistory.size());
            mAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "addPOIOnDeck- close poi unavailable, grabbing filler");
            Log.d(TAG, "addPOIOnDeck-- (filler) before changing poiHistory, poiHistory.size() = " + poiHistory.size());
            POI fillerPOI = fillerPoi();
            if (fillerPOI != null) {
                poiHistory.add(fillerPOI);
                mAdapter.notifyDataSetChanged();
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void makeUseOfNewLocation(Location location) {
        Log.d(TAG, "makeUseOfNewLocation");
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        if (poiHistory.size() == 0) {
            Log.d(TAG, "run me once!");
            addFirstPOI();
        } else {
            replacePOIOnDeck();
            //If poiOnDeck is no longer the closest, or of it's filler... replace it
        }
        showLocation();
        //Should show current position?
        shouldShowCurrentButton();
        updateCompass();
    }

    public void showLocation() {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (poiHistory.size() <= firstCompletelyVisibleItemPosition
                || firstCompletelyVisibleItemPosition == -1) {
            return;
        }
        if (poiHistory.get(firstCompletelyVisibleItemPosition).index != null &&
                poiHistory.get(firstCompletelyVisibleItemPosition).index.equals("filler")) {
            binding.distance.setText("filler");
        } else {
            double distance = poiHistory.get(firstCompletelyVisibleItemPosition).distanceFrom(latitude, longitude);
            binding.distance.setText(String.valueOf((int) distance) + "m");
        }
    }

    public void addFirstPOI() {
        binding.ivFindingLocation.setVisibility(View.INVISIBLE);
        double minDistance = mMinDistance;
        POI closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (poi.distanceFrom(latitude, longitude) < minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            Log.d(TAG, "addFirstPOI- close poi is available");
            poiHistory.add(closestPoi);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "addFirstPOI- close poi unavailable, grabbing filler");
            POI fillerPOI = fillerPoi();
            if (fillerPOI != null) {
                poiHistory.add(fillerPOI);
                mAdapter.notifyDataSetChanged();
            }
        }
        mAdapter.notifyDataSetChanged();
        //Now we get the second closest
        minDistance = mMinDistance;
        closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (!poiHistory.contains(poi) && poi.distanceFrom(latitude, longitude) < minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            poiHistory.add(closestPoi);
            mAdapter.notifyDataSetChanged();
        } else {
            POI fillerPOI = fillerPoi();
            if (fillerPOI != null) {
                poiHistory.add(fillerPOI);
                mAdapter.notifyDataSetChanged();
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void currentPOI(View view) {
        if (poiHistory.size() > 1) {
            binding.rvTourPois.smoothScrollToPosition(poiHistory.size() - 1);
        }
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
        }
    }

    public void like(View view) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (poiHistory.size() <= firstCompletelyVisibleItemPosition
                || firstCompletelyVisibleItemPosition == -1) {
            return;
        }
        POI poi = poiHistory.get(firstCompletelyVisibleItemPosition);
        if (poi.index == null) {
            Toast.makeText(context, "Not set up to be liked, update database", Toast.LENGTH_SHORT).show();
            return;
        }
        if (poi.index.equals("filler")) {
            mDatabaseRef.child(poi.index).child(poi.imageName).child("likes").child(user.getUid()).setValue("true");
        } else {
            mDatabaseRef.child(poi.index).child(poi.coordinates).child(poi.imageName).child("likes").child(user.getUid()).setValue("true");
        }
    }

    public void dislike(View view) {
//        POI poi = mPOIHashMap.get(displayedKey);
//        if (poi == null) {
//            poi = mFillerPOIHashMap.get(displayedKey);
//            if (poi.index == null) {
//                Toast.makeText(context, "Not set up to be liked, update database", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            mDatabaseRef.child(poi.index).child(displayedKey).child("likes").child(user.getUid()).setValue("false");
//        } else {
//            Log.d(TAG, "not filler! poi.index = " + poi.index);
//            if (poi.index == null) {
//                Toast.makeText(context, "poi.index is null!", Toast.LENGTH_LONG).show();
//            }
//
////        if (poi.coordinates.equals("0,0"))
//            mDatabaseRef.child(poi.index).child(poi.coordinates).child(displayedKey).child("likes").child(user.getUid()).setValue("false");
//        }
    }

    public void endTour(View view) {
        Intent intent = new Intent(context, EndOfTourActivity.class);
        startActivity(intent);
    }

    private void makeGlow() {
        if (scaleDown == null) {
            scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    binding.ivAlight,
                    PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.1f));
            scaleDown.setDuration(700);

            scaleDown.setRepeatCount(ObjectAnimator.INFINITE);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
        }

        scaleDown.start();
    }

    public void alight(View view) {
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstCompletelyVisibleItemPosition == -1) {
            return;
        }
        String imageName = poiHistory.get(firstCompletelyVisibleItemPosition).imageName;
        Intent intent = new Intent(context, AlightActivity.class);
        intent.putExtra("TESTSTRING", imageName);
        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(30,0,0,0);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        View mapView = mapFragment.getView();
        @SuppressLint("ResourceType") View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);

        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
        rlp.setMargins(30, 0, 0, 30);

        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (firstCompletelyVisibleItemPosition == -1) {
//            Log.d(TAG, "firstCompletelyVisibleItemPosition = " + firstCompletelyVisibleItemPosition);
            return;
        }
        addMarkerToMap(firstCompletelyVisibleItemPosition);
    }

    public void setMap(PolylineOptions options) {
        if (mMap==null) {
            Log.d(TAG, "map is null");
            return;
        }
        mMap.clear();
        mMap.addPolyline(options);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        if (options.getPoints().get(0) == null) {
            Log.d(TAG, "polylineoptions latlng was null");
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : options.getPoints()) {
            boundsBuilder.include(latLngPoint);
        }

        int routePadding = 100;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(polylineOptionsHashMap.get(route).getPoints().get(center)));
//        mMap.moveCamera(CameraUpdateFactory.zoomTo(12f));
    }

    public void toggleMapImage(View view) {
        if (binding.rvTourPois.getVisibility()==View.VISIBLE) {
            binding.rvTourPois.setVisibility(View.INVISIBLE);
            setMapImage();
        } else {
            binding.rvTourPois.setVisibility(View.VISIBLE);
            binding.toggleMapImage.setImageResource(R.drawable.map_image);
        }
    }

    public void setMapImage() {
        if (binding.rvTourPois.getVisibility() == View.VISIBLE) {
            return;
        }
        LinearLayoutManager layoutManager = ((LinearLayoutManager) binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (poiHistory.size() <= firstCompletelyVisibleItemPosition
                || firstCompletelyVisibleItemPosition == -1) {
            return;
        }
        POI currentPoi = poiHistory.get(firstCompletelyVisibleItemPosition);
        String route = currentPoi.route;
        String key = currentPoi.imageName;
        String fileName = (String) context.getFilesDir().getPath()+"/"+language+"/"+route+"/"+readableKey(key);
//        Log.d(TAG, "addImage-- check fileexists for " + fileName);
        if (!fileExist(fileName)) {
//            Log.d(TAG, "onBindViewHolder-- couldn't find non-filler poi, key = " + key);
            fileName = context.getFilesDir().getPath()+"/"+language+"/"+route+"/filler/"+readableKey(key);
            if (!fileExist(fileName)) {
                Log.d(TAG, "onBindViewHolder-- couldn't find filler poi, key = " + key);
                return;
            }
        }
        binding.toggleMapImage.setImageDrawable(null);
        Picasso.with(context).load(new File(fileName))
//                    .placeholder(R.drawable.profile_wall_picture)
//                    .resize(holder.backgroundImage.getWidth(), holder.backgroundImage.getHeight())
                .fit()
                .centerCrop()
                .into(binding.toggleMapImage);
    }



    public void setPlayPauseButton() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            binding.ibStart.setImageResource(R.drawable.ic_pause_white_24dp);
            binding.ibStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pauseMusic(view);
                }
            });
        } else {
            binding.ibStart.setImageResource(R.drawable.ic_play_arrow_white_24dp);
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
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        setPlayPauseButton();
    }

    public void pauseMusic(View v) {
        Log.d(TAG, "pauseMusic");
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        setPlayPauseButton();
    }

    public void rewindMusic(View v) {
        if (mMediaPlayer != null) {
            int position = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.seekTo(position - mSkipTime);
        }
    }

    public void setMediaPlayer(Object object) throws IOException {
        mMediaPlayer = new MediaPlayer();
        mMetaRetriever = new MediaMetadataRetriever();
        mMediaPlayer.setOnCompletionListener(onCompletionListener);

        if (object instanceof Uri) {
            Log.d(TAG, "Uri object = " + object);
            mMetaRetriever.setDataSource(this, (Uri) object);
            mMediaPlayer.setDataSource(this, (Uri) object);
            mMetaRetriever.setDataSource(this, (Uri) object);
        } else {
            Log.d(TAG, "What's the instanceof");
        }
        mMediaPlayer.prepare();
        mDuration = Integer.parseInt(mMetaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        startMusic(null);
    }

}
