package org.esa.beam.meris.qaa.brewin;

import org.esa.beam.meris.qaa.algorithm.ImaginaryNumberException;
import org.esa.beam.meris.qaa.algorithm.QaaResult;

public class ConfAlgorithm {

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

    public QaaResult process(float[] Rrs_in, QaaResult recycle) throws ImaginaryNumberException {
        QaaResult result = ensureResult(recycle);
        final double up_667 = 20.0 * Math.pow(Rrs_in[4], 1.5);
        final double lw_667 = 0.9 * Math.pow(Rrs_in[4], 1.7);
        final double[] Rrs = new double[Rrs_in.length];

        for (int i = 0; i < Rrs.length; i++) {
            Rrs[i] = Rrs_in[i];
        }

        // Check Rrs(667 or 665)
        if (Rrs[5] > up_667 || Rrs[5] < lw_667) {
            Rrs[5] = 1.27 * Math.pow(Rrs[4], 1.47);
            Rrs[5] += 0.00018 * Math.pow((Rrs[2] / Rrs[4]), -3.19);
        }

        // Coefficients for converting Rrs to rrs (above to below sea-surface)
        final double[] rrs = new double[Rrs.length];
        for (int i = 0; i < Rrs.length; i++) {
            rrs[i] = Rrs[i] / (0.52 + 1.7 * Rrs[i]);
        }

        // Coefficients as defined by Gordon et al. (1988) and modified by Lee et al. (2002) to estimate bb/a+bb referred to as U
        final double g0 = 0.089;
        final double g0_square = g0 * g0;
        final double g1 = 0.125;
        final double[] U = new double[Rrs_in.length];
        for (int i = 0; i < U.length; i++) {
            final double nom = g0_square + 4.0 * g1 * rrs[i];
            if (nom >= 0.0) {
                U[i] = (Math.sqrt(nom) - g0) / (2.0 * g1);
            } else {
                throw new ImaginaryNumberException("Will produce an imaginary number", nom);
            }
        }

        // Estimation of a at reference wavelength
        final double numer = rrs[1] + rrs[2];
        final double denom = rrs[4] + 5. * (rrs[5] / rrs[2]) * rrs[5];
        final double quot = numer / denom;
        if (quot <= 0.0) {
            throw new ImaginaryNumberException("Will produce an imaginary number", quot);
        }
        final double X = Math.log10(quot);

        final double rho = a_coeffs[0] + a_coeffs[1] * X + a_coeffs[2] * X * X;
        final double a_555 = aw[4] + Math.pow(10.0, rho);

        // Estimation of bbp at reference wavelength
        final double bbp_555 = U[4] * a_555 / (1 - U[4]) - bbw[4];

        // Exponent of bbp
        final double ratio = rrs[1] / rrs[4];
        final double N = 2.0 * (1.0 - 1.2 * Math.exp(-0.9 * ratio));

        // Estimate ratio of aph411/aph443
        final double Ratio_aph = 0.74 + 0.2 / (0.8 + ratio);

        // Estimate ratio of adg411/adg443
        final double Slope_adg = 0.015 + 0.002 / (0.6 + ratio);
        final double Ratio_adg = Math.exp(Slope_adg * (wavelengths[1] - wavelengths[0]));

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

        // Estimation of adg at all wavelengths
        final double adg_443 = ((a[0] - Ratio_aph * a[1]) - (aw[0] - Ratio_aph * aw[1])) / (Ratio_adg - Ratio_aph);
        final double[] adg = new double[wavelengths.length];
        for (int i = 0; i < adg.length; i++) {
            adg[i] = adg_443 * Math.exp(-1.0 * Slope_adg * (wavelengths[i] - wavelengths[1]));
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
