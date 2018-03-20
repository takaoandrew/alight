package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/12/18.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewViewHolder> {
    private Context context;
    private ArrayList<ArrayList<POI>> poiArrayListArrayList;
    private final String TAG = RecyclerViewAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    LayoutInflater inflater;

    public RecyclerViewAdapter(Context context, ArrayList<ArrayList<POI>> poiArrayListArrayList) {
        this.context = context;
        this.poiArrayListArrayList = poiArrayListArrayList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerViewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_view, parent, false);
        return new RecyclerViewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder-- position, poiAdapters.get(position) = " + position + ", "
        + poiArrayListArrayList.get(position));
        final POIAdapter poiAdapter = new POIAdapter(context, poiArrayListArrayList.get(position));
        holder.recyclerView.setAdapter(poiAdapter);
        holder.recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public int getItemCount() {
//        Log.d(TAG, "getItemCount-- count = " + busRoutes.size());
        return poiArrayListArrayList.size();
    }

    class RecyclerViewViewHolder extends RecyclerView.ViewHolder {

        RecyclerView recyclerView;

        public RecyclerViewViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.rv_recycler_view);
        }
    }




}
