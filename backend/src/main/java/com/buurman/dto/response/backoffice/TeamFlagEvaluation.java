package com.buurman.dto.response.backoffice;

import java.util.Map;

public record TeamFlagEvaluation(
    String teamIdentifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isActive,
    Map<String, Object> flags) {}
