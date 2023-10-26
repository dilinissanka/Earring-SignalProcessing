package com.example.ble_keyboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// These are the images that we will be using for the recyclerView that we will be getting
import com.squareup.picasso.Picasso;
import android.widget.ImageView;

public class recyclerAdapter extends RecyclerView.Adapter<recyclerAdapter.ViewHolder> {

    private List<Necklace> necklaceList;

    public recyclerAdapter(List<Necklace> items) {
        this.necklaceList = items;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.necklace_component, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        Necklace item = necklaceList.get(position);
        holder.bind(item);
        holder.itemView.setClickable(false);
        holder.itemView.setFocusable(false);
    }

    @Override
    public int getItemCount() {
        return necklaceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final View view;

        public ViewHolder(@NotNull View view) {
            super(view);
            this.view = view;
        }

        public void bind(Necklace result) {
            view.setClickable(false);
            view.setFocusable(false);

            // Here we will be setting the images with the Picasso import
            ImageView heartImage = view.findViewById(R.id.necklace_image);
            String imageUrl = "https://icons-for-free.com/iconfiles/png/512/colored+necklace-1319964984513348142.png";
            Picasso.get().load(imageUrl).into(heartImage);
            heartImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

            ImageView connectionImage = view.findViewById(R.id.connection_image);
            String imageUrl2 = "https://cdn0.iconfinder.com/data/icons/social-media-2219/50/72-512.png";
            Picasso.get().load(imageUrl2).into(connectionImage);
            connectionImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

            ImageView heartRateImage = view.findViewById(R.id.heart_rate_image);
            String imageUrl3 = "https://cdn-icons-png.flaticon.com/512/865/865969.png";
            Picasso.get().load(imageUrl3).into(heartRateImage);
            heartRateImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

            TextView necklace_name = view.findViewById(R.id.necklace_name);
            TextView connection_status = view.findViewById(R.id.connection_value);
            TextView heart_rate_status = view.findViewById(R.id.heart_rate_value);

            necklace_name.setText(result.getName() != null ? result.getName() : "Unnamed");
            connection_status.setText("" + result.getConnectionStatus());
            heart_rate_status.setText("" + result.getHeartRate() + "bpm");
        }
    }
}
