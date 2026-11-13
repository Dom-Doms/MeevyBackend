
package it.domrita.meevy.resource.exception;

public class ResourceStorageException extends RuntimeException {

    public ResourceStorageException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
