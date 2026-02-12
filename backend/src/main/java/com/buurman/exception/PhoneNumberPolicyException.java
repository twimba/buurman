package com.buurman.exception;

public class PhoneNumberPolicyException extends BadRequestException {
    public PhoneNumberPolicyException(String message) {
        super(message);
    }
}
