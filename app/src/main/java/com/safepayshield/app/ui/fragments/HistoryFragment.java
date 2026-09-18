package com.safepayshield.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.safepayshield.app.databinding.FragmentHistoryBinding;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.adapters.HistoryAdapter;
import com.safepayshield.app.ui.main.MainViewModel;

public final class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private MainViewModel viewModel;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        adapter = new HistoryAdapter();
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecyclerView.setAdapter(adapter);
        viewModel.history.observe(getViewLifecycleOwner(), items -> {
            binding.historyProgress.setVisibility(View.GONE);
            adapter.setItems(items);
            boolean empty = items == null || items.isEmpty();
            binding.historyEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.historyRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        binding.historyProgress.setVisibility(View.VISIBLE);
        viewModel.refreshHistory();
    }
}
