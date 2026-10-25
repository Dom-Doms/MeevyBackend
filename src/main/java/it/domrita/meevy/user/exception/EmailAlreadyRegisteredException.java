package it.domrita.meevy.user.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("L'indirizzo email è già registrato.");
    }
}
