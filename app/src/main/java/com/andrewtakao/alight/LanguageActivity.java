package com.andrewtakao.alight;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LanguageActivity extends AppCompatActivity {
    private final String LANGUAGE_EXTRA = "language_extra";
    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = Utils.getDatabase();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        setContentView(R.layout.activity_language);
        Toast.makeText(this,
                "Welcome, " + userId,
                Toast.LENGTH_LONG).show();
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
