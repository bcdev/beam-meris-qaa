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
        assertEquals(-0.02f, config.getBbSpmsLower());
        assertEquals(5.0f, config.getBbSpmsUpper());
        assertEquals(-0.02f, config.getAPigLower());
        assertEquals(3.0f, config.getAPigUpper());
        assertEquals(0.f, config.getAYsLower());
        assertEquals(1.0f, config.getAYsUpper());
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
        final float a_total_upper_1 = 3.6f;
        final float a_total_upper_2 = 0.345f;

        config.setATotalUpper(a_total_upper_1);
        assertEquals(a_total_upper_1, config.getATotalUpper(), 1e-8);

        config.setATotalUpper(a_total_upper_2);
        assertEquals(a_total_upper_2, config.getATotalUpper(), 1e-8);
    }

    @Test
    public void testSetGetBbSpmLower() {
        final float bb_spm_lower_1 = 4.7f;
        final float bb_spm_lower_2 = 0.356f;

        config.setBbSpmsLower(bb_spm_lower_1);
        assertEquals(bb_spm_lower_1, config.getBbSpmsLower(), 1e-8);

        config.setBbSpmsLower(bb_spm_lower_2);
        assertEquals(bb_spm_lower_2, config.getBbSpmsLower(), 1e-8);
    }

    @Test
    public void testSetGetBbSpmUpper() {
        final float bb_spm_upper_1 = 5.8f;
        final float bb_spm_upper_2 = 0.367f;

        config.setBbSpmsUpper(bb_spm_upper_1);
        assertEquals(bb_spm_upper_1, config.getBbSpmsUpper(), 1e-8);

        config.setBbSpmsUpper(bb_spm_upper_2);
        assertEquals(bb_spm_upper_2, config.getBbSpmsUpper(), 1e-8);
    }

    @Test
    public void testSetGetAPigLower() {
        final float a_pig_lower_1 = 6.9f;
        final float a_pig_lower_2 = 0.378f;

        config.setAPigLower(a_pig_lower_1);
        assertEquals(a_pig_lower_1, config.getAPigLower(), 1e-8);

        config.setAPigLower(a_pig_lower_2);
        assertEquals(a_pig_lower_2, config.getAPigLower(), 1e-8);
    }

    @Test
    public void testSetGetAPigUpper() {
        final float a_pig_upper_1 = 7.f;
        final float a_pig_upper_2 = 0.389f;

        config.setAPigUpper(a_pig_upper_1);
        assertEquals(a_pig_upper_1, config.getAPigUpper(), 1e-8);

        config.setAPigUpper(a_pig_upper_2);
        assertEquals(a_pig_upper_2, config.getAPigUpper(), 1e-8);
    }

    @Test
    public void testSetGetAYsLower() {
        final float a_ys_lower_1 = 8.1f;
        final float a_ys_lower_2 = 0.401f;

        config.setAYsLower(a_ys_lower_1);
        assertEquals(a_ys_lower_1, config.getAYsLower(), 1e-8);

        config.setAYsLower(a_ys_lower_2);
        assertEquals(a_ys_lower_2, config.getAYsLower(), 1e-8);
    }

    @Test
    public void testSetGetAYsUpper() {
        final float a_ys_upper_1 = 9.2f;
        final float a_ys_upper_2 = 0.412f;

        config.setAYsUpper(a_ys_upper_1);
        assertEquals(a_ys_upper_1, config.getAYsUpper(), 1e-8);

        config.setAYsUpper(a_ys_upper_2);
        assertEquals(a_ys_upper_2, config.getAYsUpper(), 1e-8);
    }
}
