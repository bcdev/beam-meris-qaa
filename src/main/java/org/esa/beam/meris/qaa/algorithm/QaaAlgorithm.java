package org.esa.beam.meris.qaa.algorithm;

import org.esa.beam.meris.qaa.ImaginaryNumberException;

public class QaaAlgorithm {

    private static final double ONE_DIV_PI = 1.0 / Math.PI;

    private QaaConfig config;
    private final Qaa qaa;

    public QaaAlgorithm() {
        qaa = new Qaa(QaaConstants.NO_DATA_VALUE);
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
     *
     * @return the computation result
     */
    public QaaResult process(float[] rrs_in, QaaResult recycle) {
        QaaResult result = ensureResult(recycle);

        try {
            final float[] rrs = new float[rrs_in.length];
            final boolean divideByPi = config.isDivideByPi();
            for (int i = 0; i < rrs.length; i++) {
                rrs[i] = rrs_in[i];
                if (divideByPi) {
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

            result = computeATotal(aPig_pixel, aYs_pixel, result);
            result = computeBbSpm(bbSpm_pixel, result);
            result = computeAPig(aPig_pixel, result);
            result = computeAYs(aYs_pixel, result);
        } catch (ImaginaryNumberException ignore) {
            result.invalidateImaginary();
        }

        return result;
    }

    static QaaResult ensureResult(QaaResult recycle) {
        QaaResult result = recycle;
        if (result == null) {
            result = new QaaResult();
        }
        result.reset();

        return result;
    }

    private QaaResult computeATotal(float[] aph_pixel, float[] adg_pixel, QaaResult qaaResult) {
        for (int i = 0; i < QaaConstants.NUM_A_TOTAL_BANDS; i++) {
            float a = (float) QaaConstants.AW_COEFS[i] + aph_pixel[i] + adg_pixel[i];
            boolean isOob = isOutOfBounds(a, config.getATotalLower(), config.getATotalUpper());
            if (isOob) {
                // @todo 2 tb/tb case not covered by tests tb 2013-02-25
                qaaResult.setATotalOutOfBounds(true);
                qaaResult.setA_Total(QaaConstants.NO_DATA_VALUE, i);
            } else {
                qaaResult.setA_Total(a, i);
            }
        }

        return qaaResult;
    }

    private QaaResult computeBbSpm(float[] bbSpm_pixel, QaaResult qaaResult) {
        for (int i = 0; i < QaaConstants.NUM_BB_SPM_BANDS; i++) {
            float bbSpm = (float) QaaConstants.BBW_COEFS[i] + bbSpm_pixel[i];
            boolean isOob = isOutOfBounds(bbSpm, config.getBbSpmsLower(), config.getBbSpmsUpper());
            if (isOob) {
                // @todo 2 tb/tb case not covered by tests tb 2013-02-25
                qaaResult.setBbSpmOutOfBounds(true);
                qaaResult.setBB_SPM(QaaConstants.NO_DATA_VALUE, i);
            } else {
                qaaResult.setBB_SPM(bbSpm, i);
            }
        }
        return qaaResult;
    }

    private QaaResult computeAPig(float[] aPig_pixel, QaaResult qaaResult) {
        for (int i = 0; i < QaaConstants.NUM_A_PIG_BANDS; i++) {
            float aPig = aPig_pixel[i];
            boolean isOob = isOutOfBounds(aPig, config.getAPigLower(), config.getAPigUpper());
            if (isOob) {
                // @todo 2 tb/tb case not covered by tests tb 2013-02-25
                qaaResult.setAPigOutOfBounds(true);
                qaaResult.setA_PIG(QaaConstants.NO_DATA_VALUE, i);
            } else {
                qaaResult.setA_PIG(aPig, i);
            }
        }
        return qaaResult;
    }

    private QaaResult computeAYs(float[] ays_pixel, QaaResult qaaResult) {
        for (int i = 0; i < QaaConstants.NUM_A_YS_BANDS; i++) {
            float ays = ays_pixel[i];
            boolean isOob = isOutOfBounds(ays, config.getAYsLower(), config.getAYsUpper());
            if (isOob) {
                // @todo 2 tb/tb case not covered by tests tb 2013-02-25
                qaaResult.setAYsOutOfBounds(true);
                if (ays < 0) {
                    // @todo 2 tb/tb case not covered by tests tb 2013-02-25
                    qaaResult.setAYsNegative(true);
                }
                qaaResult.setA_YS(QaaConstants.NO_DATA_VALUE, i);
            } else {
                qaaResult.setA_YS(ays, i);
            }
        }
        return qaaResult;
    }

    static boolean isOutOfBounds(float value, float lowerBound, float upperBound) {
        return value < lowerBound || value > upperBound;
    }
}
