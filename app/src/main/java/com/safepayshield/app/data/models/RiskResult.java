package com.safepayshield.app.data.models;

import java.util.List;
import java.util.ArrayList;

public final class RiskResult {
    private final String pa;
    private final String payee;
    private final String amount;
    private final String note;
    private final int score;
    private final Verdict verdict;
    private final List<String> reasons;
    private final List<LayerScore> layers;
    private final NormalizedPaymentRequest.Source source;

    public RiskResult(String pa, String payee, String amount, String note, int score, Verdict verdict, 
                      List<String> reasons, List<LayerScore> layers, NormalizedPaymentRequest.Source source) {
        this.pa = pa;
        this.payee = payee;
        this.amount = amount;
        this.note = note;
        this.score = score;
        this.verdict = verdict;
        this.reasons = reasons != null ? new ArrayList<>(reasons) : new ArrayList<>();
        this.layers = layers != null ? new ArrayList<>(layers) : new ArrayList<>();
        this.source = source;
    }

    public String getPa() { return pa; }
    public String getPayee() { return payee; }
    public String getAmount() { return amount; }
    public String getNote() { return note; }
    public int getScore() { return score; }
    public Verdict getVerdict() { return verdict; }
    public List<String> getReasons() { return reasons; }
    public List<LayerScore> getLayers() { return layers; }
    public NormalizedPaymentRequest.Source getSource() { return source; }

    public static class LayerScore {
        public final String label;
        public final int score;
        public LayerScore(String label, int score) {
            this.label = label;
            this.score = score;
        }
    }
}
