package org.esa.beam.meris.qaa.brewin;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MerisConfigTest {

    private MerisConfig config;

    @Before
    public void setUp() {
        config = new MerisConfig();
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
        assertEquals(560.0, config.getReferenceWavelength(), 1e-8);
    }

    @Test
    public void testGetWavelengths() {
        final double[] wavelengths = config.getWavelengths();
        assertEquals(6, wavelengths.length);
        assertEquals(413, wavelengths[0], 1e-8);
        assertEquals(443, wavelengths[1], 1e-8);
        assertEquals(490, wavelengths[2], 1e-8);
        assertEquals(510, wavelengths[3], 1e-8);
        assertEquals(560, wavelengths[4], 1e-8);
        assertEquals(665, wavelengths[5], 1e-8);
    }

    @Test
    public void testGetSpecificAbsorptions() {
        final double[] aw = config.getSpecificAbsorptions();
        assertEquals(6, aw.length);
        assertEquals(0.00449607, aw[0], 1e-8);
        assertEquals(0.00706914, aw[1], 1e-8);
        assertEquals(0.015, aw[2], 1e-8);
        assertEquals(0.0325, aw[3], 1e-8);
        assertEquals(0.0619, aw[4], 1e-8);
        assertEquals(0.429, aw[5], 1e-8);
    }

    @Test
    public void testGetSpecificBackscatters() {
        final double[] bbw = config.getSpecficBackscatters();
        assertEquals(6, bbw.length);
        assertEquals(0.00573196, bbw[0], 1e-8);
        assertEquals(0.00424592, bbw[1], 1e-8);
        assertEquals(0.00276835, bbw[2], 1e-8);
        assertEquals(0.0023387, bbw[3], 1e-8);
        assertEquals(0.00157958, bbw[4], 1e-8);
        assertEquals(0.000772104, bbw[5], 1e-8);
    }
}
