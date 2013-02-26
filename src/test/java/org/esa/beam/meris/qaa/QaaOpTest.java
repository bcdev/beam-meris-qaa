package org.esa.beam.meris.qaa;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class QaaOpTest {

    @Test
    public void testWriteResult() {
        final QaaResult result = new QaaResult();
        result.setA_Total(1, 0);
        result.setA_Total(2, 1);
        result.setA_Total(3, 2);
        result.setA_Total(4, 3);
        result.setA_Total(5, 4);
        result.setBB_SPM(6, 0);
        result.setBB_SPM(7, 1);
        result.setBB_SPM(8, 2);
        result.setBB_SPM(9, 3);
        result.setBB_SPM(10, 4);
        result.setA_PIG(11, 0);
        result.setA_PIG(12, 1);
        result.setA_PIG(13, 2);
        result.setA_YS(14, 0);
        result.setA_YS(15, 1);
        result.setA_YS(16, 2);
        result.setAPigOutOfBounds(true);

        final WritableSample[] writableSamples = new WritableSample[17];
        for (int i = 0; i < writableSamples.length; i++) {
            writableSamples[i] = new TestWritableSample();
        }

        QaaOp.writeResult(writableSamples, result);

        for (int i = 0; i < writableSamples.length - 1; i++) {
            assertEquals(i + 1, writableSamples[i].getFloat(), 1e-8);
        }
        assertEquals(65, writableSamples[16].getInt());
    }

    private class TestWritableSample implements WritableSample {
        private float floatVal;
        private int intVal;

        public RasterDataNode getNode() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void set(int i, boolean b) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void set(boolean b) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void set(int i) {
            intVal = i;
        }

        public void set(float v) {
            this.floatVal = v;
        }

        public void set(double v) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getIndex() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getDataType() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean getBit(int i) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean getBoolean() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getInt() {
            return intVal;
        }

        public float getFloat() {
            return floatVal;
        }

        public double getDouble() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
