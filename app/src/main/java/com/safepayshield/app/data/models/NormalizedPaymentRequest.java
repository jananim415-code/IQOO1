package com.safepayshield.app.data.models;

import java.util.HashMap;
import java.util.Map;

public final class NormalizedPaymentRequest {
    public enum Source {
        QR, NFC, URI, PASTE, DEMO_OFFLINE
    }

    private final Source source;
    private final String pa; // Payee VPA
    private final String payeeName;
    private final String amount;
    private final String transactionNote;
    private final String rawPayload;
    private final long timestamp;
    private final boolean isOffline;
    private final Map<String, String> additionalParams;

    public NormalizedPaymentRequest(Source source, String pa, String payeeName, String amount, 
                                    String transactionNote, String rawPayload, boolean isOffline) {
        this.source = source;
        this.pa = (pa != null) ? pa.trim() : "";
        this.payeeName = (payeeName != null) ? payeeName.trim() : "";
        this.amount = (amount != null) ? amount.trim() : "0";
        this.transactionNote = (transactionNote != null) ? transactionNote.trim() : "";
        this.rawPayload = (rawPayload != null) ? rawPayload : "";
        this.timestamp = System.currentTimeMillis();
        this.isOffline = isOffline;
        this.additionalParams = new HashMap<>();
    }

    public Source getSource() { return source; }
    public String getPa() { return pa; }
    public String getPayeeName() { return payeeName; }
    public String getAmount() { return amount; }
    public String getTransactionNote() { return transactionNote; }
    public String getRawPayload() { return rawPayload; }
    public long getTimestamp() { return timestamp; }
    public boolean isOffline() { return isOffline; }
    public void addParam(String key, String value) { additionalParams.put(key, value); }
    public String getParam(String key) { return additionalParams.get(key); }
}
