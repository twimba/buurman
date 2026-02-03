package com.buurman.exception;

public class CrossTeamAccessException extends RuntimeException {
    public CrossTeamAccessException(String message) {
        super(message);
    }
}
