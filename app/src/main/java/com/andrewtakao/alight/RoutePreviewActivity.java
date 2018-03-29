package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.andrewtakao.alight.databinding.ActivityRoutePreviewBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.HashMap;

import static com.andrewtakao.alight.Utils.audioKey;
import static com.andrewtakao.alight.Utils.readableKey;

public class RoutePreviewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String TAG = RoutePreviewActivity.class.getSimpleName();
    private final String LANGUAGE_EXTRA = "language_extra";
    public static String language = "English";
    private Context context;
    ActivityRoutePreviewBinding binding;

    //Firebase
    private FirebaseDatabase database;
    public static DatabaseReference routesRef;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    private ChildEventListener routesRefListener;

    //General
    public static HashMap<String, Route> busRoutes;
    private RecyclerViewAdapter recyclerViewAdapter;

    //Map
    private GoogleMap mMap;
    private HashMap<String, PolylineOptions> polylineOptionsHashMap;
    private PolylineOptions polylineOptions;
    private MarkerOptions lastMarkerOptions;
    private Marker lastMarker;

    //Misc
    private ArrayList<ArrayList<POI>> poiArrayListArrayList;
    private ArrayList<POI> currentPoiArrayList;
    private ArrayList<String> existingRouteNames;
    int childCount = 0;
    int downloadedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        context = getBaseContext();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_route_preview);
        final Intent receivingIntent = getIntent();
        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);

        polylineOptions = new PolylineOptions();
        polylineOptionsHashMap = new HashMap<>();
        poiArrayListArrayList = new ArrayList<>();
        busRoutes = new HashMap<>();
        recyclerViewAdapter = new RecyclerViewAdapter(this, poiArrayListArrayList, busRoutes);
        existingRouteNames = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.rvRecyclerViews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
        binding.rvPreviewPois.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPreviewPois.addOnItemTouchListener(new RecyclerViewDisabler());

        database = Utils.getDatabase();
        routesRef = database.getReference(language+"/routes");

        binding.rvRecyclerViews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.d(TAG, "onScrolled");
                changeFocus(-1);
            }
        });

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot routeSnapshot, String s) {
                //HIDE all but mit
                if (!routeSnapshot.getKey().equals("mit")) {
                    return;
                }
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
                            if (Utils.fileExist((String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/filler/"+
                                    readableKey(individualSnapshot.getKey()))) {
                                downloadedCount+=1;
                            }
                            childCount += 1;
                        }
                    } else {
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
                                    if (Utils.fileExist((String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/"+
                                            readableKey(individualSnapshot.getKey()))) {
                                        downloadedCount+=1;
                                    }
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
                                    Log.d(TAG, "Important: addedPoi " + addedPoi.imageName);
                                    childCount += 1;
                                }
                            }
                            else {
                            }
                        }
                    }
                }
                busRoutes.put(routeSnapshot.getKey(), new Route(routeSnapshot.getKey(), childCount, downloadedCount));
                if (poiArrayList.size()==0) {
                    return;
                }

                if (!existingRouteNames.contains(routeSnapshot.getKey())) {
                    Log.d(TAG, "Important: adding poiArrayList " + poiArrayList.size());
                    poiArrayListArrayList.add(poiArrayList);
                    polylineOptionsHashMap.put(routeSnapshot.getKey(), polylineOptions);
                    recyclerViewAdapter.notifyDataSetChanged();
                    existingRouteNames.add(routeSnapshot.getKey());
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
//                for (Route route: busRoutes) {
//                    if (route.route.equals(routeSnapshot.getKey())) {
//                        routeToRemove = route;
//                    }
////                }
//                if (null!=routeToRemove) {
////                    currentRouteDatabase.routeDao().delete(routeToRemove);
//                    busRoutes.remove(routeToRemove);
//                }
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

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        moveTaskToBack(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        if (language.equals("Chinese")) {
            (menu.findItem(R.id.sign_out)).setTitle(R.string.sign_out_ch);
            (menu.findItem(R.id.change_language)).setTitle(R.string.change_language_ch);
        } else {
            (menu.findItem(R.id.sign_out)).setTitle(R.string.sign_out);
            (menu.findItem(R.id.change_language)).setTitle(R.string.change_language);
        }
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sign_out) {
            signOut();
        } else if (item.getItemId() == R.id.change_language){
            Intent intent = new Intent(context, LanguageActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
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
        mMap.clear();
        mMap.addPolyline(polylineOptionsHashMap.get(route));
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        if (polylineOptionsHashMap.get(route).getPoints().get(0) == null) {
            Log.d(TAG, "polylineoptions latlng was null");
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : polylineOptionsHashMap.get(route).getPoints()) {
            boundsBuilder.include(latLngPoint);
        }

        int routePadding = 100;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));

        scrollToPosition(0);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(polylineOptionsHashMap.get(route).getPoints().get(center)));
//        mMap.moveCamera(CameraUpdateFactory.zoomTo(12f));

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

                            //Different procedure for filler content
                            if (indexChildSnapshot.getKey().equals("filler")) {
                                for (DataSnapshot poiChildSnapshot : indexChildSnapshot.getChildren()) {
                                    try {
                                        addFillerImageToFile(poiChildSnapshot.getKey(), (String) poiChildSnapshot.child("route").getValue());
                                    } catch (IOException e) { e.printStackTrace(); }

                                    //Store audio location
                                    try {
                                        addFillerAudioToFile(poiChildSnapshot.getKey(), (String) poiChildSnapshot.child("route").getValue());
                                    } catch (IOException e) { e.printStackTrace();}
                                }
                            } else {
                                for (DataSnapshot coordinateChildSnapshot: indexChildSnapshot.getChildren()) {
                                    for (DataSnapshot poiChildSnapshot : coordinateChildSnapshot.getChildren()) {
                                        //Set POI
//                                        Log.d(TAG, "indexChildSnapshot.getKey() = " + indexChildSnapshot.getKey());
//                                        Log.d(TAG, "poiChildSnapshot.getKey() = " + poiChildSnapshot.getKey());
                                        try {
                                            addImageToFile(poiChildSnapshot.getKey(), (String) poiChildSnapshot.child("route").getValue());
                                        } catch (IOException e) { e.printStackTrace(); }
                                        try {
                                            addAudioToFile(poiChildSnapshot.getKey(), (String) poiChildSnapshot.child("route").getValue());
                                        } catch (IOException e) { e.printStackTrace();}
                                    }
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

    private void addAudioToFile(final String key, final String route) throws IOException {
        StorageReference mAudioRef = mStorageRef.child(audioKey(readableKey(key)));

        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route);
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, audioKey(readableKey(key)));

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "addAudioToFile--Failure: attempted to add key " + audioKey(readableKey(key)) + " into local file " + localFile);
            }
        });
    }

    private void addImageToFile(final String key, final String route) throws IOException {
        StorageReference mImageRef = mStorageRef.child(readableKey(key));

        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route);
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, readableKey(key));

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "addAudioToFile--Failure: attempted to add key " + readableKey(key) + " into local file " + localFile);
            }
        });

        binding.rvPreviewPois.getAdapter().notifyDataSetChanged();
    }
    private void addFillerAudioToFile(final String key, final String route) throws IOException {
        StorageReference mAudioRef = mStorageRef.child("filler").child(audioKey(readableKey(key)));

        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route+"/filler");
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, audioKey(readableKey(key)));

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
//                Log.d(TAG,"addFillerAudioToFile-- onFailure");
                Log.d(TAG, "failed for key" + localFile);
            }
        });
        binding.rvPreviewPois.getAdapter().notifyDataSetChanged();
    }

    private void addFillerImageToFile(final String key, final String route) throws IOException {
        StorageReference mImageRef = mStorageRef.child("filler").child(readableKey(key));

        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route+"/filler");
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, readableKey(key));

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "failed for key" + localFile);
            }
        });
        binding.rvPreviewPois.getAdapter().notifyDataSetChanged();
    }

    public void scrollToPosition(int position) {
        Log.d(TAG, "position is " + position);
        binding.rvPreviewPois.smoothScrollToPosition(position);
        if (currentPoiArrayList==null) {
            return;
        }
        if (currentPoiArrayList.size()<=position) {
            return;
        }

        double latitude = Double.valueOf(currentPoiArrayList.get(position).latitude);
        double longitude = Double.valueOf(currentPoiArrayList.get(position).longitude);
        String poiName = currentPoiArrayList.get(position).imageName;

        if (lastMarker != null) {
            lastMarker.remove();
        }
        lastMarkerOptions = new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(poiName);

        lastMarker = mMap.addMarker(lastMarkerOptions);

    }

    public void scrollToCard(int position) {
        Log.d(TAG, "position is " + position);
        ((LinearLayoutManager)binding.rvRecyclerViews.getLayoutManager()).scrollToPositionWithOffset(position, 0);
//        binding.rvRecyclerViews.smoothScrollToPosition(position);
        recyclerViewAdapter.selected = position;
        recyclerViewAdapter.notifyDataSetChanged();
//        changeFocus();
    }

    public void changeRoute(String route, ArrayList<POI> poiArrayList) {
        currentPoiArrayList = poiArrayList;
        binding.rvPreviewPois.setAdapter(new POIAdapter(context, poiArrayList));
        setMap(route);
    }

    public void toggleMapImage(View view) {
        if (binding.rvPreviewPois.getVisibility()==View.VISIBLE) {
            binding.rvPreviewPois.setVisibility(View.INVISIBLE);
            binding.toggleMapImage.setImageResource(R.drawable.mit);
        } else {
            binding.rvPreviewPois.setVisibility(View.VISIBLE);
            binding.toggleMapImage.setImageResource(R.drawable.map_image);
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(context, LoginActivity.class);
        startActivity(intent);
    }

    public boolean checkDifferent(int position) {
        if (position == recyclerViewAdapter.selected) {
            return false;
        } else {
            return true;
        }
    }

    public void changeFocus(int position) {
        Log.d(TAG, "changeFocus");
        LinearLayoutManager layoutManager = ((LinearLayoutManager)binding.rvRecyclerViews.getLayoutManager());
        int firstCompletelyVisibleItemPosition;
        if (position >= 0) {
            firstCompletelyVisibleItemPosition = position;
            recyclerViewAdapter.selected = position;
        } else {
            firstCompletelyVisibleItemPosition= layoutManager.findFirstCompletelyVisibleItemPosition();
        }
        if (firstCompletelyVisibleItemPosition == -1 )
//                || firstCompletelyVisibleItemPosition == recyclerViewAdapter.selected)
        {
            Log.d(TAG, "firstCompletelyVisibleItemPosition, recyclerViewAdapter.selected = " +
                    firstCompletelyVisibleItemPosition + ", " +recyclerViewAdapter.selected);
            return;
        }
        recyclerViewAdapter.selected = firstCompletelyVisibleItemPosition;
        recyclerViewAdapter.notifyDataSetChanged();
        Log.d(TAG, "current position = " + firstCompletelyVisibleItemPosition);
//                Should have poi at 0 if hiding empty routes
        Log.d(TAG, "current route = " + poiArrayListArrayList.get(firstCompletelyVisibleItemPosition).get(0).route);
        changeRoute(poiArrayListArrayList.get(firstCompletelyVisibleItemPosition).get(0).route,
                poiArrayListArrayList.get(firstCompletelyVisibleItemPosition)
        );
    }

}
