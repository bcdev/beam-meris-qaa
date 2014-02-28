package org.esa.beam.meris.waterclarity;

public class ImaginaryNumberException extends Exception {

    private static final long serialVersionUID = 3594480090816778312L;

    private final String message;
    private final double number;

    public ImaginaryNumberException(String message, double number) {
        this.message = message;
        this.number = number;
    }

    @Override
    public String getMessage() {
        return message + ": " + number;
    }

}
