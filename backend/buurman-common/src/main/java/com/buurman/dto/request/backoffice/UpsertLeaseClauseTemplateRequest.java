package com.buurman.dto.request.backoffice;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.LeaseKind;

/**
 * Upsert body. {@code leaseKind} is required on create (no default) and ignored-if-null on update,
 * where the kind of an existing template can never change.
 */
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
