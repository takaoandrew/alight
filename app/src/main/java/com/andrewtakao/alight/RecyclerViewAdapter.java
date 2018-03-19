package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
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

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.BusRouteViewHolder> {
    private Context context;
    private ArrayList<POIAdapter> poiAdapters;
    private final String TAG = RecyclerViewAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    LayoutInflater inflater;

    public RecyclerViewAdapter(Context context, ArrayList<POIAdapter> poiAdapters) {
        this.context = context;
        this.poiAdapters = poiAdapters;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public BusRouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_view, parent, false);
        return new BusRouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BusRouteViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder-- position, poiAdapters.get(position) = " + position + ", "
        + poiAdapters.get(position));
        final POIAdapter poiAdapter = poiAdapters.get(position);
        holder.recyclerView.setAdapter(poiAdapter);
    }

    @Override
    public int getItemCount() {
//        Log.d(TAG, "getItemCount-- count = " + busRoutes.size());
        return poiAdapters.size();
    }

    class BusRouteViewHolder extends RecyclerView.ViewHolder {

        RecyclerView recyclerView;

        public BusRouteViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.rv_recycler_view);
        }
    }




}
