package com.rokidsmartlife.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.R;
import com.rokidsmartlife.utils.PoiCategory;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final PoiCategory[] categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onClick(PoiCategory category);
    }

    public CategoryAdapter(PoiCategory[] categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PoiCategory cat = categories[position];
        holder.iconText.setText(cat.icon);
        holder.nameText.setText(cat.name);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(12);
        bg.setColor(0xFF2A2A2A);
        holder.itemView.setBackground(bg);

        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                GradientDrawable focusBg = new GradientDrawable();
                focusBg.setShape(GradientDrawable.RECTANGLE);
                focusBg.setCornerRadius(12);
                focusBg.setColor(cat.color);
                v.setBackground(focusBg);
                v.setScaleX(1.05f);
                v.setScaleY(1.05f);
            } else {
                GradientDrawable normalBg = new GradientDrawable();
                normalBg.setShape(GradientDrawable.RECTANGLE);
                normalBg.setCornerRadius(12);
                normalBg.setColor(0xFF2A2A2A);
                v.setBackground(normalBg);
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(cat);
        });
    }

    @Override
    public int getItemCount() {
        return categories.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iconText, nameText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconText = itemView.findViewById(R.id.category_icon);
            nameText = itemView.findViewById(R.id.category_name);
        }
    }
}
