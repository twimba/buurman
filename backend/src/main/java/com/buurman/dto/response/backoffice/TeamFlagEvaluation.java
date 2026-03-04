package com.buurman.dto.response.backoffice;

import java.util.Map;

import com.buurman.domain.Sid;

public record TeamFlagEvaluation(
    Sid teamIdentifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isActive,
    Map<String, Object> flags) {}
