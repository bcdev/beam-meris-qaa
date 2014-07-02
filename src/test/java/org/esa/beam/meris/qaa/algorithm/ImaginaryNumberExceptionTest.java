package org.esa.beam.meris.qaa.algorithm;


import org.esa.beam.meris.qaa.ImaginaryNumberException;
import org.junit.Test;

import static junit.framework.Assert.*;

public class ImaginaryNumberExceptionTest {

    @Test
    public void testConstructAndGetMessage() {
        final ImaginaryNumberException exception = new ImaginaryNumberException("oopsi", -98);

        assertEquals("oopsi: -98.0", exception.getMessage());
    }
}
