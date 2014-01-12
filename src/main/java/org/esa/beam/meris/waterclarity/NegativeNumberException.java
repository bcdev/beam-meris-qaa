package org.esa.beam.meris.waterclarity;

/**
 * Created by IntelliJ IDEA.
 * User: yjiang09
 * Date: 2/29/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class NegativeNumberException extends Exception {


    private final String message;
    private final double number;

    NegativeNumberException(String message, double number) {
        this.message = message;
        this.number = number;
    }

    @Override
    public String getMessage() {
        return message + ": " + number;
    }
}
