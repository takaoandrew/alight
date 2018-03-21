package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.andrewtakao.alight.databinding.ActivityBoardingBinding;

public class BoardingActivity extends AppCompatActivity {

    ActivityBoardingBinding binding;

    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    private String route;
    private String language;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding);
        context = getBaseContext();
        Intent receivingIntent = getIntent();
        route = receivingIntent.getStringExtra(BUS_ROUTE_EXTRA);
        language = receivingIntent.getStringExtra(LANGUAGE_EXTRA);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_boarding);

        if (language.equals("Chinese")) {
            binding.costTitle.setText(R.string.board_cost_title_ch);
            binding.cost.setText(R.string.board_cost_ch);
            binding.descriptionTitle.setText(R.string.board_description_title_ch);
            binding.description.setText(R.string.board_description_ch);
            binding.timeTitle.setText(R.string.board_time_title_ch);
            binding.time.setText(R.string.board_time_ch);
            binding.instructionsTitle.setText(R.string.board_instructions_title_ch);
            binding.instructions.setText(R.string.board_instructions_ch);
            binding.start.setText(R.string.board_start_ch);
            binding.busImage.setImageResource(R.drawable.board_me_ch);
        } else {
            binding.busImage.setImageResource(R.drawable.board_me);
            binding.costTitle.setText(R.string.board_cost_title);
            binding.cost.setText(R.string.board_cost);
            binding.descriptionTitle.setText(R.string.board_description_title);
            binding.description.setText(R.string.board_description);
            binding.timeTitle.setText(R.string.board_time_title);
            binding.time.setText(R.string.board_time);
            binding.instructionsTitle.setText(R.string.board_instructions_title);
            binding.instructions.setText(R.string.board_instructions);
            binding.start.setText(R.string.board_start);
        }

    }

    public void startTour(View view) {

        Intent intent = new Intent(context, ChangingTourActivity.class);

        intent.putExtra(BUS_ROUTE_EXTRA, route);
        intent.putExtra(LANGUAGE_EXTRA, language);
        context.startActivity(intent);

    }
}
