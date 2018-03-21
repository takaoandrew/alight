package com.andrewtakao.alight;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class EndOfTourActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_of_tour);
        if (MainActivity.language.equals("Chinese")) {
            ((TextView) findViewById(R.id.survey_prompt)).setText(R.string.end_of_tour_survey_prompt_ch);
            ((TextView) findViewById(R.id.survey_button)).setText(R.string.end_of_tour_survey_button_ch);
        } else {
            ((TextView) findViewById(R.id.survey_prompt)).setText(R.string.end_of_tour_survey_prompt);
            ((TextView) findViewById(R.id.survey_button)).setText(R.string.end_of_tour_survey_button);
        }
    }

    public void survey(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLScuzW_T_zNU3xb-ZnBoSIvNnKnJAwHbVluTI4IQwSzCzBiuXQ/viewform"));
        startActivity(browserIntent);
    }
}
