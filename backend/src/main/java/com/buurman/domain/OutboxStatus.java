package com.buurman.domain;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
