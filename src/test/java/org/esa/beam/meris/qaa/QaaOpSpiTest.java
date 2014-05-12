package org.esa.beam.meris.qaa;

import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class QaaOpSpiTest {

    @Test
    public void testCreateOperatorWithDeprecatedParameters() throws Exception {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("a_lower", 0.3f);
        parameters.put("bbUpperBound", 4.6f);
        parameters.put("aPigLower", -0.9f);

        parameters.put("unchangedUnknownParameter", "John Doe");

        Operator operator = new QaaOp.Spi().createOperator(parameters, GPF.NO_SOURCES);

        assertEquals(4, parameters.size());
        assertNull(operator.getParameter("a_lower"));
        assertEquals(0.3f, operator.getParameter("aTotalLower"));
        assertNull(operator.getParameter("bbUpperBound"));
        assertEquals(4.6f, operator.getParameter("bbSpmUpper"));
        assertEquals(-0.9f, operator.getParameter("aPigLower"));

        // Ok for BEAM 4.11! But why and for what?!? (asks nf)
        // assertEquals("John Doe", operator.getParameter("unchangedUnknownParameter"));
        // BEAM 5:
        assertNull(operator.getParameter("unchangedUnknownParameter"));
    }
}
