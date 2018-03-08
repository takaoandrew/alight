package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/12/18.
 */

public class BusRouteAdapter extends RecyclerView.Adapter<BusRouteAdapter.BusRouteViewHolder> {
    private Context context;
    private ArrayList<Route> busRoutes;
    private final String TAG = BusRouteAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    private final String LANGUAGE_EXTRA = "language_extra";
    LayoutInflater inflater;

    public BusRouteAdapter(Context context, ArrayList<Route> busRoutes) {
        this.context = context;
        this.busRoutes = busRoutes;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public BusRouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.bus_route_row, parent, false);
        return new BusRouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BusRouteViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder-- position, MainActivity.busRoutes.get(position) = " + position + ", "
        + busRoutes.get(position));
        final Route currentRoute = busRoutes.get(position);
        holder.busRoute.setText(currentRoute.route);
        holder.firebaseCount.setText(""+currentRoute.firebaseCount);
        holder.downloadedCount.setText(""+currentRoute.downloadedCount);
        if (currentRoute.firebaseCount != currentRoute.downloadedCount) {
            holder.downloadButton.setVisibility(View.VISIBLE);
        } else {
            holder.downloadButton.setVisibility(View.GONE);
        }
        holder.downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).downloadPOIs(currentRoute.route);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onBindViewHolder-- onClick-- position, busRoutes.get(position) = "
                        + position + ", " + busRoutes.get(position));
                //Toggle between TourActivity and OrderedTourActivity and FortySevenTourActivity
//                Intent intent = new Intent(context, TourActivity.class);
//                Intent intent = new Intent(context, OrderedTourActivity.class);
//                Intent intent = new Intent(context, FortySevenTourActivity.class);
                Intent intent = new Intent(context, ChangingTourActivity.class);

                intent.putExtra(BUS_ROUTE_EXTRA, currentRoute.route);
                intent.putExtra(LANGUAGE_EXTRA, MainActivity.language);
                context.startActivity(intent);
            }
        });
    }






    @Override
    public int getItemCount() {
//        Log.d(TAG, "getItemCount-- count = " + busRoutes.size());
        return busRoutes.size();
    }

    class BusRouteViewHolder extends RecyclerView.ViewHolder {

        TextView busRoute, downloadedCount, firebaseCount;
        ImageButton downloadButton;

        public BusRouteViewHolder(View itemView) {
            super(itemView);
            busRoute = itemView.findViewById(R.id.bus_route);
            downloadedCount = itemView.findViewById(R.id.downloaded_count);
            firebaseCount = itemView.findViewById(R.id.firebase_count);
            downloadButton = itemView.findViewById(R.id.download_button);
        }
    }




}
