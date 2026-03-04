package com.buurman.dto.response;

import java.util.List;

import com.buurman.domain.Sid;

public record AmenityResponse(
    Sid identifier, String name, String category, String icon, List<String> applicableCategories) {}
