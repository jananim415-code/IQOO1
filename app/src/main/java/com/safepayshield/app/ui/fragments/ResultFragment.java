package com.safepayshield.app.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.safepayshield.app.R;
import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import com.safepayshield.app.data.models.RiskResult;
import com.safepayshield.app.data.models.Verdict;
import com.safepayshield.app.databinding.FragmentResultBinding;
import com.safepayshield.app.security.BiometricHelper;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.main.MainViewModel;
import com.safepayshield.app.voice.VoiceManager;

import java.util.Set;

public final class ResultFragment extends Fragment {

    private FragmentResultBinding binding;
    private MainViewModel viewModel;
    private VoiceManager voiceManager;
    private boolean isOverrideActive = false;
    private String pendingVerificationContact;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        voiceManager = new VoiceManager(requireContext(), new VoiceManager.VoiceListener() {
            @Override public void onCommand(String command) {}
            @Override public void onReady() {
                if (Boolean.TRUE.equals(viewModel.elderMode.getValue())) {
                    announceResult();
                }
            }
            @Override public void onError(String error) {}
        });

        viewModel.result.observe(getViewLifecycleOwner(), this::bindResult);
        viewModel.verificationCompleted.observe(getViewLifecycleOwner(), completed -> {
            if (Boolean.TRUE.equals(completed) && viewModel.result.getValue() != null) {
                bindResult(viewModel.result.getValue());
            }
        });

        binding.btnPause.setOnClickListener(v -> {
            viewModel.clearResult();
            Navigation.findNavController(v).popBackStack(R.id.homeFragment, false);
        });

        binding.btnShare.setOnClickListener(v -> handleShare());
        
        binding.statusCard.setOnLongClickListener(v -> {
            announceResult();
            return true;
        });

        binding.btnVerifyContact.setOnClickListener(v -> showVerifyContactDialog());
    }

    private void handleShare() {
        RiskResult res = viewModel.result.getValue();
        if (res != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Warning");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "I detected a risky UPI payment: " + 
                    res.getVerdict().name() + " (Score: " + res.getScore() + "). Reasons: " + 
                    String.join(", ", res.getReasons()));
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_concern)));
        }
    }

    private void showVerifyContactDialog() {
        Set<String> contacts = viewModel.contacts.getValue();
        if (contacts == null || contacts.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_contacts), Toast.LENGTH_LONG).show();
            return;
        }

        String[] contactArray = contacts.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.verify_with_contact)
            .setItems(contactArray, (dialog, which) -> {
                String contact = contactArray[which];
                pendingVerificationContact = contact;
                confirmVerification(contact);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void confirmVerification(String contact) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Local verification")
            .setMessage("Verify this payment with " + contact + " using an in-app local review flow. The original risk result will remain visible.")
            .setPositiveButton("Continue", (d, w) -> authenticateAndRecord(contact))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void authenticateAndRecord(String contact) {
        RiskResult result = viewModel.result.getValue();
        if (result == null) {
            return;
        }

        boolean biometricEnabled = Boolean.TRUE.equals(viewModel.biometricEnabled.getValue());
        if (biometricEnabled && BiometricHelper.isBiometricAvailable(requireContext())) {
            BiometricHelper.authenticate(requireActivity(), "Verify payment", "Confirm the payment review for " + contact, new BiometricHelper.BiometricCallback() {
                @Override public void onSuccess() {
                    viewModel.setVerificationState(contact, true);
                    isOverrideActive = true;
                    bindResult(result);
                    Toast.makeText(requireContext(), "Local verification completed for " + contact + ".", Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(String error) {
                    viewModel.setVerificationState(contact, false);
                    Toast.makeText(requireContext(), error != null ? error : "Verification failed.", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (!biometricEnabled) {
            viewModel.setVerificationState(contact, true);
            isOverrideActive = true;
            bindResult(result);
            Toast.makeText(requireContext(), "Local verification recorded without biometric support.", Toast.LENGTH_SHORT).show();
        } else {
            viewModel.setVerificationState(contact, false);
            Toast.makeText(requireContext(), "Biometric or device credential authentication is unavailable.", Toast.LENGTH_LONG).show();
        }
    }

    private void announceResult() {
        RiskResult result = viewModel.result.getValue();
        if (result != null) {
            String text = "Payment amount is " + result.getAmount() + " rupees. " +
                    "Result is " + result.getVerdict().name() + ". " + 
                    (result.getVerdict() == Verdict.SAFE ? "Safe to proceed." : "Please be careful. ") +
                    String.join(". ", result.getReasons());
            voiceManager.speak(text);
        }
    }

    private void bindResult(RiskResult result) {
        if (result == null) return;

        String sourceText = "Source: " + result.getSource().name();
        if (result.getSource() == NormalizedPaymentRequest.Source.NFC) sourceText = "Source: NFC Tap & Pay";
        else if (result.getSource() == NormalizedPaymentRequest.Source.DEMO_OFFLINE) sourceText = "OFFLINE DEMO MODE";
        
        binding.paymentSourceText.setText(sourceText);
        binding.payeeName.setText(result.getPayee());
        binding.vpaText.setText(result.getPa());
        binding.amountText.setText("₹" + result.getAmount());
        binding.riskScore.setText(getString(R.string.layers_desc) + ": " + result.getScore() + "/100");

        int color = ContextCompat.getColor(requireContext(), R.color.mint);
        int icon = R.drawable.ic_check;
        int titleRes = R.string.safe_to_proceed;
        
        if (result.getVerdict() == Verdict.CAUTION) {
            color = ContextCompat.getColor(requireContext(), R.color.amber);
            icon = R.drawable.ic_warning;
            titleRes = R.string.pause_and_verify;
        } else if (result.getVerdict() == Verdict.BLOCK) {
            color = ContextCompat.getColor(requireContext(), R.color.red);
            icon = R.drawable.ic_report;
            titleRes = R.string.payment_blocked;
        }

        binding.statusCard.setCardBackgroundColor(color);
        binding.statusIcon.setImageResource(icon);
        binding.statusName.setText(result.getVerdict().name());
        binding.statusTitle.setText(getString(titleRes));

        binding.reasonsContainer.removeAllViews();
        for (String reason : result.getReasons()) {
            TextView tv = (TextView) LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, binding.reasonsContainer, false);
            tv.setText("• " + reason);
            tv.setTextSize(16);
            tv.setPadding(0, 4, 0, 4);
            binding.reasonsContainer.addView(tv);
        }

        boolean isFamilyApproved = Boolean.TRUE.equals(viewModel.familyApproved.getValue());
        boolean isVerified = Boolean.TRUE.equals(viewModel.verificationCompleted.getValue());

        if (result.getVerdict() == Verdict.BLOCK) {
            binding.btnProceed.setText("Blocked by risk engine");
            binding.btnProceed.setEnabled(false);
            binding.btnProceed.setAlpha(0.5f);
        } else {
            binding.btnProceed.setEnabled(true);
            binding.btnProceed.setAlpha(1.0f);
            if (result.getVerdict() == Verdict.CAUTION || result.getSource() == NormalizedPaymentRequest.Source.DEMO_OFFLINE || result.getVerdict() == Verdict.BLOCK) {
                binding.btnProceed.setText(isFamilyApproved || isVerified ? "Proceed (reviewed)" : "Verify & proceed");
            } else {
                binding.btnProceed.setText(R.string.safe_to_proceed);
            }

            binding.btnProceed.setOnClickListener(v -> handleProceed(result));
        }

        if (Boolean.TRUE.equals(viewModel.verificationCompleted.getValue())) {
            binding.btnVerifyContact.setText("Verified by " + viewModel.verificationContact.getValue());
        }
    }

    private void handleProceed(RiskResult result) {
        boolean needsBiometric = result.getVerdict() != Verdict.SAFE || result.getSource() == NormalizedPaymentRequest.Source.DEMO_OFFLINE;
        
        if (needsBiometric) {
            if (BiometricHelper.isBiometricAvailable(requireContext())) {
                BiometricHelper.authenticate(requireActivity(), getString(R.string.biometric_required), "Authorize Payment", new BiometricHelper.BiometricCallback() {
                    @Override public void onSuccess() { executeHandoff(result); }
                    @Override public void onError(String error) { 
                        Toast.makeText(getContext(), "Authentication failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Biometric not available. Proceeding with caution.", Toast.LENGTH_SHORT).show();
                executeHandoff(result);
            }
        } else {
            executeHandoff(result);
        }
    }

    private void executeHandoff(RiskResult result) {
        if (result.getSource() == NormalizedPaymentRequest.Source.DEMO_OFFLINE) {
            new AlertDialog.Builder(requireContext())
                .setTitle("Demo Completed")
                .setMessage("Security validation completed successfully.\n\nNOTE: No money was transferred.")
                .setPositiveButton("Done", (d, w) -> {
                    viewModel.clearResult();
                    Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, false);
                })
                .show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("upi://pay?pa=" + result.getPa() + "&pn=" + Uri.encode(result.getPayee()) + "&am=" + result.getAmount()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No compatible UPI payment app is available.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (voiceManager != null) voiceManager.destroy();
    }
}
