package com.andrewtakao.alight;

import android.*;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.andrewtakao.alight.databinding.ActivityBoardingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class BoardingActivity extends AppCompatActivity {

    ActivityBoardingBinding binding;

    private final String TAG = BoardingActivity.class.getSimpleName();

    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    private String route = "47";
    private String language = "English";
    private Context context;

    //Firebase
    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;

    //MBTA API
    ProgressDialog pd;
    ArrayList<Stop> stopList;
    ArrayList<String> arrivalTimes;
    String arrivalTime;
    long highMinToArrival = 999999;

    //GPS
    LocationManager locationManager;
    LocationListener locationListener;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding);
        context = getBaseContext();
        Intent receivingIntent = getIntent();
        if (receivingIntent.getStringExtra(BUS_ROUTE_EXTRA)!=null) {

            route = receivingIntent.getStringExtra(BUS_ROUTE_EXTRA);
            language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_boarding);
        Typeface ticketing = Typeface.createFromAsset(getAssets(), "fonts/ticketing.ttf");
        arrivalTimes = new ArrayList<>();
        arrivalTime = "";
        stopList = new ArrayList<>();

        binding.boardingRoute.setTypeface(ticketing);
        binding.boardingArrivalTime.setTypeface(ticketing);
        binding.boardingArrivesIn.setTypeface(ticketing);
        binding.boardingDepartsFrom.setTypeface(ticketing);
        binding.boardingExtraArrivalTimes.setTypeface(ticketing);
        binding.boardingLocationAddress.setTypeface(ticketing);
        binding.boardingLocationName.setTypeface(ticketing);
        binding.boardingPayment.setTypeface(ticketing);
        binding.boardingPaymentAmount.setTypeface(ticketing);
//
//        new JsonTask().execute("https://api-v3.mbta.com/schedules?filter[stop]=43551");

        database = Utils.getDatabase();
        routesRef = database.getReference(language + "/routes");
        routesRef.child(route).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                binding.boardingRoute.setText(dataSnapshot.getKey());
                for (DataSnapshot routeInformationSnapshot : dataSnapshot.getChildren()) {
                    if (routeInformationSnapshot.getKey().equals("stops")) {
                        for (DataSnapshot stopSnapshot : routeInformationSnapshot.getChildren()) {
                            stopList.add(new Stop(stopSnapshot.child("address").getValue().toString(),
                                    stopSnapshot.child("id").getValue().toString(),
                                    stopSnapshot.getKey()));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                //Commented out while using button to debug
                try {
                    findClosestStop(location);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (language.equals("Chinese")) {
            binding.boardingPayment.setText(R.string.board_cost_title_ch);
//            binding.cost.setText(R.string.board_cost_ch);
//            binding.descriptionTitle.setText(R.string.board_description_title_ch);
//            binding.description.setText(R.string.board_description_ch);
            binding.boardingArrivesIn.setText(R.string.board_time_title_ch);
//            binding.time.setText(R.string.board_time_ch);
//            binding.instructionsTitle.setText(R.string.board_instructions_title_ch);
//            binding.instructions.setText(R.string.board_instructions_ch);
            binding.boardingBoardNow.setText(R.string.board_start_ch);
        } else {
            binding.boardingPayment.setText(R.string.board_cost_title);
//            binding.cost.setText(R.string.board_cost);
//            binding.descriptionTitle.setText(R.string.board_description_title);
//            binding.description.setText(R.string.board_description);
            binding.boardingArrivesIn.setText(R.string.board_time_title);
//            binding.time.setText(R.string.board_time);
//            binding.instructionsTitle.setText(R.string.board_instructions_title);
//            binding.instructions.setText(R.string.board_instructions);
            binding.boardingBoardNow.setText(R.string.board_start);
        }

        checkPermission();

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    public void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Toast.makeText(context, "no permission", Toast.LENGTH_SHORT).show();
//                ActivityCompat#requestPermissions
//             here to request the missing permissions, and then overriding
//               public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                                      int[] grantResults)
//             to handle the case where the user grants the permission. See the documentation
//             for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    return;

                } else {
                    Log.d(TAG, "permission denied");
                }
            }
        }
    }


    public void startTour(View view) {

        Intent intent = new Intent(context, ChangingTourActivity.class);

        intent.putExtra(BUS_ROUTE_EXTRA, route);
        intent.putExtra(LANGUAGE_EXTRA, language);
        context.startActivity(intent);

    }

    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(BoardingActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, "result = " + result);
            arrivalTimes = new ArrayList<>();
//            JSONArray arr = null;
//            try {
//                arr = new JSONArray(result);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
            if (result == null) {
                return;
            }
            JSONObject json = null;
            try {
                json = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                assert json != null;
                JSONArray dataArray = json.getJSONArray("data");
                Log.d(TAG, "dataArray.length = " + dataArray.length());
                for (int i=0; i < dataArray.length(); i++) {
                    JSONObject firstObject = dataArray.getJSONObject(i);
                    JSONObject attributesObject = firstObject.getJSONObject("attributes");
                    String newArrivalTime = attributesObject.getString("arrival_time");
                    arrivalTimes.add(newArrivalTime);
//                    arrivalTime = newArrivalTime;
                    Log.d(TAG, "arrivalTimes.length = " + arrivalTimes.size());
                }
//                JSONObject firstObject = dataArray.getJSONObject(0);
//                JSONObject attributesObject = firstObject.getJSONObject("attributes");
//                String arrivalTime = attributesObject.getString("arrival_time");
                binding.boardingArrivalTime.setText(closestArrivalTime(arrivalTimes));

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (pd.isShowing()){
                pd.dismiss();
            }
        }
    }

//    private String closestArrivalTime(String arrivalTime) throws ParseException {
//        //Function for prediction API
//        //Using the prediction API, we only get one time.
//        //Using the schedule API, we get three times.
//
//        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
////        "2001-07-04T12:08:56.235-07:00"
//        Date currentTime = Calendar.getInstance().getTime();
//        long minToArrival;
//        Date date = df2.parse(arrivalTime);
//        Log.d(TAG, "date = " + date.toString());
//        Log.d(TAG, "now date = " + currentTime.toString());
////            Log.d(TAG, ""+(date.getTime()-currentTime.getTime()));
//        Log.d(TAG, "date.getTime() = " + date.getTime());
//        Log.d(TAG, "currentTime.getTime() = " + currentTime.getTime());
//        long diff = date.getTime() - currentTime.getTime();
//        long diffMinutes = diff / (60 * 1000) % 60;
//        long diffHours = diff / (60 * 60 * 1000) % 24;
//        minToArrival = diffMinutes + diffHours*60;
//        if (minToArrival<0) {
////                continue;
//            return "missed";
//        }
//        Log.d(TAG, "min = " + (diffMinutes+diffHours*60));
//        return minToArrival+" min";
//    }

    private String closestArrivalTime(ArrayList<String> arrivalTimes) throws ParseException {
        //Using the prediction API, we only get one time.
        //Using the schedule API, we get three times.

        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
//        "2001-07-04T12:08:56.235-07:00"
        Date currentTime = Calendar.getInstance().getTime();
        long smallestMinToArrival = highMinToArrival;
        long secondSmallestMinToArrival = highMinToArrival;
        long thirdSmallestMinToArrival = highMinToArrival;
        long minToArrival;
        for (String ISODate : arrivalTimes) {
//            Log.d(TAG, "closestArrivalTime, inside loop");
            Date date = df2.parse(ISODate);
            Log.d(TAG, "date = " + date.toString());
            Log.d(TAG, "now date = " + currentTime.toString());
//            Log.d(TAG, ""+(date.getTime()-currentTime.getTime()));
            Log.d(TAG, "date.getTime() = " + date.getTime());
            Log.d(TAG, "currentTime.getTime() = " + currentTime.getTime());
            long diff = date.getTime() - currentTime.getTime();
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            minToArrival = diffMinutes + diffHours*60;
            if (minToArrival<0) {
//                continue;
                return "missed";
            }
            if (minToArrival<smallestMinToArrival) {
                thirdSmallestMinToArrival = secondSmallestMinToArrival;
                secondSmallestMinToArrival = smallestMinToArrival;
                smallestMinToArrival = minToArrival;
            } else if (minToArrival<secondSmallestMinToArrival) {
                thirdSmallestMinToArrival = secondSmallestMinToArrival;
                secondSmallestMinToArrival = minToArrival;
            } else if (minToArrival<thirdSmallestMinToArrival) {
                thirdSmallestMinToArrival = minToArrival;
            }
//            long difference = date.getTime()-currentTime.getTime();
//            long hours = difference % (24 * 3600) / 3600; // Calculating Hours
//            long minute = difference % 3600 / 60; // Calculating minutes if there is any minutes difference
//            long min = minute + (hours * 60); // This will be our final minutes. Multiplying by 60 as 1 hour contains 60 mins
            Log.d(TAG, "min = " + (diffMinutes+diffHours*60));
        }
        Log.d(TAG, "first, second, and third arrival times = " + smallestMinToArrival+","+secondSmallestMinToArrival+","+thirdSmallestMinToArrival);
        return smallestMinToArrival+" min";
    }

    public void findClosestStop(Location location) throws ParseException {
        Log.d(TAG, String.valueOf(location.getLatitude()));

        Stop closestStop = new Stop();
        double minDistance = 10000;
        for (Stop stop : stopList) {
            if (stop.distanceFrom(location.getLatitude(), location.getLongitude())<minDistance) {
                minDistance = stop.distanceFrom(location.getLatitude(), location.getLongitude());
                closestStop = stop;
                Log.d(TAG, "closestStop address = " + closestStop.address);
//                Log.d(TAG, String.valueOf(minDistance));
            }
        }
        if (closestStop.address != null) {
            Log.d(TAG, "id = " + closestStop.id);
            if (!binding.boardingLocationAddress.getText().equals(closestStop.id)) {
                //Stop has changed, new call to api
                Log.d(TAG, "Stop has changed, new call to api");
                new JsonTask().execute("https://api-v3.mbta.com/predictions?filter[stop]="+closestStop.id);
                binding.boardingLocationName.setText(closestStop.address);
                binding.boardingLocationAddress.setText(closestStop.id);
            }
//            else if (arrivalTime.equals("")) {
//                //First attempt, new call to api
                  //stop has changed should take care of first attempt
//                Log.d(TAG, "First attempt, new call to api");
//                new JsonTask().execute("https://api-v3.mbta.com/predictions?filter[stop]="+closestStop.id);
//            }
            else if (closestArrivalTime(arrivalTimes).equals("missed")) {
                //Missed the bus, new call to api
                Log.d(TAG, "Missed the bus, new call to api");
                new JsonTask().execute("https://api-v3.mbta.com/predictions?filter[stop]="+closestStop.id);
            } else {
                binding.boardingArrivalTime.setText(closestArrivalTime(arrivalTimes));
                binding.boardingLocationName.setText(closestStop.address);
                binding.boardingLocationAddress.setText(closestStop.id);
            }
//            new JsonTask().execute("https://api-v3.mbta.com/schedules?filter[stop]="+closestStop.id);
        } else {
            Log.d(TAG, "was no closest stop");
        }
    }
}
