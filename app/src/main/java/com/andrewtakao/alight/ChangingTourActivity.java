package com.andrewtakao.alight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
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
import android.widget.SeekBar;

import com.andrewtakao.alight.databinding.ActivityChangingTourBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ChangingTourActivity extends AppCompatActivity {

//    public static StorageReference mStorageRef;
//    public static StorageReference mAudioRef;
//    public static StorageReference mImageRef;
    public static HashMap<String, POI> mPOIHashMap;
//    private ChildEventListener mImagesListener;
//    public static DatabaseReference mImagesDatabaseRef;
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

        binding.nextPoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPOI();
            }
        });

        //Get bus route
        Intent intent = getIntent();
        busRoute = intent.getStringExtra(BUS_ROUTE_EXTRA);

//        mStorageRef = FirebaseStorage.getInstance().getReference("routes").child(busRoute);

        //run first time only
        if (MainActivity.currentPoiDatabase == null) {
            Log.d(TAG, "Creating database");
            MainActivity.currentPoiDatabase = Room.databaseBuilder(getApplicationContext(),
                    PoiDatabase.class, "poi-database").allowMainThreadQueries().build();

        }
        Log.d(TAG, "size of database is " + MainActivity.currentPoiDatabase.poiDao().getAll(busRoute).size());

        //First, populate mPOIHashMap with local data
        if (MainActivity.currentPoiDatabase.poiDao().getAll(busRoute).size() > 0) {
            Log.d(TAG, "Setting mPOIHashMap from local database!");
            for (POI databasePoi : MainActivity.currentPoiDatabase.poiDao().getAll(busRoute)) {
                Log.d(TAG, "databasePoi image name is " + databasePoi.imageName);
                mPOIHashMap.put(databasePoi.imageName, databasePoi);
            }
        }

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
    protected void onStart() {
        super.onStart();
        setPlayPauseButton();
        hideMediaButtons();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (mMediaPlayer!=null && mMediaPlayer.isPlaying())
        mMediaPlayer.stop();
//        mImagesDatabaseRef.removeEventListener(mImagesListener);

        super.onPause();
    }
    
    private void addImage(String key) {
        final POI poi = mPOIHashMap.get(key);
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
                Log.d(TAG, "You clicked on poi.imageName " + poi.imageName);
                Log.d(TAG, "You clicked on poi.imageStorageLocation " + poi.imageLocalStorageLocation);

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
        String fileName;
        final POI poi = mPOIHashMap.get(key);
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

            Location lastKnownLocation = getLastKnownLocation();

            Log.d(TAG, "lastKnownLocation = " + lastKnownLocation);
            makeUseOfNewLocation(lastKnownLocation);

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

    public void makeUseOfNewLocation(Location location) {
        Log.d(TAG, "makeUseOfNewLocation");
        if (location == null) {
            Log.d(TAG, "location == null");
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
        if (closestPOI.imageName == null) {
            Log.d(TAG, "closestPOI.imagename is null");
            return;
        }

        if (currentKey.equals(closestPOI.imageName)) {
            if (currentKey==null || currentKey.equals("")) {
                Log.d(TAG, "makeUseOfNewLocation-- currentKey = " + currentKey);
                return;
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

            if (binding.nextPoi.getVisibility()==View.GONE) {
                binding.nextPoi.setText("Next: " + userFriendlyName(closestPOI.imageName));
                binding.nextPoi.setVisibility(View.VISIBLE);
            }

            if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
                Log.d(TAG, "makeUseOfNewLocation-- waiting for mediaplayer to stop");
                return;
            }
            else {
                nextPOI();
            }

            binding.directionToolbar.setText((int)minDistance+"m");

            //DEBUG ONLY
//                binding.closestPoi.setText(closestPOI.imageName);

        }
        //DEBUG ONLY
//            binding.location.setText(currentLocation);



    }

    private void nextPOI() {
        binding.nextPoi.setVisibility(View.GONE);
        addImage(currentKey);
        addAudio(currentKey);
        binding.closestPoiToolbar.setText(userFriendlyName(currentKey));
    }

    // Checks if user has enabled permission. If they have, get the last location.
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
            Log.d(TAG, "Uri");
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
