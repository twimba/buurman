package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record RentRegulationRegionResponse(
    Sid identifier, String regionCode, String regionName, Optional<String> summary) {}
