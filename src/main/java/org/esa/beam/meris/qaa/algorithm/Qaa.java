package org.esa.beam.meris.qaa.algorithm;

public class Qaa {

    private static final int IDX_410 = 0; // 412.5nm
    private static final int IDX_440 = 1; // 442.5nm
    private static final int IDX_490 = 2; // 490nm
    // private static final int IDX_510 = 3; // 510nm
    private static final int IDX_560 = 4; // 560nm
    // private static final int IDX_620 = 5; // 620nm
    private static final int IDX_670 = 6; // 665nm
    private static final double[] acoefs = {-1.273, -1.163, -0.295};

    // aw and bbw coefficients from IOP datafile
    public static final double[] AW_COEFS = {
            0.00469, 0.00721, 0.015, 0.0325,
            0.0619, 0.2755, 0.429,
    };
    public static final double[] BBW_COEFS = {
            0.003328, 0.0023885, 0.001549,
            0.0012992, 0.0008994, 0.0005996,
            0.0004368
    };

    private final float noDataValue;

    public Qaa(float noDataValue) {
        this.noDataValue = noDataValue;
    }

    public void qaaf_v5(float[] Rrs, float[] rrs, float[] a, float[] bbp) throws org.esa.beam.meris.qaa.ImaginaryNumberException {
        // QAA constants from C version of QAA v5.
        final double g0 = 0.08945;
        final double g1 = 0.1245;

        // Arrays to be calculated.
        float a560;
        float bbp560;
        float rat;
        float Y;
        float[] u = new float[Rrs.length]; //Band 7 is only used once

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

        // step 0.2 prepare rrs    b=lanMbda
        for (int b = 0; b < Rrs.length; b++) {
            rrs[b] = (float) (Rrs[b] / (0.52 + 1.7 * Rrs[b]));
        }

        //STEP 1 ///////////////////////////////////////////////////////////////////////////////////////////////
        for (int b = 0; b < Rrs.length; b++) { //Modified by MZ, Nov-05-11
            double nom = Math.pow(g0, 2.0) + 4.0 * g1 * rrs[b]; //Removed the second g0 (n.guggenberger Mar-26-14)
            if (nom >= 0) {
                u[b] = (float) ((Math.sqrt(nom) - g0) / (2.0 * g1));
            } else {
                throw new org.esa.beam.meris.qaa.ImaginaryNumberException("Will produce an imaginary number", nom);
            }
        }

        if (Rrs[IDX_670] < 0.0015) { //Old QAA_v5
            // step 2
            float rho;
            float numer;
            float denom;
            float result;

            //STEP 2 (a) ////////////////////////////////////////////////////////////////////////////////////
            denom = rrs[IDX_560] + 5 * rrs[IDX_670] * (rrs[IDX_670] / rrs[IDX_490]);
            numer = rrs[IDX_440] + rrs[IDX_490];
            result = numer / denom;
            if (result <= 0) {
                throw new org.esa.beam.meris.qaa.ImaginaryNumberException("Will produce an imaginary number", result);
            }
            rho = (float) Math.log10(result);
            rho = (float) (acoefs[0] + acoefs[1] * rho + acoefs[2] * Math.pow(rho, 2.0));
            a560 = (float) (QaaConstants.AW_COEFS[IDX_560] + Math.pow(10.0, rho));
            /////////////////////////////////////////////////////////////////////////////////////////////////

            //STEP 3 (a) ////////////////////////////////////////////////////////////////////////////////////
            bbp560 = (float) (((u[IDX_560] * a560) / (1.0 - u[IDX_560])) - QaaConstants.BBW_COEFS[IDX_560]);
            /////////////////////////////////////////////////////////////////////////////////////////////////

            //STEP 4 (a) ////////////////////////////////////////////////////////////////////////////////////
            rat = rrs[IDX_440] / rrs[IDX_560];
            Y = (float) (2.0 * (1.0 - 1.2 * Math.exp(-0.9 * rat)));
            /////////////////////////////////////////////////////////////////////////////////////////////////

            //STEP 5 (a) ////////////////////////////////////////////////////////////////////////////////////
            for (int b = 0; b < Rrs.length - 1; b++) {
                bbp[b] = (float) (bbp560 * Math.pow(
                        (float) QaaConstants.WAVELENGTH[IDX_560] / (float) QaaConstants.WAVELENGTH[b], Y));
            }
            /////////////////////////////////////////////////////////////////////////////////////////////////
        }
            //Added per ZP Lee by MZ, Nov-05-11
            //We may combine wavelengths 560nm and 670nm.
        else {

            float rat670;
            float a670;
            float bbp670;

            //STEP 2 (b) ////////////////////////////////////////////////////////////////////////////////////
            rat670 = Rrs[IDX_670] / (Rrs[IDX_440] + Rrs[IDX_490]);
            a670 = (float) (AW_COEFS[IDX_670] + 0.39 * Math.pow(rat670, 1.14));
            /////////////////////////////////////////////////////////////////////////////////////////////////

            //STEP 3 (b) ////////////////////////////////////////////////////////////////////////////////////
            bbp670 = (float) (((u[IDX_670] * a670) / (1.0 - u[IDX_670])) - BBW_COEFS[IDX_670]);
            /////////////////////////////////////////////////////////////////////////////////////////////////

            //STEP 4 (b) ////////////////////////////////////////////////////////////////////////////////////
            rat = rrs[IDX_440] / rrs[IDX_560];
            Y = (float) (2.0 * (1.0 - 1.2 * Math.exp(-0.9 * rat)));
            /////////////////////////////////////////////////////////////////////////////////////////////////

            //STEP 5 (b) ////////////////////////////////////////////////////////////////////////////////////
            for (int b = 0; b < Rrs.length - 1; b++) {
                bbp[b] = (float) (bbp670 * Math.pow((float) QaaConstants.WAVELENGTH[IDX_670] / (float) QaaConstants.WAVELENGTH[b], Y));
            }
            /////////////////////////////////////////////////////////////////////////////////////////////////
        }

        //STEP 6 ////////////////////////////////////////////////////////////////////////////////////////////
        for (int b = 0; b < Rrs.length - 1; b++) {
            a[b] = (float) (((1.0 - u[b]) * (QaaConstants.BBW_COEFS[b] + bbp[b])) / u[b]);
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////

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

        //STEP 7 ////////////////////////////////////////////////////////////////////////////////////////////////
        rat = rrs[IDX_440] / rrs[IDX_560];
        symbol = (float) (0.74 + (0.2 / (0.8 + rat)));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        //STEP 8 ////////////////////////////////////////////////////////////////////////////////////////////////
        double S = 0.015 + 0.002 / (0.6 + rat); // new in QAA v5
        zeta = (float) Math.exp(S * (QaaConstants.WAVELENGTH[IDX_440] - QaaConstants.WAVELENGTH[IDX_410]));
        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        //STEP 9 & 10 ///////////////////////////////////////////////////////////////////////////////////////////
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
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public float qaaf_zeu(float alpha490, float bb490, float theta,int waterClar) throws org.esa.beam.meris.qaa.ImaginaryNumberException { //y.jiang

        //Values of model parameters
        float  x0 = (float) -0.057;
        float  x1 = (float)  0.482;
        float  x2 = (float)  4.221;
        float zt0 = (float)   0.183;
        float zt1 = (float)   0.702;
        float zt2 = (float)  -2.567;
        float  a0 = (float)  0.090;
        float  a1 = (float)  1.465;
        float  a2 = (float) -0.667 ;


        //set the minima for a(490) = 0.0195
        //    float alpha490 = a[IDX_490];    y.jiang
        if(alpha490 < 0.0195) alpha490 = (float) 0.0195;

        //set the minima for bb(490) = 0.0016
        //   float bb490  = bb[IDX_490];      y.jiang
        if(bb490 < 0.0016) bb490 = (float) 0.0016;

        //
        double K1 = (   (x0 + x1*Math.sqrt(alpha490) + x2*(bb490)) *          //changed Feb-22-2012 Y.Jiang
                (1 + a0 * Math.sin(theta))  );
        double K2 = (   (zt0 + zt1*Math.sqrt(alpha490) + zt2*(bb490)) *
                (a1 + a2 * Math.cos(theta))  );

        //
        float[] water = new float[] {   (float) (4.605),
                (float) (2.303),
                (float) (0.693)       };
        float t;
        switch (waterClar){
            case 1:t=water[0];break;
            case 10:t=water[1];break;
            case 50:t=water[2];break;
            default:t=water[0];
        }

        double y1,y2,y3;
        // float z1 = -1; //initialize

        float imaginary = (float) -1;

        // for(int i = 0; i < t.length; i++){

        y1 =  (  ( K1*K1  - K2*K2 - 2*( t*K1) )  /
                ( K1*K1 )       );
        y2 =  (  ( t*t  - 2*( t*K1) )  /
                ( K1*K1 )       );
        y3 =  (  ( t*t )  /
                ( K1*K1 )       );
        double Q=(y1*y1-3*y2)/9;
        double R=(2*y1*y1*y1-9*y1*y2+27*y3)/54;
        if(Q<0) throw new org.esa.beam.meris.qaa.ImaginaryNumberException("Q<0",Q);
        double x= Math.acos(R/Math.pow(Q*Q*Q,0.5));
        float z= (float) (-2*Math.pow(Q,0.5)*Math.cos((x-2*Math.PI)/3)-y1/3);

        return z;
    }



}
