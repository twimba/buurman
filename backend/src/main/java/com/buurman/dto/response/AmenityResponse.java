package com.buurman.dto.response;

public record AmenityResponse(
        String identifier,
        String name,
        String category,
        String icon
) {}
