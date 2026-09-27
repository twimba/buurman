package com.buurman.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection of a unit's active contract for the units grid view: the rent charged and the primary
 * tenant's display name, keyed by {@code unitId}. {@code tenantName} may be {@code null} when the
 * active contract has no primary-tenant party recorded.
 */
public record UnitActiveTenancy(
    UUID unitId, BigDecimal rentAmount, String rentAmountCurrency, String tenantName) {}
