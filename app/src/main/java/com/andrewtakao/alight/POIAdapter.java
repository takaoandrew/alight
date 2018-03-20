package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by andrewtakao on 2/12/18.
 */

public class POIAdapter extends RecyclerView.Adapter<POIAdapter.POIViewHolder> {
    private Context context;
    public ArrayList<POI> poiArrayList;
    private final String TAG = POIAdapter.class.getSimpleName();
    private final String BUS_ROUTE_EXTRA = "bus_route_extra";
    LayoutInflater inflater;
    Handler handler;
    Runnable runnable;

    public POIAdapter(Context context, ArrayList<POI> poiArrayList) {
        this.context = context;
        Log.d(TAG, "public POIAdapter");
//
//        Collections.sort(poiArrayList, new Comparator<POI>() {
//            @Override
//            public int compare(POI poi, POI poi2) {
//                return poi.imageName.compareTo(poi2.imageName);
//            }
//        });

        this.poiArrayList = poiArrayList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public POIViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.poi_row, parent, false);
        return new POIViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final POIViewHolder holder, final int position) {
        final POI poi = poiArrayList.get(position);
        final String fileName = poi.imageLocalStorageLocation;
        holder.backgroundImage.setImageDrawable(null);
        Log.d(TAG, "onBindViewHolder-- position, MainActivity.poiArrayList.get(position) = " + position + ", "
        + fileName);

        if (fileName != null) {
            Log.d(TAG, "onBindViewHolder-- fileName (localFile) is " + fileName);

            Log.d(TAG, "onBindViewHolder-- Uri.parse(fileName) = " + Uri.parse(fileName));

            Picasso.with(context).load(new File(fileName))
//                    .placeholder(R.drawable.profile_wall_picture)
//                    .resize(holder.backgroundImage.getWidth(), holder.backgroundImage.getHeight())
                    .fit()
                    .centerCrop()
                    .into(holder.backgroundImage);
        } else {
            Log.d(TAG, "onBindViewHolder-- fileName is null, downloading image");
        }


        String location = poi.latitude + ", " + poi.longitude;
        holder.locationView.setText(location);
        holder.imageNameView.setText(userFriendlyName(poi.imageName));

//        holder.backgroundImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "You clicked on poi.imageName " + poi.imageName);
//                Log.d(TAG, "You clicked on poi.imageStorageLocation " + poi.imageLocalStorageLocation);
//                Log.d(TAG, "You clicked on holder.imageNameView " + holder.imageNameView);
//                Log.d(TAG, "You clicked on holder.locationView " + holder.locationView);
//
//                ((ChangingTourActivity)context).showMediaButtons();
//                //Make unclickable
////                holder.backgroundImage.setClickable(false);
//                if (handler != null && runnable != null) {
//                    handler.removeCallbacks(runnable);
//                }
//                runnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        ((ChangingTourActivity)context).hideMediaButtons();
//                        holder.backgroundImage.setClickable(true);
//                    }
//                };
//                handler = new Handler();
//                handler.postDelayed(runnable, 3000);
//
////                if (OrderedTourActivity.mMediaPlayer!=null && OrderedTourActivity.mMediaPlayer.isPlaying()) {
////                    OrderedTourActivity.mMediaPlayer.stop();
////                } else {
////                    String fileName;
////                    fileName = poi.audioLocalStorageLocation;
////                    if (fileName != null) {
////                        OrderedTourActivity.mMediaPlayer = MediaPlayer.create(OrderedTourActivity.mContext, Uri.parse(fileName));
////                        OrderedTourActivity.mMediaPlayer.start();
////                    }
////                }
//
//            }
//        });
    }

    public void updateAdapter(ArrayList<POI> poiArrayList) {
        Log.d(TAG, "updateAdapter");
        ///TODO Order commented may ruin everything
        //Remove order and hope for the best
//        Collections.sort(poiArrayList, new Comparator<POI>() {
//            @Override
//            public int compare(POI poi, POI poi2) {
//                if (poi.order>poi2.order) {
//                    return 1;
//                } else if (poi.order == poi2.order) {
//                    return 0;
//                }
//                return -1;
//            }
//        });
        this.poiArrayList = poiArrayList;
    }

    @Override
    public int getItemCount() {
//        Log.d(TAG, "getItemCount-- count = " + poiArrayList.size());
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

    private String readableKey(String key) {
        return key.replace("*", ".");
    }

    private String userFriendlyName(String name) {
        name = name.substring(0, name.indexOf("*"));
        return name.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );

    }

}
