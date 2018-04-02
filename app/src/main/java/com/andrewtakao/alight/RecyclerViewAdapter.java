package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    public int selected = 0;
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
    public void onBindViewHolder(final RecyclerViewViewHolder holder, final int position) {
        //Don't show empties

        if (poiArrayListArrayList.get(position).size() == 0) {
            return;
        }

        final POI firstPOI = poiArrayListArrayList.get(position).get(0);
        Log.d(TAG, "at position " + position + ", firstPOI.imageName = " + firstPOI.imageName);
//        Log.d(TAG, "busRoutes.get(position).firebaseCount = " + busRoutes.get(firstPOI.route).firebaseCount);
//        Log.d(TAG, "busRoutes.get(position).downloadCount = " + busRoutes.get(firstPOI.route).downloadedCount);

        if (firstPOI.route!=null) {
            if (busRoutes.get(firstPOI.route).firebaseCount != busRoutes.get(firstPOI.route).downloadedCount) {
                holder.downloadView.setVisibility(View.VISIBLE);
            } else {
                holder.downloadView.setVisibility(View.INVISIBLE);
            }
            holder.title.setText(firstPOI.route);
            holder.downloadView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.downloadView.setVisibility(View.INVISIBLE);
                    ((RoutePreviewActivity)context).downloadPOIs(firstPOI.route, RoutePreviewActivity.language);
                }
            });

        }

        holder.chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firstPOI.route == null) {
                    return;
                }
                Intent intent;
                if (firstPOI.route.equals("mit")||firstPOI.route.equals("home")) {
                    intent = new Intent(context, BoardingMITActivity.class);
                } else {
                    intent = new Intent(context, BoardingActivity.class);
                }
                intent.putExtra(BUS_ROUTE_EXTRA, firstPOI.route);
                intent.putExtra(LANGUAGE_EXTRA, RoutePreviewActivity.language);
                context.startActivity(intent);
            }
        });

        holder.seekBar.setMax(poiArrayListArrayList.get(position).size()-1);

        if (selected == position) {
            Log.d(TAG, "selected, position = " + selected + ", " + position);
            holder.seekBar.setClickable(true);
            holder.title.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
        } else {
            Log.d(TAG, "selected, position = " + selected + ", " + position);
            holder.seekBar.setClickable(false);
            holder.title.setBackgroundColor(Color.GRAY);
            holder.seekBar.setProgress(0);
            Log.d(TAG, "setting progress to 0 from onBindViewHolder");
            Log.d(TAG, "selected, position = " + selected + ", " + position);
//            return;
        }

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (selected == position) {
                    ((RoutePreviewActivity)context).scrollToPosition(seekBar.getProgress());
                } else {
                    Log.d(TAG, "setting progress to 0 from onProgresChanged");
                    holder.seekBar.setProgress(0);
                }
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

    public static class RecyclerViewViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener  {

        TextView title;
        ImageView downloadView, chooseButton;
        SeekBar seekBar;

        public RecyclerViewViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            downloadView = itemView.findViewById(R.id.download_button);
            seekBar = itemView.findViewById(R.id.sb_poi);
            chooseButton = itemView.findViewById(R.id.choose_poi);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
//            Toast.makeText(view.getContext(), "You clicked adapter, old, layout " + getAdapterPosition() +", " + getOldPosition()+", " + getLayoutPosition(), Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "You clicked adapter, old, layout " + getAdapterPosition() +", " + getOldPosition()+", " + getLayoutPosition());

            if (((RoutePreviewActivity)view.getContext()).checkDifferent(getLayoutPosition())) {
                ((RoutePreviewActivity)view.getContext()).changeFocus(getLayoutPosition());
                ((RoutePreviewActivity)view.getContext()).scrollToCard(getLayoutPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
//            Log.d(TAG, "Long click");
            return false;
        }
    }




}
