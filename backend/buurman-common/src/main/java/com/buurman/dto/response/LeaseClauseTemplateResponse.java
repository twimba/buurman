package com.buurman.dto.response;

import com.buurman.domain.Sid;

public record LeaseClauseTemplateResponse(
    Sid identifier,
    String countryCode,
    String clauseKey,
    String titleI18nKey,
    String bodyI18nKey,
    boolean defaultIncluded,
    boolean optional,
    int sortOrder,
    int version) {}
