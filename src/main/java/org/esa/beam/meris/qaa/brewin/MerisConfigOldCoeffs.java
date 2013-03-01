package org.esa.beam.meris.qaa.brewin;

public class MerisConfigOldCoeffs implements SensorConfig {

    private static final double[] awCoefficients = {-1.273, -1.163, -0.295};
    private static final double referenceWavelength = 560.0;
    private static final double[] wavelengths = {412, 443, 490, 510, 560, 665};
    private static final double[] specificAbsorptions = {0.00469, 0.00721, 0.015, 0.0325, 0.0619, 0.429};
    private static final double[] specificBackscatters = {0.003328, 0.0023885, 0.001549, 0.0012992, 0.0008994, 0.0005996};

    @Override
    public double[] getAwCoefficients() {
        return awCoefficients;
    }

    @Override
    public double getReferenceWavelength() {
        return referenceWavelength;
    }

    @Override
    public double[] getWavelengths() {
        return wavelengths;
    }

    @Override
    public double[] getSpecificAbsorptions() {
        return specificAbsorptions;
    }

    @Override
    public double[] getSpecficBackscatters() {
        return specificBackscatters;
    }
}
