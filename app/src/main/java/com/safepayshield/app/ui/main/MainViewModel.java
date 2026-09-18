package com.safepayshield.app.ui.main;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safepayshield.app.ai.OnDeviceInference;
import com.safepayshield.app.data.local.SecureRepository;
import com.safepayshield.app.data.models.HistoryItem;
import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import com.safepayshield.app.data.models.RiskResult;
import com.safepayshield.app.data.models.Verdict;
import com.safepayshield.app.domain.RiskEngine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MainViewModel extends AndroidViewModel {
    private final SecureRepository repository;
    private final RiskEngine engine;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private final MutableLiveData<RiskResult> _result = new MutableLiveData<>(null);
    public final LiveData<RiskResult> result = _result;

    private final MutableLiveData<Boolean> _elderMode = new MutableLiveData<>();
    public final LiveData<Boolean> elderMode = _elderMode;

    private final MutableLiveData<String> _language = new MutableLiveData<>();
    public final LiveData<String> language = _language;

    private final MutableLiveData<Boolean> _familyApproval = new MutableLiveData<>();
    public final LiveData<Boolean> familyApproval = _familyApproval;

    private final MutableLiveData<Boolean> _biometricEnabled = new MutableLiveData<>();
    public final LiveData<Boolean> biometricEnabled = _biometricEnabled;

    private final MutableLiveData<String> _verificationContact = new MutableLiveData<>(null);
    public final LiveData<String> verificationContact = _verificationContact;

    private final MutableLiveData<Boolean> _verificationCompleted = new MutableLiveData<>(false);
    public final LiveData<Boolean> verificationCompleted = _verificationCompleted;

    private final MutableLiveData<List<HistoryItem>> _history = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<HistoryItem>> history = _history;

    private final MutableLiveData<Set<String>> _merchants = new MutableLiveData<>(new HashSet<>());
    public final LiveData<Set<String>> merchants = _merchants;

    private final MutableLiveData<Set<String>> _contacts = new MutableLiveData<>(new HashSet<>());
    public final LiveData<Set<String>> contacts = _contacts;
    
    private final MutableLiveData<Boolean> _familyApproved = new MutableLiveData<>(false);
    public final LiveData<Boolean> familyApproved = _familyApproved;

    private final MutableLiveData<Boolean> _nfcAvailable = new MutableLiveData<>(false);
    public final LiveData<Boolean> nfcAvailable = _nfcAvailable;

    private final MutableLiveData<Boolean> _nfcEnabled = new MutableLiveData<>(false);
    public final LiveData<Boolean> nfcEnabled = _nfcEnabled;

    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    public final LiveData<String> error = _error;

    private final MutableLiveData<String> _message = new MutableLiveData<>(null);
    public final LiveData<String> message = _message;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new SecureRepository(application);
        engine = new RiskEngine(new OnDeviceInference(application));
        
        _elderMode.setValue(repository.getElderMode());
        _language.setValue(repository.getLanguage());
        _familyApproval.setValue(repository.getFamilyApproval());
        _biometricEnabled.setValue(repository.getBiometricEnabled());
        _merchants.setValue(repository.getTrustedMerchants());
        _contacts.setValue(repository.getTrustedContacts());
        refreshHistory();
    }

    public void analyze(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            showError("Clipboard is empty.");
            return;
        }

        String safe = raw.trim();
        if (!safe.toLowerCase(Locale.US).startsWith("upi://")) {
            showError("This is not a valid UPI payment link.");
            return;
        }

        NormalizedPaymentRequest request = engine.parseUri(safe, NormalizedPaymentRequest.Source.PASTE);
        if (request == null || request.getPa() == null || request.getPa().trim().isEmpty()) {
            showError("UPI link is missing the payee or VPA.");
            return;
        }
        analyzePaymentRequest(request);
    }

    public void analyzePaymentRequest(NormalizedPaymentRequest request) {
        if (request == null) return;
        if (request.getPa() == null || request.getPa().trim().isEmpty()) {
            showError("UPI payment is incomplete.");
            return;
        }

        RiskResult analyzed = engine.analyzePayment(request, _merchants.getValue(), _history.getValue(), null);
        _result.postValue(analyzed);
        _familyApproved.postValue(false);
        clearVerificationState();

        repository.saveHistory(new HistoryItem(
                request.getRawPayload(),
                analyzed.getPayee(),
                analyzed.getPa(),
            analyzed.getAmount(),
            String.join("; ", analyzed.getReasons()),
                analyzed.getVerdict().name(),
                analyzed.getScore(),
                System.currentTimeMillis(),
                request.getSource().name()
        ));
        refreshHistory();
    }

    public void refreshHistory() {
        repository.getAllHistory(items -> _history.postValue(items != null ? items : new ArrayList<>()));
    }

    public void setElderMode(boolean value) {
        repository.setElderMode(value);
        _elderMode.setValue(value);
    }

    public void setLanguage(String value) {
        repository.setLanguage(value);
        _language.setValue(value);
    }

    public void setFamilyApproval(boolean value) {
        repository.setFamilyApproval(value);
        _familyApproval.setValue(value);
    }

    public void setBiometricEnabled(boolean value) {
        repository.setBiometricEnabled(value);
        _biometricEnabled.setValue(value);
    }

    public void setVerificationState(String contact, boolean completed) {
        _verificationContact.setValue(contact);
        _verificationCompleted.setValue(completed);
    }

    public void clearVerificationState() {
        _verificationContact.setValue(null);
        _verificationCompleted.setValue(false);
    }
    
    public void addMerchant(String vpa) {
        if (vpa == null || !vpa.matches("^[A-Za-z0-9._-]+@[A-Za-z]+$")) {
            showError("Invalid VPA format");
            return;
        }
        repository.addMerchant(vpa);
        _merchants.setValue(repository.getTrustedMerchants());
        showMessage("Merchant added");
    }
    
    public void removeMerchant(String vpa) {
        repository.removeMerchant(vpa);
        _merchants.setValue(repository.getTrustedMerchants());
        showMessage("Merchant removed");
    }
    
    public void addContact(String contact) {
        if (contact == null || contact.trim().isEmpty()) {
            showError("Invalid contact");
            return;
        }
        repository.addContact(contact);
        _contacts.setValue(repository.getTrustedContacts());
        showMessage("Contact added");
    }
    
    public void removeContact(String contact) {
        repository.removeContact(contact);
        _contacts.setValue(repository.getTrustedContacts());
        showMessage("Contact removed");
    }

    public void clearResult() {
        _result.setValue(null);
    }

    public void approveFamily() {
        _familyApproved.setValue(true);
        showMessage("Family approval granted");
    }

    public void setNfcStatus(boolean available, boolean enabled) {
        _nfcAvailable.postValue(available);
        _nfcEnabled.postValue(enabled);
    }

    public void setNfcError(String message) {
        _error.postValue(message);
    }

    public void showError(String msg) {
        _error.postValue(msg);
    }

    public void showMessage(String msg) {
        _message.postValue(msg);
    }

    public void clearMessage() {
        _message.setValue(null);
    }

    public void clearError() {
        _error.setValue(null);
    }

    public void clearAllData() {
        repository.clearAllData();
        _elderMode.setValue(false);
        _language.setValue("en");
        _familyApproval.setValue(false);
        _merchants.setValue(new HashSet<>());
        _contacts.setValue(new HashSet<>());
        _history.setValue(new ArrayList<>());
        _result.setValue(null);
        clearVerificationState();
        showMessage("All data cleared");
    }
}
