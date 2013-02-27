package org.esa.beam.meris.qaa.brewin;

import org.esa.beam.meris.qaa.algorithm.ImaginaryNumberException;
import org.esa.beam.meris.qaa.algorithm.QaaResult;

public class QaaConfAlgorithm {

    private static final double[] A_COEFFS = {-1.146, -1.366, -0.469};
    // @todo 2 tb/tb old implementation (qaa) constants tn 2013-02-27
    // according to paper(An Update of the Quasi-Analytical Algorithm (QAA_v5), equation (6)), these constants should vary on a per-sensor basis
    // private static final double[] A_COEFFS = {-1.273, -1.163, -0.295};

    // @todo 1 tb/tb the following constants are sensor specific - extract sensor configuration class
    private static final double[] aw_meris = {0.00449607, 0.00706914, 0.015, 0.0325, 0.0619, 0.429};
    private static final double[] bbw_meris = {0.00573196, 0.00424592, 0.00276835, 0.00233870, 0.00157958, 0.000772104};
    private static final double[] wavelength = {413.0, 443.0, 490.0, 510.0, 560.0, 665.0};

    public QaaResult process(float[] Rrs_in, QaaResult recycle) throws ImaginaryNumberException {
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
        final double[] U = new double[Rrs_in.length - 1];
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

        final double rho = A_COEFFS[0] + A_COEFFS[1] * X + A_COEFFS[2] * X * X;
        final double a_555 = aw_meris[4] + Math.pow(10.0, rho);

        // Estimation of bbp at reference wavelength
        final double bbp_555 = U[4] * a_555 / (1 - U[4]) - bbw_meris[4];

        // Exponent of bbp
        final double ratio = rrs[1] / rrs[4];
        final double N = 2.0 * (1.0 - 1.2 * Math.exp(-0.9 * ratio));

        // Estimate ratio of aph411/aph443
        final double Ratio_aph = 0.74 + 0.2 / (0.8 + ratio);

        // Estimate ratio of adg411/adg443
        final double Slope_adg = 0.015 + 0.002 / (0.6 + ratio);
        final double Ratio_adg = Math.exp(Slope_adg * (wavelength[1] - wavelength[0]));

//        ;;;Estimation of bbp  and bb at all wavelengths
//        bbp = bbp_555*(REF_wave/wavelength)^N
//        bb  = bbp + bbw
//        ;;;Estimation of a at all wavelengths
//        a = (1.-U)*(bbp+bbw)/U
//        ;;;Estimation of adg at all wavelengths
//        adg_443 =((a(0)-Ratio_aph*a(1))-(aw(0)-Ratio_aph*aw(1)))/(Ratio_adg-Ratio_aph)
//        adg     = adg_443*exp(-Slope_adg*(wavelength-wavelength(1)))
//        ;;;Estimation of aph at all wavelengths
//        aph = a-aw-adg
//        QAA_MATRIX = [[a],[aph],[adg],[bb],[bbp]]


        return new QaaResult();
    }
}
