package com.buurman.dto.response;

public record TenantSummary(
    String identifier, String firstName, String lastName, String email, String phone) {}
