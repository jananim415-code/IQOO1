package com.safepayshield.app.nfc;

import android.app.Activity;
import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;

import com.safepayshield.app.data.models.NormalizedPaymentRequest;

public final class NfcManager {
    private static final String TAG = "NfcManager";
    private final NfcAdapter nfcAdapter;
    private final NfcListener listener;

    public interface NfcListener {
        void onNfcDetected(NormalizedPaymentRequest request);
        void onNfcError(String message);
        void onNfcStatusChanged(boolean available, boolean enabled);
    }

    public NfcManager(Context context, NfcListener listener) {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        this.listener = listener;
    }

    public boolean isAvailable() {
        return nfcAdapter != null;
    }

    public boolean isEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    public void checkStatus() {
        listener.onNfcStatusChanged(isAvailable(), isEnabled());
    }

    public void enableReaderMode(Activity activity) {
        if (nfcAdapter == null) return;

        Bundle options = new Bundle();
        nfcAdapter.enableReaderMode(activity, tag -> {
            Log.d(TAG, "Tag detected");
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                listener.onNfcError("Unsupported NFC tag format");
                return;
            }

            try {
                ndef.connect();
                NdefMessage message = ndef.getNdefMessage();
                NormalizedPaymentRequest request = NfcPayloadParser.parseNdef(message);
                if (request != null) {
                    listener.onNfcDetected(request);
                } else {
                    listener.onNfcError("Unsupported NFC payment payload");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading NDEF", e);
                listener.onNfcError("Error reading NFC tag");
            } finally {
                try {
                    ndef.close();
                } catch (Exception ignored) {}
            }
        }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, options);
    }

    public void disableReaderMode(Activity activity) {
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(activity);
        }
    }
}
