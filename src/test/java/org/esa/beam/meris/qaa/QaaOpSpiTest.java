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
        QaaOp.Spi spi = new QaaOp.Spi();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("a_lower", 0.3);
        parameters.put("bbUpperBound", 4.6);
        parameters.put("unchangedUnknownParameter", "John Doe");
        parameters.put("aPigLower", -0.9);
        Operator operator = spi.createOperator(parameters, GPF.NO_SOURCES);
        assertEquals(4, parameters.size());
        assertNull(operator.getParameter("a_lower"));
        assertEquals(0.3, operator.getParameter("aTotalLower"));
        assertNull(operator.getParameter("bbUpperBound"));
        assertEquals(4.6, operator.getParameter("bbSpmUpper"));
        assertEquals("John Doe", operator.getParameter("unchangedUnknownParameter"));
        assertEquals(-0.9, operator.getParameter("aPigLower"));

    }
}
