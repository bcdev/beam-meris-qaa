package org.esa.beam.visat.processor.quasi;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.logging.Logger;

public class AnalyticalProcessor extends Processor {

    public static final String PROCESSOR_NAME = "QAA for IOP Processor";
    private static final String PROCESSOR_VERSION = "1.0.2";
    private static final String PROCESSOR_COPYRIGHT = "Copyright (C) 2009 by NRL and WSU";

    public static final String REQUEST_TYPE = "QAA_v5";

    private Product l2Product;
    private Product analyticalProduct;

    private Logger _logger;

    private AnalyticalPN analyticalNode;
    private Band[] analyticalNodeBands;
    private AnalyticalFrameSizeCalculator frameSizeCalculator;

    public static final String HELP_ID = "QAATool";

    public AnalyticalProcessor() {
        _logger = Logger.getLogger(AnalyticalConstants.LOGGER_NAME);
        setDefaultHelpId(AnalyticalProcessor.HELP_ID);
    }

    @Override
    public void initProcessor() throws ProcessorException {
        super.initProcessor();
    }

    @Override
    public ProcessorUI createUI() throws ProcessorException {
        return new AnalyticalProcessorUI();
    }

    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        ProcessorUtils.setProcessorLoggingHandler(
                AnalyticalConstants.DEFAULT_LOG_PREFIX, getRequest(),
                getName(), getVersion(), getCopyrightInformation());
        try {
            _logger.info(AnalyticalConstants.LOG_MSG_START_REQUEST);

            // check the request type
            Request.checkRequestType(getRequest(),
                                     AnalyticalProcessor.REQUEST_TYPE);
            analyticalNode = new AnalyticalPN();
            initOutputProduct();
            prepareProcessing();
            processAnalysis(pm);
            _logger.info(AnalyticalConstants.LOG_MSG_SUCCESS);
        } catch (Exception e) {
            _logger.severe(AnalyticalConstants.LOG_MSG_PROC_ERROR);
            _logger.severe(e.getMessage());
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            try {
                if (isAborted()) {
                    // deleteOutputProduct();
                }
            } finally {
                closeProducts();
                _logger.info(AnalyticalConstants.LOG_MSG_FINISHED_REQUEST);
            }
        }
    }

    private void closeProducts() {
        if (analyticalProduct != null) {
            analyticalProduct.dispose();
            analyticalProduct = null;
        }
        if (l2Product != null) {
            l2Product.dispose();
            l2Product = null;
        }
    }

    @Override
    public String getCopyrightInformation() {
        return PROCESSOR_COPYRIGHT;
    }

    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }

    @Override
    public String getVersion() {
        return PROCESSOR_VERSION;
    }

    private void initOutputProduct() throws ProcessorException, IOException {
        final ProductRef outputRef = getRequest().getOutputProductAt(0);
        l2Product = loadInputProduct(0);
        analyticalProduct = analyticalNode.readProductNodes(l2Product, null);
        analyticalNodeBands = analyticalProduct.getBands();

        ProductUtils.copyFlagBands(l2Product, analyticalProduct);
        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(l2Product, analyticalProduct);
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(l2Product, analyticalProduct);
        analyticalProduct.setStartTime(l2Product.getStartTime());
        analyticalProduct.setEndTime(l2Product.getEndTime());
        copyRequestMetaData(analyticalProduct);
        ProcessingNode.initWriter(outputRef, analyticalProduct, _logger);
        _logger.info(AnalyticalConstants.LOG_MSG_OUTPUT_CREATED);
    }

    private void prepareProcessing() throws Exception {
        final int width = l2Product.getSceneRasterWidth();
        final int height = l2Product.getSceneRasterHeight();

        frameSizeCalculator = new AnalyticalFrameSizeCalculator(width, height);
        analyticalNode.setFrameSizeCalculator(frameSizeCalculator);


        Object pi_check_value = getRequest()
                .getParameter(AnalyticalConstants.PI_CHECK_PARAM_NAME).getValue();
        boolean pi_check = true;
        if (pi_check_value instanceof Boolean) {
            pi_check = (Boolean) pi_check_value;
        }
        analyticalNode.setAphCheck(pi_check);


        float a_lower = AnalyticalConstants.A_LOWER_BOUND_DEFAULT;
        float a_upper = AnalyticalConstants.A_UPPER_BOUND_DEFAULT;
        float bb_lower = AnalyticalConstants.BB_LOWER_BOUND_DEFAULT;
        float bb_upper = AnalyticalConstants.BB_UPPER_BOUND_DEFAULT;
        float aph_lower = AnalyticalConstants.APH_LOWER_BOUND_DEFAULT;
        float aph_upper = AnalyticalConstants.APH_UPPER_BOUND_DEFAULT;
        float adg_upper = AnalyticalConstants.ADG_UPPER_BOUND_DEFAULT;
        Object numObj = getRequest().getParameter(
                AnalyticalConstants.A_LOWER_BOUND).getValue();
        if (numObj instanceof Float) {
            a_lower = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.A_LOWER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            a_lower = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.A_UPPER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            a_upper = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.BB_LOWER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            bb_lower = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.BB_UPPER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            bb_upper = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.APH_LOWER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            aph_lower = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.APH_UPPER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            aph_upper = (Float) numObj;
        }
        numObj = getRequest().getParameter(AnalyticalConstants.ADG_UPPER_BOUND)
                .getValue();
        if (numObj instanceof Float) {
            adg_upper = (Float) numObj;
        }
        analyticalNode.startProcessing(a_lower, a_upper, bb_lower, bb_upper,
                                       aph_lower, aph_upper, adg_upper);
    }

    private void processAnalysis(ProgressMonitor pm) throws IOException {
        final int frameCount = frameSizeCalculator.getFrameCount();

        pm.beginTask("Generating Analytical product...", frameCount);
        try {
            for (int frameNumber = 0; frameNumber < frameCount; frameNumber++) {
                // logger.println("frame "+(frameNumber+1)+"/"+frameCount);
                final Rectangle frameRect = frameSizeCalculator
                        .getFrameRect(frameNumber);
                _logger.info("Processing Analytical frame: "
                             + (frameNumber + 1) + "/" + frameCount);
                ProcessingNode.copyBandData(analyticalNodeBands,
                                            analyticalProduct, frameRect, SubProgressMonitor
                                .create(pm, 1));
                ProcessingNode.copyBandData(l2Product
                        .getBand(AnalyticalConstants.DEFAULT_FLAG_BAND_NAME),
                                            analyticalProduct, frameRect, SubProgressMonitor
                                .create(pm, 1));
                if (pm.isCanceled()) {
                    // Processing terminated
                    setCurrentStatus(AnalyticalConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }
    }
}