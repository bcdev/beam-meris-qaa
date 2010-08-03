package org.esa.beam.visat.processor.quasi.exceptions;

public class ImaginaryNumberException extends Throwable {

	private static final long serialVersionUID = 3594480090816778312L;
	
	private String message;
	private double number;
	
	public ImaginaryNumberException(String message, double number) {
		this.message = message;
		this.number = number;
	}

	public String getMessage() {
		return message+": "+number;
	}

}
