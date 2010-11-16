// This is the main program for QAA algorithm. It mainly consists of
// two methods, qaaf_v5() and qaaf_decomp(). When make change to the
// algorithm, be careful of the the sizes of arrays, such as Rrs, pixel
// a, bb, aph, ... and QaaABand..., they are different.

package org.esa.beam.visat.processor.quasi;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.processor.quasi.exceptions.ImaginaryNumberException;

import java.awt.Color;
import java.io.IOException;
import java.util.logging.Logger;

public class AnalyticalPN extends ProcessingNode {

    private static final float noDataValue = Float.MAX_VALUE;

    /**
     * Default index of our bands.
     */
    private int idx410 = 0; // 415.5nm
    private int idx440 = 1; // 442.5nm
    private int idx490 = 2; // 490nm
    // private int idx510 = 3; // 510nm
    private int idx560 = 4; // 560nm
    // private int idx620 = 5; // 620nm
    private int idx670 = 6; // 665nm

    private static final int cloud_threshold = 4194304;

    /**
     * QAA constants
     */
    // Coefficients QAA v5, from Zhongping Lee's QAA v5
    //public static final double[] acoefs = { -1.146, -1.366, -.469 };
    public static final double[] acoefs = {-1.273, -1.163, -0.295};
    public static final int[] wavel = {412, 443, 490, 510, 560, 620};

    // aw and bbw coefficients from IOP datafile
    public static final double[] aw = {
            0.00469, 0.00721, 0.015, 0.0325,
            0.0619, 0.2755
    };
    public static final double[] bbw = {
            0.003328, 0.0023885, 0.001549,
            0.0012992, 0.0008994, 0.0005996
    };

    // Hard-coded here, could be adjusted in user interface
    private float a_lower_bound = -5.0f;
    private float a_upper_bound = 5.0f;
    private float bb_lower_bound = -5.0f;
    private float bb_upper_bound = 5.0f;
    private float aph_lower_bound = -3.0f;
    private float aph_upper_bound = 3.0f;
    private float adg_upper_bound = 1.0f;


    // Factor that all values are scaled by when added to output product.
    public static final float SCALING_FACTOR = 1.0f;

    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "QAAv5";
    public static final String PRODUCT_TYPE = "QAA_L2";

    public static final String FLAG_CODING = "analytical_flag";
    public static final String ANALYSIS_FLAG_BAND_NAME = "analytical_flags";

    private static final byte FLAG_INVALID = 8;

    private static final byte FLAG_NEGATIVE_ADG = 4;

    private static final byte FLAG_IMAGINARY = 2;

    private static final byte FLAG_VALID = 1;

    //To determine if we need to divide Rrs by Pi(3.14)
    private boolean pi_check;

    /**
     * Data structures used for QAA Notes: -The images is processed in strips
     * instead of pixels for speed. -The image may be processed pixel by pixel
     * if you change the frame size calculator.
     */

    // input product bands
    private Band[] reflectanceBands;

    // output product bands
    private Band[] QaaABands;
    private Band[] QaaBbBands;
    private Band[] QaaAphBands;
    private Band[] QaaAdgBands;

    // output flag band(not sure if it will be used)
    private Band analyticalFlagBand;

    // input flag band used to decipher between water, land, and clouds
    private Band inputFlagBand;

    private Logger _logger;

    // source product used for processing
    private Product sourceProduct;

    /**
     * Constructor Initializes the logger.
     */
    public AnalyticalPN() {
        _logger = BeamLogManager.getSystemLogger();
    }

    /**
     * Creates and initializes the output product along with initializing the
     * source product.
     */
    @Override
    protected Product createTargetProductImpl() {
        // Total of 7 bands are used in QAA v5
        final String[] reflectanceBandNames = {
                EnvisatConstants.MERIS_L2_REFLEC_1_BAND_NAME,
                EnvisatConstants.MERIS_L2_REFLEC_2_BAND_NAME,
                EnvisatConstants.MERIS_L2_REFLEC_3_BAND_NAME,
                EnvisatConstants.MERIS_L2_REFLEC_4_BAND_NAME,
                EnvisatConstants.MERIS_L2_REFLEC_5_BAND_NAME,
                EnvisatConstants.MERIS_L2_REFLEC_6_BAND_NAME,
                EnvisatConstants.MERIS_L2_REFLEC_7_BAND_NAME
        };
        final String qaa_a = "Qaa a ";
        final String qaa_bb = "Qaa bb ";
        final String qaa_aph = "Qaa aph ";
        final String qaa_adg = "Qaa adg ";
        // input product
        sourceProduct = getSourceProduct();

        // bands used from the input product
        reflectanceBands = new Band[reflectanceBandNames.length];
        for (int bandIndex = 0; bandIndex < reflectanceBandNames.length; bandIndex++) {
            String reflectanceBandName = reflectanceBandNames[bandIndex];
            reflectanceBands[bandIndex] = sourceProduct.getBand(reflectanceBandName);
            if (reflectanceBands[bandIndex] == null) {
                throw new IllegalArgumentException("Source product does not contain band " + reflectanceBandName + ".");
            }
        }

        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        // create the output product
        Product outputProduct = new Product(AnalyticalPN.DEFAULT_OUTPUT_PRODUCT_NAME,
                                            AnalyticalPN.PRODUCT_TYPE, sceneWidth, sceneHeight);

        // Setup our output bands
        // NOTE: We only compute 5 bands for a, bb and 3 bands for adg.
        QaaABands = new Band[5];
        for (int i = 0; i < QaaABands.length; i++) {
            QaaABands[i] = new Band(qaa_a + wavel[i], ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            QaaABands[i].setDescription("Quasi-Analytical a - " + wavel[i] + "nm");
            QaaABands[i].setUnit("1/m");
            QaaABands[i].setScalingFactor(AnalyticalPN.SCALING_FACTOR);
            QaaABands[i].setNoDataValueUsed(true);
            QaaABands[i].setNoDataValue(noDataValue);
            outputProduct.addBand(QaaABands[i]);
        }

        QaaBbBands = new Band[5];
        for (int i = 0; i < QaaBbBands.length; i++) {
            QaaBbBands[i] = new Band(qaa_bb + wavel[i], ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            QaaBbBands[i].setDescription("Quasi-Analytical bb - " + wavel[i] + "nm");
            QaaBbBands[i].setUnit("1/m");
            QaaBbBands[i].setScalingFactor(AnalyticalPN.SCALING_FACTOR);
            QaaBbBands[i].setNoDataValueUsed(true);
            QaaBbBands[i].setNoDataValue(noDataValue);
            outputProduct.addBand(QaaBbBands[i]);
        }

        QaaAphBands = new Band[3];
        for (int i = 0; i < QaaAphBands.length; i++) {
            QaaAphBands[i] = new Band(qaa_aph + wavel[i], ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            QaaAphBands[i].setDescription("Quasi-Analytical aph - " + wavel[i] + "nm");
            QaaAphBands[i].setUnit("1/m");
            QaaAphBands[i].setScalingFactor(AnalyticalPN.SCALING_FACTOR);
            QaaAphBands[i].setNoDataValueUsed(true);
            QaaAphBands[i].setNoDataValue(noDataValue);
            outputProduct.addBand(QaaAphBands[i]);
        }

        QaaAdgBands = new Band[3];
        for (int i = 0; i < QaaAdgBands.length; i++) {
            QaaAdgBands[i] = new Band(qaa_adg + wavel[i], ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            QaaAdgBands[i].setDescription("Quasi-Analytical adg - " + wavel[i] + "nm");
            QaaAdgBands[i].setUnit("1/m");
            QaaAdgBands[i].setScalingFactor(AnalyticalPN.SCALING_FACTOR);
            QaaAdgBands[i].setNoDataValueUsed(true);
            QaaAdgBands[i].setNoDataValue(noDataValue);
            outputProduct.addBand(QaaAdgBands[i]);
        }

        inputFlagBand = sourceProduct.getBand(AnalyticalConstants.DEFAULT_FLAG_BAND_NAME);

        final FlagCoding flagCoding = new FlagCoding(FLAG_CODING);
        flagCoding.setDescription("QAA-for-IOP specific flags");
        outputProduct.getFlagCodingGroup().add(flagCoding);

        MetadataAttribute analyticalAttr = new MetadataAttribute("normal", ProductData.TYPE_UINT8);
        analyticalAttr.getData().setElemInt(FLAG_VALID);
        analyticalAttr.setDescription("A valid water pixel.");
        flagCoding.addAttribute(analyticalAttr);

        final Mask normalMask = Mask.BandMathsType.create(analyticalAttr.getName(), analyticalAttr.getDescription(),
                                                          outputProduct.getSceneRasterWidth(),
                                                          outputProduct.getSceneRasterHeight(),
                                                          ANALYSIS_FLAG_BAND_NAME + "." + analyticalAttr.getName(),
                                                          Color.BLUE, 0.5f);
        outputProduct.getMaskGroup().add(normalMask);
        analyticalAttr = new MetadataAttribute("Imaginary_number", ProductData.TYPE_UINT8);
        analyticalAttr.getData().setElemInt(AnalyticalPN.FLAG_IMAGINARY);
        analyticalAttr.setDescription("Classified as water, but an imaginary number would have been produced.");
        flagCoding.addAttribute(analyticalAttr);

        final Mask imagNumMask = Mask.BandMathsType.create(analyticalAttr.getName(), analyticalAttr.getDescription(),
                                                           outputProduct.getSceneRasterWidth(),
                                                           outputProduct.getSceneRasterHeight(),
                                                           ANALYSIS_FLAG_BAND_NAME + "." + analyticalAttr.getName(),
                                                           Color.RED, 0.5f);
        outputProduct.getMaskGroup().add(imagNumMask);

        analyticalAttr = new MetadataAttribute("Negative_Adg", ProductData.TYPE_UINT8);
        analyticalAttr.getData().setElemInt(FLAG_NEGATIVE_ADG);
        analyticalAttr.setDescription(
                "Classified as water, but one or more of the bands contain a negative Adg value.");
        flagCoding.addAttribute(analyticalAttr);

        final Mask negAdgMask = Mask.BandMathsType.create(analyticalAttr.getName(), analyticalAttr.getDescription(),
                                                          outputProduct.getSceneRasterWidth(),
                                                          outputProduct.getSceneRasterHeight(),
                                                          ANALYSIS_FLAG_BAND_NAME + "." + analyticalAttr.getName(),
                                                          Color.YELLOW, 0.5f);
        outputProduct.getMaskGroup().add(negAdgMask);

        analyticalAttr = new MetadataAttribute("non_water", ProductData.TYPE_UINT8);
        analyticalAttr.getData().setElemInt(FLAG_INVALID);
        analyticalAttr.setDescription("Not classified as a water pixel (land/cloud).");
        flagCoding.addAttribute(analyticalAttr);

        final Mask nonWaterMask = Mask.BandMathsType.create(analyticalAttr.getName(), analyticalAttr.getDescription(),
                                                            outputProduct.getSceneRasterWidth(),
                                                            outputProduct.getSceneRasterHeight(),
                                                            ANALYSIS_FLAG_BAND_NAME + "." + analyticalAttr.getName(),
                                                            Color.BLACK, 0.5f);
        outputProduct.getMaskGroup().add(nonWaterMask);

        analyticalFlagBand = new Band(ANALYSIS_FLAG_BAND_NAME, ProductData.TYPE_UINT8, sceneWidth, sceneHeight);
        analyticalFlagBand.setSampleCoding(flagCoding);
        outputProduct.addBand(analyticalFlagBand);

        _logger.info("Output product successfully created");
        return outputProduct;
    }

    /**
     * Process a frame and write results to output product.
     *
     * @param frameX frame x position
     * @param frameY frame y position
     * @param frameW frame width
     * @param frameH frame height
     * @param pm     progress monitor
     */
    @Override
    protected void processFrame(int frameX, int frameY, int frameW, int frameH, ProgressMonitor pm) throws IOException {
        final int frameSize = frameW * frameH;
        pm.beginTask("Processing frame...", frameSize * 2);
        try {
            // Array for flag data for current frame.
            int[] flagData = new int[frameSize];
            byte[] analyticalFlagData = new byte[frameSize];
            float[][] Rrs = new float[reflectanceBands.length][frameSize];
            SubProgressMonitor subPM = new SubProgressMonitor(pm, 1);
            try {
                subPM.beginTask("Read reflectance band pixel data...",
                                reflectanceBands.length);
                // populate Rrs arrays with corresponding input band data.
                for (int b = 0; b < reflectanceBands.length; b++) {
                    // select current band
                    final Band reflectanceBand = reflectanceBands[b];
                    // read data from current band and store to current Rrs
                    reflectanceBand.readPixels(frameX, frameY, frameW, frameH,
                                               Rrs[b], SubProgressMonitor.create(pm, 1));
                }

                // populate flagData array
                flagData = inputFlagBand.readPixels(frameX, frameY, frameW,
                                                    frameH, flagData, SubProgressMonitor.create(pm, 1));
            } finally {
                subPM.done();
            }
            float[][] a = new float[5][frameSize];
            float[][] bb = new float[5][frameSize];
            float[][] aph = new float[3][frameSize];
            float[][] adg = new float[3][frameSize];

            // Pi check see if Rrs/3.14 is needed
            double denom_pi = 1.0;
            if (pi_check) {
                denom_pi = Math.PI;
            }

            for (int i = 0; i < frameSize; i++) {

                /**
                 * Note: -Values below the value of clouds is considered water.
                 * -0 is used for non-water areas.
                 */
                try {
                    if (flagData[i] < cloud_threshold) { // Check if it is water or not
                        float[] pixel = new float[reflectanceBands.length];

                        //Take care of Pi
                        for (int b = 0; b < pixel.length; b++) {
                            pixel[b] = (float) (Rrs[b][i] / denom_pi);
                        }

                        float[] rrs_pixel = new float[7];
                        float[] a_pixel = new float[6];
                        float[] bbp_pixel = new float[6];
                        float[] aph_pixel = new float[6];
                        float[] adg_pixel = new float[6];

                        /**
                         * QAA v5 processing
                         */
                        // steps 0-6
                        // The length of pixel is 7 bands, rrs_pixel... are 6 bands
                        qaaf_v5(pixel, rrs_pixel, a_pixel, bbp_pixel, SubProgressMonitor.create(pm, 1));

                        // steps 7-10
                        qaaf_decomp(rrs_pixel, a_pixel, aph_pixel, adg_pixel, SubProgressMonitor.create(pm, 1));

                        analyticalFlagData[i] = FLAG_VALID;

                        for (int b = 0; b < a.length; b++) {
                            a[b][i] = (float) aw[b] + aph_pixel[b] + adg_pixel[b];
                            if (a[b][i] > a_upper_bound || a[b][i] < a_lower_bound) {
                                a[b][i] = AnalyticalPN.noDataValue;
                            }
                            bb[b][i] = (float) bbw[b] + bbp_pixel[b];
                            if (bb[b][i] > bb_upper_bound || bb[b][i] < bb_lower_bound) {
                                bb[b][i] = AnalyticalPN.noDataValue;
                            }
                        }
                        for (int b = 0; b < aph.length; b++) {
                            aph[b][i] = aph_pixel[b];
                            if (aph[b][i] > aph_upper_bound || aph[b][i] < aph_lower_bound) {
                                aph[b][i] = AnalyticalPN.noDataValue;
                            }
                            adg[b][i] = adg_pixel[b];
                            if (adg[b][i] < 0.0f) {
                                analyticalFlagData[i] = FLAG_NEGATIVE_ADG;
                                adg[b][i] = AnalyticalPN.noDataValue;
                            } else if (adg[b][i] > adg_upper_bound) {
                                adg[b][i] = AnalyticalPN.noDataValue;
                            }
                        }
                    } else {
                        analyticalFlagData[i] = FLAG_INVALID;
                        for (int b = 0; b < a.length; b++) {
                            a[b][i] = noDataValue;
                            bb[b][i] = noDataValue;
                        }
                        for (int b = 0; b < aph.length; b++) {
                            aph[b][i] = noDataValue;
                            adg[b][i] = noDataValue;
                        }
                    }
                } catch (ImaginaryNumberException e) {
                    analyticalFlagData[i] = AnalyticalPN.FLAG_IMAGINARY;
                    for (int b = 0; b < a.length; b++) {
                        a[b][i] = noDataValue;
                        bb[b][i] = noDataValue;
                    }
                    for (int b = 0; b < aph.length; b++) {
                        aph[b][i] = noDataValue;
                        adg[b][i] = noDataValue;
                    }
                }
            }

            subPM = new SubProgressMonitor(pm, 1);
            /**
             * Data to be written to output product.
             */
            try {
                subPM.beginTask("Write qaa band pixel data...",
                                QaaABands.length);
                for (int b = 0; b < QaaABands.length; b++) {
                    // Get output product data for current frame.
                    ProductData data = getFrameData(QaaABands[b]);
                    // Make it useable.
                    float[] scanLine = (float[]) data.getElems();
                    // Write data
                    for (int i = 0; i < frameSize; i++) {
                        scanLine[i] = a[b][i];
                    }
                }
                for (int b = 0; b < QaaBbBands.length; b++) {
                    // Get output product data for current frame.
                    ProductData data = getFrameData(QaaBbBands[b]);
                    // Make it useable.
                    float[] scanLine = (float[]) data.getElems();
                    // Write data
                    for (int i = 0; i < frameSize; i++) {
                        scanLine[i] = bb[b][i];
                    }
                }
                for (int b = 0; b < QaaAphBands.length; b++) {
                    // Get output product data for current frame.
                    ProductData data = getFrameData(QaaAphBands[b]);
                    // Make it useable.
                    float[] scanLine = (float[]) data.getElems();
                    // Write data
                    for (int i = 0; i < frameSize; i++) {
                        scanLine[i] = aph[b][i];
                    }
                }
                for (int b = 0; b < QaaAdgBands.length; b++) {
                    // Get output product data for current frame.
                    ProductData data = getFrameData(QaaAdgBands[b]);
                    // Make it useable.
                    float[] scanLine = (float[]) data.getElems();
                    // Write data
                    for (int i = 0; i < frameSize; i++) {
                        scanLine[i] = adg[b][i];
                    }
                }

                // Get output product data for current frame.
                ProductData data = getFrameData(analyticalFlagBand);
                // Make it useable.
                byte[] scanLine = (byte[]) data.getElems();
                // Write data
                for (int i = 0; i < frameSize; i++) {
                    scanLine[i] = (byte) analyticalFlagData[i];
                }
            } finally {
                subPM.done();
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    /**
     * Steps 0 through 6 of QAA v5.
     *
     * @param pm progress monitor
     *
     * @throws ImaginaryNumberException
     */
    protected void qaaf_v5(float[] Rrs, float[] rrs, float[] a, float[] bbp,
                           ProgressMonitor pm) throws ImaginaryNumberException {
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
        float Rrs670;
        Rrs670_upper = (float) (20.0 * Math.pow(Rrs[idx560], 1.5));
        Rrs670_lower = (float) (0.9 * Math.pow(Rrs[idx560], 1.7));
        Rrs670 = (float) (0.00018 * Math.pow(Rrs[idx490] / Rrs[idx560], 3.19));
        Rrs670 += (float) (1.27 * Math.pow(Rrs[idx560], 1.47));
        // if Rrs[670] out of bounds, reassign its value by QAA v5.
        if (Rrs[idx670] > Rrs670_upper || Rrs[idx670] < Rrs670_lower || Rrs[idx670] == noDataValue) {
            Rrs[idx670] = Rrs670;
        }

        // step 0.2 prepare rrs
        for (int b = 0; b < Rrs.length; b++) {
            rrs[b] = (float) (Rrs[b] / (0.52 + 1.7 * Rrs[b]));
        }
        pm.worked(1);

        // step 1
        for (int b = 0; b < Rrs.length - 1; b++) {
            double nom = Math.pow(g0, 2.0) + 4.0 * g1 * rrs[b];
            if (nom >= 0) {
                u[b] = (float) ((Math.sqrt(nom) - g0) / (2.0 * g1));
            } else {
                throw new ImaginaryNumberException("Will produce an imaginary number", nom);
            }
        }
        pm.worked(1);

        // step 2
        float rho;
        float numer;
        float denom;
        float result;

        denom = (float) (rrs[idx560] + 5 * rrs[idx670] * (rrs[idx670] / rrs[idx490]));
        numer = rrs[idx440] + rrs[idx490];
        result = numer / denom;
        if (result <= 0) {
            throw new ImaginaryNumberException(
                    "Will produce an imaginary number", result);
        }
        rho = (float) Math.log10(result);
        rho = (float) (acoefs[0] + acoefs[1] * rho + acoefs[2] * Math.pow(rho, 2.0));
        a560 = (float) (aw[idx560] + Math.pow(10.0, rho));
        pm.worked(1);

        // step 3
        bbp560 = (float) (((u[idx560] * a560) / (1.0 - u[idx560])) - bbw[idx560]);
        pm.worked(1);

        // step 4
        rat = rrs[idx440] / rrs[idx560];
        Y = (float) (2.0 * (1.0 - 1.2 * Math.exp(-0.9 * rat)));
        pm.worked(1);

        // step 5
        for (int b = 0; b < Rrs.length - 1; b++) {
            bbp[b] = (float) (bbp560 * Math.pow((float) wavel[idx560] / (float) wavel[b], Y));
        }
        pm.worked(1);

        // step 6
        for (int b = 0; b < Rrs.length - 1; b++) {
            a[b] = (float) (((1.0 - u[b]) * (bbw[b] + bbp[b])) / u[b]);
        }
        pm.worked(1);
    }

    /**
     * Steps 7 through 10 of QAA v5.
     *
     * @param pm
     */
    protected void qaaf_decomp(float[] rrs, float[] a, float[] aph,
                               float[] adg, ProgressMonitor pm) {
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
        pm.worked(1);

        // step 8
        double S = 0.015 + 0.002 / (0.6 + rat); // new in QAA v5
        zeta = (float) Math.exp(S * (wavel[idx440] - wavel[idx410]));
        pm.worked(1);

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
        pm.worked(1);

    }

    public void setAphCheck(boolean pi_check) {
        this.pi_check = pi_check;
    }

    public void startProcessing(float a_lower, float a_upper, float bb_lower,
                                float bb_upper, float aph_lower, float aph_upper, float adg_upper) {
        a_lower_bound = a_lower;
        a_upper_bound = a_upper;
        bb_lower_bound = bb_lower;
        bb_upper_bound = bb_upper;
        aph_lower_bound = aph_lower;
        aph_upper_bound = aph_upper;
        adg_upper_bound = adg_upper;
    }
}