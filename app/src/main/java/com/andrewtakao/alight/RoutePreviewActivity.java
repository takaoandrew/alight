package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import com.andrewtakao.alight.databinding.ActivityRoutePreviewBinding;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class RoutePreviewActivity extends AppCompatActivity {

    private final String TAG = RoutePreviewActivity.class.getSimpleName();
    private final String LANGUAGE_EXTRA = "language_extra";
    private Context context;

    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;
    public static StorageReference mStorageRef;
    public static DatabaseReference mDatabaseRef;
    private ChildEventListener routesRefListener;

    public static String language = "English";

    public static ArrayList<Route> busRoutes;

    ActivityRoutePreviewBinding binding;
    ArrayList<ArrayList<POI>> poiArrayListArrayList;
    ArrayList<POI> poiArrayList;
    ArrayList<POI> poiArrayList2;
    ArrayList<POI> poiArrayList3;
    ArrayList<POI> poiArrayList4;
    ArrayList<String> theme;

    int childCount = 0;
    int downloadedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_route_preview);

        context = getApplicationContext();

        Intent receivingIntent = getIntent();
        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot routeSnapshot, String s) {
                Log.d(TAG, "routesRefListener onChildAdded--");
                childCount = 0;
                downloadedCount = 0;

                //Count how many pois there should be
                for (DataSnapshot indexSnapshot : routeSnapshot.getChildren()) {
                    if (indexSnapshot.getKey().equals("filler")) {
                        for (DataSnapshot individualSnapshot: indexSnapshot.getChildren()) {
                            Log.d(TAG, "individualSnapshot.getKey() = " + individualSnapshot.getKey());
                            Log.d(TAG, "fileExist() is checking " + (String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/filler"+
                                    Utils.readableKey(individualSnapshot.getKey()));
                            if (Utils.fileExist((String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/filler/"+
                                    Utils.readableKey(individualSnapshot.getKey()))) {
                                downloadedCount+=1;
                            }
                            childCount += 1;
                        }
                    } else {

//                    Log.d(TAG, "snapshot.getKey() = " + snapshot.getKey());
                        for (DataSnapshot coordinateSnapshot: indexSnapshot.getChildren()) {
                            if (("empty").equals(""+coordinateSnapshot.getValue())) {
                            }
                            else if (coordinateSnapshot.hasChildren()) {
                                for (DataSnapshot individualSnapshot: coordinateSnapshot.getChildren()) {
                                    Log.d(TAG, "individualSnapshot.getKey() = " + individualSnapshot.getKey());
                                    if (Utils.fileExist((String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/"+
                                            Utils.readableKey(individualSnapshot.getKey()))) {
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
                }

                Route routeToRemove = null;
                //Search through all routes and sees if the same number have been downloaded
                for (Route route : busRoutes) {
                    if (route.route.equals(routeSnapshot.getKey())) {
                        Log.d(TAG, "key " + routeSnapshot.getKey() + " is already in busRoutes");
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

                busRoutes.add(new Route(routeSnapshot.getKey(), childCount, downloadedCount));

            }

            @Override
            public void onChildChanged(DataSnapshot routeSnapshot, String s) {
                Log.d(TAG, "routesRefListener onChildChanged--");
                Log.d(TAG, "dataSnapshot.getKey() = " + routeSnapshot.getKey());
                Log.d(TAG, "String s = " + s);
            }

            @Override
            public void onChildRemoved(DataSnapshot routeSnapshot) {
                Log.d(TAG, "routesRefListener onChildRemoved--");
                Log.d(TAG, "dataSnapshot.getKey() = " + routeSnapshot.getKey());
                Route routeToRemove = null;
                for (Route route: busRoutes) {
                    if (route.route.equals(routeSnapshot.getKey())) {
                        routeToRemove = route;
                    }
                }
                if (null!=routeToRemove) {
//                    currentRouteDatabase.routeDao().delete(routeToRemove);
                    busRoutes.remove(routeToRemove);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        theme = new ArrayList<>();
        theme.add("theme test");
        poiArrayList = new ArrayList<>();
        poiArrayList.add(new POI("test*jpg", "test",
                "test", "test", "test", "test",
                "test", "test", "test", "test", theme,"test"));
        poiArrayList.add(new POI("testie*jpg", "testie",
                "testie", "testie", "testie", "testie",
                "testie", "testie", "testie", "testie", theme,"testie"));
        poiArrayList2 = new ArrayList<>();
        poiArrayList2.add(new POI("test2*jpg", "test2",
                "test2", "test2", "test2", "test2",
                "test2", "test2", "test2", "test2", theme,"test2"));
        poiArrayList2.add(new POI("testie2*jpg", "testie2",
                "testie2", "testie2", "testie2", "testie2",
                "testie2", "testie2", "testie2", "testie2", theme,"testie2"));
        poiArrayList3 = new ArrayList<>();
        poiArrayList3.add(new POI("test3*jpg", "test3",
                "test3", "test3", "test3", "test3",
                "test3", "test3", "test3", "test3", theme,"test3"));
        poiArrayList3.add(new POI("testie3*jpg", "testie3",
                "testie3", "testie3", "testie3", "testie3",
                "testie3", "testie3", "testie3", "testie3", theme,"testie3"));
        poiArrayList4 = new ArrayList<>();
        poiArrayList4.add(new POI("test4*jpg", "test4",
                "test4", "test4", "test4", "test4",
                "test4", "test4", "test4", "test4", theme,"test4"));
        poiArrayList4.add(new POI("testie4*jpg", "testie4",
                "testie4", "testie4", "testie4", "testie4",
                "testie4", "testie4", "testie4", "testie4", theme,"testie4"));
        poiArrayListArrayList = new ArrayList<>();
        poiArrayListArrayList.add(poiArrayList);
        poiArrayListArrayList.add(poiArrayList2);
        poiArrayListArrayList.add(poiArrayList3);
        poiArrayListArrayList.add(poiArrayList4);
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(this, poiArrayListArrayList);
        binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvRecyclerViews.setLayoutManager(layoutManager);
    }
}
