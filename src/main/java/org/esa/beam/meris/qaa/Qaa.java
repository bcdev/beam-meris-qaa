package org.esa.beam.meris.qaa;

import org.esa.beam.visat.processor.quasi.exceptions.ImaginaryNumberException;

public class Qaa {

    private int idx410 = 0; // 415.5nm
    private int idx440 = 1; // 442.5nm
    private int idx490 = 2; // 490nm
    // private int idx510 = 3; // 510nm
    private int idx560 = 4; // 560nm
    // private int idx620 = 5; // 620nm
    private int idx670 = 6; // 665nm
    private static final int[] wavel = {412, 443, 490, 510, 560, 620};
    private static final double[] acoefs = {-1.273, -1.163, -0.295};

    // aw and bbw coefficients from IOP datafile
    private static final double[] aw = {
            0.00469, 0.00721, 0.015, 0.0325,
            0.0619, 0.2755
    };
    private static final double[] bbw = {
            0.003328, 0.0023885, 0.001549,
            0.0012992, 0.0008994, 0.0005996
    };

    private float noDataValue;

    public Qaa(float noDataValue) {
        this.noDataValue = noDataValue;
    }

    protected void qaaf_v5(float[] Rrs, float[] rrs, float[] a, float[] bbp) throws ImaginaryNumberException {
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
        Rrs670_upper = (float) (20.0 * Math.pow(Rrs[idx560], 1.5));
        Rrs670_lower = (float) (0.9 * Math.pow(Rrs[idx560], 1.7));
        // if Rrs[670] out of bounds, reassign its value by QAA v5.
        if (Rrs[idx670] > Rrs670_upper || Rrs[idx670] < Rrs670_lower || Rrs[idx670] == noDataValue) {
            float Rrs670 = (float) (0.00018 * Math.pow(Rrs[idx490] / Rrs[idx560], 3.19));
            Rrs670 += (float) (1.27 * Math.pow(Rrs[idx560], 1.47));
            Rrs[idx670] = Rrs670;
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

        denom = rrs[idx560] + 5 * rrs[idx670] * (rrs[idx670] / rrs[idx490]);
        numer = rrs[idx440] + rrs[idx490];
        result = numer / denom;
        if (result <= 0) {
            throw new ImaginaryNumberException("Will produce an imaginary number", result);
        }
        rho = (float) Math.log10(result);
        rho = (float) (acoefs[0] + acoefs[1] * rho + acoefs[2] * Math.pow(rho, 2.0));
        a560 = (float) (aw[idx560] + Math.pow(10.0, rho));

        // step 3
        bbp560 = (float) (((u[idx560] * a560) / (1.0 - u[idx560])) - bbw[idx560]);

        // step 4
        rat = rrs[idx440] / rrs[idx560];
        Y = (float) (2.0 * (1.0 - 1.2 * Math.exp(-0.9 * rat)));

        // step 5
        for (int b = 0; b < Rrs.length - 1; b++) {
            bbp[b] = (float) (bbp560 * Math.pow((float) wavel[idx560] / (float) wavel[b], Y));
        }

        // step 6
        for (int b = 0; b < Rrs.length - 1; b++) {
            a[b] = (float) (((1.0 - u[b]) * (bbw[b] + bbp[b])) / u[b]);
        }
    }

    /**
     * Steps 7 through 10 of QAA v5.
     */
    protected void qaaf_decomp(float[] rrs, float[] a, float[] aph, float[] adg) {
        // Arrays to be calculated.
        float rat;
        float denom;
        float symbol;
        float zeta;
        float dif1;
        float dif2;
        float ag440;

        // step 7
        rat = rrs[idx440] / rrs[idx560];
        symbol = (float) (0.74 + (0.2 / (0.8 + rat)));

        // step 8
        double S = 0.015 + 0.002 / (0.6 + rat); // new in QAA v5
        zeta = (float) Math.exp(S * (wavel[idx440] - wavel[idx410]));

        // step 9 & 10s
        denom = zeta - symbol;
        dif1 = a[idx410] - symbol * a[idx440];
        dif2 = (float) (aw[idx410] - symbol * aw[idx440]);
        ag440 = (dif1 - dif2) / denom;
        //NOTE: only the first 6 band of rrs[] are used
        for (int b = 0; b < rrs.length - 1; b++) {
            adg[b] = (float) (ag440 * Math.exp(-1 * S * (wavel[b] - wavel[idx440])));
            aph[b] = (float) (a[b] - adg[b] - aw[b]);
        }
    }

}
