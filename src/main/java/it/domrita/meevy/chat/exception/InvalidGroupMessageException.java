package it.domrita.meevy.chat.exception;

public class InvalidGroupMessageException extends RuntimeException {

    public InvalidGroupMessageException(String message) {
        super(message);
    }
}