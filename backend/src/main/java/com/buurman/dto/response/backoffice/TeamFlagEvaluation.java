package com.buurman.dto.response.backoffice;

import java.util.Map;

import com.buurman.domain.Ulid;

public record TeamFlagEvaluation(
    Ulid teamIdentifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isActive,
    Map<String, Object> flags) {}
