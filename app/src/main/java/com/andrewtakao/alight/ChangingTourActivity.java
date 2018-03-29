package com.andrewtakao.alight;

import android.Manifest;
import android.animation.ObjectAnimator;
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
import android.widget.Toast;

import com.andrewtakao.alight.databinding.ActivityChangingTourBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static com.andrewtakao.alight.Utils.angleFromCoordinate;
import static com.andrewtakao.alight.Utils.audioKey;
import static com.andrewtakao.alight.Utils.fileExist;
import static com.andrewtakao.alight.Utils.readableKey;

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
    public static HashMap<String, POI> mPOIHashMap;
    public static HashMap<String, POI> mFillerPOIHashMap;
    public static ArrayList<POI> poiHistory;
//    private final double mMinDistance = 5750.22644;
    private final double mMinDistance = 20;
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
    private static final int mSkipTime = 1000;
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
    double longitude;
    double poiLatitude;
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
                Log.d(TAG, "onScrolled");
                checkSwipedToLast();
            }
        });

//        //Language
//        if (RoutePreviewActivity.language.equals("Chinese")) {
//            binding.nearby.setText(R.string.nearby_ch);
//            binding.playFiller.setText(R.string.skip_ch);
//        } else {
//            binding.nearby.setText(R.string.nearby);
//            binding.playFiller.setText(R.string.skip);
//        }

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

    private void addFillerAudio(String key) {
        Log.d(TAG, "addFillerAudio-- Bus route = " + busRoute);
        POI poi = mFillerPOIHashMap.get(key);
        String fileName = (String) context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language
                +"/"+busRoute+"/filler/"+audioKey(readableKey(key));

        if (poi == null) {
            Log.d(TAG, "addFillerAudio-- poi == null");
            return;
        }

        if (mMediaPlayer!=null) {
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

    public void playAudio(int position) {
        POI currentPoi = poiHistory.get(position);
        String fileName = context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language
                +"/"+busRoute+"/"+audioKey(readableKey(currentPoi.imageName));
        if (!fileExist(fileName)) {
            fileName = context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language
                    +"/"+busRoute+"/filler/"+audioKey(readableKey(currentPoi.imageName));
            if (!fileExist(fileName)) {
                Toast.makeText(context, "Can't find audio", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (mMediaPlayer!=null) {
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

    public void checkSwipedToLast() {
        Log.d(TAG, "checkSwipedToLast");
        LinearLayoutManager layoutManager = ((LinearLayoutManager)binding.rvTourPois.getLayoutManager());
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstCompletelyVisibleItemPosition == previouslyFirstCompletelyVisibleItemPosition) {
            Log.d(TAG, "checkSwipedToLast-- wasn't changed to a new POI");
            return;
        }
        if (firstCompletelyVisibleItemPosition == -1 )
        {
            Log.d(TAG, "firstCompletelyVisibleItemPosition = " + firstCompletelyVisibleItemPosition);
            return;
        }
        previouslyFirstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPosition;
        playAudio(firstCompletelyVisibleItemPosition);
        Log.d(TAG, "current position = " + firstCompletelyVisibleItemPosition);
        Log.d(TAG, "previouslyFirstCompletelyVisibleItemPosition = " + previouslyFirstCompletelyVisibleItemPosition);

        //Check if swiped to last
        if (firstCompletelyVisibleItemPosition == poiHistory.size()-1) {
            addPOIOnDeck();
        }
    }


    private POI fillerPoi(){
        Random rand = new Random();
        if (mFillerPOIHashMap.size()==0) {
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
        if (poiHistory.size()==0) {
            Log.d(TAG, "replacePOIOnDeck-- Nothing to replace");
            return;
        }
        POI poiOnDeck = poiHistory.get(poiHistory.size()-1);
        double minDistance = mMinDistance;
        POI closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (!poiHistory.contains(poi) && poi.distanceFrom(latitude, longitude)<minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            if (poiOnDeck.index!=null && poiOnDeck.index.equals("filler")||
                    poiOnDeck.distanceFrom(latitude, longitude) > closestPoi.distanceFrom(latitude, longitude)) {
                Log.d(TAG, "replacePOIOnDeck-- poiOnDeck is no longer closest");
                poiHistory.remove(poiOnDeck);
                poiHistory.add(closestPoi);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void addPOIOnDeck() {
        double minDistance = mMinDistance;
        POI closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (!poiHistory.contains(poi) && poi.distanceFrom(latitude, longitude)<minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            Log.d(TAG, "adding " + closestPoi.imageName + ", distance = " + minDistance);
            poiHistory.add(closestPoi);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "addPOIOnDeck- close poi unavailable, grabbing filler");
            POI fillerPOI = fillerPoi();
            if (fillerPOI !=null) {
                poiHistory.add(fillerPoi());
                mAdapter.notifyDataSetChanged();
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void makeUseOfNewLocation(Location location) {
        Log.d(TAG, "makeUseOfNewLocation");
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        if (poiHistory.size()==0) {
            Log.d(TAG, "run me once!");
            addFirstPOI();
        } else {
            replacePOIOnDeck();
            //If poiOnDeck is no longer the closest, or of it's filler... replace it
        }
    }

    public void addFirstPOI() {
        double minDistance = mMinDistance;
        POI closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (poi.distanceFrom(latitude, longitude)<minDistance) {
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
            if (fillerPOI !=null) {
                poiHistory.add(fillerPoi());
                mAdapter.notifyDataSetChanged();
            }
        }
        mAdapter.notifyDataSetChanged();
        //Now we get the second closest
        minDistance = mMinDistance;
        closestPoi = new POI();
        for (POI poi : mPOIHashMap.values()) {
            if (!poiHistory.contains(poi) && poi.distanceFrom(latitude, longitude)<minDistance) {
                minDistance = poi.distanceFrom(latitude, longitude);
                closestPoi = poi;
            }
        }
        if (closestPoi.imageName != null) {
            poiHistory.add(closestPoi);
            mAdapter.notifyDataSetChanged();
        } else {
            POI fillerPOI = fillerPoi();
            if (fillerPOI !=null) {
                poiHistory.add(fillerPoi());
                mAdapter.notifyDataSetChanged();
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void currentPOI(View view) {
        if (poiHistory.size()>1) {
            binding.rvTourPois.smoothScrollToPosition(poiHistory.size()-2);
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
//        POI poi = mPOIHashMap.get(displayedKey);
//        if (poi == null) {
//            poi = mFillerPOIHashMap.get(displayedKey);
//            Log.d(TAG, "filler! poi.index = " + poi.index);
//            if (poi.index == null) {
//                Toast.makeText(context, "Not set up to be liked, update database", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            mDatabaseRef.child(poi.index).child(displayedKey).child("likes").child(user.getUid()).setValue("true");
//        } else {
//            if (poi.index == null) {
//                Toast.makeText(context, "poi.index is null!", Toast.LENGTH_LONG).show();
//            }
////        if (poi.coordinates.equals("0,0"))
//            mDatabaseRef.child(poi.index).child(poi.coordinates).child(displayedKey).child("likes").child(user.getUid()).setValue("true");
//        }
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

    public void endTour(View view) {
        Intent intent = new Intent(context, EndOfTourActivity.class);
        startActivity(intent);
    }
}
