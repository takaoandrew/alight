package com.andrewtakao.alight;

import android.arch.persistence.room.Room;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import com.andrewtakao.alight.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;
    public static StorageReference mStorageRef;
    public static DatabaseReference mDatabaseRef;
    private ChildEventListener routesRefListener;
    public static ArrayList<Route> busRoutes;
    private BusRouteAdapter busRouteAdapter;

    //Route database
    private static RouteDatabase routeDatabase;
    //POI Database
    public static AppDatabase poiDatabase;



    int childCount = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //Firebase download data
        database = FirebaseDatabase.getInstance();
        routesRef = database.getReference("routes");
        busRoutes = new ArrayList<>();

        //Add adapter, set recyclerview to have the adapter
        busRouteAdapter = new BusRouteAdapter(this, busRoutes);
        binding.rvBusRoutes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBusRoutes.setAdapter(busRouteAdapter);


        if (routeDatabase == null) {
            Log.d(TAG, "Creating database");
            routeDatabase = Room.databaseBuilder(getApplicationContext(),
                    RouteDatabase.class, "route-database").allowMainThreadQueries().build();

        }
        if (poiDatabase == null) {
            Log.d(TAG, "Creating database");
            poiDatabase = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "poi-database").allowMainThreadQueries().build();

        }

        Log.d(TAG, "size of route database is " + routeDatabase.routeDao().getAll().size());
        Log.d(TAG, "size of poi database is " + poiDatabase.poiDao().getAll().size());

        if (routeDatabase.routeDao().getAll().size() > 0) {
            Log.d(TAG, "Setting mPOIHashMap from local database!");
            for (Route routeNumber : routeDatabase.routeDao().getAll()) {
                Log.d(TAG, "routenumber is " + routeNumber);
                Log.d(TAG, "routenumber.route is " + routeNumber.route);
                busRoutes.add(new Route(routeNumber.route, routeNumber.firebaseCount, routeNumber.downloadedCount));
            }
            busRouteAdapter.notifyDataSetChanged();
        }

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                childCount = 0;
//                Log.d(TAG, "onChildAdded-- dataSnapshot.getKey() = " + dataSnapshot.getKey());

                int downloadedCount = poiDatabase.poiDao().getAll(dataSnapshot.getKey()).size();
                Log.d(TAG, "downloadedCount = " + downloadedCount);

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    Log.d(TAG, "snapshot.getKey() = " + snapshot.getKey());
                    for (DataSnapshot coordinateSnapshot: snapshot.getChildren()) {
                        if (("empty").equals(""+coordinateSnapshot.getValue())) {
//                            Log.d(TAG, "Coordinate Snapshot = " + coordinateSnapshot.getValue());
                        }
                        else if (coordinateSnapshot.hasChildren()) {
                            for (DataSnapshot individualSnapshot: coordinateSnapshot.getChildren()) {
                                childCount += 1;
                                Log.d(TAG, "Child count = " + childCount);
                            }
                        }

                        else {
                            childCount += 1;
                            Log.d(TAG, "Child count = " + childCount);
                        }
                    }
                }

                Route routeToRemove = null;
                for (Route route : busRoutes) {
                    if (route.route.equals(dataSnapshot.getKey())) {
                        Log.d(TAG, "key " + dataSnapshot.getKey() + " is already in busRoutes");
                        if (route.downloadedCount == childCount) {
                            Log.d(TAG, "route.downloadedCount = " + route.downloadedCount + " and " +
                                    "childCount = " + childCount);
                            Log.d(TAG, "already up to date");
                            return;
                        }
                        else {
                            Log.d(TAG, "route.downloadedCount = " + route.downloadedCount + " and " +
                                    "childCount = " + childCount);
                            Log.d(TAG, "should replace old route");
                            routeToRemove = route;
                        }
                    }
                    // else should be added anyways
                }


                Route addedRoute;
                //Need to replace manually here
                if (null != routeToRemove) {
                    busRoutes.remove(routeToRemove);
                    //Will show how many there used to be
                    addedRoute = new Route(dataSnapshot.getKey(), childCount, downloadedCount);
                    Log.d(TAG, "added route " + addedRoute.route + " with firebase children " + addedRoute.firebaseCount);
                } else {
                    //Never added before
                    addedRoute = new Route(dataSnapshot.getKey(), childCount, downloadedCount);
                    Log.d(TAG, "added route " + addedRoute.route + " with firebase children " + addedRoute.firebaseCount);
                }

                //This should replace old
                routeDatabase.routeDao().insertAll(addedRoute);
                busRoutes.add(addedRoute);
                busRouteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                busRoutes.remove(dataSnapshot.getKey());
                busRouteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        routesRef.addChildEventListener(routesRefListener);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (busRouteAdapter!=null) {
            busRouteAdapter.notifyDataSetChanged();
        }

        listenToDatabase();

//        int downloadedCount = poiDatabase.poiDao().getAll("1").size();
//        Log.d(TAG, "downloadedCount = " + downloadedCount);
    }

    public void listenToDatabase() {
        if (routesRef!=null&&routesRefListener!=null) {
            Log.d(TAG, "resetting listener");
            routesRef.removeEventListener(routesRefListener);
            routesRef.addChildEventListener(routesRefListener);
        }
    }

    public void downloadPOIs(final String route) {
        Log.d(TAG, "downloadPOIs");

        mStorageRef = FirebaseStorage.getInstance().getReference("routes").child(route);
        mDatabaseRef = routesRef.child(route);

        mDatabaseRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot indexChildSnapshot : dataSnapshot.getChildren()) {
                            for (DataSnapshot coordinateChildSnapshot: indexChildSnapshot.getChildren()) {
                                for (DataSnapshot poiChildSnapshot : coordinateChildSnapshot.getChildren()) {
                                    //Ignore it if it's already in the HashMap (was stored locally)
//                                if (mPOIHashMap.containsKey(poiChildSnapshot.getKey())) {
//                                    Log.d(TAG, "key " + poiChildSnapshot.getKey() + " is already in mPOIHashMap");
//                                } else {
//                                    Log.d(TAG, "poiDatabase.poiDao().findByName(poiChildSnapshot.getKey()) = " +
//                                            poiDatabase.poiDao().findByName(poiChildSnapshot.getKey()));
                                    if (poiDatabase.poiDao().findByNameAndRoute(poiChildSnapshot.getKey(),route)!=null) {
                                        Log.d(TAG, "onDataChange- poi " + poiChildSnapshot.getKey() + " is already downloaded" +
                                                "in route " + route);
                                        break;
                                    }
                                    //Set POI
                                    Log.d(TAG, "indexChildSnapshot.getKey() = " + indexChildSnapshot.getKey());
                                    Log.d(TAG, "poiChildSnapshot.getKey() = " + poiChildSnapshot.getKey());
                                    POI addedPoi = new POI(
                                            poiChildSnapshot.getKey(),
                                            Double.valueOf((String) poiChildSnapshot.child("lat").getValue()),
                                            Double.valueOf((String) poiChildSnapshot.child("long").getValue()),
                                            Integer.valueOf(indexChildSnapshot.getKey()),
                                            route
                                    );
                                    Log.d(TAG, "addedPOI.busRoute = " + addedPoi.busRoute);
                                    poiDatabase.poiDao().insertAll(addedPoi);
//                                    mPOIHashMap.put(poiChildSnapshot.getKey(), addedPoi);
                                    try {
                                        addImageToTempFile(poiChildSnapshot.getKey(), addedPoi);
                                    } catch (IOException e) { e.printStackTrace(); }

                                    //Store audio location
                                    try {
                                        addAudioToTempFile(poiChildSnapshot.getKey(), addedPoi);
                                    } catch (IOException e) { e.printStackTrace();}
//                                }
                                }
                            }

                        }
                        listenToDatabase();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

    }


    private void addAudioToTempFile(final String key, final POI addedPoi) throws IOException {

        Log.d(TAG, "addAudioToTempFile-- key = " + key);
        Log.d(TAG, "addAudioToTempFile-- readable key = " + readableKey(key));
        Log.d(TAG, "addAudioToTempFile-- audio key = " + audioKey(readableKey(key)));
        //Get local file

        StorageReference mAudioRef = mStorageRef.child(audioKey(readableKey(key)));

        Log.d(TAG, "addAudioToTempFile-- mAudioRef.getPath() = " + mAudioRef.getPath());

        final File localFile = File.createTempFile(audioKey(readableKey(key)), "");
        Log.d(TAG, "addAudioToTempFile-- localFile = " + localFile);

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addAudioToTempFile-- onSuccess");
                if (poiDatabase == null) {
                    Log.d(TAG, "The poiDatabase was null!");
                    return;
                }
                //TODO commented this out, might change things
                addedPoi.setAudioLocalStorageLocation(localFile.toString());
                poiDatabase.poiDao().insertAll(addedPoi);

            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addAudioToTempFile-- onFailure");
            }
        });
    }

    private void addImageToTempFile(final String key, final POI addedPOI) throws IOException {

        Log.d(TAG, "addImageToTempFile-- key = " + key);
        Log.d(TAG, "addImageToTempFile-- readable key = " + readableKey(key));
        //Get local file

        StorageReference mImageRef = mStorageRef.child(readableKey(key));

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

                if (poiDatabase == null) {
                    Log.d(TAG, "The poiDatabase was null!");
                    return;
                }
                Log.d(TAG, "Setting imageLocalStorageLocation");
                addedPOI.setImageLocalStorageLocation(localFile.toString());
                poiDatabase.poiDao().insertAll(addedPOI);
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
}
