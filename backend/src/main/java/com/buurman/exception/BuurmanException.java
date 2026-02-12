package com.buurman.exception;

public abstract class BuurmanException extends RuntimeException {
    protected BuurmanException(String message) {
        super(message);
    }
    protected BuurmanException(String message, Throwable cause) {
        super(message, cause);
    }
}
