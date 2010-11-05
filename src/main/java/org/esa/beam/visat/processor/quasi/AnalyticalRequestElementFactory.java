package org.esa.beam.visat.processor.quasi;

import java.io.File;

import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;

public class AnalyticalRequestElementFactory implements RequestElementFactory {

	private static AnalyticalRequestElementFactory _instance;

	private DefaultRequestElementFactory _defFactory = DefaultRequestElementFactory
			.getInstance();

	/**
	 * Returns an instance of AnalysisRequestElementFactory.
	 *
	 * @return _instance AnalysisRequestElementFactory instance
	 */
	public static AnalyticalRequestElementFactory getInstance() {
		if (_instance == null) {
			_instance = new AnalyticalRequestElementFactory();
		}
		return _instance;
	}

	/**
	 * Creates a new processing parameter for the current processing request.
	 *
	 * @param name
	 *            the parameter name, must not be null or empty
	 * @param value
	 *            the parameter value, an be null if not yet known
	 *
	 * @throws IllegalArgumentException
	 *             if name is null or empty
	 * @throws org.esa.beam.framework.processor.RequestElementFactoryException
	 *             if the parameter is not valid or could not be created
	 */
	public Parameter createParameter(final String name, final String value)
			throws RequestElementFactoryException {
		return _defFactory.createParameter(name, value);
	}

	/**
	 * Creates a parameter for the default input product path.
	 */
	public Parameter createDefaultInputProductParameter() {
		final Parameter defaultInputProductParameter = _defFactory
				.createDefaultInputProductParameter();
		final ParamProperties properties = defaultInputProductParameter
				.getProperties();
		final Object defaultValue = properties.getDefaultValue();
		if (defaultValue instanceof File) {
			properties.setDefaultValue((File) defaultValue);
		}
		defaultInputProductParameter.setDefaultValue();
		return defaultInputProductParameter;
		// return _defFactory.createDefaultInputProductParameter();
	}

	/**
	 * Creates a parameter for the default output product path.
	 */
	public Parameter createDefaultOutputProductParameter() {
		final Parameter defaultOutputProductParameter = _defFactory
				.createDefaultOutputProductParameter();
		final ParamProperties properties = defaultOutputProductParameter
				.getProperties();
		final Object defaultValue = properties.getDefaultValue();
		if (defaultValue instanceof File) {
			properties.setDefaultValue(new File((File) defaultValue,
					AnalyticalConstants.DEFAULT_OUTPUT_PRODUCT_NAME));
		}
		defaultOutputProductParameter.setDefaultValue();
		return defaultOutputProductParameter;
		// return _defFactory.createDefaultOutputProductParameter();
	}

	/**
	 * Creates a default logging pattern parameter set to the prefix passed in.
	 *
	 * @param prefix
	 *            the default setting for the logging pattern
	 *
	 * @return a logging pattern parameter conforming to the system settings
	 */
	public Parameter createDefaultLogPatternParameter(String prefix) {
		return _defFactory.createDefaultLogPatternParameter(prefix);
	}

	/**
	 * Creates a logging to output product parameter set to false.
	 *
	 * @return the created logging to output product parameter
	 */
	public Parameter createLogToOutputParameter(final String value)
			throws ParamValidateException {
		final Parameter logToOutputParameter = _defFactory
				.createLogToOutputParameter(value);
		logToOutputParameter.getProperties()
				.setDefaultValue(new Boolean(false));
		return logToOutputParameter;
	}

	/**
	 * Creates output format parameter.
	 */
	public Parameter createOutputFormatParameter() {
		return _defFactory.createOutputFormatParameter();
	}

	/**
	 * Creates aph check parameter.
	 */
	public Parameter createPiCheckFormatParameter(final String value)
									throws ParamValidateException {
		final ParamProperties props = _defFactory.createBooleanParamProperties();
		final Parameter param;

		props.setLabel(AnalyticalConstants.PI_CHECK_LABELTEXT);
		props.setDescription(AnalyticalConstants.PI_CHECK_DESCRIPTION);
		props.setDefaultValue(true);

		param = new Parameter(AnalyticalConstants.PI_CHECK_PARAM_NAME, props);
		try {
			param.setValueAsText(value);
		} catch (ParamParseException e) {
			throw new ParamValidateException(param, e.getMessage());
		}
		return param;
	}

	/**
	 * Creates a new reference to an input product for the current processing
	 * request.
	 *
	 * @param url
	 *            the input product's URL, must not be null
	 * @param fileType
	 *            the file format, can be null if not known
	 * @param typeId
	 *            the product type identifier, can be null if not known
	 *
	 * @throws IllegalArgumentException
	 *             if url is null
	 * @throws org.esa.beam.framework.processor.RequestElementFactoryException
	 *             if the could not be created
	 */
	public ProductRef createInputProductRef(final File url,
			final String fileFormat, final String typeId)
			throws RequestElementFactoryException {
		return _defFactory.createOutputProductRef(url, fileFormat, typeId);
	}

	/**
	 * Creates a new reference to an output product for the current processing
	 * request.
	 *
	 * @param url
	 *            the output product's URL, must not be null
	 * @param fileType
	 *            the file format, can be null if not known
	 * @param typeId
	 *            the product type identifier, can be null if not known
	 *
	 * @throws IllegalArgumentException
	 *             if url is null
	 * @throws org.esa.beam.framework.processor.RequestElementFactoryException
	 *             if the could not be created
	 */
	public ProductRef createOutputProductRef(final File url,
			final String fileFormat, final String typeId)
			throws RequestElementFactoryException {
		return _defFactory.createOutputProductRef(url, fileFormat, typeId);
	}

	public Parameter createDefaultFloatBox(String paramName, String label, float value) {
		ParamProperties paramProps = new ParamProperties(Float.class);
		paramProps.setLabel(label);
//		paramProps.setDescription("bounding value");
		paramProps.setDefaultValue(value);
		final Parameter param = new Parameter(paramName, paramProps);
        param.setDefaultValue();
        return param;
	}
}
