package com.buurman.dto.response;

import java.util.List;

import com.buurman.domain.Ulid;

public record AmenityResponse(
    Ulid identifier,
    String name,
    String category,
    String icon,
    List<String> applicableCategories) {}
