package com.andrewtakao.alight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.andrewtakao.alight.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    private final String TAG = SplashActivity.class.getSimpleName();
    Context context;
    FirebaseDatabase database;
    private final String LANGUAGE_EXTRA = "language_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mAuth = FirebaseAuth.getInstance();
        database = Utils.getDatabase();
        context = getBaseContext();

        Intent receivingIntent = getIntent();
        Log.d(TAG, "receiving intent = " + receivingIntent);


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser()!=null) {
            String userid = mAuth.getCurrentUser().getUid();
            database.getReference("users/" + userid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String language = (String) dataSnapshot.child("language").getValue();
                    if (language == null) {
                        Intent intent = new Intent(context, LanguageActivity.class);
                        startActivity(intent);
                    } else if (language.equals("english")){
                        Intent intent = new Intent(context, RoutePreviewActivity.class);
                        intent.putExtra(LANGUAGE_EXTRA, "English");
                        startActivity(intent);
                    } else if (language.equals("chinese")){
                        Intent intent = new Intent(context, RoutePreviewActivity.class);
                        intent.putExtra(LANGUAGE_EXTRA, "Chinese");
                        startActivity(intent);
                    } else {
                        Log.d(TAG, "Unable to handle language, language = " + language);
                    }
                    Log.d(TAG, "language = " + language);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Intent intent = new Intent(context, LoginActivity.class);
            startActivity(intent);
        }

    }
}
