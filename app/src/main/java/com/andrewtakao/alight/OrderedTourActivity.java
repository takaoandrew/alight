package com.andrewtakao.alight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

public class OrderedTourActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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


        //Initialize Adapter
        mPOIAdapter =  new POIAdapter(this, new ArrayList<>(mPOIHashMap.values()));
        binding.rvPois.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPois.setAdapter(mPOIAdapter);

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

        checkPermission();

        //Get bus route
        Intent intent = getIntent();
        busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);
        Log.d(TAG, "busRoute = "+busRoute);

        mStorageRef = FirebaseStorage.getInstance().getReference("routes").child(busRoute);

        //run first time only
        if (savedInstanceState == null) {
            //Listens to firebase database for changes in route content pointers
            mImagesListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    if (dataSnapshot.hasChildren()) {
                        firstChildSnapshot = dataSnapshot.getChildren().iterator().next();
                        Log.d(TAG, "firstChildSnapshot.getKey() = " + firstChildSnapshot.getKey());

                        mPOIHashMap.put(firstChildSnapshot.getKey(), new POI(
                                firstChildSnapshot.getKey(),
                                Double.valueOf((String) firstChildSnapshot.child("latitude").getValue()),
                                Double.valueOf((String) firstChildSnapshot.child("longitude").getValue()),
                                Integer.valueOf(dataSnapshot.getKey())));
                        mPOIAdapter.updateAdapter(new ArrayList<>(mPOIHashMap.values()));

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
                        Log.d(TAG, "onChildAdded-- this snapshot has no children");
                    }


                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    mPOIHashMap.remove(dataSnapshot.getKey());
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
                mPOIHashMap.get(key).setAudioLocalStorageLocation(localFile.toString());
//                mPOIList.get(mPOIList.indexOf(key)).setImageLocalStorageLocation(localFile.toString());
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addAudioToTempFile-- onFailure");
//                mAudioRef = mStorageRef.child(audioWavKey(audioKey(readableKey(key))));
//                mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                        Log.d(TAG,"addAudioToTempFile-- onSuccess, trying to get wav");
//                        mPOIHashMap.get(key).setAudioLocalStorageLocation(localFile.toString());
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.d(TAG,"addAudioToTempFile-- onFailure, trying to get wav");
//                    }
//                });
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
                mPOIHashMap.get(key).setImageLocalStorageLocation(localFile.toString());
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

    private String audioWavKey(String key) {
        return key.replace(".mp3", ".wav");
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

    public void playAudio(View view) {
        Log.d(TAG, "playAudio pressed");

        String fileName;

        if (mPOIHashMap.get(currentKey) != null) {
            fileName = mPOIHashMap.get(currentKey).audioLocalStorageLocation;
            Log.d(TAG, "playAudio-- fileName = " + fileName);
        } else {
            fileName = null;
        }

//        if (mMediaPlayer!=null) {
//            if (mMediaPlayer.isPlaying()) {
//                mMediaPlayer.stop();
//            }
//
//        }
        if (mMediaPlayer != null ) {
            if (fileName != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                else {
                    mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
                    mMediaPlayer.start();
                }

            }
        } else {
            mMediaPlayer = new MediaPlayer();
        }
    }


    public void makeUseOfNewLocation(Location location) {
        Log.d(TAG, "makeUseOfNewLocation");
        if (location == null) {
            return;
        }
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        String currentLocation = latitude + ", " + longitude;
        //.001 is around a block away
//        double minDistance = .001;
        //Choosing this as minDistance will show closest POI, as opposed to the POI right around the corner
        double minDistance = 1000;
        POI closestPOI = new POI();
        for (POI POI : mPOIHashMap.values()) {
            if (POI.distanceFrom(Double.parseDouble(latitude), Double.parseDouble(longitude)) < minDistance) {
                minDistance = POI.distanceFrom(Double.parseDouble(latitude), Double.parseDouble(longitude));
                closestPOI = POI;
            }
        }
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
                binding.rvPois.scrollToPosition(mPOIAdapter.poiArrayList.indexOf(closestPOI));
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
            binding.location.setText("no permission");


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
}
