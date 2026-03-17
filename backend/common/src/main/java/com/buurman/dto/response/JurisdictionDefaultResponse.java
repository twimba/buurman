package com.buurman.dto.response;

import java.util.Map;

public record JurisdictionDefaultResponse(
    String countryCode, Map<String, String> defaults, String disclaimer) {}
