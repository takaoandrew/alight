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
import com.google.firebase.database.ValueEventListener;
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
        Log.d(TAG, "onCreate");
        binding = DataBindingUtil.setContentView(this, R.layout.activity_route_preview);

        context = getApplicationContext();

        Intent receivingIntent = getIntent();
//        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);

        poiArrayListArrayList = new ArrayList<>();

        database = Utils.getDatabase();
        routesRef = database.getReference(language+"/routes");

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot routeSnapshot, String s) {
                Log.d(TAG, "routesRefListener onChildAdded--");
                childCount = 0;
                downloadedCount = 0;
                ArrayList<POI> poiArrayList = new ArrayList<>();

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
                                        POI addedPoi = new POI(
                                            (String) individualSnapshot.getKey(),
                                            (String) individualSnapshot.child("audio").getValue(),
                                            (String) individualSnapshot.child("coordinates").getValue(),
                                            (String) individualSnapshot.child("image").getValue(),
                                            (String) individualSnapshot.child("index").getValue(),
                                            (String) individualSnapshot.child("language").getValue(),
                                            (String) individualSnapshot.child("latitude").getValue(),
                                            (String) individualSnapshot.child("longitude").getValue(),
                                            (String) individualSnapshot.child("purpose").getValue(),
                                            (String) individualSnapshot.child("route").getValue(),
                                            (ArrayList<String>) individualSnapshot.child("theme").getValue(),
                                            (String) individualSnapshot.child("transcript").getValue()
                                        );
                                        poiArrayList.add(addedPoi);
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
//                for (Route route : busRoutes) {
//                    if (route.route.equals(routeSnapshot.getKey())) {
//                        Log.d(TAG, "key " + routeSnapshot.getKey() + " is already in busRoutes");
//                        Log.d(TAG, "route.downloadedCount = " + route.downloadedCount + " and " +
//                                "childCount = " + childCount);
//                        Log.d(TAG, "should replace old route");
//                        //Shouldn't remove while iterating through busRoutes
//                        routeToRemove = route;
//                    }
//                    // else should be added anyways
//                }
//
//                //Need to replace manually here
//                if (null != routeToRemove) {
//                    //We do this here because earlier we were iterating through busRoutes
//                    busRoutes.remove(routeToRemove);
//                    //Will show how many there used to be
//                }
//
//                busRoutes.add(new Route(routeSnapshot.getKey(), childCount, downloadedCount));

                poiArrayListArrayList.add(poiArrayList);
                RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(context, poiArrayListArrayList);
                binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
                LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                binding.rvRecyclerViews.setLayoutManager(layoutManager);

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


        listenToDatabase();
    }

    public void listenToDatabase() {
        if (routesRef!=null&&routesRefListener!=null) {
            Log.d(TAG, "resetting listener");
            routesRef.removeEventListener(routesRefListener);
            routesRef.addChildEventListener(routesRefListener);
        }
    }
}
