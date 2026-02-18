package com.buurman.dto.response;

import java.util.List;

public record AmenityResponse(
    String identifier,
    String name,
    String category,
    String icon,
    List<String> applicableCategories) {}
