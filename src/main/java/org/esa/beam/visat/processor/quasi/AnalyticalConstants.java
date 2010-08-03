// Labels or descriptions the graphical user interface units

package org.esa.beam.visat.processor.quasi;

import org.esa.beam.framework.processor.ProcessorConstants;

public class AnalyticalConstants implements ProcessorConstants {

	public static final String LOGGER_NAME = "quasi.processor.analysis";
    public static final String DEFAULT_LOG_PREFIX = "analysis";
    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "analysis.dim";

    public static final String DEFAULT_FLAG_BAND_NAME = "l2_flags";
    
    public static final String LOG_MSG_OUTPUT_CREATED = "Output product successfully created";
    
    public static final String PI_CHECK_PARAM_NAME = "PI_CHECK_BOOL";
	public static final String PI_CHECK_LABELTEXT = "Select this to divide Rrs by Pi(3.14)";
	public static final String PI_CHECK_DESCRIPTION = "Divide Rrs by Pi(3.14)";

	public static final String A_LOWER_BOUND = "a lower bound";
    public static final String BB_LOWER_BOUND = "bb lower bound";
    public static final String APH_LOWER_BOUND = "aph lower bound";
    public static final String ADG_LOWER_BOUND = "adg lower bound";
    
    public static final String A_UPPER_BOUND = "a upper bound";
    public static final String BB_UPPER_BOUND = "bb upper bound";
    public static final String APH_UPPER_BOUND = "aph upper bound";
    public static final String ADG_UPPER_BOUND = "adg upper bound";
    
	public static final String A_LOWER_BOUND_LABEL = "\"A\" Lower Bound";
	public static final String A_UPPER_BOUND_LABEL = "\"A\" Upper Bound";
	public static final String BB_LOWER_BOUND_LABEL = "\"BB\" Lower Bound";
	public static final String BB_UPPER_BOUND_LABEL = "\"BB\" Upper Bound";
	public static final String APH_LOWER_BOUND_LABEL = "\"APH\" Lower Bound";
	public static final String APH_UPPER_BOUND_LABEL = "\"APH\" Upper Bound";
	public static final String ADG_UPPER_BOUND_LABEL = "\"ADG\" Upper Bound";

	public static final float A_LOWER_BOUND_DEFAULT = -0.02f;
	public static final float A_UPPER_BOUND_DEFAULT = 5.0f;
	public static final float BB_LOWER_BOUND_DEFAULT = -0.02f;
	public static final float BB_UPPER_BOUND_DEFAULT = 5.0f;
	public static final float APH_LOWER_BOUND_DEFAULT = -0.02f;
	public static final float APH_UPPER_BOUND_DEFAULT = 3.0f;
	public static final float ADG_UPPER_BOUND_DEFAULT = 1.0f;
}