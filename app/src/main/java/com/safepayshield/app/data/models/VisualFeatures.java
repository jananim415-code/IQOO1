package com.safepayshield.app.data.models;

public final class VisualFeatures {
    public final double contrast;
    public final double edgeContinuity;
    public final double occlusion;
    public final double alignment;

    public VisualFeatures(double contrast, double edgeContinuity, double occlusion, double alignment) {
        this.contrast = contrast;
        this.edgeContinuity = edgeContinuity;
        this.occlusion = occlusion;
        this.alignment = alignment;
    }

    public static VisualFeatures createDefault() {
        return new VisualFeatures(0.82, 0.88, 0.04, 0.92);
    }
}
