package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.SnapHelper;
import android.util.Log;

import com.andrewtakao.alight.databinding.ActivityRoutePreviewBinding;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;

public class RoutePreviewActivity extends FragmentActivity implements OnMapReadyCallback {

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

    //Map
    private GoogleMap mMap;
    HashMap<String, PolylineOptions> polylineOptionsHashMap;
    PolylineOptions polylineOptions;
    LatLng latLng;


    ActivityRoutePreviewBinding binding;
    ArrayList<ArrayList<POI>> poiArrayListArrayList;

    int childCount = 0;
    int downloadedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
//        latLng = new LatLng(42.1, -71.1);
//        LatLng latLng2 = new LatLng(42.2, -71.2);
//        LatLng latLng3 = new LatLng(42.3, -71.3);
//        LatLng latLng4 = new LatLng(42.4, -71.4);
        polylineOptions = new PolylineOptions();
        polylineOptionsHashMap = new HashMap<>();
//        polylineOptions.add(latLng);
//        polylineOptions.add(latLng2);
//        polylineOptions.add(latLng3);
//        polylineOptions.add(latLng4);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_route_preview);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        context = getApplicationContext();

        Intent receivingIntent = getIntent();
//        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);

        poiArrayListArrayList = new ArrayList<>();

        database = Utils.getDatabase();
        routesRef = database.getReference(language+"/routes");



        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(binding.rvRecyclerViews);

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot routeSnapshot, String s) {
                Log.d(TAG, "routesRefListener onChildAdded--");
                childCount = 0;
                downloadedCount = 0;
                ArrayList<POI> poiArrayList = new ArrayList<>();

                //Reset for each route
                polylineOptions = new PolylineOptions();

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
                            String thisLongitudeString = coordinateSnapshot.getKey()
                                    .substring(0, coordinateSnapshot.getKey().indexOf(",")).replace("*",".");
                            String thisLatitudeString = coordinateSnapshot.getKey()
                                    .substring(coordinateSnapshot.getKey().indexOf(",")+1).replace("*",".");
                            LatLng latLng = new LatLng(Double.valueOf(thisLatitudeString), Double.valueOf(thisLongitudeString));
                            polylineOptions.add(latLng);
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
//                                        LatLng latLng = new LatLng(Double.valueOf(addedPoi.latitude),
//                                                Double.valueOf(addedPoi.longitude));
//                                        polylineOptions.add(latLng);
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

                if (poiArrayList.size()==0) {
                    Log.d(TAG, "Hiding empty routes");
                    return;
                }
                poiArrayListArrayList.add(poiArrayList);
                RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(context, poiArrayListArrayList);
                binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
                LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                binding.rvRecyclerViews.setLayoutManager(layoutManager);

                polylineOptionsHashMap.put(routeSnapshot.getKey(), polylineOptions);
                if (routeSnapshot.getKey().equals("mit")) {
                    Log.d(TAG, "Found mit");
                    setMap(routeSnapshot.getKey());
                }

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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);

    }

    public void setMap(String route) {
        if (mMap==null) {
            Log.d(TAG, "map is null");
            return;
        }
        mMap.addPolyline(polylineOptionsHashMap.get(route));
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        if (polylineOptionsHashMap.get("mit").getPoints().get(0) == null) {
            Log.d(TAG, "polylineoptions latlng was null");
            return;
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(polylineOptionsHashMap.get(route).getPoints().get(0)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));

    }
}
