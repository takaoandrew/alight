package com.andrewtakao.alight;

import android.arch.persistence.room.Room;
import android.content.Intent;
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
    //Language Extra
    private final String LANGUAGE_EXTRA = "language_extra";


    private final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;
    public static StorageReference mStorageRef;
    public static DatabaseReference mDatabaseRef;
    private ChildEventListener routesRefListener;
    public static ArrayList<Route> busRoutes;
    private BusRouteAdapter busRouteAdapter;
    public static String language = "English";

//    //Route database
//    private static RouteDatabase currentRouteDatabase;
//    private static RouteDatabase englishRouteDatabase;
//    private static RouteDatabase chineseRouteDatabase;
//    //POI Database
//    public static PoiDatabase currentPoiDatabase;
//    public static PoiDatabase chinesePoiDatabase;
//    public static PoiDatabase englishPoiDatabase;
    int childCount = 0;
    int downloadedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent receivingIntent = getIntent();
        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //Firebase download data
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        database = Utils.getDatabase();
//        database.setPersistenceEnabled(true);
        routesRef = database.getReference(language+"/routes");
        busRoutes = new ArrayList<>();

        //Add adapter, set recyclerview to have the adapter
        busRouteAdapter = new BusRouteAdapter(this, busRoutes);
        binding.rvBusRoutes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBusRoutes.setAdapter(busRouteAdapter);

        //to prevent uploading to wrong database temporarily
//        currentPoiDatabase = null;
//        currentRouteDatabase = null;

//        if (language.equals("English")) {
//            if (englishRouteDatabase == null) {
//                Log.d(TAG, "Creating englishRouteDatabase");
//                englishRouteDatabase = Room.databaseBuilder(getApplicationContext(),
//                        RouteDatabase.class, "english-route-database").allowMainThreadQueries().build();
//            }
//            if (englishPoiDatabase == null) {
//                Log.d(TAG, "Creating englishPoiDatabase");
//                englishPoiDatabase = Room.databaseBuilder(getApplicationContext(),
//                        PoiDatabase.class, "english-poi-database").allowMainThreadQueries().build();
//
//            }
//            Log.d(TAG,"language is english");
//            currentRouteDatabase = englishRouteDatabase;
//            currentPoiDatabase = englishPoiDatabase;
//        } else {
//            if (chineseRouteDatabase == null) {
//                Log.d(TAG, "Creating chineseRouteDatabase");
//                chineseRouteDatabase = Room.databaseBuilder(getApplicationContext(),
//                        RouteDatabase.class, "chinese-route-database").allowMainThreadQueries().build();
//
//            }
//            if (chinesePoiDatabase == null) {
//                Log.d(TAG, "Creating chinesePoiDatabase");
//                chinesePoiDatabase = Room.databaseBuilder(getApplicationContext(),
//                        PoiDatabase.class, "chinese-poi-database").allowMainThreadQueries().build();
//            }
//            Log.d(TAG,"language is chinese");
//            currentRouteDatabase = chineseRouteDatabase;
//            currentPoiDatabase = chinesePoiDatabase;
//        }

//        Log.d(TAG, "size of current route database is " + currentRouteDatabase.routeDao().getAll().size());
//        Log.d(TAG, "size of current poi database is " + currentPoiDatabase.poiDao().getAll().size());

//        if (currentRouteDatabase.routeDao().getAll().size() > 0) {
//            Log.d(TAG, "Setting mPOIHashMap from local currentRouteDatabase!");
//            for (Route routeNumber : currentRouteDatabase.routeDao().getAll()) {
//                Log.d(TAG, "routenumber is " + routeNumber);
//                Log.d(TAG, "routenumber.route is " + routeNumber.route);
//                busRoutes.add(new Route(routeNumber.route, routeNumber.firebaseCount, routeNumber.downloadedCount));
//            }
//            busRouteAdapter.notifyDataSetChanged();
//        }

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "routesRefListener onChildAdded--");
                childCount = 0;
                downloadedCount = 0;

                //Count how many pois there should be
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    Log.d(TAG, "snapshot.getKey() = " + snapshot.getKey());
                    for (DataSnapshot coordinateSnapshot: snapshot.getChildren()) {
                        if (("empty").equals(""+coordinateSnapshot.getValue())) {
                        }
                        else if (coordinateSnapshot.hasChildren()) {
                            for (DataSnapshot individualSnapshot: coordinateSnapshot.getChildren()) {
                                Log.d(TAG, "individualSnapshot.getKey() = " + individualSnapshot.getKey());
                                if (null!=individualSnapshot.child("imageLocalStorageLocation").getValue() &&
                                        fileExist((String) individualSnapshot.child("imageLocalStorageLocation").getValue())) {
                                    downloadedCount+=1;
                                }
                                childCount += 1;
                            }
                        }
                        else {
                            //This probably never happens, coordinatesnapshot will always have children if not empty
                            Log.d(TAG, "coordinateSnapshot.getKey() = " + coordinateSnapshot.getKey());
                        }
                    }
                }

                Route routeToRemove = null;
                //Search through all routes and sees if the same number have been downloaded
                for (Route route : busRoutes) {
                    if (route.route.equals(dataSnapshot.getKey())) {
                        Log.d(TAG, "key " + dataSnapshot.getKey() + " is already in busRoutes");
                        Log.d(TAG, "route.downloadedCount = " + route.downloadedCount + " and " +
                                "childCount = " + childCount);
                        Log.d(TAG, "should replace old route");
                        //Shouldn't remove while iterating through busRoutes
                        routeToRemove = route;
                    }
                    // else should be added anyways
                }

                //Need to replace manually here
                if (null != routeToRemove) {
                    //We do this here because earlier we were iterating through busRoutes
                    busRoutes.remove(routeToRemove);
                    //Will show how many there used to be
                }

                busRoutes.add(new Route(dataSnapshot.getKey(), childCount, downloadedCount));

//                addedRoute = new Route(dataSnapshot.getKey(), childCount, downloadedCount);
//                Log.d(TAG, "added route " + addedRoute.route + " with firebase children " + addedRoute.firebaseCount);

                //This should replace old, so no need to remove old as was done for busroutes
//                currentRouteDatabase.routeDao().insertAll(addedRoute);

//                busRoutes.add(addedRoute);
                busRouteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "routesRefListener onChildChanged--");
                Log.d(TAG, "dataSnapshot.getKey() = " + dataSnapshot.getKey());
                Log.d(TAG, "String s = " + s);
//                onChildRemoved(dataSnapshot);
//                onChildAdded(dataSnapshot, "");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "routesRefListener onChildRemoved--");
                Log.d(TAG, "dataSnapshot.getKey() = " + dataSnapshot.getKey());
                Route routeToRemove = null;
                for (Route route: busRoutes) {
                    if (route.route.equals(dataSnapshot.getKey())) {
                        routeToRemove = route;
                    }
                }
                if (null!=routeToRemove) {
//                    currentRouteDatabase.routeDao().delete(routeToRemove);
                    busRoutes.remove(routeToRemove);
                    busRouteAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (busRouteAdapter!=null) {
            busRouteAdapter.notifyDataSetChanged();
        }

        listenToDatabase();

//        Log.d(TAG, "downloadedCount = " + downloadedCount);
    }

    @Override
    protected void onPause() {
        routesRef.removeEventListener(routesRefListener);
        super.onPause();
    }

    public void listenToDatabase() {
        if (routesRef!=null&&routesRefListener!=null) {
            Log.d(TAG, "resetting listener");
            routesRef.removeEventListener(routesRefListener);
            routesRef.addChildEventListener(routesRefListener);
        }
    }

    public void downloadPOIs(final String route, String language) {
        Log.d(TAG, "downloadPOIs");

        mStorageRef = FirebaseStorage.getInstance().getReference(language+"/routes").child(route);
        mDatabaseRef = routesRef.child(route);
        mDatabaseRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot indexChildSnapshot : dataSnapshot.getChildren()) {
                            for (DataSnapshot coordinateChildSnapshot: indexChildSnapshot.getChildren()) {
                                for (DataSnapshot poiChildSnapshot : coordinateChildSnapshot.getChildren()) {
                                    DatabaseReference databaseToChange = mDatabaseRef.child(
                                            indexChildSnapshot.getKey()).child(coordinateChildSnapshot.getKey());
                                    //Ignore it if it's already in the HashMap (was stored locally)
//                                if (mPOIHashMap.containsKey(poiChildSnapshot.getKey())) {
//                                    Log.d(TAG, "key " + poiChildSnapshot.getKey() + " is already in mPOIHashMap");
//                                } else {
//                                    if (databaseToDownloadTo.poiDao().findByNameAndRoute(poiChildSnapshot.getKey(),route)!=null) {
//                                        Log.d(TAG, "onDataChange- poi " + poiChildSnapshot.getKey() + " is already downloaded" +
//                                                "in route " + route);
//                                        break;
//                                    }
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
                                    Log.d(TAG, "addedPOI.busRoute = " + addedPoi.route);
//                                    databaseToDownloadTo.poiDao().insertAll(addedPoi);
//                                    mPOIHashMap.put(poiChildSnapshot.getKey(), addedPoi);
                                    try {
                                        addImageToTempFile(poiChildSnapshot.getKey(), addedPoi, databaseToChange);
                                    } catch (IOException e) { e.printStackTrace(); }

                                    //Store audio location
                                    try {
                                        addAudioToTempFile(poiChildSnapshot.getKey(), addedPoi, databaseToChange);
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


    private void addAudioToTempFile(final String key, final POI addedPoi, final DatabaseReference databaseToChange) throws IOException {

//        Log.d(TAG, "addAudioToTempFile-- key = " + key);
//        Log.d(TAG, "addAudioToTempFile-- readable key = " + readableKey(key));
//        Log.d(TAG, "addAudioToTempFile-- audio key = " + audioKey(readableKey(key)));
        //Get local file

        StorageReference mAudioRef = mStorageRef.child(audioKey(readableKey(key)));

//        Log.d(TAG, "addAudioToTempFile-- mAudioRef.getPath() = " + mAudioRef.getPath());

        final File localFile = File.createTempFile(audioKey(readableKey(key)), "");
//        Log.d(TAG, "addAudioToTempFile-- localFile = " + localFile);

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addAudioToTempFile-- onSuccess");
//                Log.d(TAG,"addAudioToTempFile-- databaseToAddTo = " + databaseToAddTo);
                Log.d(TAG,"addAudioToTempFile-- localFile = " + localFile);
//                if (databaseToAddTo == null) {
//                    Log.d(TAG, "The databaseToAddTo was null!");
//                    return;
//                }
                //TODO commented this out, might change things
                addedPoi.setAudioLocalStorageLocation(localFile.toString());
                databaseToChange.child(key).setValue(addedPoi);
                listenToDatabase();

//                databaseToAddTo.poiDao().insertAll(addedPoi);

//                firebase.database().ref('users/' + userId).set({
//                        username: name,
//                        email: email,
//                        profile_picture : imageUrl
//  });


            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addAudioToTempFile-- onFailure");
            }
        });
    }

    private void addImageToTempFile(final String key, final POI addedPOI, final DatabaseReference databaseToChange) throws IOException {

//        Log.d(TAG, "addImageToTempFile-- key = " + key);
//        Log.d(TAG, "addImageToTempFile-- readable key = " + readableKey(key));
        //Get local file

        StorageReference mImageRef = mStorageRef.child(readableKey(key));

//        Log.d(TAG, "addImageToTempFile-- mImageRef.getPath() = " + mImageRef.getPath());

        //TODO there is a / here before the imageName child. It may not be there in the future and cause errors.
        //For now we get rid of it

        String slashlessKey = key.replace("/", "");
        slashlessKey = slashlessKey.replace("*", ".");

        final File localFile = File.createTempFile(slashlessKey, "");
//        Log.d(TAG, "addImageToTempFile-- localFile = " + localFile);

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addImageToTempFile-- onSuccess");
//                Log.d(TAG,"addImageToTempFile-- databaseToAddTo = " + databaseToAddTo);
                Log.d(TAG,"addImageToTempFile-- localFile = " + localFile);

//                if (databaseToAddTo == null) {
//                    Log.d(TAG, "The databaseToAddTo was null!");
//                    return;
//                }
                Log.d(TAG, "Setting imageLocalStorageLocation");
                addedPOI.setImageLocalStorageLocation(localFile.toString());
                databaseToChange.child(key).setValue(addedPOI);
                listenToDatabase();
//                databaseToAddTo.poiDao().insertAll(addedPOI);
//                mPOIList.get(mPOIList.indexOf(key)).setImageLocalStorageLocation(localFile.toString());
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Exception " + exception);
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

    public boolean fileExist(String fname){
        File file = new File(fname);
        return file.exists();
    }
}
