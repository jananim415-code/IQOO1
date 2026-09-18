package com.safepayshield.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.safepayshield.app.data.models.HistoryItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SecureRepository {
    private final SharedPreferences prefs;
    private final HistoryDao historyDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SecureRepository(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    "safepay_shield_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize secure storage", e);
        }
        historyDao = AppDatabase.getDatabase(context).historyDao();
    }

    public boolean getElderMode() { return prefs.getBoolean("elder_mode", false); }
    public void setElderMode(boolean value) { prefs.edit().putBoolean("elder_mode", value).apply(); }

    public String getLanguage() { return prefs.getString("app_locale", "en"); }
    public void setLanguage(String value) { prefs.edit().putString("app_locale", value).apply(); }

    public boolean getFamilyApproval() { return prefs.getBoolean("family_approval", false); }
    public void setFamilyApproval(boolean value) { prefs.edit().putBoolean("family_approval", value).apply(); }

    public boolean getBiometricEnabled() { return prefs.getBoolean("biometric_enabled", true); }
    public void setBiometricEnabled(boolean value) { prefs.edit().putBoolean("biometric_enabled", value).apply(); }

    public Set<String> getTrustedMerchants() {
        return prefs.getStringSet("merchants", new HashSet<>());
    }
    public void addMerchant(String vpa) {
        Set<String> set = new HashSet<>(getTrustedMerchants());
        set.add(vpa);
        prefs.edit().putStringSet("merchants", set).apply();
    }
    public void removeMerchant(String vpa) {
        Set<String> set = new HashSet<>(getTrustedMerchants());
        set.remove(vpa);
        prefs.edit().putStringSet("merchants", set).apply();
    }

    public Set<String> getTrustedContacts() {
        return prefs.getStringSet("contacts", new HashSet<>());
    }
    public void addContact(String contact) {
        Set<String> set = new HashSet<>(getTrustedContacts());
        set.add(contact);
        prefs.edit().putStringSet("contacts", set).apply();
    }
    public void removeContact(String contact) {
        Set<String> set = new HashSet<>(getTrustedContacts());
        set.remove(contact);
        prefs.edit().putStringSet("contacts", set).apply();
    }

    public void saveHistory(HistoryItem item) {
        executor.execute(() -> historyDao.insert(item));
    }

    public void getAllHistory(OnHistoryLoaded callback) {
        executor.execute(() -> {
            List<HistoryItem> items = historyDao.getAll();
            callback.onLoaded(items);
        });
    }

    public void clearAllData() {
        prefs.edit().clear().apply();
        executor.execute(historyDao::deleteAll);
    }

    public interface OnHistoryLoaded {
        void onLoaded(List<HistoryItem> items);
    }
}
