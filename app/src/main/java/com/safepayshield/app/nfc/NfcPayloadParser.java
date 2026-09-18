package com.safepayshield.app.nfc;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.net.Uri;
import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import java.nio.charset.Charset;
import java.util.Arrays;

public final class NfcPayloadParser {

    public static NormalizedPaymentRequest parseNdef(NdefMessage message) {
        if (message == null) return null;
        for (NdefRecord record : message.getRecords()) {
            NormalizedPaymentRequest request = parseRecord(record);
            if (request != null) return request;
        }
        return null;
    }

    private static NormalizedPaymentRequest parseRecord(NdefRecord record) {
        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
            if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                return parseUriRecord(record);
            } else if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                return parseTextRecord(record);
            }
        }
        return null;
    }

    private static NormalizedPaymentRequest parseUriRecord(NdefRecord record) {
        Uri uri = record.toUri();
        if (uri != null && "upi".equals(uri.getScheme())) {
            return fromUpiUri(uri.toString());
        }
        return null;
    }

    private static NormalizedPaymentRequest parseTextRecord(NdefRecord record) {
        try {
            byte[] payload = record.getPayload();
            if (payload == null || payload.length < 2) return null;
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 63;
            if (languageCodeLength + 1 >= payload.length) return null;
            String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            if (text.startsWith("upi://")) {
                return fromUpiUri(text);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private static NormalizedPaymentRequest fromUpiUri(String raw) {
        Uri uri = Uri.parse(raw);
        String pa = uri.getQueryParameter("pa");
        String payee = uri.getQueryParameter("pn");
        String amount = uri.getQueryParameter("am");
        String note = uri.getQueryParameter("tn");
        
        return new NormalizedPaymentRequest(
            NormalizedPaymentRequest.Source.NFC,
            pa, payee, amount, note, raw, true // NFC is treated as offline capable
        );
    }
}
