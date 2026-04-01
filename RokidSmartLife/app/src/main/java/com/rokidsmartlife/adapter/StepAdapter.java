package com.rokidsmartlife.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.R;
import com.rokidsmartlife.api.RouteResult;
import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {

    private final List<RouteResult.Step> steps;

    public StepAdapter(List<RouteResult.Step> steps) {
        this.steps = steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteResult.Step step = steps.get(position);

        holder.stepNum.setText(String.valueOf(position + 1));
        holder.actionIcon.setText(step.getActionIcon());
        holder.instruction.setText(step.instruction != null ? step.instruction : "");

        String meta = "";
        if (step.distance != null) {
            try {
                int d = Integer.parseInt(step.distance);
                meta = d >= 1000 ? String.format("%.1fkm", d / 1000.0) : d + "m";
            } catch (NumberFormatException e) {
                meta = step.distance;
            }
        }
        holder.distance.setText(meta);

        holder.itemView.setFocusable(true);
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(8);
            bg.setColor(hasFocus ? 0xFF2962FF : 0xFF1E1E1E);
            v.setBackground(bg);
        });
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stepNum, actionIcon, instruction, distance;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNum = itemView.findViewById(R.id.step_num);
            actionIcon = itemView.findViewById(R.id.step_action_icon);
            instruction = itemView.findViewById(R.id.step_instruction);
            distance = itemView.findViewById(R.id.step_distance);
        }
    }
}
