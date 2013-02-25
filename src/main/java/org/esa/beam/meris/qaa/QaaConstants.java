package org.esa.beam.meris.qaa;

/**
 * @author Marco Peters
 */
final class QaaConstants {

    static final int[] WAVELENGTH = {412, 443, 490, 510, 560, 620};

    // aw and bbw coefficients from IOP datafile
    static final double[] AW_COEFS = {
            0.00469, 0.00721, 0.015,
            0.0325, 0.0619, 0.2755
    };
    static final double[] BBW_COEFS = {
            0.003328, 0.0023885, 0.001549,
            0.0012992, 0.0008994, 0.0005996
    };

    static final int NUM_A_TOTAL_BANDS = 5;
    static final int[] A_TOTAL_BAND_INDEXES = {0, 1, 2, 3, 4};
    static final float A_TOTAL_LOWER_DEFAULT = -0.02f;
    static final float A_TOTAL_UPPER_DEFAULT = 5.f;

    static final int NUM_BB_SPM_BANDS = 5;
    static final int[] BB_SPM_BAND_INDEXES = {5, 6, 7, 8, 9};
    static final float BB_SPM_LOWER_DEFAULT = -0.02f;
    static final float BB_SPM_UPPER_DEFAULT = 5.f;

    static final int NUM_A_PIG_BANDS = 3;
    static final int[] A_PIG_BAND_INDEXES = {10, 11, 12};
    static final float A_PIG_LOWER_DEFAULT = -0.02f;
    static final float A_PIG_UPPER_DEFAULT = 3.f;

    static final int NUM_A_YS_BANDS = 3;
    static final int[] A_YS_BAND_INDEXES = {13, 14, 15};
    static final float A_YS_LOWER_DEFAULT = 0.f;
    static final float A_YS_UPPER_DEFAULT = 1.f;
    // @todo tb/tb 4 make configurable?? 2013-02-22
    static final float NO_DATA_VALUE = Float.NaN;

    private QaaConstants() {
    }
}
