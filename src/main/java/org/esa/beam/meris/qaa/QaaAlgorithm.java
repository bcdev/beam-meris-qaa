package org.esa.beam.meris.qaa;

public class QaaAlgorithm {

    private static final double ONE_DIV_PI = 1.0 / Math.PI;
    // @todo tb/tb 4 make configurable?? 2013-02-22
    private static final float NO_DATA_VALUE = Float.NaN;

    private QaaConfig config;
    private final Qaa qaa;


    public QaaAlgorithm() {
        qaa = new Qaa(NO_DATA_VALUE);
        config = new QaaConfig();
    }

    public void setConfig(QaaConfig config) {
        this.config = config;
    }

    public QaaConfig getConfig() {
        return config;
    }

    /**
     * process QAA algorithm on a single pixel
     *
     * @param rrs_in reflectances at wavelengths
     *               rrs_in[0] @ 415.5 nm
     *               rrs_in[1] @ 442.5 nm
     *               rrs_in[2] @ 490   nm
     *               rrs_in[3] @ 510   nm
     *               rrs_in[4] @ 560   nm
     *               rrs_in[5] @ 620   nm
     *               rrs_in[6] @ 665   nm
     * @return the computation result
     */
    public QaaResult process(float[] rrs_in, QaaResult recycle) throws ImaginaryNumberException {
        final QaaResult result = ensureResult(recycle);

        final float[] rrs = new float[rrs_in.length];
        for (int i = 0; i < rrs.length; i++) {
            rrs[i] = rrs_in[i];
            if (config.isDivideByPi()) {
                rrs[i] *= ONE_DIV_PI;
            }
        }

        // @todo 3 tb/tb convert these to fields? 2013-02-22
        final float[] rrs_pixel = new float[7];
        final float[] a_pixel = new float[6];
        final float[] bbSpm_pixel = new float[6];
        final float[] aPig_pixel = new float[6];
        final float[] aYs_pixel = new float[6];

        /**
         * QAA v5 processing
         */
        // steps 0-6
        // The length of pixel is 7 bands, rrs_pixel... are 6 bands
        qaa.qaaf_v5(rrs, rrs_pixel, a_pixel, bbSpm_pixel);

        // steps 7-10
        qaa.qaaf_decomp(rrs_pixel, a_pixel, aPig_pixel, aYs_pixel);

        // if we came here without exception the data is valid
        result.setValid(true);


        return result;
    }

    static QaaResult ensureResult(QaaResult recycle) {
        QaaResult result = recycle;
        if (result == null) {
            result = new QaaResult();
        }
        return result;
    }

    private QaaResult computeATotal(float[] aph_pixel, float[] adg_pixel, QaaResult qaaResult) {
        for (int i = 0; i < QaaConstants.NUM_A_TOTAL_BANDS; i++) {
            float a = (float) QaaConstants.AW_COEFS[i] + aph_pixel[i] + adg_pixel[i];
            boolean isOob = isOutOfBounds(a, config.getATotalLower(), config.getATotalUpper());
            // @todo 1 tb/tb continue here
//            if (isOob) {
//                targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_A_TOTAL_OOB, true);
//            }
//            targetSamples[A_TOTAL_BAND_INDEXES[i]].set(isOob ? NO_DATA_VALUE : a);
        }

        return qaaResult;
    }

    static boolean isOutOfBounds(float value, float lowerBound, float upperBound) {
        return value < lowerBound || value > upperBound;
    }
}
