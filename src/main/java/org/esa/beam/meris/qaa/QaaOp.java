package org.esa.beam.meris.qaa;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
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
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"UnusedDeclaration"})
@OperatorMetadata(alias = "Meris.QaaIOP",
                  description = "Performs retrieval of inherent optical properties (IOPs) for " +
                                "coastal and open ocean waters for MERIS.",
                  authors = " Zhongping Lee, Mingrui Zhang (WSU); Marco Peters (Brockmann Consult)",
                  copyright = "(C) 2011 by NRL and WSU",
                  version = "1.1")
public class QaaOp extends PixelOperator {

    private static final String PRODUCT_TYPE = "QAA_L2";

    private static final int FLAG_BAND_INDEX = 16;
    private static final String A_TOTAL_PATTERN = "a_total_%d";
    private static final String BB_SPM_PATTERN = "bb_spm_%d";
    private static final String A_PIG_PATTERN = "a_pig_%d";
    private static final String A_YS_PATTERN = "a_ys_%d";
    private static final String FLAG_CODING = "analytical_flags";
    private static final String ANALYSIS_FLAG_BAND_NAME = FLAG_CODING;

    private static final int FLAG_INDEX_A_YS_OOB = 7;
    private static final int FLAG_INDEX_A_PIG_OOB = 6;
    private static final int FLAG_INDEX_BB_SPM_OOB = 5;
    private static final int FLAG_INDEX_A_TOTAL_OOB = 4;
    private static final int FLAG_INDEX_INVALID = 3;
    private static final int FLAG_INDEX_NEGATIVE_AYS = 2;
    private static final int FLAG_INDEX_IMAGINARY = 1;
    private static final int FLAG_INDEX_VALID = 0;

    private static final float NO_DATA_VALUE = Float.NaN;


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

    @Parameter(defaultValue = "-0.02", label = "'A_TOTAL' lower bound",
               description = "The lower bound of the valid value range.")
    private float aTotalLower;
    @Parameter(defaultValue = "5.0", label = "'A_TOTAL' upper bound",
               description = "The upper bound of the valid value range.")
    private float aTotalUpper;
    @Parameter(defaultValue = "-0.2", label = "'BB_SPM' lower bound",
               description = "The lower bound of the valid value range.")
    private float bbSpmLower;
    @Parameter(defaultValue = "5.0", label = "'BB_SPM' upper bound",
               description = "The upper bound of the valid value range.")
    private float bbSpmUpper;
    @Parameter(defaultValue = "-0.02", label = "'A_PIG' lower bound",
               description = "The lower bound of the valid value range.")
    private float aPigLower;
    @Parameter(defaultValue = "3.0", label = "'A_PIG' upper bound",
               description = "The upper bound of the valid value range.")
    private float aPigUpper;
    @Parameter(defaultValue = "1.0", label = "'A_YS' upper bound",
               description = "The upper bound of the valid value range. The lower bound is always 0.")
    private float aYsUpper;
    @Parameter(defaultValue = "true", label = "Divide source Rrs by PI(3.14)",
               description = "If selected the source remote reflectances are divided by PI")
    private boolean divideByPI;

    private Qaa qaa;
    private VirtualBandOpImage invalidOpImage;
    private QaaAlgorithm qaaAlgorithm;
    private ThreadLocal<QaaResult> qaaResult;

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

        // -----------------
        qaaAlgorithm = new QaaAlgorithm();
        final QaaConfig qaaConfig = createConfiguredConfig();
        qaaAlgorithm.setConfig(qaaConfig);
        qaaResult = new ThreadLocal<QaaResult>() {
            @Override
            protected QaaResult initialValue() {
                return new QaaResult();
            }
        };
        // -----------------
    }

    private QaaConfig createConfiguredConfig() {
        final QaaConfig config = new QaaConfig();
        config.setDivideByPi(divideByPI);
        config.setAPigLower(aPigLower);
        config.setAPigUpper(aPigUpper);
        config.setATotalLower(aTotalLower);
        config.setATotalUpper(aTotalUpper);
        config.setAYsLower(0.f);
        config.setAYsUpper(aYsUpper);
        config.setBbSpmsLower(bbSpmLower);
        config.setBbSpmsUpper(bbSpmUpper);
        return config;
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer configurer) {
        super.configureTargetProduct(configurer);
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

        Product targetProduct = configurer.getTargetProduct();
        final int sceneWidth = targetProduct.getSceneRasterWidth();
        final int sceneHeight = targetProduct.getSceneRasterHeight();
        final FlagCoding flagCoding = new FlagCoding(FLAG_CODING);
        flagCoding.setDescription("QAA-for-IOP specific flags.");
        targetProduct.getFlagCodingGroup().add(flagCoding);

        //noinspection PointlessBitwiseExpression
        addFlagAndMask(targetProduct, flagCoding, "normal", "A valid water pixel.",
                       1 << FLAG_INDEX_VALID, Color.BLUE);
        addFlagAndMask(targetProduct, flagCoding, "imaginary_number",
                       "Classified as water, but an imaginary number would have been produced.",
                       1 << FLAG_INDEX_IMAGINARY, Color.RED);
        addFlagAndMask(targetProduct, flagCoding, "negative_a_ys",
                       "Classified as water, but one or more of the bands contain a negative a_ys value.",
                       1 << FLAG_INDEX_NEGATIVE_AYS, Color.YELLOW);
        addFlagAndMask(targetProduct, flagCoding, "non_water",
                       "Not classified as a water pixel (land/cloud).",
                       1 << FLAG_INDEX_INVALID, Color.BLACK);
        addFlagAndMask(targetProduct, flagCoding, "a_total_oob",
                       "At least one value of the a_total spectrum is out of bounds.",
                       1 << FLAG_INDEX_A_TOTAL_OOB, Color.CYAN);
        addFlagAndMask(targetProduct, flagCoding, "bb_spm_oob",
                       "At least one value of the bb_spm spectrum is out of bounds.",
                       1 << FLAG_INDEX_BB_SPM_OOB, Color.MAGENTA);
        addFlagAndMask(targetProduct, flagCoding, "a_pig_oob",
                       "At least one value of the a_pig spectrum is out of bounds.",
                       1 << FLAG_INDEX_A_PIG_OOB, Color.ORANGE);
        addFlagAndMask(targetProduct, flagCoding, "a_ys_oob",
                       "At least one value of the a_ys spectrum is out of bounds.",
                       1 << FLAG_INDEX_A_YS_OOB, Color.PINK);

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
        new_impl(x, y, sourceSamples, targetSamples);


//        old_impl(x, y, sourceSamples, targetSamples);
    }

    private void new_impl(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        //        // @todo 2 tb/tb move to algorithm class 2013-02-26
        QaaResult result = qaaResult.get();

        if (isSampleValid(x, y)) { // Check if it is water
            final float[] rrs = new float[sourceSamples.length];
            for (int i = 0; i < rrs.length; i++) {
                rrs[i] = sourceSamples[i].getFloat();
            }
            result = qaaAlgorithm.process(rrs, result);
        } else {
            result.invalidate();
        }

        writeResult(targetSamples, result);
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

    private void old_impl(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
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
                float[] bbSpm_pixel = new float[6];
                float[] aPig_pixel = new float[6];
                float[] aYs_pixel = new float[6];

                /**
                 * QAA v5 processing
                 */
                // steps 0-6
                // The length of pixel is 7 bands, rrs_pixel... are 6 bands
                qaa.qaaf_v5(rrs, rrs_pixel, a_pixel, bbSpm_pixel);
                // steps 7-10
                qaa.qaaf_decomp(rrs_pixel, a_pixel, aPig_pixel, aYs_pixel);

                // fill target samples
                targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_VALID, true);

                computeATotal(targetSamples, aPig_pixel, aYs_pixel);
                computeBbSpm(targetSamples, bbSpm_pixel);
                computeAPig(targetSamples, aPig_pixel);
                computeAYs(targetSamples, aYs_pixel);

            } catch (ImaginaryNumberException ignored) {
                handleInvalid(targetSamples, FLAG_INDEX_IMAGINARY);
            }
        } else {
            handleInvalid(targetSamples, FLAG_INDEX_INVALID);
        }
    }

    private void computeAYs(WritableSample[] targetSamples, float[] ays_pixel) {
        for (int i = 0; i < QaaConstants.A_YS_BAND_INDEXES.length; i++) {
            float ays = ays_pixel[i];
            boolean isOob = isOutOfBounds(ays, 0.0f, aYsUpper);
            if (isOob) {
                targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_A_YS_OOB, true);
                if (ays < 0) {
                    targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_NEGATIVE_AYS, true);
                }
            }
            targetSamples[QaaConstants.A_YS_BAND_INDEXES[i]].set(isOob ? NO_DATA_VALUE : ays);
        }
    }

    private void computeAPig(WritableSample[] targetSamples, float[] aPig_pixel) {
        for (int i = 0; i < QaaConstants.A_PIG_BAND_INDEXES.length; i++) {
            float aPig = aPig_pixel[i];
            boolean isOob = isOutOfBounds(aPig, aPigLower, aPigUpper);
            if (isOob) {
                targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_A_PIG_OOB, true);
            }
            targetSamples[QaaConstants.A_PIG_BAND_INDEXES[i]].set(isOob ? NO_DATA_VALUE : aPig);
        }
    }

    private void computeBbSpm(WritableSample[] targetSamples, float[] bbSpm_pixel) {
        for (int i = 0; i < QaaConstants.BB_SPM_BAND_INDEXES.length; i++) {
            float bbSpm = (float) QaaConstants.BBW_COEFS[i] + bbSpm_pixel[i];
            boolean isOob = isOutOfBounds(bbSpm, bbSpmLower, bbSpmUpper);
            if (isOob) {
                targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_BB_SPM_OOB, true);
            }
            targetSamples[QaaConstants.BB_SPM_BAND_INDEXES[i]].set(isOob ? NO_DATA_VALUE : bbSpm);
        }
    }

    private void computeATotal(WritableSample[] targetSamples, float[] aph_pixel, float[] adg_pixel) {
        for (int i = 0; i < QaaConstants.A_TOTAL_BAND_INDEXES.length; i++) {
            float a = (float) QaaConstants.AW_COEFS[i] + aph_pixel[i] + adg_pixel[i];
            boolean isOob = isOutOfBounds(a, aTotalLower, aTotalUpper);
            if (isOob) {
                targetSamples[FLAG_BAND_INDEX].set(FLAG_INDEX_A_TOTAL_OOB, true);
            }
            targetSamples[QaaConstants.A_TOTAL_BAND_INDEXES[i]].set(isOob ? NO_DATA_VALUE : a);
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

    private boolean isOutOfBounds(float value, float lowerBound, float upperBound) {
        return value < lowerBound || value > upperBound;
    }

    private void handleInvalid(WritableSample[] targetSamples, int flagIndex) {
        targetSamples[FLAG_BAND_INDEX].set(flagIndex, true);
        for (int i = 0; i < targetSamples.length - 1; i++) {
            targetSamples[i].set(NO_DATA_VALUE);
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
        Band band = configurer.addBand(bandName, ProductData.TYPE_FLOAT32, NO_DATA_VALUE);
        band.setDescription(String.format(descriptionPattern, wavelength));
        band.setUnit("m^-1");
        band.setSpectralWavelength(wavelength);
        band.setNoDataValueUsed(true);
        return band;
    }

    public static class Spi extends OperatorSpi {

        private static final Map<String[], String> DEPRECATED_PARAMETERS = new HashMap<String[], String>();

        static {
            DEPRECATED_PARAMETERS.put(new String[]{"a_lower", "aLowerBound"}, "aTotalLower");
            DEPRECATED_PARAMETERS.put(new String[]{"a_upper", "aUpperBound"}, "aTotalUpper");
            DEPRECATED_PARAMETERS.put(new String[]{"bb_lower", "bbLowerBound"}, "bbSpmLower");
            DEPRECATED_PARAMETERS.put(new String[]{"bb_upper", "bbUpperBound"}, "bbSpmUpper");
            DEPRECATED_PARAMETERS.put(new String[]{"aph_lower", "aphLowerBound"}, "aPigLower");
            DEPRECATED_PARAMETERS.put(new String[]{"aph_upper", "aphUpperBound"}, "aPigUpper");
            DEPRECATED_PARAMETERS.put(new String[]{"adg_upper", "adgUpperBound"}, "aYsUpper");
        }

        private Logger logger;

        public Spi() {
            super(QaaOp.class);
            logger = BeamLogManager.getSystemLogger();
        }

        @Override
        public Operator createOperator(Map<String, Object> parameters, Map<String, Product> sourceProducts) throws
                                                                                                            OperatorException {
            if (isDeprecatedParameterUsed(parameters)) {
                logWarning();
                mapParameterValuesToNewParameter(parameters);
            }
            return super.createOperator(parameters, sourceProducts);
        }

        private boolean isDeprecatedParameterUsed(Map<String, Object> parameters) {
            Set<String[]> deprecatedParamSet = DEPRECATED_PARAMETERS.keySet();
            for (String[] deprecatedParameterNames : deprecatedParamSet) {
                for (String deprecatedParameterName : deprecatedParameterNames) {
                    if (parameters.containsKey(deprecatedParameterName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void mapParameterValuesToNewParameter(Map<String, Object> parameters) {
            Map<String, Object> parameterCopy = new HashMap<String, Object>(parameters);
            Map<String, Object> parameterIterator = new HashMap<String, Object>(parameters);
            Set<Map.Entry<String[], String>> deprecatedParameterEntrySet = DEPRECATED_PARAMETERS.entrySet();
            for (Map.Entry<String, Object> usedParameter : parameterCopy.entrySet()) {
                String usedParameterName = usedParameter.getKey();
                Object usedParameterValue = usedParameter.getValue();
                for (Map.Entry<String[], String> deprecatedParameterEntry : deprecatedParameterEntrySet) {
                    String[] deprecatedParameterNames = deprecatedParameterEntry.getKey();
                    if (ArrayUtils.isMemberOf(usedParameterName, deprecatedParameterNames)) {
                        mapParameterValue(parameters, usedParameterName, usedParameterValue, deprecatedParameterEntry);
                    }
                }
            }
        }

        private void mapParameterValue(Map<String, Object> parameters, String usedParameterName,
                                       Object usedParameterValue,
                                       Map.Entry<String[], String> deprecatedParameterEntry) {
            parameters.remove(usedParameterName);
            String newParameterName = deprecatedParameterEntry.getValue();
            parameters.put(newParameterName, usedParameterValue);
            logger.log(Level.INFO, String.format("Mapping value [%s] from '%s' to '%s'",
                                                 usedParameterValue, usedParameterName,
                                                 newParameterName));
        }

        private void logWarning() {
            Set<String[]> deprecatedParamSet = DEPRECATED_PARAMETERS.keySet();
            StringBuilder sb = new StringBuilder();
            for (String[] names : deprecatedParamSet) {
                sb.append(StringUtils.arrayToString(names, ", "));
                sb.append("\n");
            }
            String deprecatedParamsList = sb.toString();
            logger.log(Level.WARNING, "Deprecated parameter names are used.\n" +
                                      "Deprecated names are:\n" +
                                      deprecatedParamsList +
                                      "The given parameter values are mapped to the new parameters.\n" +
                                      "Please update your parameter configuration."
            );
        }
    }
}
