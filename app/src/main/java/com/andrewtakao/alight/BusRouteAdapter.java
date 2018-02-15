package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by andrewtakao on 2/12/18.
 */

public class BusRouteAdapter extends RecyclerView.Adapter<BusRouteAdapter.BusRouteViewHolder> {
    private Context context;
    private ArrayList<String> busRoutes;
    private final String TAG = BusRouteAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    LayoutInflater inflater;

    public BusRouteAdapter(Context context, ArrayList<String> busRoutes) {
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
        holder.busRoute.setText(busRoutes.get(position));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onBindViewHolder-- onClick-- position, busRoutes.get(position) = "
                        + position + ", " + busRoutes.get(position));
                //Toggle between TourActivity and OrderedTourActivity
//                Intent intent = new Intent(context, TourActivity.class);
                Intent intent = new Intent(context, OrderedTourActivity.class);

                intent.putExtra(BUS_ROUTE_EXTRA, busRoutes.get(position));
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

        TextView busRoute;

        public BusRouteViewHolder(View itemView) {
            super(itemView);
            busRoute = itemView.findViewById(R.id.bus_route);
        }
    }
}
