package com.buurman.dto.response;

import java.util.Map;
import com.buurman.util.Generated;

@Generated
public record JurisdictionDefaultResponse(
    String countryCode, Map<String, String> defaults, String disclaimer) {}
