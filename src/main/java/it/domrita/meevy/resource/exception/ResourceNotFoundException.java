
package it.domrita.meevy.resource.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long id) {
        super("File non trovato: " + id);
    }
}
