package org.esa.beam.meris.qaa.brewin;

import org.esa.beam.meris.qaa.algorithm.ImaginaryNumberException;
import org.esa.beam.meris.qaa.algorithm.QaaResult;
import org.junit.Test;

public class QaaConfAlgorithmTest {

    @Test
    public void testProcess_Meris() throws ImaginaryNumberException {
        final float[] rrs_in = {0.030262154f, 0.031086152f, 0.022717977f, 0.013177891f, 0.0072450927f, 0.0028870495f, 0.0024475828f};

        final QaaConfAlgorithm algorithm = new QaaConfAlgorithm();

        final QaaResult result = algorithm.process(rrs_in, null);
    }
}
