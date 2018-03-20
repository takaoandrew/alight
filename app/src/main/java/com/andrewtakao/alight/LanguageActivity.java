package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LanguageActivity extends AppCompatActivity {
    private final String LANGUAGE_EXTRA = "language_extra";
    private final String TAG = LanguageActivity.class.getSimpleName();
    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    String userId;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getBaseContext();
        database = Utils.getDatabase();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        setContentView(R.layout.activity_language);
        database.getReference("users/"+userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String username = (String) dataSnapshot.child("userName").getValue();
                if (username != null) {
                    Toast.makeText(context,
                            "Welcome, " + username,
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void launchEnglish(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(LANGUAGE_EXTRA, "English");
        DatabaseReference userRef = database.getReference("users/"+userId);
        userRef.child("language").setValue("english");
        startActivity(intent);
    }
    public void launchChinese(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(LANGUAGE_EXTRA, "Chinese");
        DatabaseReference userRef = database.getReference("users/"+userId);
        userRef.child("language").setValue("chinese");
        startActivity(intent);
    }
}
