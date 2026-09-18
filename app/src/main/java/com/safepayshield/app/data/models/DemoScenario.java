package com.safepayshield.app.data.models;

public final class DemoScenario {
    public final String title;
    public final String description;
    public final String upi;
    public final Verdict expected;

    public DemoScenario(String title, String description, String upi, Verdict expected) {
        this.title = title;
        this.description = description;
        this.upi = upi;
        this.expected = expected;
    }
}
