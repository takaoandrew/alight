package com.andrewtakao.alight;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class AlightActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alight);

        Intent intent = getIntent();
        Toast.makeText(this, intent.getStringExtra("TESTSTRING"), Toast.LENGTH_SHORT).show();

    }
}
