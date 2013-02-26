package org.esa.beam.meris.qaa;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class QaaResultTest {

    private QaaResult result;

    @Before
    public void setUp() {
        result = new QaaResult();
    }

    @Test
    public void testConstruction() {
        final float[] a_total = result.getA_Total();
        assertNotNull(a_total);
        assertEquals(QaaConstants.NUM_A_TOTAL_BANDS, a_total.length);

        final float[] bb_spm = result.getBB_SPM();
        assertNotNull(bb_spm);
        assertEquals(QaaConstants.NUM_BB_SPM_BANDS, bb_spm.length);

        final float[] a_pig = result.getA_PIG();
        assertNotNull(a_pig);
        assertEquals(QaaConstants.NUM_A_PIG_BANDS, a_pig.length);

        final float[] a_ys = result.getA_YS();
        assertNotNull(a_ys);
        assertEquals(QaaConstants.NUM_A_YS_BANDS, a_ys.length);

        assertEquals(1, result.getFlags());
    }

    @Test
    public void testSetGet_A_Total() {
        final float a_total = 0.45f;

        result.setA_Total(a_total, 3);
        final float[] actual = result.getA_Total();
        assertEquals(a_total, actual[3], 1e-8);
    }

    @Test
    public void testSetGetBB_SPM() {
        final float bb_spm = 0.42f;

        result.setBB_SPM(bb_spm, 4);
        final float[] actual = result.getBB_SPM();
        assertEquals(bb_spm, actual[4], 1e-8);
    }

    @Test
    public void testSetGetA_PIG() {
        final float a_pig = 0.11f;

        result.setA_PIG(a_pig, 0);
        final float[] actual = result.getA_PIG();
        assertEquals(a_pig, actual[0], 1e-8);
    }

    @Test
    public void testSetGetA_YS() {
        final float a_ys = 0.008f;

        result.setA_YS(a_ys, 1);
        final float[] actual = result.getA_YS();
        assertNotNull(actual);
        assertEquals(a_ys, actual[1], 1e-8);
    }

    @Test
    public void testSetValid() {
        result.setValid(true);
        assertEquals(1, result.getFlags());

        result.setValid(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetInvalidValid() {
        result.setValid(false); // remove the valid flag

        result.setInvalid(true);
        assertEquals(8, result.getFlags());

        result.setInvalid(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetATotalOutOfBounds() {
        result.setValid(false); // remove the valid flag

        result.setATotalOutOfBounds(true);
        assertEquals(16, result.getFlags());

        result.setATotalOutOfBounds(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetBbSpmOutOfBounds() {
        result.setValid(false); // remove the valid flag

        result.setBbSpmOutOfBounds(true);
        assertEquals(32, result.getFlags());

        result.setBbSpmOutOfBounds(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetAPigOutOfBounds() {
        result.setValid(false); // remove the valid flag

        result.setAPigOutOfBounds(true);
        assertEquals(64, result.getFlags());

        result.setAPigOutOfBounds(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetAYsOutOfBounds() {
        result.setValid(false); // remove the valid flag

        result.setAYsOutOfBounds(true);
        assertEquals(128, result.getFlags());

        result.setAYsOutOfBounds(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetAYsNegative() {
        result.setValid(false); // remove the valid flag

        result.setAYsNegative(true);
        assertEquals(4, result.getFlags());

        result.setAYsNegative(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testSetImaginary() {
        result.setValid(false); // remove the valid flag

        result.setImaginary(true);
        assertEquals(2, result.getFlags());

        result.setImaginary(false);
        assertEquals(0, result.getFlags());
    }

    @Test
    public void testInvalidate() {
        result.invalidate();

        assertAllMeasurementsSetTo(QaaConstants.NO_DATA_VALUE);

        final int flags = result.getFlags();
        assertEquals(8, flags);
    }

    @Test
    public void testInvalidateImaginary() {
        result.invalidateImaginary();

        assertAllMeasurementsSetTo(QaaConstants.NO_DATA_VALUE);

        final int flags = result.getFlags();
        assertEquals(2, flags);
    }

    @Test
    public void testReset() {
        result.setATotalOutOfBounds(true);
        result.setBbSpmOutOfBounds(true);
        for (int i = 0; i < 3; i++) {
            result.setA_YS(i + 1, i);
        }
        for (int i = 0; i < 3; i++) {
            result.setA_PIG(i + 3, i);
        }
        for (int i = 0; i < 5; i++) {
            result.setA_Total(i + 7, i);
        }
        for (int i = 0; i < 5; i++) {
            result.setBB_SPM(i + 12, i);
        }

        result.reset();
        assertEquals(1, result.getFlags());

        assertAllMeasurementsSetTo(0.f);
    }

    private void assertAllMeasurementsSetTo(float expected) {
        final float[] a_pig = result.getA_PIG();
        assertEquals(QaaConstants.NUM_A_PIG_BANDS, a_pig.length);
        for (float anA_pig : a_pig) {
            assertEquals(expected, anA_pig, 1e-8);
        }

        final float[] a_total = result.getA_Total();
        assertEquals(QaaConstants.NUM_A_TOTAL_BANDS, a_total.length);
        for (float anA_total : a_total) {
            assertEquals(expected, anA_total, 1e-8);
        }

        final float[] a_ys = result.getA_YS();
        assertEquals(QaaConstants.NUM_A_YS_BANDS, a_ys.length);
        for (float a_y : a_ys) {
            assertEquals(expected, a_y, 1e-8);
        }

        final float[] bb_spm = result.getBB_SPM();
        assertEquals(QaaConstants.NUM_BB_SPM_BANDS, bb_spm.length);
        for (float aBb_spm : bb_spm) {
            assertEquals(expected, aBb_spm, 1e-8);
        }
    }
}
