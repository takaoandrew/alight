package com.andrewtakao.alight;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.andrewtakao.alight.databinding.ActivityBoardingMitBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BoardingMITActivity extends AppCompatActivity {

    ActivityBoardingMitBinding binding;

    private final String TAG = BoardingMITActivity.class.getSimpleName();

    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    private String route;
    private String language;
    private Context context;

    //Firebase
    public static FirebaseDatabase database;
    public static DatabaseReference routesRef;

    //MBTA API
    ProgressDialog pd;

    //GPS
    LocationManager locationManager;
    LocationListener locationListener;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3463;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_mit);
        context = getBaseContext();
        Intent receivingIntent = getIntent();
        route = receivingIntent.getStringExtra(BUS_ROUTE_EXTRA);
        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_boarding_mit);

        binding.busImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new JsonTask().execute("https://api-v3.mbta.com/predictions?filter[stop]=50");
            }
        });

        database = Utils.getDatabase();
        routesRef = database.getReference(language+"/routes");
        routesRef.child(route).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot routeInformationSnapshot : dataSnapshot.getChildren()) {
                    if (routeInformationSnapshot.getKey().equals("cost")) {
                        binding.cost.setText(routeInformationSnapshot.getValue().toString());
                    } else if (routeInformationSnapshot.getKey().equals("time")) {
                        binding.time.setText(routeInformationSnapshot.getValue().toString());
                    } else if (routeInformationSnapshot.getKey().equals("description")) {
                        binding.description.setText(routeInformationSnapshot.getValue().toString());
                    } else if (routeInformationSnapshot.getKey().equals("directions")) {
                        binding.instructions.setText(routeInformationSnapshot.getValue().toString());
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
                findClosestStop(location);
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
            binding.costTitle.setText(R.string.board_cost_title_ch);
//            binding.cost.setText(R.string.board_cost_ch);
            binding.descriptionTitle.setText(R.string.board_description_title_ch);
//            binding.description.setText(R.string.board_description_ch);
            binding.timeTitle.setText(R.string.board_time_title_ch);
//            binding.time.setText(R.string.board_time_ch);
            binding.instructionsTitle.setText(R.string.board_instructions_title_ch);
//            binding.instructions.setText(R.string.board_instructions_ch);
            binding.start.setText(R.string.board_start_ch);
            binding.busImage.setImageResource(R.drawable.board_me_ch);
        } else {
            binding.busImage.setImageResource(R.drawable.board_me);
            binding.costTitle.setText(R.string.board_cost_title);
//            binding.cost.setText(R.string.board_cost);
            binding.descriptionTitle.setText(R.string.board_description_title);
//            binding.description.setText(R.string.board_description);
            binding.timeTitle.setText(R.string.board_time_title);
//            binding.time.setText(R.string.board_time);
            binding.instructionsTitle.setText(R.string.board_instructions_title);
//            binding.instructions.setText(R.string.board_instructions);
            binding.start.setText(R.string.board_start);
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

            pd = new ProgressDialog(BoardingMITActivity.this);
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
            JSONObject json = null;
            try {
                json = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                assert json != null;
                String team = json.getString("jsonapi");
                binding.instructions.setText(team);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (pd.isShowing()){
                pd.dismiss();
            }
        }
    }

    public void findClosestStop(Location location) {
        Log.d(TAG, String.valueOf(location.getLatitude()));
    }
}
