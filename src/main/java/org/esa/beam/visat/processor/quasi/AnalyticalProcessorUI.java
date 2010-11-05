package org.esa.beam.visat.processor.quasi;

import java.awt.GridBagConstraints;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

public class AnalyticalProcessorUI extends AbstractProcessorUI {

	private JTabbedPane _tabbedPane;
	private ParamGroup _paramGroup;
	private File _requestFile;
	private AnalyticalRequestElementFactory _factory;
	private final static String[] VALID_INPUT_TYPES = new String[] {
			EnvisatConstants.MERIS_FR_L2_PRODUCT_TYPE_NAME,
			EnvisatConstants.MERIS_RR_L2_PRODUCT_TYPE_NAME };
	private final Logger _logger;

	/**
	 * Creates the UI class with default parameters.
	 */
	public AnalyticalProcessorUI() {
		_factory = AnalyticalRequestElementFactory.getInstance();
		_logger = Logger.getLogger(AnalyticalConstants.LOGGER_NAME);
	}

	/**
	 * Retrieves the base components for the processor specific UI classes.
	 * Creates the UI from scratch if not present.
	 */
	public JComponent getGuiComponent() {
		if (_tabbedPane == null) {
			createUI();
		}
		return _tabbedPane;
	}

	/**
	 * Retrieves the requests currently edited.
	 *
	 * @throws ProcessorException
	 *             if outputProductParam is empty string
	 */
	public Vector<Request> getRequests() throws ProcessorException {
		final Vector<Request> requests = new Vector<Request>();
		final Parameter outputProductParam = _paramGroup
				.getParameter(AnalyticalConstants.OUTPUT_PRODUCT_PARAM_NAME);
		if (hasParameterEmptyString(outputProductParam)) {
			throw new ProcessorException("No output product specified.");
		}
		requests.add(createRequest());
		return requests;
	}

	/**
	 * Sets a new request list to be edited.
	 *
	 * @param requests
	 *            the request list to be edited must not be null
	 */
	public void setRequests(final Vector requests) throws ProcessorException {
		Guardian.assertNotNull("requests", requests);
		if (requests.size() > 0) {
			final Request request = (Request) requests.elementAt(0);
			_requestFile = request.getFile();
			updateParamInputFile(request);
			updateParamOutputFile(request);
			updateParamOutputFormat(request);
			updateLogParameter(request);
		} else {
			setDefaultRequests();
		}
	}

	/**
	 * Create a new defualt request for the processor and sets it to the UI.
	 */
	public void setDefaultRequests() throws ProcessorException {
		final Vector<Request> requests = new Vector<Request>();
		requests.add(createDefaultRequest());
		setRequests(requests);
	}

	/**
	 * Sets the processor app for the UI.
	 */
	@Override
	public void setApp(final ProcessorApp app) {
		super.setApp(app);
		if (_paramGroup != null) {
			app.markIODirChanges(_paramGroup);
		}
	}

	/**
	 * Creates all of the UI components.
	 */
	private void createUI() {
		initParamGroup();
		_tabbedPane = new JTabbedPane();
		_tabbedPane.add("I/O Parameters", createIOTab());
		_tabbedPane.add("Bounding Settings", createBoundingSettingsTab());
	}

	/**
	 * Initializes the parameter group to hold all the parameters needed for the
	 * processor.
	 */
	private void initParamGroup() {
		_paramGroup = new ParamGroup();
		final Parameter inputProductParameter = _factory
				.createDefaultInputProductParameter();
		_paramGroup.addParameter(inputProductParameter);
		final Parameter outputProductParameter = _factory
				.createDefaultOutputProductParameter();
		_paramGroup.addParameter(outputProductParameter);
		_paramGroup.addParameter(_factory.createOutputFormatParameter());

		try {
			_paramGroup.addParameter(_factory
					.createPiCheckFormatParameter("true"));
		} catch ( ParamValidateException e) {
			_logger.warning("Unable to validate the parameter '"
					+ AnalyticalConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
			Debug.trace(e);
		}

		_paramGroup
		.addParameter(_factory
				.createDefaultLogPatternParameter(AnalyticalConstants.DEFAULT_LOG_PREFIX));
		try {
			_paramGroup.addParameter(_factory
					.createLogToOutputParameter("false"));
		} catch (ParamValidateException e) {
			_logger.warning("Unable to validate the parameter '"
					+ AnalyticalConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
			Debug.trace(e);
		}

		// settings panel
		final Parameter aLowerBoundParameter = _factory.createDefaultFloatBox(
				AnalyticalConstants.A_LOWER_BOUND,
				AnalyticalConstants.A_LOWER_BOUND_LABEL,
				AnalyticalConstants.A_LOWER_BOUND_DEFAULT);
		_paramGroup.addParameter(aLowerBoundParameter);

		final Parameter aUpperBoundParameter = _factory.createDefaultFloatBox(
				AnalyticalConstants.A_UPPER_BOUND,
				AnalyticalConstants.A_UPPER_BOUND_LABEL,
				AnalyticalConstants.A_UPPER_BOUND_DEFAULT);
		_paramGroup.addParameter(aUpperBoundParameter);

		final Parameter bbLowerBoundParameter = _factory.createDefaultFloatBox(
				AnalyticalConstants.BB_LOWER_BOUND,
				AnalyticalConstants.BB_LOWER_BOUND_LABEL,
				AnalyticalConstants.BB_LOWER_BOUND_DEFAULT);
		_paramGroup.addParameter(bbLowerBoundParameter);

		final Parameter bbUpperBoundParameter = _factory.createDefaultFloatBox(
				AnalyticalConstants.BB_UPPER_BOUND,
				AnalyticalConstants.BB_UPPER_BOUND_LABEL,
				AnalyticalConstants.BB_UPPER_BOUND_DEFAULT);
		_paramGroup.addParameter(bbUpperBoundParameter);

		final Parameter aphLowerBoundParameter = _factory
				.createDefaultFloatBox(AnalyticalConstants.APH_LOWER_BOUND,
						AnalyticalConstants.APH_LOWER_BOUND_LABEL,
						AnalyticalConstants.APH_LOWER_BOUND_DEFAULT);
		_paramGroup.addParameter(aphLowerBoundParameter);

		final Parameter aphUpperBoundParameter = _factory
				.createDefaultFloatBox(AnalyticalConstants.APH_UPPER_BOUND,
						AnalyticalConstants.APH_UPPER_BOUND_LABEL,
						AnalyticalConstants.APH_UPPER_BOUND_DEFAULT);
		_paramGroup.addParameter(aphUpperBoundParameter);

		final Parameter adgUpperBoundParameter = _factory
				.createDefaultFloatBox(AnalyticalConstants.ADG_UPPER_BOUND,
						AnalyticalConstants.ADG_UPPER_BOUND_LABEL,
						AnalyticalConstants.ADG_UPPER_BOUND_DEFAULT);
		_paramGroup.addParameter(adgUpperBoundParameter);

		// change listeners
		inputProductParameter.addParamChangeListener(new ParamChangeListener() {
			public void parameterValueChanged(final ParamChangeEvent event) {
				checkForValidInputProduct(inputProductParameter);
			}
		});
		outputProductParameter
				.addParamChangeListener(new ParamChangeListener() {
					public void parameterValueChanged(
							final ParamChangeEvent event) {
						if (hasParameterEmptyString(outputProductParameter)) {
							getApp().showWarningDialog(
									"No output product specified.");
						}
					}
				});
	}

	private JPanel createBoundingSettingsTab() {
		final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
		final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
		gbc.gridy = 0;
		Parameter param;

		// A lower bound
		param = _paramGroup.getParameter(AnalyticalConstants.A_LOWER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// A upper bound
		param = _paramGroup.getParameter(AnalyticalConstants.A_UPPER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// BB lower bound
		param = _paramGroup.getParameter(AnalyticalConstants.BB_LOWER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// BB upper bound
		param = _paramGroup.getParameter(AnalyticalConstants.BB_UPPER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// Aph lower bound
		param = _paramGroup.getParameter(AnalyticalConstants.APH_LOWER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// Aph upper bound
		param = _paramGroup.getParameter(AnalyticalConstants.APH_UPPER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// ADG upper bound
		param = _paramGroup.getParameter(AnalyticalConstants.ADG_UPPER_BOUND);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, weightx=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);
		return panel;
	}

	private JPanel createIOTab() {
		final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
		final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
		gbc.gridy = 0;
		Parameter param;

		// input product
		param = _paramGroup
				.getParameter(AnalyticalConstants.INPUT_PRODUCT_PARAM_NAME);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"anchor=NORTHWEST, fill=HORIZONTAL, weightx=1, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// output product
		param = _paramGroup
				.getParameter(AnalyticalConstants.OUTPUT_PRODUCT_PARAM_NAME);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"insets.top=7, anchor=SOUTHWEST, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"insets.top=0, anchor=NORTHWEST, fill=HORIZONTAL, weighty=1");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// output format
		param = _paramGroup
				.getParameter(AnalyticalConstants.OUTPUT_FORMAT_PARAM_NAME);
		gbc.gridy++;
		GridBagUtils
				.setAttributes(gbc,
						"insets.top=7, anchor=SOUTHWEST, fill=NONE, weightx=0, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"insets.top=0, anchor=NORTHWEST, fill=NONE, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		// aph check
		param = _paramGroup
				.getParameter(AnalyticalConstants.PI_CHECK_PARAM_NAME);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"insets.bottom=0, anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);


		/*
		// log information
		param = _paramGroup
				.getParameter(AnalyticalConstants.LOG_PREFIX_PARAM_NAME);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"insets.top=7, anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(),
				gbc);
		gbc.gridy++;
		GridBagUtils.setAttributes(gbc,
				"insets.top=0, anchor=NORTHWEST, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

		param = _paramGroup
				.getParameter(AnalyticalConstants.LOG_TO_OUTPUT_PARAM_NAME);
		gbc.gridy++;
		GridBagUtils
				.setAttributes(gbc,
						"insets.bottom=0, anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5");
		GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);
		*/

		return panel;
	}

	/**
	 * Checks to see if a parameter is empty string. If so it returns true, else
	 * false.
	 *
	 * @param parameter
	 *            parameter to test for empty string
	 *
	 * @return true if parameter is empty string, else false
	 */
	private static boolean hasParameterEmptyString(final Parameter parameter) {
		final String valueAsText = parameter.getValueAsText();
		if (valueAsText.trim().length() <= 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a request with all the parameters set to their respective
	 * defaults.
	 *
	 * @return the default request
	 */
	private Request createDefaultRequest() {
		return createRequest();
	}

	/**
	 * Creates a request.
	 *
	 * @return the request
	 */
	private Request createRequest() {
		final Request request = new Request();
		request.setType(AnalyticalProcessor.REQUEST_TYPE);
		request.setFile(_requestFile);
		request.addInputProduct(createInputProductRef());
		request.addOutputProduct(createOutputProductRef());
		request.addParameter(createOutputFormatParamForRequest());
		request.addParameter(_paramGroup
				.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME));
		request.addParameter(_paramGroup
				.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.A_LOWER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.A_UPPER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.BB_LOWER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.BB_UPPER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.APH_LOWER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.APH_UPPER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.ADG_UPPER_BOUND));
		request.addParameter(_paramGroup
				.getParameter(AnalyticalConstants.PI_CHECK_PARAM_NAME));
		return request;
	}

	/**
	 * Creates an input product reference from the request.
	 *
	 * @return the product reference
	 */
	private ProductRef createInputProductRef() {
		final String filePath = _paramGroup.getParameter(
				AnalyticalConstants.INPUT_PRODUCT_PARAM_NAME).getValueAsText();
		return new ProductRef(new File(filePath), null, null);
	}

	/**
	 * Creates an output product reference from the request.
	 *
	 * @return the product reference
	 */
	private ProductRef createOutputProductRef() {
		final String fileName = _paramGroup.getParameter(
				AnalyticalConstants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();
		final String fileFormat = _paramGroup.getParameter(
				AnalyticalConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
		return ProcessorUtils.createProductRef(fileName, fileFormat);
	}

	private Parameter createOutputFormatParamForRequest() {
		final String outputFormat = _paramGroup.getParameter(
				AnalyticalConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
		return new Parameter(AnalyticalConstants.OUTPUT_FORMAT_PARAM_NAME,
				outputFormat);
	}

	/**
	 * Update output format parameter.
	 *
	 * @param request
	 *            request to update
	 */
	private void updateParamOutputFormat(final Request request) {
		final String format = request.getParameter(
				AnalyticalConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
		_paramGroup.getParameter(AnalyticalConstants.OUTPUT_FORMAT_PARAM_NAME)
				.setValue(format, null);
	}

	/**
	 * Update output file parameter.
	 *
	 * @param request
	 *            request to update
	 */
	private void updateParamOutputFile(final Request request) {
		final File file = new File(request.getOutputProductAt(0).getFilePath());
		_paramGroup.getParameter(AnalyticalConstants.OUTPUT_PRODUCT_PARAM_NAME)
				.setValue(file, null);
	}

	/**
	 * Update input file parameter.
	 *
	 * @param request
	 *            request to update
	 */
	private void updateParamInputFile(final Request request) {
		final File file = new File(request.getInputProductAt(0).getFilePath());
		_paramGroup.getParameter(AnalyticalConstants.INPUT_PRODUCT_PARAM_NAME)
				.setValue(file, null);
	}

	/**
	 * Update log parameter.
	 *
	 * @param request
	 *            request to update
	 */
	private void updateLogParameter(final Request request) {
		Parameter param;
		Parameter toUpdate;

		param = request.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
		if (param != null) {
			toUpdate = _paramGroup
					.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
			toUpdate.setValue(param.getValue(), null);
		}

		param = request
				.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
		if (param != null) {
			toUpdate = _paramGroup
					.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
			toUpdate.setValue(param.getValue(), null);
		}
	}

	private void checkForValidInputProduct(final Parameter parameter) {
		final Object value = parameter.getValue();
		File file = null;
		if (value instanceof File) {
			file = (File) value;
		}
		if (value instanceof String) {
			file = new File((String) value);
		}
		if (file == null || !file.exists()) {
			return;
		}
		String msg = null;
		Product product = null;
		try {
			product = ProductIO.readProduct(file);
			if (product != null) {
				final String productType = product.getProductType();
				final boolean isValidType = StringUtils.contains(
						VALID_INPUT_TYPES, productType);
				if (!isValidType) {
					msg = "The specified input product is not of the expected type.\n"
							+ "The type of the product must be 'MERIS_L2'.";
				} else {
					final String[] reflectanceBandNames = {
							EnvisatConstants.MERIS_L2_REFLEC_1_BAND_NAME,
							EnvisatConstants.MERIS_L2_REFLEC_2_BAND_NAME,
							EnvisatConstants.MERIS_L2_REFLEC_3_BAND_NAME,
							EnvisatConstants.MERIS_L2_REFLEC_4_BAND_NAME,
							EnvisatConstants.MERIS_L2_REFLEC_5_BAND_NAME,
							EnvisatConstants.MERIS_L2_REFLEC_6_BAND_NAME,
							EnvisatConstants.MERIS_L2_REFLEC_7_BAND_NAME,
							AnalyticalConstants.DEFAULT_FLAG_BAND_NAME };
					final Band[] reflectanceBands = new Band[reflectanceBandNames.length];
					for (int bandIndex = 0; bandIndex < reflectanceBandNames.length; bandIndex++) {
						final String reflectanceBandName = reflectanceBandNames[bandIndex];
						reflectanceBands[bandIndex] = product
								.getBand(reflectanceBandName);
						if (reflectanceBands[bandIndex] == null) {
							msg = "Source product does not contain band "
									+ reflectanceBandName + ".";
							break;
						}
					}
				}
			} else {
				msg = "Uknown file format.";
			}
		} catch (IOException e) {
			msg = e.getMessage();
		}
		if (msg != null) {
			getApp().showWarningDialog("Invalid input file:\n" + msg);
		}
	}
}
