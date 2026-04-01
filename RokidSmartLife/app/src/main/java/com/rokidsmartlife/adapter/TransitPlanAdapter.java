package com.rokidsmartlife.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.R;
import com.rokidsmartlife.api.TransitResult;
import java.util.List;

public class TransitPlanAdapter extends RecyclerView.Adapter<TransitPlanAdapter.ViewHolder> {

    private final List<TransitResult.TransitPlan> plans;
    private final OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onClick(TransitResult.TransitPlan plan, int position);
    }

    public TransitPlanAdapter(List<TransitResult.TransitPlan> plans, OnPlanClickListener listener) {
        this.plans = plans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transit_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransitResult.TransitPlan plan = plans.get(position);

        holder.indexText.setText("方案 " + (position + 1));
        holder.durationText.setText(plan.getFormattedDuration());
        holder.costText.setText(plan.getFormattedCost());

        StringBuilder routeSummary = new StringBuilder();
        if (plan.segments != null) {
            for (int i = 0; i < plan.segments.size(); i++) {
                TransitResult.Segment seg = plan.segments.get(i);
                if (i > 0) routeSummary.append(" → ");
                if (seg.type == TransitResult.Segment.TYPE_WALKING) {
                    routeSummary.append("步行").append(seg.getFormattedDistance());
                } else {
                    routeSummary.append(seg.getIcon());
                    if (seg.lineName != null) {
                        String shortName = seg.lineName;
                        int paren = shortName.indexOf("(");
                        if (paren > 0) shortName = shortName.substring(0, paren);
                        routeSummary.append(shortName);
                    }
                }
            }
        }
        holder.routeText.setText(routeSummary.toString());

        if (plan.nightflag) {
            holder.nightText.setVisibility(View.VISIBLE);
        } else {
            holder.nightText.setVisibility(View.GONE);
        }

        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(10);
            bg.setColor(hasFocus ? 0xFF1A73E8 : 0xFF1E1E1E);
            v.setBackground(bg);
            v.setElevation(hasFocus ? 4 : 0);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(plan, position);
        });
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView indexText, durationText, costText, routeText, nightText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indexText = itemView.findViewById(R.id.plan_index);
            durationText = itemView.findViewById(R.id.plan_duration);
            costText = itemView.findViewById(R.id.plan_cost);
            routeText = itemView.findViewById(R.id.plan_route);
            nightText = itemView.findViewById(R.id.plan_night);
        }
    }
}
