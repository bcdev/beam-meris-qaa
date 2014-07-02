package org.esa.beam.meris.qaa.algorithm;

/**
 * @author Marco Peters
 */
public enum WaterClarity {

    WATER_CLARITY_1(4.605f),
    WATER_CLARITY_10(2.303f),
    WATER_CLARITY_50(0.693f);

    private final float tau;

    WaterClarity(float tau) {
        this.tau = tau;
    }

    public float getTau() {
        return tau;
    }
}