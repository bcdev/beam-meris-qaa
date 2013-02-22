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
}
