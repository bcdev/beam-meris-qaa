package org.esa.beam.meris.qaa.brewin;

import org.esa.beam.meris.qaa.ImaginaryNumberException;
import org.esa.beam.meris.qaa.algorithm.QaaResult;

public class ConfAlgorithm {

    private static final int IDX_410 = 0; // 415.5nm
    private static final int IDX_440 = 1; // 442.5nm
    private static final int IDX_490 = 2; // 490nm
    private static final int IDX_510 = 3; // 510nm
    private static final int IDX_560 = 4; // 560nm
    private static final int IDX_670 = 5; // 670nm

    private final double[] a_coeffs;
    private final double[] aw;
    private final double[] bbw;
    private final double[] wavelengths;
    private final double reference_wavelength;

    // according to paper(An Update of the Quasi-Analytical Algorithm (QAA_v5), equation (6)), these constants should vary on a per-sensor basis
    // private static final double[] A_COEFFS = {-1.273, -1.163, -0.295};

    public ConfAlgorithm(SensorConfig sensorConfig) {
        a_coeffs = sensorConfig.getAwCoefficients();
        aw = sensorConfig.getSpecificAbsorptions();
        bbw = sensorConfig.getSpecficBackscatters();
        wavelengths = sensorConfig.getWavelengths();
        reference_wavelength = sensorConfig.getReferenceWavelength();
    }

    public QaaResult process(float[] Rrs, QaaResult recycle) throws ImaginaryNumberException {
        QaaResult result = ensureResult(recycle);
        final double up_667 = 20.0 * Math.pow(Rrs[IDX_560], 1.5);
        final double lw_667 = 0.9 * Math.pow(Rrs[IDX_560], 1.7);
        final double[] Rrs_in = new double[Rrs.length];

        for (int i = 0; i < Rrs_in.length; i++) {
            Rrs_in[i] = Rrs[i];
        }

        // Check Rrs(667 or 665)
        if (Rrs_in[IDX_670] > up_667 || Rrs_in[IDX_670] < lw_667) {
            Rrs_in[IDX_670] = 1.27 * Math.pow(Rrs_in[IDX_560], 1.47);
            // @todo 1 tb/tb old implementation has a positive exponent
            Rrs_in[IDX_670] += 0.00018 * Math.pow((Rrs_in[IDX_490] / Rrs_in[IDX_560]), -3.19);
        }

        // Coefficients for converting Rrs to rrs (above to below sea-surface)
        final double[] rrs = new double[Rrs_in.length];
        for (int i = 0; i < Rrs_in.length; i++) {
            rrs[i] = Rrs_in[i] / (0.52 + 1.7 * Rrs_in[i]);
        }

        // Coefficients as defined by Gordon et al. (1988) and modified by Lee et al. (2002) to estimate bb/a+bb referred to as U
        // @todo 1 tb/tb gx constants differ wrt old implementation
        final double g0 = 0.089;
        final double g0_square = g0 * g0;
        final double g1 = 0.125;
        final double[] U = new double[Rrs.length];
        for (int i = 0; i < U.length; i++) {
            final double nom = g0_square + 4.0 * g1 * rrs[i];
            if (nom >= 0.0) {
                U[i] = (Math.sqrt(nom) - g0) / (2.0 * g1);
            } else {
                throw new ImaginaryNumberException("Will produce an imaginary number", nom);
            }
        }

        // Estimation of a at reference wavelength
        final double numer = rrs[IDX_440] + rrs[IDX_490];
        final double denom = rrs[IDX_560] + 5. * (rrs[IDX_670] / rrs[IDX_490]) * rrs[IDX_670];
        final double quot = numer / denom;
        if (quot <= 0.0) {
            throw new ImaginaryNumberException("Will produce an imaginary number", quot);
        }
        final double X = Math.log10(quot);

        final double rho = a_coeffs[0] + a_coeffs[1] * X + a_coeffs[2] * X * X;
        final double a_555 = aw[IDX_560] + Math.pow(10.0, rho);

        // Estimation of bbp at reference wavelength
        final double bbp_555 = U[IDX_560] * a_555 / (1 - U[IDX_560]) - bbw[IDX_560];

        // Exponent of bbp
        final double ratio = rrs[IDX_440] / rrs[IDX_560];
        final double N = 2.0 * (1.0 - 1.2 * Math.exp(-0.9 * ratio));


        // Estimation of bbp and bb at all wavelengths
        final double[] bbp = new double[wavelengths.length];
        final double[] bb = new double[wavelengths.length];
        for (int i = 0; i < bbp.length; i++) {
            final double ratio_wl = reference_wavelength / wavelengths[i];
            bbp[i] = bbp_555 * Math.pow(ratio_wl, N);
            bb[i] = bbp[i] + bbw[i];
        }

        // Estimation of a at all wavelengths
        final double[] a = new double[wavelengths.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = (1.0 - U[i]) * (bbp[i] + bbw[i]) / U[i];
        }

        // Estimate ratio of aph411/aph443
        final double Ratio_aph = 0.74 + 0.2 / (0.8 + ratio);

        // Estimate ratio of adg411/adg443
        final double Slope_adg = 0.015 + 0.002 / (0.6 + ratio);
        final double Ratio_adg = Math.exp(Slope_adg * (wavelengths[IDX_440] - wavelengths[IDX_410]));

        // Estimation of adg at all wavelengths
        final double adg_443 = ((a[IDX_410] - Ratio_aph * a[IDX_440]) - (aw[IDX_410] - Ratio_aph * aw[IDX_440])) / (Ratio_adg - Ratio_aph);
        final double[] adg = new double[wavelengths.length];
        for (int i = 0; i < adg.length; i++) {
            adg[i] = adg_443 * Math.exp(-1.0 * Slope_adg * (wavelengths[i] - wavelengths[IDX_440]));
        }

        // Estimation of aph at all wavelengths
        final double[] aph = new double[wavelengths.length];
        for (int i = 0; i < aph.length; i++) {
            aph[i] = a[i] - aw[i] - adg[i];
        }

        for (int i = 0; i < 5; i++) {
            result.setA_Total((float) a[i], i);
            result.setBB_SPM((float) bb[i], i);
        }

        for (int i = 0; i < 3; i++) {
            result.setA_PIG((float) aph[i], i);
            result.setA_YS((float) adg[i], i);
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
}
