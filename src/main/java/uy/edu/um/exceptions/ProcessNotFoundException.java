// ProcessNotFoundException.java
package uy.edu.um.exceptions;

public class ProcessNotFoundException extends RuntimeException {
    public ProcessNotFoundException(int pid) {

        super("No existe proceso en memoria para ese id de proceso: " + pid);
    }
}