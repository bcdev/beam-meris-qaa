package org.esa.beam.meris.waterclarity.algorithm;

public class QaaConfig {

    private boolean divideByPi;
    private float aTotalLower;
    private float aTotalUpper;
    private float bbSpmsLower;
    private float bbSpmsUpper;
    private float aPigLower;
    private float aPigUpper;
    private float aYsLower;
    private float aYsUpper;

    public QaaConfig() {
        divideByPi = true;
        aTotalLower = QaaConstants.A_TOTAL_LOWER_DEFAULT;
        aTotalUpper = QaaConstants.A_TOTAL_UPPER_DEFAULT;
        bbSpmsLower = QaaConstants.BB_SPM_LOWER_DEFAULT;
        bbSpmsUpper = QaaConstants.BB_SPM_UPPER_DEFAULT;
        aPigLower = QaaConstants.A_PIG_LOWER_DEFAULT;
        aPigUpper = QaaConstants.A_PIG_UPPER_DEFAULT;
        aYsLower = QaaConstants.A_YS_LOWER_DEFAULT;
        aYsUpper = QaaConstants.A_YS_UPPER_DEFAULT;
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

    public float getBbSpmsLower() {
        return bbSpmsLower;
    }

    public void setBbSpmsLower(float bbSpmsLower) {
        this.bbSpmsLower = bbSpmsLower;
    }

    public float getBbSpmsUpper() {
        return bbSpmsUpper;
    }

    public void setBbSpmsUpper(float bbSpmsUpper) {
        this.bbSpmsUpper = bbSpmsUpper;
    }

    public float getAPigLower() {
        return aPigLower;
    }

    public void setAPigLower(float aPigLower) {
        this.aPigLower = aPigLower;
    }

    public float getAPigUpper() {
        return aPigUpper;
    }

    public void setAPigUpper(float aPigUpper) {
        this.aPigUpper = aPigUpper;
    }

    public float getAYsLower() {
        return aYsLower;
    }

    public void setAYsLower(float AYsLower) {
        this.aYsLower = AYsLower;
    }

    public float getAYsUpper() {
        return aYsUpper;
    }

    public void setAYsUpper(float AYsUpper) {
        this.aYsUpper = AYsUpper;
    }
}
