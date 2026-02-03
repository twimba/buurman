package com.buurman.exception;

public class TeamMembershipNotFoundException extends RuntimeException {
    public TeamMembershipNotFoundException(String message) {
        super(message);
    }
}
