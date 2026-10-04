package com.buurman.dto.response;

import java.time.Instant;
import java.util.Map;

import com.buurman.domain.Sid;

public record SavedContractFilterResponse(
    Sid identifier, String name, Map<String, Object> criteria, Instant createdAt) {}
