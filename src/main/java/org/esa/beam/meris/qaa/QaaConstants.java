package org.esa.beam.meris.qaa;

/**
 * @author Marco Peters
 */
public final class QaaConstants {

    public static final int[] WAVELENGTH = {412, 443, 490, 510, 560, 620};

    // aw and bbw coefficients from IOP datafile
    public static final double[] AW_COEFS = {
            0.00469, 0.00721, 0.015,
            0.0325, 0.0619, 0.2755
    };
    public static final double[] BBW_COEFS = {
            0.003328, 0.0023885, 0.001549,
            0.0012992, 0.0008994, 0.0005996
    };

    private QaaConstants() {
    }
}
