package com.safepayshield.app.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.safepayshield.app.R;
import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import com.safepayshield.app.databinding.ActivityMainBinding;
import com.safepayshield.app.nfc.NfcPayloadParser;
import com.safepayshield.app.ui.ElderModeHelper;

public final class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = getSecurePreferences(newBase);
        String localeCode = prefs.getString("app_locale", "en");
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        if ("ta".equals(localeCode)) {
            config.setLocale(new java.util.Locale("ta", "IN"));
        } else {
            config.setLocale(java.util.Locale.ENGLISH);
        }
        if (prefs.getBoolean("elder_mode", false)) {
            config.fontScale = 1.25f;
        }
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSecurePreferences(this).getBoolean("elder_mode", false)) {
            setTheme(R.style.Theme_SafePayShield_Elder);
        }

        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel.elderMode.observe(this, elder -> {
            applyGlobalElderMode();
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
            NavigationUI.setupWithNavController(binding.toolbar, navController);
        }

        applyGlobalElderMode();
        handleIntent(getIntent());
    }

    private void applyGlobalElderMode() {
        ElderModeHelper.apply(this, binding.getRoot());
        if (Boolean.TRUE.equals(viewModel.elderMode.getValue())) {
            binding.bottomNav.setMinimumHeight(72);
            binding.toolbar.setMinimumHeight(72);
        } else {
            binding.bottomNav.setMinimumHeight(56);
            binding.toolbar.setMinimumHeight(56);
        }
    }

    public void applyLanguage(String localeCode) {
        SharedPreferences prefs = getSecurePreferences(this);
        prefs.edit().putString("app_locale", localeCode).apply();
        recreate();
    }

    private static SharedPreferences getSecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    "safepay_shield_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception error) {
            return context.getSharedPreferences("safepay_shield_fallback", Context.MODE_PRIVATE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage.class);
            } else {
                rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            }
            if (rawMsgs != null && rawMsgs.length > 0) {
                NormalizedPaymentRequest request = NfcPayloadParser.parseNdef((NdefMessage) rawMsgs[0]);
                if (request != null) {
                    viewModel.analyzePaymentRequest(request);
                    return;
                }
            }
        }

        if (intent.getData() != null) {
            String data = intent.getData().toString();
            if (data.startsWith("upi://")) {
                viewModel.analyze(data);
            }
        }
    }
}
