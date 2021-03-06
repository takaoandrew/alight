package com.andrewtakao.alight;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import static com.andrewtakao.alight.Utils.fileExist;
import static com.andrewtakao.alight.Utils.readableKey;
import static com.andrewtakao.alight.Utils.userFriendlyName;

/**
 * Created by andrewtakao on 2/12/18.
 */

public class POITourAdapter extends RecyclerView.Adapter<POITourAdapter.POIViewHolder> {
    private Context context;
    public ArrayList<POI> poiArrayList;
    private final String TAG = POITourAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    LayoutInflater inflater;
    Handler handler;
    Runnable runnable;

    public POITourAdapter(Context context, ArrayList<POI> poiArrayList) {
        this.context = context;
        this.poiArrayList = poiArrayList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public POIViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.poi_tour_row, parent, false);
        return new POIViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final POIViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder-- position, poiHistory.size, poiArrayList.size = " +
                position + ", " + ChangingTourActivity.poiHistory.size() + ", " + poiArrayList.size());
        for (int i = 0; i<position; i++) {
            Log.d(TAG, "poiArrayList.get("+i+") = " + poiArrayList.get(i).imageName);
        }
        final POI poi = poiArrayList.get(position);
        if (poi == null) {
            Toast.makeText(context, "poi couldn't be found", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "poi couldn't be found");
            return;
        }
        String route = poi.route;
        String key = poi.imageName;
        String fileName = (String) context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language+"/"+route+"/"+readableKey(key);
//        Log.d(TAG, "addImage-- check fileexists for " + fileName);
        if (!fileExist(fileName)) {
//            Log.d(TAG, "onBindViewHolder-- couldn't find non-filler poi, key = " + key);
            fileName = context.getFilesDir().getPath()+"/"+RoutePreviewActivity.language+"/"+route+"/filler/"+readableKey(key);
            if (!fileExist(fileName)) {
                Log.d(TAG, "onBindViewHolder-- couldn't find filler poi, key = " + key);
                return;
            }
        }

        holder.backgroundImage.setImageDrawable(null);

        Picasso.with(context).load(new File(fileName))
//                    .placeholder(R.drawable.profile_wall_picture)
//                    .resize(holder.backgroundImage.getWidth(), holder.backgroundImage.getHeight())
                .fit()
                .centerCrop()
                .into(holder.backgroundImage
                , new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {

                            }}
                            );

        String location = poi.latitude + ", " + poi.longitude;
        holder.locationView.setText(location);
        holder.imageNameView.setText(userFriendlyName(poi.imageName));
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount-- count = " + poiArrayList.size());
        return poiArrayList.size();
    }

    class POIViewHolder extends RecyclerView.ViewHolder {

        ImageView backgroundImage;
        TextView locationView, imageNameView;

        public POIViewHolder(View itemView) {
            super(itemView);
            //Set is Recyclable fixes wrong images being displayed. AMAZING!!!
            setIsRecyclable(false);
            backgroundImage = itemView.findViewById(R.id.ordered_tour_background_image);
            locationView = itemView.findViewById(R.id.location);
            imageNameView = itemView.findViewById(R.id.image_name);
        }
    }

}
