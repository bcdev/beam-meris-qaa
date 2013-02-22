package org.esa.beam.meris.qaa;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class QaaConfigTest {

    private QaaConfig config;

    @Before
    public void setUp() {
        config = new QaaConfig();
    }

    @Test
    public void testDefaultConstruction() {
        assertTrue(config.isDivideByPi());

        assertEquals(-0.02f, config.getATotalLower());
        assertEquals(5.0f, config.getATotalUpper());
    }

    @Test
    public void testSetIsDivideByPi() {
        config.setDivideByPi(false);
        assertFalse(config.isDivideByPi());

        config.setDivideByPi(true);
        assertTrue(config.isDivideByPi());
    }

    @Test
    public void testSetGetATotalLower() {
        final float a_total_lower_1 = 2.5f;
        final float a_total_lower_2 = 0.334f;

        config.setATotalLower(a_total_lower_1);
        assertEquals(a_total_lower_1, config.getATotalLower(), 1e-8);

        config.setATotalLower(a_total_lower_2);
        assertEquals(a_total_lower_2, config.getATotalLower(), 1e-8);
    }

    @Test
    public void testSetGetATotalUpper() {
        final float a_total_upper_1 = 2.5f;
        final float a_total_upper_2 = 0.334f;

        config.setATotalUpper(a_total_upper_1);
        assertEquals(a_total_upper_1, config.getATotalUpper(), 1e-8);

        config.setATotalUpper(a_total_upper_2);
        assertEquals(a_total_upper_2, config.getATotalUpper(), 1e-8);
    }

}
