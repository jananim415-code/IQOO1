package com.safepayshield.app.data.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public final class HistoryItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public final String raw;
    public final String payee;
    public final String pa;
    public final String amount;
    public final String reasons;
    public final String verdictName;
    public final int score;
    public final long timestamp;
    public final String source;

    public HistoryItem(String raw, String payee, String pa, String amount, String reasons, String verdictName, int score, long timestamp, String source) {
        this.raw = raw;
        this.payee = payee;
        this.pa = pa;
        this.amount = amount;
        this.reasons = reasons;
        this.verdictName = verdictName;
        this.score = score;
        this.timestamp = timestamp;
        this.source = source;
    }
}
