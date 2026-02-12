package com.buurman.exception;

public class VerificationCodeException extends BadRequestException {
    public VerificationCodeException(String message) {
        super(message);
    }
}
