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
    public static ArrayList<String> busRoutes;
    private BusRouteAdapter busRouteAdapter;

    private static RouteDatabase routeDatabase;


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
        Log.d(TAG, "size of database is " + routeDatabase.routeDao().getAll().size());

        if (routeDatabase.routeDao().getAll().size() > 0) {
            Log.d(TAG, "Setting mPOIHashMap from local database!");
            for (Route routeNumber : routeDatabase.routeDao().getAll()) {
                Log.d(TAG, "routenumber is " + routeNumber);
                Log.d(TAG, "routenumber.route is " + routeNumber.route);
                busRoutes.add(routeNumber.route);
            }
            busRouteAdapter.notifyDataSetChanged();
        }

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildAdded-- dataSnapshot.getKey() = " + dataSnapshot.getKey());
                if (busRoutes.contains(dataSnapshot.getKey())) {
                    Log.d(TAG, "key " + dataSnapshot.getKey() + " is already in busRoutes");
                    return;
                }
                Route addedRoute = new Route(
                        dataSnapshot.getKey()
                );

                routeDatabase.routeDao().insertAll(addedRoute);
                busRoutes.add(dataSnapshot.getKey());
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
}
