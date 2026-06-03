package uy.edu.um.exceptions;

public class OwnerNotFoundException extends RuntimeException {
    public OwnerNotFoundException(String message)
    {
        super(message);
    }
}
