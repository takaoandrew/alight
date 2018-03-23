package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by andrewtakao on 2/12/18.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewViewHolder> {
    private Context context;
    private ArrayList<ArrayList<POI>> poiArrayListArrayList;
    private HashMap<String, Route> busRoutes;
    private final String TAG = RecyclerViewAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    LayoutInflater inflater;

    public RecyclerViewAdapter(Context context, ArrayList<ArrayList<POI>> poiArrayListArrayList, HashMap<String, Route> busRoutes) {
        this.context = context;
        this.poiArrayListArrayList = poiArrayListArrayList;
        this.inflater = LayoutInflater.from(context);
        this.busRoutes = busRoutes;
    }

    @Override
    public RecyclerViewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recycler_view, parent, false);
        return new RecyclerViewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewViewHolder holder, final int position) {
        //Don't show empties
        if (poiArrayListArrayList.get(position).size() == 0) {
            return;
        }

//        Log.d(TAG, "onBindViewHolder-- position, poiArrayListArrayList.get(position) = " + position + ", "
//        + poiArrayListArrayList.get(position));
        final POI firstPOI = poiArrayListArrayList.get(position).get(0);
        Log.d(TAG, "at position " + position + ", firstPOI.imageName = " + firstPOI.imageName);
        Log.d(TAG, "busRoutes.get(position).firebaseCount = " + busRoutes.get(firstPOI.route).firebaseCount);
        Log.d(TAG, "busRoutes.get(position).downloadCount = " + busRoutes.get(firstPOI.route).downloadedCount);
        String route = "";
        if (firstPOI.route!=null) {
            route = firstPOI.route;
            if (busRoutes.get(firstPOI.route).firebaseCount != busRoutes.get(firstPOI.route).downloadedCount) {
                holder.downloadView.setVisibility(View.VISIBLE);
            } else {
                holder.downloadView.setVisibility(View.INVISIBLE);
            }
            holder.title.setText(firstPOI.route);
            holder.downloadView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((RoutePreviewActivity)context).downloadPOIs(firstPOI.route, RoutePreviewActivity.language);
                }
            });

        }
        final POIAdapter poiAdapter = new POIAdapter(context, poiArrayListArrayList.get(position));
        holder.recyclerView.setAdapter(poiAdapter);
        holder.recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.recyclerView.setLayoutManager(layoutManager);

        holder.chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firstPOI.route == null) {
                    return;
                }
                Intent intent = new Intent(context, BoardingActivity.class);
                intent.putExtra(BUS_ROUTE_EXTRA, firstPOI.route);
                intent.putExtra(LANGUAGE_EXTRA, RoutePreviewActivity.language);
                context.startActivity(intent);
            }
        });

        holder.seekBar.setMax(poiArrayListArrayList.get(position).size()-1);
        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                ((RoutePreviewActivity)context).scrollToPosition(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.d(TAG, "seekbar.getProgress = " + seekBar.getProgress());
//                ((RoutePreviewActivity)context).scrollToPosition(seekBar.getProgress());
            }
        });

    }

    @Override
    public int getItemCount() {
//        Log.d(TAG, "getItemCount-- count = " + busRoutes.size());
        return poiArrayListArrayList.size();
    }


    class RecyclerViewViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        RecyclerView recyclerView;
        ImageView downloadView;
        SeekBar seekBar;
        Button chooseButton;

        public RecyclerViewViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            recyclerView = itemView.findViewById(R.id.rv_recycler_view);
            downloadView = itemView.findViewById(R.id.download_button);
            seekBar = itemView.findViewById(R.id.sb_poi);
            chooseButton = itemView.findViewById(R.id.choose_poi);

        }


    }




}
