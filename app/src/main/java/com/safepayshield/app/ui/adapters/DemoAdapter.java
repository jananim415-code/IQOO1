package com.safepayshield.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.safepayshield.app.R;
import com.safepayshield.app.data.models.DemoScenario;
import com.safepayshield.app.data.models.Verdict;
import com.safepayshield.app.databinding.ItemDemoBinding;

import java.util.List;

public final class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {

    private final List<DemoScenario> scenarios;
    private final OnDemoClickListener listener;

    public interface OnDemoClickListener {
        void onDemoClick(DemoScenario scenario);
    }

    public DemoAdapter(List<DemoScenario> scenarios, OnDemoClickListener listener) {
        this.scenarios = scenarios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemDemoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DemoScenario scenario = scenarios.get(position);
        holder.binding.demoTitle.setText(scenario.title);
        holder.binding.demoDesc.setText(scenario.description);
        holder.binding.demoVerdict.setText(scenario.expected.name());
        
        int color = ContextCompat.getColor(holder.itemView.getContext(), R.color.mint);
        if (scenario.expected == Verdict.CAUTION) color = ContextCompat.getColor(holder.itemView.getContext(), R.color.amber);
        else if (scenario.expected == Verdict.BLOCK) color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red);
        
        holder.binding.demoVerdict.setTextColor(color);
        holder.itemView.setOnClickListener(v -> listener.onDemoClick(scenario));
    }

    @Override
    public int getItemCount() {
        return scenarios.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemDemoBinding binding;
        ViewHolder(ItemDemoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
