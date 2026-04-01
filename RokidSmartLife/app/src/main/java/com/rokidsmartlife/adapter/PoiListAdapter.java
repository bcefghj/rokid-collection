package com.rokidsmartlife.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.R;
import com.rokidsmartlife.api.PoiResult;
import java.util.ArrayList;
import java.util.List;

public class PoiListAdapter extends RecyclerView.Adapter<PoiListAdapter.ViewHolder> {

    private final List<PoiResult> items = new ArrayList<>();
    private final OnPoiClickListener listener;

    public interface OnPoiClickListener {
        void onClick(PoiResult poi);
    }

    public PoiListAdapter(OnPoiClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PoiResult> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItems(List<PoiResult> moreItems) {
        int start = items.size();
        items.addAll(moreItems);
        notifyItemRangeInserted(start, moreItems.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PoiResult poi = items.get(position);

        holder.indexText.setText(String.valueOf(position + 1));
        holder.nameText.setText(poi.name);
        holder.iconText.setText(poi.getCategoryIcon());

        String meta = "";
        String rating = poi.getFormattedRating();
        String cost = poi.getFormattedCost();
        String dist = poi.getFormattedDistance();

        if (!rating.isEmpty()) meta += "★" + rating;
        if (!cost.isEmpty()) meta += (meta.isEmpty() ? "" : "  ") + cost;
        if (!dist.isEmpty()) meta += (meta.isEmpty() ? "" : "  ") + dist;
        holder.metaText.setText(meta);

        String addr = poi.address != null ? poi.address : "";
        if (addr.length() > 22) addr = addr.substring(0, 22) + "...";
        holder.addressText.setText(addr);

        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(10);
            if (hasFocus) {
                bg.setColor(0xFF1A73E8);
                v.setElevation(4);
            } else {
                bg.setColor(0xFF1E1E1E);
                v.setElevation(0);
            }
            v.setBackground(bg);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(poi);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView indexText, iconText, nameText, metaText, addressText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.poi_index);
            iconText = itemView.findViewById(R.id.poi_icon);
            nameText = itemView.findViewById(R.id.poi_name);
            metaText = itemView.findViewById(R.id.poi_meta);
            addressText = itemView.findViewById(R.id.poi_address);
        }
    }
}
