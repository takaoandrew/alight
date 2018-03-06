package com.andrewtakao.alight;

import android.arch.persistence.room.Room;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import com.andrewtakao.alight.databinding.ActivityMainBinding;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;
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
                Log.d(TAG, "onChildAdded-- dataSnapshot.getKey() = " + dataSnapshot.getKey());

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
                                Log.d(TAG, "Downloading");
                                childCount += 1;
                                Log.d(TAG, "Child count = " + childCount);
                            }
                        }

                        else {
                            Log.d(TAG, "Downloading");
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
                            busRouteAdapter.notifyDataSetChanged();
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
//                if (busRoutes.contains(dataSnapshot.getKey())) {
//                    Log.d(TAG, "key " + dataSnapshot.getKey() + " is already in busRoutes");
//                    return;
//                }


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

//        int downloadedCount = poiDatabase.poiDao().getAll("1").size();
//        Log.d(TAG, "downloadedCount = " + downloadedCount);
    }
}
