package com.safepayshield.app.ui.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.safepayshield.app.R;
import com.safepayshield.app.data.models.DemoScenario;
import com.safepayshield.app.data.models.RiskResult;
import com.safepayshield.app.databinding.FragmentHomeBinding;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.adapters.DemoAdapter;
import com.safepayshield.app.ui.main.MainViewModel;
import com.safepayshield.app.voice.VoiceManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class HomeFragment extends Fragment implements VoiceManager.VoiceListener {

    private FragmentHomeBinding binding;
    private MainViewModel viewModel;
    private VoiceManager voiceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding.greetingText.setText(getGreetingText());

        List<DemoScenario> scenarios = com.safepayshield.app.domain.RiskEngine.DEMO_SCENARIOS;
        DemoAdapter adapter = new DemoAdapter(scenarios, scenario -> {
            String raw = scenario.upi;
            if (raw != null) {
                viewModel.analyze(raw);
                if (viewModel.result.getValue() != null) {
                    Navigation.findNavController(view).navigate(R.id.resultFragment);
                }
            }
        });
        binding.demoRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.demoRecyclerView.setAdapter(adapter);

        binding.btnScan.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.scanFragment));
        binding.btnNfc.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nfcFragment));
        binding.btnPaste.setOnClickListener(v -> handlePasteFromClipboard());
        binding.fabVoice.setOnClickListener(v -> startVoiceAssistant());

        viewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Navigation.findNavController(view).navigate(R.id.resultFragment);
            }
        });
    }

    private String getGreetingText() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return getString(R.string.good_morning);
        if (hour < 18) return getString(R.string.good_afternoon);
        return getString(R.string.good_evening);
    }

    private void handlePasteFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                Toast.makeText(requireContext(), R.string.no_history, Toast.LENGTH_SHORT).show();
                return;
            }

            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                Toast.makeText(requireContext(), R.string.no_history, Toast.LENGTH_SHORT).show();
                return;
            }

            CharSequence itemText = clip.getItemAt(0).getText();
            String raw = itemText != null ? itemText.toString() : null;
            if (raw == null || raw.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Clipboard is empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            String trimmed = raw.trim();
            if (!trimmed.toLowerCase(Locale.getDefault()).startsWith("upi://")) {
                Toast.makeText(requireContext(), "Clipboard does not contain a valid UPI link.", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.analyze(trimmed);
            if (viewModel.result.getValue() != null) {
                Navigation.findNavController(requireView()).navigate(R.id.resultFragment);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not read clipboard.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceAssistant() {
        if (voiceManager == null) {
            voiceManager = new VoiceManager(requireContext(), this);
        }
        String language = viewModel.language.getValue();
        voiceManager.startListening("ta".equals(language) ? new Locale("ta", "IN") : Locale.ENGLISH);
        Toast.makeText(requireContext(), "Listening...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommand(String command) {
        if (command == null) return;
        String normalized = command.trim();
        if (normalized.isEmpty()) return;
        String lower = normalized.toLowerCase();

        if (lower.contains("disable elder") || lower.contains("disable elder mode")) {
            viewModel.setElderMode(false);
            voiceManager.speak("Elder mode disabled.");
            return;
        }
        if (lower.contains("scan") || lower.contains("qr") || lower.contains("ஸ்கேன்")) {
            Navigation.findNavController(requireView()).navigate(R.id.scanFragment);
            voiceManager.speak("Opening scan screen.");
            return;
        }
        if (lower.contains("tap") || lower.contains("nfc") || lower.contains("தொட")) {
            Navigation.findNavController(requireView()).navigate(R.id.nfcFragment);
            voiceManager.speak("Opening tap and pay.");
            return;
        }
        if (lower.contains("family") || lower.contains("contact") || lower.contains("குடும்ப")) {
            Navigation.findNavController(requireView()).navigate(R.id.familyFragment);
            voiceManager.speak("Opening trusted contacts.");
            return;
        }
        if (lower.contains("settings") || lower.contains("அமைப்ப")) {
            Navigation.findNavController(requireView()).navigate(R.id.settingsFragment);
            voiceManager.speak("Opening settings.");
            return;
        }
        if (lower.contains("history") || lower.contains("வரலாறு")) {
            Navigation.findNavController(requireView()).navigate(R.id.historyFragment);
            voiceManager.speak("Opening payment history.");
            return;
        }
        if (lower.contains("home") || lower.contains("முகப்பு")) {
            Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
            voiceManager.speak("Back to home.");
            return;
        }
        if (lower.contains("tamil") || lower.contains("english") || lower.contains("தமிழ்") || lower.contains("ஆங்கில")) {
            Toast.makeText(requireContext(), "Language support is available in Settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lower.contains("result") || lower.contains("read")) {
            RiskResult result = viewModel.result.getValue();
            if (result != null) {
                voiceManager.speak("Payment to " + result.getPayee() + ", amount " + result.getAmount() + ", verdict " + result.getVerdict().name() + ".");
            } else {
                voiceManager.speak("No recent result available.");
            }
            return;
        }
        if (lower.contains("enable elder") || lower.contains("elder mode") || lower.contains("முதியோர்")) {
            viewModel.setElderMode(true);
            voiceManager.speak("Elder mode enabled.");
            return;
        }
        if (lower.contains("disable elder") || lower.contains("disable elder mode")) {
            viewModel.setElderMode(false);
            voiceManager.speak("Elder mode disabled.");
            return;
        }

        Toast.makeText(requireContext(), "Command not recognized: " + normalized, Toast.LENGTH_SHORT).show();
        voiceManager.speak("Command not recognized.");
    }

    @Override
    public void onReady() {
        Toast.makeText(requireContext(), "Voice assistant ready.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String error) {
        Toast.makeText(requireContext(), error != null ? error : "Voice assistant unavailable.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (voiceManager != null) {
            voiceManager.destroy();
            voiceManager = null;
        }
    }

}
