package com.buurman.dto.response;

import com.buurman.domain.Sid;

public record ResolvedLeaseClauseResponse(
    Sid templateIdentifier,
    String clauseKey,
    String title,
    String body,
    boolean included,
    boolean optional,
    int sortOrder) {}
