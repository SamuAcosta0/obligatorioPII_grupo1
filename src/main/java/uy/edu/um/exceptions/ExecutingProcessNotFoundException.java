package uy.edu.um.exceptions;

public class ExecutingProcessNotFoundException extends RuntimeException {
    public ExecutingProcessNotFoundException(String message) {
        super(message);
    }
}
