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

public class TransitSegmentAdapter extends RecyclerView.Adapter<TransitSegmentAdapter.ViewHolder> {

    private final List<TransitResult.Segment> segments;

    public TransitSegmentAdapter(List<TransitResult.Segment> segments) {
        this.segments = segments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transit_segment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransitResult.Segment seg = segments.get(position);

        holder.iconText.setText(seg.getIcon());
        holder.typeText.setText(seg.getTypeName());
        holder.summaryText.setText(seg.getSummary());

        String detail = "";
        if (seg.type == TransitResult.Segment.TYPE_WALKING) {
            if (seg.instruction != null && !seg.instruction.isEmpty()) {
                detail = seg.instruction;
            }
        } else if (seg.type == TransitResult.Segment.TYPE_BUS || seg.type == TransitResult.Segment.TYPE_SUBWAY) {
            if (seg.passStops > 0) {
                detail = "经过 " + seg.passStops + " 站";
                if (seg.busDuration != null) {
                    try {
                        int sec = Integer.parseInt(seg.busDuration);
                        if (sec >= 60) detail += "，约 " + (sec / 60) + " 分钟";
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        holder.detailText.setText(detail);
        holder.detailText.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);

        boolean isLast = position == segments.size() - 1;
        holder.arrowText.setVisibility(isLast ? View.GONE : View.VISIBLE);

        int bgColor;
        switch (seg.type) {
            case TransitResult.Segment.TYPE_SUBWAY: bgColor = 0xFF1565C0; break;
            case TransitResult.Segment.TYPE_BUS: bgColor = 0xFF2E7D32; break;
            default: bgColor = 0xFF1E1E1E; break;
        }

        holder.itemView.setFocusable(true);
        final int normalColor = bgColor;
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(8);
            bg.setColor(hasFocus ? 0xFF1A73E8 : normalColor);
            v.setBackground(bg);
        });

        GradientDrawable initBg = new GradientDrawable();
        initBg.setShape(GradientDrawable.RECTANGLE);
        initBg.setCornerRadius(8);
        initBg.setColor(normalColor);
        holder.itemView.setBackground(initBg);
    }

    @Override
    public int getItemCount() {
        return segments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iconText, typeText, summaryText, detailText, arrowText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconText = itemView.findViewById(R.id.segment_icon);
            typeText = itemView.findViewById(R.id.segment_type);
            summaryText = itemView.findViewById(R.id.segment_summary);
            detailText = itemView.findViewById(R.id.segment_detail);
            arrowText = itemView.findViewById(R.id.segment_arrow);
        }
    }
}
