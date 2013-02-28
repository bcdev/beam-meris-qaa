package org.esa.beam.meris.qaa.brewin;

public interface SensorConfig {
    double[] getAwCoefficients();

    double getReferenceWavelength();

    double[] getWavelengths();

    double[] getSpecificAbsorptions();

    double[] getSpecficBackscatters();
}
