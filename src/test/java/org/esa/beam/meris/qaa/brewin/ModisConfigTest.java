package org.esa.beam.meris.qaa.brewin;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModisConfigTest {

    private ModisConfig config;

    @Before
    public void setUp() {
        config = new ModisConfig();
    }

    @Test
    public void testGetAWCoefficients() {
        final double[] aw_coeffs = config.getAwCoefficients();
        assertEquals(3, aw_coeffs.length);
        assertEquals(-1.146, aw_coeffs[0], 1e-8);
        assertEquals(-1.366, aw_coeffs[1], 1e-8);
        assertEquals(-0.469, aw_coeffs[2], 1e-8);
    }

    @Test
    public void testGetReferenceWavelength() {
        assertEquals(547.0, config.getReferenceWavelength(), 1e-8);
    }

    @Test
    public void testGetWavelengths() {
        final double[] wavelengths = config.getWavelengths();
        assertEquals(6, wavelengths.length);
        assertEquals(412.0, wavelengths[0], 1e-8);
        assertEquals(443.0, wavelengths[1], 1e-8);
        assertEquals(488.0, wavelengths[2], 1e-8);
        assertEquals(531.0, wavelengths[3], 1e-8);
        assertEquals(547.0, wavelengths[4], 1e-8);
        assertEquals(667.0, wavelengths[5], 1e-8);
    }

    @Test
    public void testGetSpecificAbsorptions() {
        final double[] aw = config.getSpecificAbsorptions();
        assertEquals(6, aw.length);
        assertEquals(0.00455056, aw[0], 1e-8);
        assertEquals(0.00706914, aw[1], 1e-8);
        assertEquals(0.0145167, aw[2], 1e-8);
        assertEquals(0.0439153, aw[3], 1e-8);
        assertEquals(0.0531686, aw[4], 1e-8);
        assertEquals(0.434888, aw[5], 1e-8);
    }

    @Test
    public void testGetSpecificBackscatters() {
        final double[] bbw = config.getSpecficBackscatters();
        assertEquals(6, bbw.length);
        assertEquals(0.00579201, bbw[0], 1e-8);
        assertEquals(0.00424592, bbw[1], 1e-8);
        assertEquals(0.00281659, bbw[2], 1e-8);
        assertEquals(0.00197385, bbw[3], 1e-8);
        assertEquals(0.00174280, bbw[4], 1e-8);
        assertEquals(0.000762543, bbw[5], 1e-8);
    }
}
