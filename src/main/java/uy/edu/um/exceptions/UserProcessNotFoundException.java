// UserProcessNotFoundException.java
package uy.edu.um.exceptions;

public class UserProcessNotFoundException extends RuntimeException {
    public UserProcessNotFoundException(int uid) {
        super("No existe proceso en memoria para ese id de usuario: " + uid);
    }
}