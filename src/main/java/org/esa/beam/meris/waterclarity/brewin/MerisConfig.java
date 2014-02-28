package org.esa.beam.meris.waterclarity.brewin;

public class MerisConfig implements SensorConfig {

    private static final double[] awCoefficients = {-1.146, -1.366, -0.469};
    private static final double referenceWavelength = 560.0;
    private static final double[] wavelengths = {413.0, 443.0, 490.0, 510.0, 560.0, 665.0};
    private static final double[] specificAbsorptions = {0.00449607, 0.00706914, 0.015, 0.0325, 0.0619, 0.429};
    private static final double[] specificBackscatters = {0.00573196, 0.00424592, 0.00276835, 0.00233870, 0.00157958, 0.000772104};

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
