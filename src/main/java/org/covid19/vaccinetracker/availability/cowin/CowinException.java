package org.covid19.vaccinetracker.availability.cowin;

public class CowinException extends RuntimeException {
    private int statusCode;

    public CowinException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
