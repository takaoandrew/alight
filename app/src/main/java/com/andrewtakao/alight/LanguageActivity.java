package com.andrewtakao.alight;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class LanguageActivity extends AppCompatActivity {
    private final String LANGUAGE_EXTRA = "language_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
    }

    public void launchEnglish(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(LANGUAGE_EXTRA, "English");
        startActivity(intent);
    }
    public void launchChinese(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(LANGUAGE_EXTRA, "Chinese");
        startActivity(intent);
    }
}
