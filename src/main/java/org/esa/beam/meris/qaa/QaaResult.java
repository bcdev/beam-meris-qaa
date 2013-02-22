package org.esa.beam.meris.qaa;

public class QaaResult {

    private static final int FLAG_INDEX_VALID = 0;

    private float[] A_Total;
    private float[] BB_SPM;
    private float[] A_PIG;
    private float[] A_YS;
    private int flags;

    public QaaResult() {
        A_Total = new float[QaaConstants.NUM_A_TOTAL_BANDS];
        BB_SPM = new float[QaaConstants.NUM_BB_SPM_BANDS];
        A_PIG = new float[QaaConstants.NUM_A_PIG_BANDS];
        A_YS = new float[QaaConstants.NUM_A_YS_BANDS];
    }

    public void setA_Total(float a_total, int bandIndex) {
        A_Total[bandIndex] = a_total;
    }

    public float[] getA_Total() {
        return A_Total;
    }

    public void setBB_SPM(float bb_spm, int bandIndex) {
        BB_SPM[bandIndex] = bb_spm;
    }

    public float[] getBB_SPM() {
        return BB_SPM;
    }

    public void setA_PIG(float a_pig, int bandIndex) {
        A_PIG[bandIndex] = a_pig;
    }

    public float[] getA_PIG() {
        return A_PIG;
    }

    public void setA_YS(float a_ys, int bandIndex) {
        A_YS[bandIndex] = a_ys;
    }

    public float[] getA_YS() {
        return A_YS;
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    public void setValid(boolean valid) {
        flags = valid ? (flags | (1 << FLAG_INDEX_VALID)) : (flags & ~(1 << FLAG_INDEX_VALID));
    }

    public int getFlags() {
        return flags;
    }
}
