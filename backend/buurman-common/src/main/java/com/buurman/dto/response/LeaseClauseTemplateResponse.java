package com.buurman.dto.response;

import com.buurman.domain.LeaseKind;
import com.buurman.domain.Sid;

public record LeaseClauseTemplateResponse(
    Sid identifier,
    String countryCode,
    LeaseKind leaseKind,
    String clauseKey,
    String titleI18nKey,
    String bodyI18nKey,
    boolean defaultIncluded,
    boolean optional,
    boolean pinned,
    int sortOrder,
    int version) {}
