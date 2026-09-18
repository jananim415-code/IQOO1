package com.safepayshield.app.domain;

import android.net.Uri;
import com.safepayshield.app.ai.OnDeviceInference;
import com.safepayshield.app.data.models.DemoScenario;
import com.safepayshield.app.data.models.HistoryItem;
import com.safepayshield.app.data.models.NormalizedPaymentRequest;
import com.safepayshield.app.data.models.RiskResult;
import com.safepayshield.app.data.models.Verdict;
import com.safepayshield.app.data.models.VisualFeatures;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.util.Map;
import java.util.HashMap;

public final class RiskEngine {
    private final OnDeviceInference inference;
    
    // Simple short-lived cache for NFC replay detection
    private static final Map<String, Long> RECENT_PAYLOADS = new HashMap<>();
    private static final long REPLAY_THRESHOLD_MS = 30000; // 30 seconds

    public static final List<DemoScenario> DEMO_SCENARIOS = Arrays.asList(
        new DemoScenario("Normal merchant QR", "Known verified payee", "upi://pay?pa=verified@okaxis&pn=City%20Cafe&am=249&tn=Lunch", Verdict.SAFE),
        new DemoScenario("Suspicious QR", "Urgent refund language", "upi://pay?pa=unknown-shop@upi&pn=Quick%20Refund&am=2400&tn=Urgent%20refund%20claim%20now", Verdict.CAUTION),
        new DemoScenario("High-risk scam QR", "Fraud VPA and tamper signal", "upi://pay?pa=refunddesk@ybl&pn=Refund%20Desk&am=75000&tn=OTP%20required%20immediately%20tampered", Verdict.BLOCK)
    );
    
    private static final Set<String> VERIFIED_MERCHANTS = new HashSet<>(Arrays.asList(
        "verified@okaxis", "citycafe@okhdfcbank", "metro@oksbi"
    ));
    
    private static final Set<String> KNOWN_FRAUD = new HashSet<>(Arrays.asList(
        "refunddesk@ybl", "support-kyc@upi", "prizeclaim@okaxis"
    ));
    
    private static final Pattern SUSPICIOUS_WORDS = Pattern.compile(
        "urgent|refund|lottery|prize|otp|immediately|kyc|verify", Pattern.CASE_INSENSITIVE
    );

    public RiskEngine(OnDeviceInference inference) {
        this.inference = inference;
    }

    public double calculateVisualTamperScore(VisualFeatures features) {
        double qualityRisk = ((1 - features.contrast) + (1 - features.edgeContinuity) 
                + features.occlusion + (1 - features.alignment)) / 4.0;
        return Math.max(0.0, Math.min(1.0, qualityRisk));
    }

    public RiskResult analyze(String raw, Set<String> userTrustedMerchants, List<HistoryItem> history, VisualFeatures visualFeatures) {
        NormalizedPaymentRequest request = parseUri(raw, NormalizedPaymentRequest.Source.QR);
        return analyzePayment(request, userTrustedMerchants, history, visualFeatures);
    }

    public RiskResult analyzePayment(NormalizedPaymentRequest request, Set<String> userTrustedMerchants, List<HistoryItem> history, VisualFeatures visualFeatures) {
        String pa = request.getPa();
        String payee = request.getPayeeName() == null ? pa : request.getPayeeName();
        String amountStr = request.getAmount() == null ? "0" : request.getAmount();
        String note = request.getTransactionNote() == null ? "" : request.getTransactionNote();
        String raw = request.getRawPayload() == null ? "" : request.getRawPayload();
        NormalizedPaymentRequest.Source source = request.getSource();

        List<String> reasons = new ArrayList<>();
        
        // 1. Visual Check (25%) - Only for QR
        double visualScore = 0.0;
        boolean hasVisual = (source == NormalizedPaymentRequest.Source.QR);
        if (hasVisual) {
            if (raw.toLowerCase().contains("tampered")) {
                visualScore = 0.94;
            } else {
                VisualFeatures feat = (visualFeatures != null) ? visualFeatures : VisualFeatures.createDefault();
                visualScore = (inference != null) ? inference.getVisualScore(feat) : calculateVisualTamperScore(feat);
            }
        }

        // 2. Payload Check (35%)
        double payloadScore;
        if (SUSPICIOUS_WORDS.matcher(note).find()) {
            payloadScore = 0.82;
        } else if (pa.length() > 45 || countOccurrences(pa, '.') > 2 || pa.toLowerCase().contains("-support")) {
            payloadScore = 0.62;
        } else {
            payloadScore = (inference != null) ? inference.getPayloadScore(note, pa) : 0.12;
        }
        
        // NFC Specific Payload Checks
        if (source == NormalizedPaymentRequest.Source.NFC) {
            String hash = hashPayload(raw);
            long now = System.currentTimeMillis();
            
            // Prune old entries
            RECENT_PAYLOADS.entrySet().removeIf(entry -> now - entry.getValue() > REPLAY_THRESHOLD_MS);
            
            if (RECENT_PAYLOADS.containsKey(hash)) {
                long lastTime = RECENT_PAYLOADS.get(hash);
                if (now - lastTime < REPLAY_THRESHOLD_MS) {
                    payloadScore = Math.max(payloadScore, 0.75);
                    reasons.add("NFC replay suspected: Rapid repeated tap detected.");
                }
            }
            RECENT_PAYLOADS.put(hash, now);
            
            if (pa.isEmpty() || !pa.contains("@")) {
                payloadScore = 1.0;
                reasons.add("Malformed NFC payload: Invalid VPA format.");
            }
        }

        // 3. Merchant Check (25%)
        double merchantBase;
        if (KNOWN_FRAUD.contains(pa.toLowerCase())) {
            merchantBase = 1.0;
        } else if (VERIFIED_MERCHANTS.contains(pa.toLowerCase()) || userTrustedMerchants.contains(pa.toLowerCase())) {
            merchantBase = 0.04;
        } else {
            merchantBase = 0.58;
        }
        
        double newPayeePenalty = 0.0;
        double amountVal = tryParseDouble(amountStr);
        boolean seen = false;
        if (history != null) {
            for (HistoryItem item : history) {
                if (item.pa.equalsIgnoreCase(pa)) {
                    seen = true;
                    break;
                }
            }
        }
        if (!seen && amountVal > 10000) newPayeePenalty = 0.35;

        double mismatchPenalty = 0.0;
        String[] signals = {"electricity", "board", "hospital", "school", "metro"};
        boolean claimsMerchant = false;
        for (String s : signals) {
            if (payee.toLowerCase().contains(s)) {
                claimsMerchant = true;
                break;
            }
        }
        boolean personalVpa = !pa.toLowerCase().contains("merchant") && !pa.toLowerCase().contains("bill") && !pa.toLowerCase().contains("official");
        if (claimsMerchant && personalVpa) mismatchPenalty = 0.28;

        int repeats = 0;
        if (history != null) {
            for (HistoryItem item : history) {
                if (item.pa.equalsIgnoreCase(pa) && !"SAFE".equals(item.verdictName)) {
                    repeats++;
                }
            }
        }
        double repeatPenalty = Math.min(0.36, repeats * 0.12);
        
        double merchantAdjusted = Math.max(0.0, Math.min(1.0, merchantBase + newPayeePenalty + mismatchPenalty + repeatPenalty));

        // 4. Behavioral Check (15%)
        double behavioralScore;
        int hour = LocalTime.now().getHour();
        boolean lateNight = hour >= 23 || hour < 5;
        if (amountVal > 50000 && lateNight) {
            behavioralScore = 0.92;
        } else if (amountVal > 50000) {
            behavioralScore = 0.74;
        } else if (countOccurrences(amountStr, '.') > 1) {
            behavioralScore = 0.64;
        } else {
            behavioralScore = 0.10;
        }

        // Composite Score Calculation
        double finalScoreRaw;
        if (hasVisual) {
            finalScoreRaw = (visualScore * 0.25 + payloadScore * 0.35 + merchantAdjusted * 0.25 + behavioralScore * 0.15);
        } else {
            // Re-normalize for NFC (Visual N/A)
            // Weights: Payload(35/75), Merchant(25/75), Behavior(15/75)
            finalScoreRaw = (payloadScore * 0.35 + merchantAdjusted * 0.25 + behavioralScore * 0.15) / 0.75;
        }
        int finalScore = (int) (finalScoreRaw * 100);
        
        Verdict verdict;
        if (finalScore >= 65) verdict = Verdict.BLOCK;
        else if (finalScore >= 35) verdict = Verdict.CAUTION;
        else verdict = Verdict.SAFE;

        if (visualScore > 0.50) reasons.add("The QR image shows signs of being changed.");
        if (payloadScore > 0.50 && reasons.isEmpty()) reasons.add("The payment note or address looks unusual.");
        if (KNOWN_FRAUD.contains(pa.toLowerCase())) {
            reasons.add("This payment address is on the local fraud list.");
        } else if (merchantAdjusted > 0.50) {
            reasons.add("This payee is not verified on this device.");
        }
        if (newPayeePenalty > 0) reasons.add("This is a new payee with an unusually high amount.");
        if (mismatchPenalty > 0) reasons.add("The payee name does not match a typical merchant address.");
        if (repeatPenalty > 0) reasons.add("This payee has appeared in recent risky scans.");
        if (behavioralScore > 0.50) reasons.add("This amount or payment time is outside your usual pattern.");
        if (reasons.isEmpty()) reasons.add("The payee and payment pattern look familiar.");

        List<RiskResult.LayerScore> layers = new ArrayList<>();
        if (hasVisual) {
            layers.add(new RiskResult.LayerScore("Visual check", (int)(visualScore * 100)));
        }
        layers.add(new RiskResult.LayerScore("Payload check", (int)(payloadScore * 100)));
        layers.add(new RiskResult.LayerScore("Merchant check", (int)(merchantAdjusted * 100)));
        layers.add(new RiskResult.LayerScore("Behavior check", (int)(behavioralScore * 100)));

        return new RiskResult(pa, payee, amountStr, note, finalScore, verdict, reasons, layers, source);
    }

    public NormalizedPaymentRequest parseUri(String raw, NormalizedPaymentRequest.Source source) {
        if (raw == null) {
            return null;
        }

        String candidate = raw.trim();
        if (candidate.isEmpty() || !candidate.toLowerCase(Locale.US).startsWith("upi://")) {
            return null;
        }

        Uri uri = Uri.parse(candidate);
        if (uri == null || uri.getScheme() == null) {
            return null;
        }

        String pa = safeQueryValue(uri, "pa");
        String payee = safeQueryValue(uri, "pn");
        String amount = safeQueryValue(uri, "am");
        String note = safeQueryValue(uri, "tn");

        if (pa == null || pa.trim().isEmpty()) {
            return null;
        }

        if (amount == null || amount.trim().isEmpty()) {
            amount = "0";
        }

        return new NormalizedPaymentRequest(source, pa, payee == null ? pa : payee, amount, note == null ? "" : note, candidate, false);
    }

    private String safeQueryValue(Uri uri, String key) {
        if (uri == null || key == null) {
            return null;
        }
        String value = uri.getQueryParameter(key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : Uri.decode(value);
    }

    private int countOccurrences(String s, char c) {
        if (s == null) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }

    private double tryParseDouble(String s) {
        if (s == null) return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private String hashPayload(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return payload;
        }
    }
}
