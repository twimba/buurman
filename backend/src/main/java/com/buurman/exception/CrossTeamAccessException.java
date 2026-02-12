package com.buurman.exception;

public class CrossTeamAccessException extends ForbiddenException {
    public CrossTeamAccessException(String message) {
        super(message);
    }
}
