package org.esa.beam.meris.waterclarity.brewin;

public class SeaWifsConfig implements SensorConfig {

    private static final double[] awCoefficients = {-1.146, -1.366, -0.469};
    private static final double referenceWavelength = 555.0;
    private static final double[] wavelengths = {412.0, 443.0, 490.0, 510.0, 555.0, 667.0};
    private static final double[] specificAbsorptions = {0.00455056, 0.00706914, 0.015, 0.0325, 0.0596, 0.434888};
    private static final double[] specificBackscatters = {0.00579201, 0.00424592, 0.00276835, 0.0023387, 0.00163999, 0.000762543};

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
