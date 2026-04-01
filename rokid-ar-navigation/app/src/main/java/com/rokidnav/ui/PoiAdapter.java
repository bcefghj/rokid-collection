package com.rokidnav.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidnav.R;
import com.rokidnav.util.AmapApi;
import java.util.ArrayList;
import java.util.List;

public class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.VH> {
    private final List<AmapApi.PoiResult> items = new ArrayList<>();
    private int selectedPos = 0;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onItemSelected(AmapApi.PoiResult poi, int position);
        void onItemConfirmed(AmapApi.PoiResult poi, int position);
    }

    public void setListener(OnItemActionListener l) {
        this.listener = l;
    }

    public void setData(List<AmapApi.PoiResult> data) {
        items.clear();
        items.addAll(data);
        selectedPos = 0;
        notifyDataSetChanged();
    }

    public int getSelectedPos() { return selectedPos; }

    public AmapApi.PoiResult getSelectedItem() {
        if (items.isEmpty() || selectedPos < 0 || selectedPos >= items.size()) return null;
        return items.get(selectedPos);
    }

    public void moveUp() {
        if (selectedPos > 0) {
            int old = selectedPos;
            selectedPos--;
            notifyItemChanged(old);
            notifyItemChanged(selectedPos);
            if (listener != null) listener.onItemSelected(items.get(selectedPos), selectedPos);
        }
    }

    public void moveDown() {
        if (selectedPos < items.size() - 1) {
            int old = selectedPos;
            selectedPos++;
            notifyItemChanged(old);
            notifyItemChanged(selectedPos);
            if (listener != null) listener.onItemSelected(items.get(selectedPos), selectedPos);
        }
    }

    public void confirm() {
        if (!items.isEmpty() && listener != null) {
            listener.onItemConfirmed(items.get(selectedPos), selectedPos);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poi, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AmapApi.PoiResult poi = items.get(pos);
        h.tvName.setText(poi.name);
        h.tvAddr.setText(poi.address);

        String dist = poi.distance;
        if (dist != null && !dist.isEmpty()) {
            try {
                int m = Integer.parseInt(dist);
                if (m >= 1000) {
                    h.tvDist.setText(String.format("%.1fkm", m / 1000.0));
                } else {
                    h.tvDist.setText(m + "m");
                }
            } catch (NumberFormatException e) {
                h.tvDist.setText(dist + "m");
            }
        } else {
            h.tvDist.setText("");
        }

        boolean selected = pos == selectedPos;
        h.root.setBackgroundColor(selected ? 0xCC2A2A4E : 0xCC1A1A2E);
        h.tvName.setTextColor(selected ? 0xFF00E5FF : 0xFFFFFFFF);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView tvName, tvAddr, tvDist;
        VH(View v) {
            super(v);
            root = v.findViewById(R.id.cardRoot);
            tvName = v.findViewById(R.id.tvPoiName);
            tvAddr = v.findViewById(R.id.tvPoiAddr);
            tvDist = v.findViewById(R.id.tvPoiDist);
        }
    }
}
