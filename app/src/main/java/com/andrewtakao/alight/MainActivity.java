package com.andrewtakao.alight;

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


        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildAdded-- dataSnapshot.getKey() = " + dataSnapshot.getKey());
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
