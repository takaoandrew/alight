package com.andrewtakao.alight;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
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

    public POIAdapter(Context context, ArrayList<POI> poiArrayList) {
        this.context = context;
        Collections.sort(poiArrayList, new Comparator<POI>() {
            @Override
            public int compare(POI poi, POI poi2) {
                return poi.imageName.compareTo(poi2.imageName);
            }
        });

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
        POI poi = poiArrayList.get(position);
        Log.d(TAG, "onBindViewHolder-- position, MainActivity.poiArrayList.get(position) = " + position + ", "
        + poi.imageName);
//TODO Toggle this bug fix by forty vs ordered
        OrderedTourActivity.mStorageRef.child(readableKey(poi.imageName)).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//        FortySevenTourActivity.mStorageRef.child(readableKey(poi.imageName)).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                // Pass it to Picasso to download, show in ImageView and caching
                Picasso.with(context).load(uri.toString())
//                        .resize(holder.backgroundImage.getWidth(), holder.backgroundImage.getHeight())
                        .fit()
                        .into(holder.backgroundImage);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
//        holder.backgroundImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                playAudio();
//            }
//        });
        String location = poi.latitude + ", " + poi.longitude;
        holder.locationView.setText(location);
        holder.imageNameView.setText(userFriendlyName(poi.imageName));
    }

    public void updateAdapter(ArrayList<POI> poiArrayList) {
        Log.d(TAG, "updateAdapter");
        Collections.sort(poiArrayList, new Comparator<POI>() {
            @Override
            public int compare(POI poi, POI poi2) {
                if (poi.order>poi2.order) {
                    return 1;
                } else if (poi.order == poi2.order) {
                    return 0;
                }
                return -1;
            }
        });
        this.poiArrayList = poiArrayList;
        notifyDataSetChanged();
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

    public void playAudio() {
        Log.d(TAG, "playAudio pressed");

        String fileName;

        if (OrderedTourActivity.mPOIHashMap.get(OrderedTourActivity.currentKey) != null) {
            fileName = OrderedTourActivity.mPOIHashMap.get(OrderedTourActivity.currentKey).audioLocalStorageLocation;
            Log.d(TAG, "playAudio-- fileName = " + fileName);
        } else {
            fileName = null;
        }

//        if (mMediaPlayer!=null) {
//            if (mMediaPlayer.isPlaying()) {
//                mMediaPlayer.stop();
//            }
//
//        }
        if (OrderedTourActivity.mMediaPlayer != null ) {
            if (fileName != null) {
                if (OrderedTourActivity.mMediaPlayer.isPlaying()) {
                    OrderedTourActivity.mMediaPlayer.stop();
                }
                else {
                    OrderedTourActivity.mMediaPlayer = MediaPlayer.create(OrderedTourActivity.mContext, Uri.parse(fileName));
                    OrderedTourActivity.mMediaPlayer.start();
                }

            }
        } else {
            OrderedTourActivity.mMediaPlayer = new MediaPlayer();
        }
    }

}
