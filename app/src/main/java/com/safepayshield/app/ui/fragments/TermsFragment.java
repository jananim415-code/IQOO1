package com.safepayshield.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.safepayshield.app.databinding.FragmentTermsBinding;
import com.safepayshield.app.ui.ElderModeHelper;

public final class TermsFragment extends Fragment {
    private FragmentTermsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTermsBinding.inflate(inflater, container, false);
        ElderModeHelper.apply(requireContext(), binding.getRoot());
        return binding.getRoot();
    }
}
