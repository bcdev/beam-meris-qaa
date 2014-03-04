package org.esa.beam.meris.waterclarity.algorithm;

/**
 * @author Marco Peters
 */
public final class QaaConstants {

    public static final int[] A_TOTAL_BAND_INDEXES = {0, 1, 2, 3, 4};
    public static final int[] BB_SPM_BAND_INDEXES = {5, 6, 7, 8, 9};
    public static final int[] A_PIG_BAND_INDEXES = {10, 11, 12};
    public static final int[] A_YS_BAND_INDEXES = {13, 14, 15};

    public static final int[] WAVELENGTH = {412, 443, 490, 510, 560, 620, 670};

    public static final int FLAG_MASK_VALID = 0x0001;
    public static final int FLAG_MASK_IMAGINARY = 0x0002;
    public static final int FLAG_MASK_NEGATIVE_AYS = 0x0004;
    public static final int FLAG_MASK_INVALID = 0x0008;
    public static final int FLAG_MASK_A_TOTAL_OOB = 0x0010;
    public static final int FLAG_MASK_BB_SPM_OOB = 0x0020;
    public static final int FLAG_MASK_A_PIG_OOB = 0x0040;
    public static final int FLAG_MASK_A_YS_OOB = 0x0080;


    // @todo tb/tb 4 make configurable?? 2013-02-22
    public static final float NO_DATA_VALUE = Float.NaN;


    // aw and bbw coefficients from IOP datafile
    public static final double[] AW_COEFS = {
            0.00469, 0.00721, 0.015,
            0.0325, 0.0619, 0.2755
    };
    public static final double[] BBW_COEFS = {
            0.003328, 0.0023885, 0.001549,
            0.0012992, 0.0008994, 0.0005996
    };

    static final int NUM_A_TOTAL_BANDS = 5;
    static final float A_TOTAL_LOWER_DEFAULT = -0.02f;
    static final float A_TOTAL_UPPER_DEFAULT = 5.f;

    static final int NUM_BB_SPM_BANDS = 5;
    static final float BB_SPM_LOWER_DEFAULT = -0.02f;
    static final float BB_SPM_UPPER_DEFAULT = 5.f;

    static final int NUM_A_PIG_BANDS = 3;
    static final float A_PIG_LOWER_DEFAULT = -0.02f;
    static final float A_PIG_UPPER_DEFAULT = 3.f;

    static final int NUM_A_YS_BANDS = 3;
    static final float A_YS_LOWER_DEFAULT = 0.f;
    static final float A_YS_UPPER_DEFAULT = 1.f;

    private QaaConstants() {
    }
}
