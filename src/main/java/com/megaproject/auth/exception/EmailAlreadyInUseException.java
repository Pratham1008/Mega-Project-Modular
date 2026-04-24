package com.megaproject.auth.exception;
public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(String message) { super(message); }
}
