package com.buurman.dto.request.backoffice;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.LeaseKind;

public record UpsertLeaseClauseTemplateRequest(
    String countryCode,
    @Nullable LeaseKind leaseKind,
    String clauseKey,
    String titleI18nKey,
    String bodyI18nKey,
    boolean defaultIncluded,
    boolean optional,
    boolean pinned,
    int sortOrder) {}
