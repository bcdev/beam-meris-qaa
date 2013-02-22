package org.esa.beam.meris.qaa;

public class QaaConfig {

    private boolean divideByPi;
    private float aTotalLower;
    private float aTotalUpper;

    public QaaConfig() {
        divideByPi = true;
        aTotalLower = QaaConstants.A_TOTAL_LOWER;
        aTotalUpper = QaaConstants.A_TOTAL_UPPER;
    }

    public boolean isDivideByPi() {
        return divideByPi;
    }

    public void setDivideByPi(boolean divideByPi) {
        this.divideByPi = divideByPi;
    }

    public float getATotalLower() {
        return aTotalLower;
    }

    public void setATotalLower(float aTotalLower) {
        this.aTotalLower = aTotalLower;
    }

    public float getATotalUpper() {
        return aTotalUpper;
    }

    public void setATotalUpper(float aTotalUpper) {
        this.aTotalUpper = aTotalUpper;
    }
}
