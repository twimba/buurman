package com.buurman.domain;

/**
 * Generic label + count projection, used for grouped count queries (e.g. notifications by status).
 */
public record LabelCount(String label, int count) {}
