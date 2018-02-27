package com.andrewtakao.alight;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.andrewtakao.alight.databinding.ActivityTourBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TourActivity extends AppCompatActivity {

    private StorageReference mStorageRef;
    private StorageReference mImageRef;
    private StorageReference mTempStorageRef;
    private HashMap<String, POI> mImagesHashMap;
    private ArrayList<POI> mImagesList;
    private ChildEventListener mImagesListener;
    public static DatabaseReference mImagesDatabaseRef;

    private ActivityTourBinding binding;
    private String busRoute;
    private final String TAG = TourActivity.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private String currentKey;

    //GPS
    Context mContext;
    LocationManager locationManager;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    //Audio
    MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tour);


        //Initialize
        mContext = this;
        mImagesList = new ArrayList<>();
        currentKey = "";
        mImagesHashMap = new HashMap<>();


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Commented out while using button to debug
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            binding.location.setText("no permission");


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            Log.d(TAG, "onCreate-- getLastLocation(), then locationmanager.requestLocationupdates()");
            getLastLocation();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 2, locationListener);
        }

        //Get bus route
        Intent intent = getIntent();
        busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);

        mStorageRef = FirebaseStorage.getInstance().getReference("routes").child(busRoute);


        //run first time only
        if (savedInstanceState == null) {
            //Listens to firebase database for changes in route content pointers
            mImagesListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

//                mImagesList.add(new POI(
//                        dataSnapshot.getKey(),
//                        (double) dataSnapshot.child("latitude").getValue(),
//                        (double) dataSnapshot.child("longitude").getValue()));
                    mImagesHashMap.put(dataSnapshot.getKey(), new POI(
                            dataSnapshot.getKey(),
                            (double) dataSnapshot.child("latitude").getValue(),
                            (double) dataSnapshot.child("longitude").getValue(),
                            Integer.valueOf(dataSnapshot.getKey())));

                    Log.d(TAG, "dataSnapshot.getKey() = " + dataSnapshot.getKey());

                    try {
                        addImageToTempFile(dataSnapshot.getKey());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        addAudioToTempFile(dataSnapshot.getKey());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

//                if (("image/png").equals((String) dataSnapshot.child("contentType").getValue()) ||
//                        ("image/png").equals((String) dataSnapshot.child("contentType").getValue())) {
//                } else {
//                    Log.d(TAG, "contentType = " +
//                            dataSnapshot.child("contentType").getValue());
//                }
//                addImage(dataSnapshot.getKey());
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    mImagesHashMap.remove(dataSnapshot.getKey());
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

    private void addImageToTempFile(final String key) throws IOException {

        mImageRef = mStorageRef.child(readableKey(key));
        Log.d(TAG, "mImageRef.getPath() = " + mImageRef.getPath());

        final File localFile = File.createTempFile(key, "");
        Log.d(TAG, "localFile = " + localFile);

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"onSuccess");
                mImagesHashMap.get(key).setImageLocalStorageLocation(localFile.toString());
//                mImagesList.get(mImagesList.indexOf(key)).setImageLocalStorageLocation(localFile.toString());
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"onFailure");
                // Handle any errors
            }
        });
    }

    private void addImage(String key) {
//        try {
        Log.d(TAG, "key = " + key);
        Log.d(TAG, "readable key = " + readableKey(key));
        //Get local file
        String fileName;
//            try {
//                fileName = File.createTempFile(key, "").toString();
//                Log.d(TAG, "try: fileName = " + fileName);
//            } catch (IOException e) {
//                e.printStackTrace();
//                fileName = "";
//            }
//            final File localFile = new File(fileName);
//            final File localFile = File.createTempFile("images", "png");
//            fileName = mImagesList.get(mImagesList.indexOf(key)).imageLocalStorageLocation;
        fileName = mImagesHashMap.get(key).imageLocalStorageLocation;
        Bitmap bmp = BitmapFactory.decodeFile(fileName);
//            imageView.setImageBitmap(bmp);
        binding.tourBackgroundImage.setImageBitmap(bmp);
//            binding.tourBackgroundImage.setImageBitmap(BitmapFactory.decodeFile(localFile.getAbsolutePath()));
//            mTempStorageRef = mStorageRef.child(readableKey(key));
//            mTempStorageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
//                    binding.tourBackgroundImage.setImageBitmap(bitmap);
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Log.d(TAG, ""+e);
//                    Log.d(TAG, "onFailure");
//                }
//            });
//        }
//        catch (IOException e) {}
    }

    private void addAudio(String key) {
//        try {
        Log.d(TAG, "key = " + key);
        Log.d(TAG, "readable key = " + readableKey(key));
        Log.d(TAG, "audio key = " + audioKey(readableKey(key)));
        //Get local file

        String fileName;

        fileName = mImagesHashMap.get(key).audioLocalStorageLocation;

        if (mMediaPlayer!=null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        }

        mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(fileName));
        mMediaPlayer.start();
    }


    private void addAudioToTempFile(final String key) throws IOException {

        Log.d(TAG, "addAudioToTempFile-- key = " + key);
        Log.d(TAG, "addAudioToTempFile-- readable key = " + readableKey(key));
        Log.d(TAG, "addAudioToTempFile-- audio key = " + audioKey(readableKey(key)));
        //Get local file
        mImageRef = mStorageRef.child(audioKey(readableKey(key)));

        Log.d(TAG, "addAudioToTempFile-- mImageRef.getPath() = " + mImageRef.getPath());

        final File localFile = File.createTempFile(key, "");
        Log.d(TAG, "addAudioToTempFile-- localFile = " + localFile);

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addAudioToTempFile-- onSuccess");
                mImagesHashMap.get(key).setAudioLocalStorageLocation(localFile.toString());
//                mImagesList.get(mImagesList.indexOf(key)).setImageLocalStorageLocation(localFile.toString());
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

        String locationProvider = LocationManager.NETWORK_PROVIDER;
// Or use LocationManager.GPS_PROVIDER
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLastLocation-- you don't have permission to access gps");
            return;
        }
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
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
    public void makeUseOfNewLocation(Location location) {
//        Log.d(TAG, "makeUseOfNewLocation");
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        String currentLocation = latitude + ", " + longitude;
        double minDistance = 999999;
        String closestPOI = "";
        for (POI POI : mImagesHashMap.values()) {
            if (POI.distanceFrom(Double.parseDouble(latitude), Double.parseDouble(longitude)) < minDistance) {
                minDistance = POI.distanceFrom(Double.parseDouble(latitude), Double.parseDouble(longitude));
                closestPOI = POI.imageName;
            }
        }
        if (currentKey.equals(closestPOI)) {
            if (currentKey.equals("")) {
                return;
            }
            //For debugging purposes. Otherwise, comment this out.
            addImage(currentKey);
            //Do nothing
        } else {
            currentKey = closestPOI;
            binding.closestPoi.setText(closestPOI);
            addImage(currentKey);
            addAudio(currentKey);
        }
        binding.location.setText(currentLocation);

    }
}
