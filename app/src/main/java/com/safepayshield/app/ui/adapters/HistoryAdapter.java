package com.safepayshield.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.safepayshield.app.R;
import com.safepayshield.app.data.models.HistoryItem;
import com.safepayshield.app.databinding.ItemHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.text.DateFormat;
import java.util.Date;

public final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryItem> items = new ArrayList<>();

    public void setItems(List<HistoryItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.binding.historyName.setText(item.payee);
        holder.binding.historyReasons.setText(item.reasons);
        holder.binding.historyStatus.setText(item.verdictName + " · " + item.score + "\n" +
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(item.timestamp)));
        
        int color = ContextCompat.getColor(holder.itemView.getContext(), R.color.mint);
        if ("CAUTION".equals(item.verdictName)) color = ContextCompat.getColor(holder.itemView.getContext(), R.color.amber);
        else if ("BLOCK".equals(item.verdictName)) color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red);
        
        holder.binding.historyStatus.setTextColor(color);
        
        if ("NFC".equals(item.source)) {
            holder.binding.historyIcon.setImageResource(R.drawable.ic_nfc);
            holder.binding.historyVpa.setText(item.pa + " · ₹" + item.amount);
        } else {
            holder.binding.historyIcon.setImageResource(R.drawable.ic_check);
            holder.binding.historyVpa.setText(item.pa + " · ₹" + item.amount);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;
        ViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
