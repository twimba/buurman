package com.buurman.dto.response;

public record ResidentialDetailsResponse(
    Integer bedrooms, Integer bathrooms, Boolean furnished, String petPolicy) {}
