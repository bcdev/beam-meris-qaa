package org.esa.beam.meris.qaa.algorithm;

class Qaa {

    private static final int IDX_410 = 0; // 415.5nm
    private static final int IDX_440 = 1; // 442.5nm
    private static final int IDX_490 = 2; // 490nm
    // private static final int IDX_510 = 3; // 510nm
    private static final int IDX_560 = 4; // 560nm
    // private static final int IDX_620 = 5; // 620nm
    private static final int IDX_670 = 6; // 665nm
    private static final double[] acoefs = {-1.273, -1.163, -0.295};


    private final float noDataValue;

    Qaa(float noDataValue) {
        this.noDataValue = noDataValue;
    }

    public void qaaf_v5(float[] Rrs, float[] rrs, float[] a, float[] bbp) throws ImaginaryNumberException {
        // QAA constants from C version of QAA v5.
        final double g0 = 0.08945;
        final double g1 = 0.1245;

        // Arrays to be calculated.
        float a560;
        float bbp560;
        float rat;
        float Y;
        float[] u = new float[Rrs.length - 1]; //Band 7 is only used once

        // step 0.1 prepare Rrs670
        float Rrs670_upper;
        float Rrs670_lower;
        Rrs670_upper = (float) (20.0 * Math.pow(Rrs[IDX_560], 1.5));
        Rrs670_lower = (float) (0.9 * Math.pow(Rrs[IDX_560], 1.7));
        // if Rrs[670] out of bounds, reassign its value by QAA v5.
        if (Rrs[IDX_670] > Rrs670_upper || Rrs[IDX_670] < Rrs670_lower || Rrs[IDX_670] == noDataValue) {
            float Rrs670 = (float) (0.00018 * Math.pow(Rrs[IDX_490] / Rrs[IDX_560], -3.19));
            Rrs670 += (float) (1.27 * Math.pow(Rrs[IDX_560], 1.47));
            Rrs[IDX_670] = Rrs670;
        }

        // step 0.2 prepare rrs
        for (int b = 0; b < Rrs.length; b++) {
            rrs[b] = (float) (Rrs[b] / (0.52 + 1.7 * Rrs[b]));
        }

        // step 1
        for (int b = 0; b < Rrs.length - 1; b++) {
            double nom = Math.pow(g0, 2.0) + 4.0 * g1 * rrs[b];
            if (nom >= 0) {
                u[b] = (float) ((Math.sqrt(nom) - g0) / (2.0 * g1));
            } else {
                throw new ImaginaryNumberException("Will produce an imaginary number", nom);
            }
        }

        // step 2
        float rho;
        float numer;
        float denom;
        float result;

        denom = rrs[IDX_560] + 5 * rrs[IDX_670] * (rrs[IDX_670] / rrs[IDX_490]);
        numer = rrs[IDX_440] + rrs[IDX_490];
        result = numer / denom;
        if (result <= 0) {
            throw new ImaginaryNumberException("Will produce an imaginary number", result);
        }
        rho = (float) Math.log10(result);
        rho = (float) (acoefs[0] + acoefs[1] * rho + acoefs[2] * Math.pow(rho, 2.0));
        a560 = (float) (QaaConstants.AW_COEFS[IDX_560] + Math.pow(10.0, rho));

        // step 3
        bbp560 = (float) (((u[IDX_560] * a560) / (1.0 - u[IDX_560])) - QaaConstants.BBW_COEFS[IDX_560]);

        // step 4
        rat = rrs[IDX_440] / rrs[IDX_560];
        Y = (float) (2.0 * (1.0 - 1.2 * Math.exp(-0.9 * rat)));

        // step 5
        for (int b = 0; b < Rrs.length - 1; b++) {
            bbp[b] = (float) (bbp560 * Math.pow(
                    (float) QaaConstants.WAVELENGTH[IDX_560] / (float) QaaConstants.WAVELENGTH[b], Y));
        }

        // step 6
        for (int b = 0; b < Rrs.length - 1; b++) {
            a[b] = (float) (((1.0 - u[b]) * (QaaConstants.BBW_COEFS[b] + bbp[b])) / u[b]);
        }
    }

    /*
     * Steps 7 through 10 of QAA v5.
     */
    public void qaaf_decomp(float[] rrs, float[] a, float[] aph, float[] adg) {
        // Arrays to be calculated.
        float rat;
        float denom;
        float symbol;
        float zeta;
        float dif1;
        float dif2;
        float ag440;

        // step 7
        rat = rrs[IDX_440] / rrs[IDX_560];
        symbol = (float) (0.74 + (0.2 / (0.8 + rat)));

        // step 8
        double S = 0.015 + 0.002 / (0.6 + rat); // new in QAA v5
        zeta = (float) Math.exp(S * (QaaConstants.WAVELENGTH[IDX_440] - QaaConstants.WAVELENGTH[IDX_410]));

        // step 9 & 10s
        denom = zeta - symbol;
        dif1 = a[IDX_410] - symbol * a[IDX_440];
        dif2 = (float) (QaaConstants.AW_COEFS[IDX_410] - symbol * QaaConstants.AW_COEFS[IDX_440]);
        ag440 = (dif1 - dif2) / denom;
        //NOTE: only the first 6 band of rrs[] are used
        for (int b = 0; b < rrs.length - 1; b++) {
            adg[b] = (float) (ag440 * Math.exp(
                    -1 * S * (QaaConstants.WAVELENGTH[b] - QaaConstants.WAVELENGTH[IDX_440])));
            aph[b] = (float) (a[b] - adg[b] - QaaConstants.AW_COEFS[b]);
        }
    }

}
