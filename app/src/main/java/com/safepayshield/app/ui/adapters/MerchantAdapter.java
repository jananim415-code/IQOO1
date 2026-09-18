package com.safepayshield.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safepayshield.app.databinding.ItemContactBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MerchantAdapter extends RecyclerView.Adapter<MerchantAdapter.ViewHolder> {

    private final List<String> items = new ArrayList<>();
    private final OnMerchantDeleteListener listener;

    public interface OnMerchantDeleteListener {
        void onDelete(String vpa);
    }

    public MerchantAdapter(OnMerchantDeleteListener listener) {
        this.listener = listener;
    }

    public void setItems(Set<String> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reusing item_contact as it's the same structure (text + delete)
        return new ViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String vpa = items.get(position);
        holder.binding.contactName.setText(vpa);
        holder.binding.btnDeleteContact.setOnClickListener(v -> listener.onDelete(vpa));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemContactBinding binding;
        ViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
