package com.safepayshield.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import com.safepayshield.app.databinding.FragmentNfcBinding;
import com.safepayshield.app.nfc.NfcManager;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.main.MainViewModel;

public final class TapAndPayFragment extends Fragment implements NfcManager.NfcListener {

    private FragmentNfcBinding binding;
    private MainViewModel viewModel;
    private NfcManager nfcManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNfcBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        nfcManager = new NfcManager(requireContext(), this);

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.nfcErrorText.setText(error);
                binding.nfcErrorText.setVisibility(View.VISIBLE);
            } else {
                binding.nfcErrorText.setVisibility(View.GONE);
            }
        });

        binding.btnCancelNfc.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        
        binding.btnDemoNfc.setOnClickListener(v -> {
            NormalizedPaymentRequest demo = new NormalizedPaymentRequest(
                NormalizedPaymentRequest.Source.DEMO_OFFLINE,
                "verified@okaxis", "NFC Demo Merchant", "500", "Offline validation demo", "demo_nfc_payload", true
            );
            viewModel.analyzePaymentRequest(demo);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcManager.enableReaderMode(requireActivity());
        nfcManager.checkStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcManager.disableReaderMode(requireActivity());
    }

    @Override
    public void onNfcDetected(NormalizedPaymentRequest request) {
        requireActivity().runOnUiThread(() -> {
            viewModel.analyzePaymentRequest(request);
        });
    }

    @Override
    public void onNfcError(String message) {
        viewModel.setNfcError(message);
    }

    @Override
    public void onNfcStatusChanged(boolean available, boolean enabled) {
        requireActivity().runOnUiThread(() -> {
            if (!available) {
                binding.nfcStatusDesc.setText("NFC is not available on this device.");
            } else if (!enabled) {
                binding.nfcStatusDesc.setText("NFC is disabled. Please enable it in Settings.");
            } else {
                binding.nfcStatusDesc.setText("Tap your phone near the NFC payment tag");
            }
            viewModel.setNfcStatus(available, enabled);
        });
    }
}
