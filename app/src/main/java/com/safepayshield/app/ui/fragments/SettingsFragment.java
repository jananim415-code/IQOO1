package com.safepayshield.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.safepayshield.app.R;
import com.safepayshield.app.databinding.FragmentSettingsBinding;
import com.safepayshield.app.security.BiometricHelper;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.adapters.MerchantAdapter;
import com.safepayshield.app.ui.main.MainActivity;
import com.safepayshield.app.ui.main.MainViewModel;

public final class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private MainViewModel viewModel;
    private MerchantAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        adapter = new MerchantAdapter(vpa -> viewModel.removeMerchant(vpa));
        binding.merchantsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.merchantsRecyclerView.setAdapter(adapter);

        viewModel.merchants.observe(getViewLifecycleOwner(), merchants -> {
            adapter.setItems(merchants);
            binding.merchantsEmptyText.setVisibility(merchants.isEmpty() ? View.VISIBLE : View.GONE);
            binding.merchantsRecyclerView.setVisibility(merchants.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.elderMode.observe(getViewLifecycleOwner(), enabled -> binding.switchElder.setChecked(enabled));
        viewModel.familyApproval.observe(getViewLifecycleOwner(), enabled -> binding.switchFamily.setChecked(enabled));
        viewModel.biometricEnabled.observe(getViewLifecycleOwner(), enabled -> binding.switchBiometric.setChecked(enabled));
        viewModel.language.observe(getViewLifecycleOwner(), locale -> {
            binding.btnLanguageEnglish.setSelected("en".equals(locale));
            binding.btnLanguageTamil.setSelected("ta".equals(locale));
        });

        binding.switchElder.setOnCheckedChangeListener((btn, checked) -> {
            viewModel.setElderMode(checked);
            if (btn.isPressed()) {
                requireActivity().recreate();
            }
        });
        binding.switchBiometric.setOnCheckedChangeListener((btn, checked) -> viewModel.setBiometricEnabled(checked));
        binding.btnLanguageEnglish.setOnClickListener(v -> setLanguage("en"));
        binding.btnLanguageTamil.setOnClickListener(v -> setLanguage("ta"));
        
        binding.switchFamily.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && BiometricHelper.isBiometricAvailable(requireContext())) {
                BiometricHelper.authenticate(requireActivity(), "Authorize Change", "Verify to enable family approval", new BiometricHelper.BiometricCallback() {
                    @Override public void onSuccess() { viewModel.setFamilyApproval(true); }
                    @Override public void onError(String error) { binding.switchFamily.setChecked(false); }
                });
            } else {
                viewModel.setFamilyApproval(checked);
            }
        });

        binding.btnManageContacts.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.familyFragment));

        binding.btnOpenTerms.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.termsFragment));
        binding.btnOpenPrivacy.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.termsFragment));
        binding.btnOpenAbout.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.aboutFragment));
        binding.btnAddMerchant.setOnClickListener(v -> showAddMerchantDialog());

        binding.btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will remove all history, contacts and merchants. Continue?")
                .setPositiveButton("Clear", (d, w) -> viewModel.clearAllData())
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setLanguage(String localeCode) {
        viewModel.setLanguage(localeCode);
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).applyLanguage(localeCode);
        }
    }

    private void showAddMerchantDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("merchant@upi");
        new AlertDialog.Builder(requireContext())
            .setTitle("Add Trusted Merchant")
            .setMessage("Enter UPI ID (VPA)")
            .setView(input)
            .setPositiveButton("Add", (d, w) -> viewModel.addMerchant(input.getText().toString()))
            .setNegativeButton("Cancel", null)
            .show();
    }
}
