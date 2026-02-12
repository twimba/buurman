package com.buurman.exception;

public class InsufficientPermissionsException extends ForbiddenException {
    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
