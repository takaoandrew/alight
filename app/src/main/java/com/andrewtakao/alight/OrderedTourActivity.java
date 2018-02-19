package com.andrewtakao.alight;

import android.Manifest;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
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

public class OrderedTourActivity extends AppCompatActivity {

    public static StorageReference mStorageRef;
    public static StorageReference mAudioRef;
    private HashMap<String, POI> mPOIHashMap;
    private ChildEventListener mImagesListener;
    public static DatabaseReference mImagesDatabaseRef;
    DataSnapshot firstChildSnapshot;

    private ActivityOrderedTourBinding binding;
    private String busRoute;
    private final String TAG = OrderedTourActivity.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private String currentKey;

    //RecyclerView
    private POIAdapter mPOIAdapter;

    //GPS
    Context mContext;
    LocationManager locationManager;
    LocationListener locationListener;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    //Audio
    MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_ordered_tour);

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

        final File localFile = File.createTempFile(key, "");
        Log.d(TAG, "addAudioToTempFile-- localFile = " + localFile);

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addAudioToTempFile-- onSuccess");
                mPOIHashMap.get(key).setAudioLocalStorageLocation(localFile.toString());
//                mPOIList.get(mPOIList.indexOf(key)).setLocalStorageLocation(localFile.toString());
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addAudioToTempFile-- onFailure");
                // Handle any errors
            }
        });
    }


    private String readableKey(String key) {
        return key.replace("*", ".");
    }

    private String audioKey(String key) {
        key = key.replace(".jpeg", ".mp3");
        key = key.replace(".png", ".mp3");
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
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
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

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void getLocation(View view) {
        Log.d(TAG, "getLocation pressed");
        getLastLocation();
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
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        String currentLocation = latitude + ", " + longitude;
        double minDistance = 999999;
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
                currentKey = closestPOI.imageName;
                binding.closestPoi.setText(closestPOI.imageName);
//            addImage(currentKey);
                addAudio(currentKey);
                binding.rvPois.smoothScrollToPosition(mPOIAdapter.poiArrayList.indexOf(closestPOI));
            }
            binding.location.setText(currentLocation);
            //Uncomment if you want it to always scroll to current position.
//            binding.rvPois.smoothScrollToPosition(mPOIAdapter.poiArrayList.indexOf(closestPOI));
        }

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
            getLastLocation();

            //TODO Choose between GPS and network provider
            //TODO listen to both GPS AND Network, then use timestamps to find most recent
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }
}
