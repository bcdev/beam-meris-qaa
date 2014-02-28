package org.esa.beam.meris.waterclarity.brewin;

public interface SensorConfig {
    double[] getAwCoefficients();

    double getReferenceWavelength();

    double[] getWavelengths();

    double[] getSpecificAbsorptions();

    double[] getSpecficBackscatters();
}
