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

import java.awt.Color;
import java.awt.Rectangle;

@SuppressWarnings({"UnusedDeclaration"})
@OperatorMetadata(alias = "Meris.QaaIOP",
                  description = "Performs retrieval of inherent optical properties (IOPs) for " +
                                "coastal and open ocean waters for MERIS.",
                  authors = " Zhongping Lee, Mingrui Zhang (WSU); Marco Peters (Brockmann Consult)",
                  copyright = "(C) 2011 by NRL and WSU",
                  version = "1.1")
public class QaaOp extends PixelOperator {

    private static final String PRODUCT_TYPE = "QAA_L2";
    private static final String FLAG_CODING = "analytical_flags";
    private static final String ANALYSIS_FLAG_BAND_NAME = FLAG_CODING;

    private static final int[] A_INDEXES = {0, 1, 2, 3, 4};
    private static final int[] BB_INDEXES = {5, 6, 7, 8, 9};
    private static final int[] APH_INDEXES = {10, 11, 12};
    private static final int[] ADG_INDEXES = {13, 14, 15};
    private static final int FLAG_INDEX = 16;
    private static final int L2_WATER_FLAG_INDEX = 21;
    private static final byte FLAG_INVALID = 8;
    private static final byte FLAG_NEGATIVE_ADG = 4;
    private static final byte FLAG_IMAGINARY = 2;
    private static final byte FLAG_VALID = 1;
    private static final float NO_DATA_VALUE = Float.MAX_VALUE;
    private static final String A_TOTAL_PATTERN = "a_total_%d";
    private static final String BB_SPM_PATTERN = "bb_spm_%d";
    private static final String A_PIG_PATTERN = "a_pig_%d";
    private static final String A_YS_PATTERN = "a_ys_%d";

    @SourceProduct(alias = "source", description = "The source product.",
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

    @Parameter(defaultValue = "not l2_flags.WATER",
               description = "Expression defining pixels not considered for processing.")
    private String invalidPixelExpression;

    @Parameter(alias = "aLowerBound", defaultValue = "-0.02", label = "'A' Lower Bound",
               description = "The lower bound of the valid value range.")
    private float a_lower;
    @Parameter(alias = "aUpperBound", defaultValue = "5.0", label = "'A' Upper Bound",
               description = "The upper bound of the valid value range.")
    private float a_upper;
    @Parameter(alias = "bbLowerBound", defaultValue = "-0.2", label = "'BB' Lower Bound",
               description = "The lower bound of the valid value range.")
    private float bb_lower;
    @Parameter(alias = "bbUpperBound", defaultValue = "5.0", label = "'BB' Upper Bound",
               description = "The upper bound of the valid value range.")
    private float bb_upper;
    @Parameter(alias = "aphLowerBound", defaultValue = "-0.02", label = "'APH' Lower Bound",
               description = "The lower bound of the valid value range.")
    private float aph_lower;
    @Parameter(alias = "aphUpperBound", defaultValue = "3.0", label = "'APH' Upper Bound",
               description = "The upper bound of the valid value range.")
    private float aph_upper;
    @Parameter(alias = "adgUpperBound", defaultValue = "1.0", label = "'ADG' Upper Bound",
               description = "The upper bound of the valid value range. The lower bound is always 0.")
    private float adg_upper;
    @Parameter(defaultValue = "true", label = "Divide source Rrs by PI(3.14)",
               description = "If selected the source remote reflectances are divided by PI")
    private boolean divideByPI;

    private Qaa qaa;
    private VirtualBandOpImage invalidOpImage;

    @Override
    protected void prepareInputs() throws OperatorException {
        validateSourceProduct();
        if (!sourceProduct.isCompatibleBandArithmeticExpression(invalidPixelExpression)) {
            String message = String.format("The given expression '%s' is not compatible with the source product.",
                                           invalidPixelExpression);
            throw new OperatorException(message);
        }
        invalidOpImage = VirtualBandOpImage.createMask(invalidPixelExpression,
                                                       sourceProduct,
                                                       ResolutionLevel.MAXRES);
        qaa = new Qaa(NO_DATA_VALUE);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer configurer) {
        super.configureTargetProduct(configurer);
        for (int i = 0; i < A_INDEXES.length; i++) {
            addBand(configurer, A_TOTAL_PATTERN, Qaa.WAVELENGTH[i],
                    "Total absorption coefficient of all water constituents at %d nm.");
        }
        for (int i = 0; i < BB_INDEXES.length; i++) {
            addBand(configurer, BB_SPM_PATTERN, Qaa.WAVELENGTH[i],
                    "Backscattering of suspended particulate matter at %d nm.");
        }

        for (int i = 0; i < APH_INDEXES.length; i++) {
            addBand(configurer, A_PIG_PATTERN, Qaa.WAVELENGTH[i], "Pigment absorption coefficient at %d nm.");
        }

        for (int i = 0; i < ADG_INDEXES.length; i++) {
            addBand(configurer, A_YS_PATTERN, Qaa.WAVELENGTH[i],
                    "Yellow substance absorption coefficient at %d nm.");
        }

        Product targetProduct = configurer.getTargetProduct();
        final int sceneWidth = targetProduct.getSceneRasterWidth();
        final int sceneHeight = targetProduct.getSceneRasterHeight();
        final FlagCoding flagCoding = new FlagCoding(FLAG_CODING);
        flagCoding.setDescription("QAA-for-IOP specific flags.");
        targetProduct.getFlagCodingGroup().add(flagCoding);

        addFlagAndMask(targetProduct, flagCoding, "normal", "A valid water pixel.", FLAG_VALID, Color.BLUE);
        addFlagAndMask(targetProduct, flagCoding, "Imaginary_number",
                       "Classified as water, but an imaginary number would have been produced.",
                       FLAG_IMAGINARY, Color.RED);
        addFlagAndMask(targetProduct, flagCoding, "Negative_Adg",
                       "Classified as water, but one or more of the bands contain a negative Adg value.",
                       FLAG_NEGATIVE_ADG, Color.YELLOW);
        addFlagAndMask(targetProduct, flagCoding, "non_water",
                       "Not classified as a water pixel (land/cloud).",
                       FLAG_INVALID, Color.BLACK);

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

        if (isSampleValid(x, y)) { // Check if it is water
            final float[] rrs = new float[sourceSamples.length];
            for (int i = 0; i < rrs.length; i++) {
                rrs[i] = sourceSamples[i].getFloat();
                if (divideByPI) {    //Take care of Pi
                    rrs[i] /= Math.PI;
                }
            }
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
                targetSamples[FLAG_INDEX].set((int) FLAG_VALID);

                for (int i = 0; i < A_INDEXES.length; i++) {
                    float a = (float) Qaa.AW_COEFS[i] + aph_pixel[i] + adg_pixel[i];
                    a = checkAgainstBounds(a, a_lower, a_upper);
                    targetSamples[A_INDEXES[i]].set(a);
                }
                for (int i = 0; i < BB_INDEXES.length; i++) {
                    float bb = (float) Qaa.BBW_COEFS[i] + bbp_pixel[i];
                    bb = checkAgainstBounds(bb, bb_lower, bb_upper);
                    targetSamples[BB_INDEXES[i]].set(bb);
                }
                for (int i = 0; i < APH_INDEXES.length; i++) {
                    float aph = aph_pixel[i];
                    aph = checkAgainstBounds(aph, aph_lower, aph_upper);
                    targetSamples[APH_INDEXES[i]].set(aph);
                }
                for (int i = 0; i < ADG_INDEXES.length; i++) {
                    float adg = adg_pixel[i];
                    if (adg < 0) {
                        targetSamples[FLAG_INDEX].set((int) FLAG_NEGATIVE_ADG);
                    }
                    adg = checkAgainstBounds(adg, 0.0f, adg_upper);
                    targetSamples[ADG_INDEXES[i]].set(adg);
                }

            } catch (ImaginaryNumberException ignored) {
                handleInvalid(targetSamples, FLAG_IMAGINARY);
            }
        } else {
            handleInvalid(targetSamples, FLAG_INVALID);
        }
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

    private boolean isSampleValid(int x, int y) {
        return invalidOpImage.getData(new Rectangle(x, y, 1, 1)).getSample(x, y, 0) == 0;
    }

    private float checkAgainstBounds(float value, float lowerBound, float upperBound) {
        if (value < lowerBound || value > upperBound) {
            value = NO_DATA_VALUE;
        }
        return value;
    }

    private void handleInvalid(WritableSample[] targetSamples, int flagIndex) {
        targetSamples[FLAG_INDEX].set(flagIndex);
        for (int i = 0; i < targetSamples.length - 1; i++) {
            targetSamples[i].set(NO_DATA_VALUE);
        }
    }

    private void addFlagAndMask(Product targetProduct, FlagCoding flagCoding, String flagName, String flagDescription,
                                byte flagMask, Color color) {
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
        Band band = configurer.addBand(bandName, ProductData.TYPE_FLOAT32, NO_DATA_VALUE);
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
