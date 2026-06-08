package uy.edu.um.exceptions;

public class NoRunningProcessException extends RuntimeException {
    public NoRunningProcessException(String message) {
        super(message);
    }
}
