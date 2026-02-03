package com.buurman.exception;

public class CannotLeaveOwnedTeamException extends RuntimeException {
    public CannotLeaveOwnedTeamException(String message) {
        super(message);
    }
}
