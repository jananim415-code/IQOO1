package com.safepayshield.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safepayshield.app.databinding.ItemContactBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final List<String> items = new ArrayList<>();
    private final OnContactDeleteListener listener;

    public interface OnContactDeleteListener {
        void onDelete(String contact);
    }

    public ContactAdapter(OnContactDeleteListener listener) {
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
        return new ViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String contact = items.get(position);
        holder.binding.contactName.setText(contact);
        holder.binding.btnDeleteContact.setOnClickListener(v -> listener.onDelete(contact));
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
