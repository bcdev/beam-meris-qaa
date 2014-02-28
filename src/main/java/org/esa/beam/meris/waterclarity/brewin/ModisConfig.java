package org.esa.beam.meris.waterclarity.brewin;

public class ModisConfig implements SensorConfig {

    private static final double[] awCoefficients = {-1.146, -1.366, -0.469};
    private static final double referenceWavelength = 547.0;
    private static final double[] wavelengths = {412.0, 443.0, 488.0, 531.0, 547.0, 667.0};
    private static final double[] specificAbsorptions = {0.00455056, 0.00706914, 0.0145167, 0.0439153, 0.0531686, 0.434888};
    private static final double[] specificBackscatters = {0.00579201, 0.00424592, 0.00281659, 0.00197385, 0.0017428, 0.000762543};

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
