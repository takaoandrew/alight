package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.andrewtakao.alight.databinding.ActivityRoutePreviewBinding;
import com.google.android.gms.maps.CameraUpdate;
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

    private final static String TAG = RoutePreviewActivity.class.getSimpleName();
    private final String LANGUAGE_EXTRA = "language_extra";
    private Context context;

    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;
    public static StorageReference mStorageRef;
    public static DatabaseReference mDatabaseRef;
    private ChildEventListener routesRefListener;

    public static String language = "English";

    //General
    public static HashMap<String, Route> busRoutes;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ArrayList<POI> currentPoiArrayList;

    //Map
    private GoogleMap mMap;
    HashMap<String, PolylineOptions> polylineOptionsHashMap;
    PolylineOptions polylineOptions;
    MarkerOptions lastMarkerOptions;
    Marker lastMarker;


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

        context = getBaseContext();

        final Intent receivingIntent = getIntent();
        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);

        poiArrayListArrayList = new ArrayList<>();
        busRoutes = new HashMap<>();

        recyclerViewAdapter = new RecyclerViewAdapter(this, poiArrayListArrayList, busRoutes);
//        binding.rvRecyclerViews.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvRecyclerViews.setLayoutManager(layoutManager);
        binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
        binding.rvPreviewPois.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));


        database = Utils.getDatabase();
        routesRef = database.getReference(language+"/routes");

        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(binding.rvRecyclerViews);

//
//        //Just to get the currentPOIArrayList initialized
//        LinearLayoutManager poiLayoutManager = ((LinearLayoutManager)binding.rvRecyclerViews.getLayoutManager());
//        int firstVisiblePosition = poiLayoutManager.findFirstVisibleItemPosition();
//
//        changeRoute(poiArrayListArrayList.get(firstVisiblePosition).get(0).route,
//                poiArrayListArrayList.get(firstVisiblePosition)
//        );

        binding.rvRecyclerViews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = ((LinearLayoutManager)binding.rvRecyclerViews.getLayoutManager());
                int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                Log.d(TAG, "current position = " + firstVisiblePosition);
//                Should have poi at 0 if hiding empty routes
                Log.d(TAG, "current route = " + poiArrayListArrayList.get(firstVisiblePosition).get(0).route);
                changeRoute(poiArrayListArrayList.get(firstVisiblePosition).get(0).route,
                        poiArrayListArrayList.get(firstVisiblePosition)
                );

//                recyclerView.getLayoutManager().findFirstVisibleItemPosition();
            }
        });

        routesRefListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot routeSnapshot, String s) {

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
                            Log.d(TAG, "individualSnapshot.getKey() = " + individualSnapshot.getKey());
                            Log.d(TAG, "fileExist() is checking " + (String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/filler"+
                                    readableKey(individualSnapshot.getKey()));
                            if (Utils.fileExist((String) context.getFilesDir().getPath()+"/"+language+"/"+routeSnapshot.getKey()+"/filler/"+
                                    readableKey(individualSnapshot.getKey()))) {
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
                                            readableKey(individualSnapshot.getKey()))) {
                                        downloadedCount+=1;
                                        //Moving this outside- now database will show when images aren't downloaded
//                                        POI addedPoi = new POI(
//                                            (String) individualSnapshot.getKey(),
//                                            (String) individualSnapshot.child("audio").getValue(),
//                                            (String) individualSnapshot.child("coordinates").getValue(),
//                                            (String) individualSnapshot.child("image").getValue(),
//                                            (String) individualSnapshot.child("index").getValue(),
//                                            (String) individualSnapshot.child("language").getValue(),
//                                            (String) individualSnapshot.child("latitude").getValue(),
//                                            (String) individualSnapshot.child("longitude").getValue(),
//                                            (String) individualSnapshot.child("purpose").getValue(),
//                                            (String) individualSnapshot.child("route").getValue(),
//                                            (ArrayList<String>) individualSnapshot.child("theme").getValue(),
//                                            (String) individualSnapshot.child("transcript").getValue()
//                                        );
//                                        poiArrayList.add(addedPoi);
//                                        LatLng latLng = new LatLng(Double.valueOf(addedPoi.latitude),
//                                                Double.valueOf(addedPoi.longitude));
//                                        polylineOptions.add(latLng);
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
                Log.d(TAG, "Route = " + routeSnapshot.getKey());
                Log.d(TAG, "downloaded, child = " + downloadedCount + ", " + childCount);



                busRoutes.put(routeSnapshot.getKey(), new Route(routeSnapshot.getKey(), childCount, downloadedCount));
                Log.d(TAG, "added to bus routes, key, firebase, download = "
                        + routeSnapshot.getKey() +","+ childCount+","+ downloadedCount);

                if (poiArrayList.size()==0) {
                    Log.d(TAG, "Hiding empty routes");
                    return;
                }

//                recyclerViewAdapter = new RecyclerViewAdapter(context, poiArrayListArrayList, busRoutes);
//                binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
//                LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
//                binding.rvRecyclerViews.setLayoutManager(layoutManager);

                poiArrayListArrayList.add(poiArrayList);
                polylineOptionsHashMap.put(routeSnapshot.getKey(), polylineOptions);

                recyclerViewAdapter.notifyDataSetChanged();

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
                                Log.d(TAG, "This index is filler");
                                for (DataSnapshot poiChildSnapshot : indexChildSnapshot.getChildren()) {
                                    DatabaseReference databaseToChange = mDatabaseRef.child(
                                            indexChildSnapshot.getKey());

                                    POI addedPoi = new POI(
                                            (String) poiChildSnapshot.getKey(),
                                            (String) poiChildSnapshot.child("audio").getValue(),
                                            (String) poiChildSnapshot.child("coordinates").getValue(),
                                            (String) poiChildSnapshot.child("image").getValue(),
                                            (String) poiChildSnapshot.child("index").getValue(),
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
                                        addFillerImageToTempFile(poiChildSnapshot.getKey(), addedPoi, addedPoi.route);
                                    } catch (IOException e) { e.printStackTrace(); }

                                    //Store audio location
                                    try {
                                        addFillerAudioToTempFile(poiChildSnapshot.getKey(), addedPoi, addedPoi.route);
                                    } catch (IOException e) { e.printStackTrace();}
                                }
                            } else {

                                for (DataSnapshot coordinateChildSnapshot: indexChildSnapshot.getChildren()) {
                                    for (DataSnapshot poiChildSnapshot : coordinateChildSnapshot.getChildren()) {

                                        //Set POI
                                        Log.d(TAG, "indexChildSnapshot.getKey() = " + indexChildSnapshot.getKey());
                                        Log.d(TAG, "poiChildSnapshot.getKey() = " + poiChildSnapshot.getKey());
                                        POI addedPoi = new POI(
                                                (String) poiChildSnapshot.getKey(),
                                                (String) poiChildSnapshot.child("audio").getValue(),
                                                (String) poiChildSnapshot.child("coordinates").getValue(),
                                                (String) poiChildSnapshot.child("image").getValue(),
                                                (String) poiChildSnapshot.child("index").getValue(),
                                                (String) poiChildSnapshot.child("language").getValue(),
                                                (String) poiChildSnapshot.child("latitude").getValue(),
                                                (String) poiChildSnapshot.child("longitude").getValue(),
                                                (String) poiChildSnapshot.child("purpose").getValue(),
                                                (String) poiChildSnapshot.child("route").getValue(),
                                                (ArrayList<String>) poiChildSnapshot.child("theme").getValue(),
                                                (String) poiChildSnapshot.child("transcript").getValue()
                                        );
                                        Log.d(TAG, "addedPOI.busRoute = " + addedPoi.route);

                                        try {
                                            addImageToTempFile(poiChildSnapshot.getKey(), addedPoi, addedPoi.route);
                                        } catch (IOException e) { e.printStackTrace(); }

                                        //Store audio location
                                        try {
                                            addAudioToTempFile(poiChildSnapshot.getKey(), addedPoi, addedPoi.route);
                                        } catch (IOException e) { e.printStackTrace();}
//                                }

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

    private void addAudioToTempFile(final String key, final POI addedPoi, final String route) throws IOException {

        Log.d(TAG, "addAudioToTempFile-- audio key = " + audioKey(readableKey(key)));
        //Get local file

        StorageReference mAudioRef = mStorageRef.child(audioKey(readableKey(key)));

//        Log.d(TAG, "addAudioToTempFile-- mAudioRef.getPath() = " + mAudioRef.getPath());

//        final File localFile = File.createTempFile(audioKey(readableKey(key)), "");
        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route);
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, audioKey(readableKey(key)));
//        final File localFile = new File(directory, language+"/"+route+"/"+audioKey(readableKey(key)));
        Log.d(TAG, "addAudioToTempFile-- localFile = " + localFile);

        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addAudioToTempFile-- onSuccess");
//                Log.d(TAG,"addAudioToTempFile-- databaseToAddTo = " + databaseToAddTo);
                Log.d(TAG,"addAudioToTempFile-- localFile = " + localFile);
                //TODO commented this out, might change things
//                databaseToChange.child(key).setValue(addedPoi);
                listenToDatabase();


            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addAudioToTempFile-- onFailure");
                Log.d(TAG, "attempted to add " + audioKey(readableKey(key)) + " into local file " + localFile);
            }
        });
    }

    private void addImageToTempFile(final String key, final POI addedPOI, final String route) throws IOException {

        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route);
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, readableKey(key));

        StorageReference mImageRef = mStorageRef.child(readableKey(key));

//        Log.d(TAG, "addImageToTempFile-- mImageRef.getPath() = " + mImageRef.getPath());

        //TODO there is a / here before the imageName child. It may not be there in the future and cause errors.
        //For now we get rid of it
//
//        String slashlessKey = key.replace("/", "");
//        slashlessKey = slashlessKey.replace("*", ".");
//
////        final File localFile = File.createTempFile(slashlessKey, "");
        Log.d(TAG, "addImageToTempFile-- localFile = " + localFile);

        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "addImageToTempFile-- onSuccess");
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Exception " + exception);
                Log.d(TAG,"addImageToTempFile-- onFailure");
                Log.d(TAG, "attempted to add key " + readableKey(key) + " into local file " + localFile);
            }
        });

        binding.rvPreviewPois.getAdapter().notifyDataSetChanged();
    }
    private void addFillerAudioToTempFile(final String key, final POI addedPoi, final String route) throws IOException {

        StorageReference mAudioRef = mStorageRef.child("filler").child(audioKey(readableKey(key)));

        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route+"/filler");
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, audioKey(readableKey(key)));

        Log.d(TAG, "addFillerAudioToTempFile-- localFile = " + localFile);
//        final File localFile = File.createTempFile(audioKey(readableKey(key)), "");
        mAudioRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addFillerAudioToTempFile-- onSuccess");
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            //Try wav?
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG,"addFillerAudioToTempFile-- onFailure");
                Log.d(TAG, "failed for key" + localFile);
            }
        });
        binding.rvPreviewPois.getAdapter().notifyDataSetChanged();
    }

    private void addFillerImageToTempFile(final String key, final POI addedPOI, final String route) throws IOException {
        StorageReference mImageRef = mStorageRef.child("filler").child(readableKey(key));
//        String slashlessKey = key.replace("/", "");
//        slashlessKey = slashlessKey.replace("*", ".");


        File directory = context.getFilesDir();
        final File localDirectory = new File(directory, language+"/"+route+"/filler");
        localDirectory.mkdirs();
        final File localFile = new File(localDirectory, readableKey(key));

//        File directory = context.getFilesDir();
//        final File localFile = new File(directory, language+"/"+route+"/"+readableKey(key));
        Log.d(TAG, "addFillerImageToTempFile-- localFile = " + localFile);
//        final File localFile = File.createTempFile(slashlessKey, "");
        mImageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG,"addFillerImageToTempFile-- onSuccess");
                listenToDatabase();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Exception " + exception);
                Log.d(TAG,"addFillerImageToTempFile-- onFailure");
                Log.d(TAG, "failed for key" + localFile);
            }
        });
        binding.rvPreviewPois.getAdapter().notifyDataSetChanged();
    }

    public void scrollToPosition(int position) {
        Log.d(TAG, "position is " + position);
        binding.rvPreviewPois.smoothScrollToPosition(position);

        if (currentPoiArrayList==null) {
            currentPoiArrayList=poiArrayListArrayList.get(0);
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

    public void changeRoute(String route, ArrayList<POI> poiArrayList) {
        currentPoiArrayList = poiArrayList;
        binding.rvPreviewPois.setAdapter(new POIAdapter(context, poiArrayList));
        setMap(route);
    }

    public void toggleMapImage(View view) {
        if (binding.rvPreviewPois.getVisibility()==View.VISIBLE) {
            binding.rvPreviewPois.setVisibility(View.INVISIBLE);
            binding.toggleMapImage.setImageResource(R.drawable.map_image);
        } else {
            binding.rvPreviewPois.setVisibility(View.VISIBLE);
            binding.toggleMapImage.setImageResource(R.drawable.mit);
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(context, LoginActivity.class);
        startActivity(intent);
    }
}
