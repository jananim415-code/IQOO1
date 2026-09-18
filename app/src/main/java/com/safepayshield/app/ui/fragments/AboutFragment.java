package com.safepayshield.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.safepayshield.app.databinding.FragmentAboutBinding;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.main.MainViewModel;

public final class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.btnClearData.setOnClickListener(v -> {
            viewModel.clearAllData();
            Snackbar.make(view, "All local data cleared.", Snackbar.LENGTH_SHORT).show();
        });
    }
}
