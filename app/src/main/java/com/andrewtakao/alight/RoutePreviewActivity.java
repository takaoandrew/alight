package com.andrewtakao.alight;

import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.andrewtakao.alight.databinding.ActivityRoutePreviewBinding;

import java.util.ArrayList;

public class RoutePreviewActivity extends AppCompatActivity {

    ActivityRoutePreviewBinding binding;
    ArrayList<POIAdapter> poiAdapters;
    ArrayList<POI> poiArrayList;
    ArrayList<POI> poiArrayList2;
    ArrayList<String> theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_route_preview);

        theme = new ArrayList<>();
        theme.add("theme test");
        poiArrayList = new ArrayList<>();
        poiArrayList.add(new POI("test", "test",
                "test", "test", "test", "test",
                "test", "test", "test", "test", theme,"test"));
        poiArrayList.add(new POI("test", "test",
                "test", "test", "test", "test",
                "test", "test", "test", "test", theme,"test"));
        poiArrayList2 = new ArrayList<>();
        poiArrayList2.add(new POI("test2", "test2",
                "test2", "test2", "test2", "test2",
                "test2", "test2", "test2", "test2", theme,"test2"));
        poiArrayList2.add(new POI("test2", "test2",
                "test2", "test2", "test2", "test2",
                "test2", "test2", "test2", "test2", theme,"test2"));
        poiAdapters = new ArrayList<>();
        poiAdapters.add(new POIAdapter(this, poiArrayList));
        poiAdapters.add(new POIAdapter(this, poiArrayList2));
        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(this, poiAdapters);
        binding.rvRecyclerViews.setAdapter(recyclerViewAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvRecyclerViews.setLayoutManager(layoutManager);
    }
}
