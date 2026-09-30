package com.buurman.dto.request.backoffice;

public record UpsertLeaseClauseTemplateRequest(
    String countryCode,
    String clauseKey,
    String titleI18nKey,
    String bodyI18nKey,
    boolean defaultIncluded,
    boolean optional,
    int sortOrder) {}
