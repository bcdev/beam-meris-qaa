package org.esa.beam.meris.qaa;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.meris.qaa.algorithm.Qaa;
import org.esa.beam.meris.qaa.algorithm.QaaAlgorithm;
import org.esa.beam.meris.qaa.algorithm.QaaConfig;
import org.esa.beam.meris.qaa.algorithm.QaaConstants;
import org.esa.beam.meris.qaa.algorithm.QaaResult;
import org.esa.beam.meris.qaa.algorithm.WaterClarity;

import java.awt.Color;
import java.awt.Rectangle;

@SuppressWarnings({"UnusedDeclaration"})
@OperatorMetadata(alias = "Meris.QaaIOP",
                  description = "Performs retrieval of inherent optical properties (IOPs) for " +
                                "coastal and open ocean waters for MERIS.",
                  authors = " Zhongping Lee (University of Massachusetts Boston), Mingrui Zhang (WSU); Marco Peters (Brockmann Consult)",
                  copyright = "(C) 2014 by NRL and WSU",
                  version = "1.4")
public class QaaOp extends PixelOperator {

    private static final String PRODUCT_TYPE = "QAA_L2";

    private static final int FLAG_BAND_INDEX = 16;
    private static final String A_TOTAL_PATTERN = "a_total_%d";
    private static final String BB_SPM_PATTERN = "bb_spm_%d";
    private static final String A_PIG_PATTERN = "a_pig_%d";
    private static final String A_YS_PATTERN = "a_ys_%d";
    private static final String FLAG_CODING = "analytical_flags";
    private static final String ANALYSIS_FLAG_BAND_NAME = FLAG_CODING;
    private static final int FLAG_INDEX = 1;
    private static final int[] WC_INDEXES = {0}; //y.jiang  water clarity


    @SourceProduct(alias = "source", label = "Source", description = "The source product containing reflectances.",
                   bands = {
                           EnvisatConstants.MERIS_L2_REFLEC_1_BAND_NAME,
                           EnvisatConstants.MERIS_L2_REFLEC_2_BAND_NAME,
                           EnvisatConstants.MERIS_L2_REFLEC_3_BAND_NAME,
                           EnvisatConstants.MERIS_L2_REFLEC_4_BAND_NAME,
                           EnvisatConstants.MERIS_L2_REFLEC_5_BAND_NAME,
                           EnvisatConstants.MERIS_L2_REFLEC_6_BAND_NAME,
                           EnvisatConstants.MERIS_L2_REFLEC_7_BAND_NAME
                   })
    private Product sourceProduct;

    @Parameter(defaultValue = "l2_flags.WATER",
               description = "Expression defining pixels considered for processing.")
    private String validPixelExpression;

    @Parameter(defaultValue = "0.001", label = "'A_TOTAL' lower bound",
               description = "The lower bound of the valid value range.")
    private float aTotalLower;
    @Parameter(defaultValue = "5.0", label = "'A_TOTAL' upper bound",
               description = "The upper bound of the valid value range.")
    private float aTotalUpper;

    @Parameter(defaultValue = "0.0001", label = "'BB_SPM' lower bound",
               description = "The lower bound of the valid value range.")
    private float bbSpmLower;
    @Parameter(defaultValue = "1.0", label = "'BB_SPM' upper bound",
               description = "The upper bound of the valid value range.")
    private float bbSpmUpper;

    @Parameter(defaultValue = "0.0001", label = "'A_PIG' lower bound",
               description = "The lower bound of the valid value range.")
    private float aPigLower;
    @Parameter(defaultValue = "3.0", label = "'A_PIG' upper bound",
               description = "The upper bound of the valid value range.")
    private float aPigUpper;

    @Parameter(defaultValue = "0.0001", label = "'A_YS' upper bound",
               description = "The lower bound of the valid value range.")
    private float aYsLower;

    @Parameter(defaultValue = "1.0", label = "'A_YS' upper bound",
               description = "The upper bound of the valid value range")
    private float aYsUpper;

    @Parameter(defaultValue = "true", label = "Divide source Rrs by PI(3.14)",
               description = "If selected the source remote reflectances are divided by PI")
    private boolean divideByPI;


    /*
    *   The next two parameters are for water clarity. The runWC is an important part
    *   due to it being used throughout the entire program to only run the selected plugin.
    *
    *
    *   n.guggenberger Mar-26-14
    */
    @Parameter(defaultValue = "false", label = "Derive Water Clarity",
               description = "Choose whether to derive Water Clarity in addition to QAA.")
    private boolean runWC;

    @Parameter(defaultValue = "WATER_CLARITY_1", label = "Water Clarity", unit = "%",
               description = "Water Clarity 1%, 10%, 50%.")
    private WaterClarity waterClarity;


    private VirtualBandOpImage validOpImage;
    private QaaAlgorithm qaaAlgorithm;
    private ThreadLocal<QaaResult> qaaResult;
    private Qaa qaa;


    /*
    *   Getters and Setters for the runWC variable. Allowing access to variable when instantiated.
    */
    public boolean getRunWC() {
        return this.runWC;
    }

    public void setRunWC(boolean runStatus) {
        runWC = runStatus;
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        validateSourceProduct();
        validateParameters();
        validOpImage = VirtualBandOpImage.createMask(validPixelExpression,
                                                     sourceProduct,
                                                     ResolutionLevel.MAXRES);

        qaaAlgorithm = new QaaAlgorithm();
        qaaAlgorithm.setConfig(createConfiguredConfig());
        qaa = new Qaa(QaaConstants.NO_DATA_VALUE);

        qaaResult = new ThreadLocal<QaaResult>() {
            @Override
            protected QaaResult initialValue() {
                return new QaaResult();
            }
        };
    }

    private QaaConfig createConfiguredConfig() {
        final QaaConfig config = new QaaConfig();
        config.setDivideByPi(divideByPI);
        config.setAPigLower(aPigLower);
        config.setAPigUpper(aPigUpper);
        config.setATotalLower(aTotalLower);
        config.setATotalUpper(aTotalUpper);
        config.setAYsLower(aYsLower);
        config.setAYsUpper(aYsUpper);
        config.setBbSpmsLower(bbSpmLower);
        config.setBbSpmsUpper(bbSpmUpper);
        return config;
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer configurer) {
        super.configureTargetProduct(configurer);

        //  First of many uses of the if(runWC) or if (!runWC) to determine if that segment needs
        //  to be ran or not.       n.guggenberger Mar-26-14
        if (!runWC) {
            for (int i = 0; i < QaaConstants.A_TOTAL_BAND_INDEXES.length; i++) {
                addBand(configurer, A_TOTAL_PATTERN, QaaConstants.WAVELENGTH[i],
                        "Total absorption coefficient of all water constituents at %d nm.");
            }
            for (int i = 0; i < QaaConstants.BB_SPM_BAND_INDEXES.length; i++) {
                addBand(configurer, BB_SPM_PATTERN, QaaConstants.WAVELENGTH[i],
                        "Backscattering of suspended particulate matter at %d nm.");
            }

            for (int i = 0; i < QaaConstants.A_PIG_BAND_INDEXES.length; i++) {
                addBand(configurer, A_PIG_PATTERN, QaaConstants.WAVELENGTH[i],
                        "Pigment absorption coefficient at %d nm.");
            }

            for (int i = 0; i < QaaConstants.A_YS_BAND_INDEXES.length; i++) {
                addBand(configurer, A_YS_PATTERN, QaaConstants.WAVELENGTH[i],
                        "Yellow substance absorption coefficient at %d nm.");
            }
        }


        if (runWC) {
            String bandnameEZ = "Water Clarity_" + waterClarity + "%%";
            String discriptionEZ = bandnameEZ;
            addBand(configurer, bandnameEZ, 490, discriptionEZ); //y.jiang
        }

        Product targetProduct = configurer.getTargetProduct();
        final int sceneWidth = targetProduct.getSceneRasterWidth();
        final int sceneHeight = targetProduct.getSceneRasterHeight();
        final FlagCoding flagCoding = new FlagCoding(FLAG_CODING);
        flagCoding.setDescription("QAA-for-IOP specific flags.");
        targetProduct.getFlagCodingGroup().add(flagCoding);

        //noinspection PointlessBitwiseExpression
        addFlagAndMask(targetProduct, flagCoding, "normal", "Valid water pixels",
                       QaaConstants.FLAG_MASK_VALID, Color.BLUE);
        addFlagAndMask(targetProduct, flagCoding, "imaginary_number",
                       "Pixels that are classified as water, but an imaginary number would have been produced",
                       QaaConstants.FLAG_MASK_IMAGINARY, Color.RED);
        addFlagAndMask(targetProduct, flagCoding, "negative_a_ys",
                       "Pixels that are classified  as water, but one or more of the bands contain a negative a_ys value",
                       QaaConstants.FLAG_MASK_NEGATIVE_AYS, Color.YELLOW);
        addFlagAndMask(targetProduct, flagCoding, "non_water",
                       "Pixels that are not classified as a water pixel (land/cloud)",
                       QaaConstants.FLAG_MASK_INVALID, Color.BLACK);
        addFlagAndMask(targetProduct, flagCoding, "a_total_oob",
                       "At least one value of the a_total spectrum is out of bounds",
                       QaaConstants.FLAG_MASK_A_TOTAL_OOB, Color.CYAN);
        addFlagAndMask(targetProduct, flagCoding, "bb_spm_oob",
                       "At least one value of the bb_spm spectrum is out of bounds",
                       QaaConstants.FLAG_MASK_BB_SPM_OOB, Color.MAGENTA);
        addFlagAndMask(targetProduct, flagCoding, "a_pig_oob",
                       "At least one value of the a_pig spectrum is out of bounds",
                       QaaConstants.FLAG_MASK_A_PIG_OOB, Color.ORANGE);
        addFlagAndMask(targetProduct, flagCoding, "a_ys_oob",
                       "At least one value of the a_ys spectrum is out of bounds",
                       QaaConstants.FLAG_MASK_A_YS_OOB, Color.PINK);

        Band analyticalFlagBand = new Band(ANALYSIS_FLAG_BAND_NAME, ProductData.TYPE_UINT8, sceneWidth, sceneHeight);
        analyticalFlagBand.setSampleCoding(flagCoding);
        targetProduct.addBand(analyticalFlagBand);

        targetProduct.setProductType(PRODUCT_TYPE);
    }


    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < 7; i++) {
            sampleConfigurer.defineSample(i, EnvisatConstants.MERIS_L2_BAND_NAMES[i]);
        }
        if (runWC) {
            sampleConfigurer.defineSample(7, EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME); //changed April-23-2014 N. Guggenberger
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        final String[] targetBandNames = getTargetProduct().getBandNames();
        int sampleIndex = 0;
        for (final String targetBandName : targetBandNames) {
            if (!getTargetProduct().getBand(targetBandName).isSourceImageSet()) {
                sampleConfigurer.defineSample(sampleIndex++, targetBandName);
            }
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        QaaResult result = qaaResult.get();

        if (isSampleValid(x, y)) { // Check if it is water
            final float[] rrs;
            if (!runWC) {
                rrs = new float[sourceSamples.length];  //y. jiang
            } else {
                rrs = new float[sourceSamples.length - 1];  //y. jiang
            }
            for (int i = 0; i < rrs.length; i++) {
                rrs[i] = sourceSamples[i].getFloat();
                if (runWC) {
                    if (divideByPI) {    //Take care of Pi
                        rrs[i] /= Math.PI;
                    }
                }
            }
            if (!runWC) {
                result = qaaAlgorithm.process(rrs, result);
            }
            if (runWC) {
                try {
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
                    qaa.qaaf_v5(rrs, rrs_pixel, a_pixel, bbp_pixel);
                    // steps 7-10
                    qaa.qaaf_decomp(rrs_pixel, a_pixel, aph_pixel, adg_pixel);

                    // fill target samples
                    targetSamples[FLAG_INDEX].set((int) QaaConstants.FLAG_MASK_VALID);

                    int IDX_490 = 2;
                    float a = 0;
                    a = 0;
                    a = (float) Qaa.AW_COEFS[IDX_490] + aph_pixel[IDX_490] + adg_pixel[IDX_490];
                    a = checkAgainstBounds(a, aTotalLower, aTotalUpper);
                    // targetSamples[A_INDEXES[i]].set(a);

                    float bb = 0;
                    bb = (float) Qaa.BBW_COEFS[IDX_490] + bbp_pixel[IDX_490];
                    bb = checkAgainstBounds(bb, bbSpmLower, bbSpmUpper);


                    float z = qaa.qaaf_zeu(a, bb, sourceSamples[7].getFloat(), waterClarity); //remove x,y from params
                    for (int WC_INDEX : WC_INDEXES) {
                        if (z > 0) {
                            targetSamples[WC_INDEXES[0]].set(z);       //y.jiang
                        } else {
                            throw new NegativeNumberException("Will produce an negative number", z);
                        }
                    }


                } catch (NegativeNumberException ignored) {
                    handleInvalid(targetSamples, QaaConstants.FLAG_MASK_INVALID);
                    // targetSamples[WC_INDEXES[0]].set(sourceSamples[2].getFloat());
                } catch (ImaginaryNumberException ignored) {
                    handleInvalid(targetSamples, QaaConstants.FLAG_MASK_IMAGINARY);
                    // targetSamples[WC_INDEXES[0]].set(sourceSamples[2].getFloat());
                }
            }
        } else {
            if (runWC) {
                handleInvalid(targetSamples, QaaConstants.FLAG_MASK_INVALID);
//           targetSamples[WC_INDEXES[0]].set(sourceSamples[2].getFloat());
            } else {
                result.invalidate();
            }
        }

        if (!runWC) {
            writeResult(targetSamples, result);
        }
    }

    static void writeResult(WritableSample[] targetSamples, QaaResult qaaResult) {
        final float[] a_total = qaaResult.getA_Total();
        for (int i = 0; i < a_total.length; i++) {
            targetSamples[QaaConstants.A_TOTAL_BAND_INDEXES[i]].set(a_total[i]);
        }

        final float[] bb_spm = qaaResult.getBB_SPM();
        for (int i = 0; i < bb_spm.length; i++) {
            targetSamples[QaaConstants.BB_SPM_BAND_INDEXES[i]].set(bb_spm[i]);
        }

        final float[] a_pig = qaaResult.getA_PIG();
        for (int i = 0; i < a_pig.length; i++) {
            targetSamples[QaaConstants.A_PIG_BAND_INDEXES[i]].set(a_pig[i]);
        }

        final float[] a_ys = qaaResult.getA_YS();
        for (int i = 0; i < a_ys.length; i++) {
            targetSamples[QaaConstants.A_YS_BAND_INDEXES[i]].set(a_ys[i]);
        }
        targetSamples[FLAG_BAND_INDEX].set(qaaResult.getFlags());
    }

    private void validateSourceProduct() {
        for (int i = 0; i < 7; i++) {
            String requiredBandName = EnvisatConstants.MERIS_L2_BAND_NAMES[i];
            if (!sourceProduct.containsBand(requiredBandName)) {
                String msg = String.format("Source product must contain a band with the name '%s'", requiredBandName);
                throw new OperatorException(msg);
            }
        }
    }

    private void validateParameters() {
        if (!sourceProduct.isCompatibleBandArithmeticExpression(validPixelExpression)) {
            String message = String.format("The given expression '%s' is not compatible with the source product.",
                                           validPixelExpression);
            throw new OperatorException(message);
        }
    }

    private boolean isSampleValid(int x, int y) {
        return validOpImage.getData(new Rectangle(x, y, 1, 1)).getSample(x, y, 0) != 0;
    }

    private float checkAgainstBounds(float value, float lowerBound, float upperBound) {
        if (value < lowerBound || value > upperBound) {
            value = QaaConstants.NO_DATA_VALUE;
        }
        return value;
    }

    private void handleInvalid(WritableSample[] targetSamples, int flagIndex) {
        targetSamples[FLAG_INDEX].set(flagIndex);
        for (int i = 0; i < targetSamples.length - 1; i++) {
            targetSamples[i].set(QaaConstants.NO_DATA_VALUE);
        }
    }

    private void addFlagAndMask(Product targetProduct, FlagCoding flagCoding, String flagName, String flagDescription,
                                int flagMask, Color color) {
        flagCoding.addFlag(flagName, flagMask, flagDescription);
        final Mask mask = Mask.BandMathsType.create(flagName, flagDescription,
                                                    targetProduct.getSceneRasterWidth(),
                                                    targetProduct.getSceneRasterHeight(),
                                                    ANALYSIS_FLAG_BAND_NAME + "." + flagName,
                                                    color, 0.5f);
        targetProduct.getMaskGroup().add(mask);
    }

    private Band addBand(ProductConfigurer configurer, String namePattern, int wavelength, String descriptionPattern) {
        String bandName = String.format(namePattern, wavelength);
        Band band = configurer.addBand(bandName, ProductData.TYPE_FLOAT32, QaaConstants.NO_DATA_VALUE);
        band.setDescription(String.format(descriptionPattern, wavelength));
        band.setUnit("m^-1");
        band.setSpectralWavelength(wavelength);
        band.setNoDataValueUsed(true);
        return band;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(QaaOp.class);
        }

    }
}
