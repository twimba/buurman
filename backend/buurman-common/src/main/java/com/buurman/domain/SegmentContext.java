package com.buurman.domain;

import java.util.Optional;

public record SegmentContext(
    boolean isDemo,
    boolean isOwner,
    Optional<TeamRole> role,
    Optional<String> email,
    boolean emailVerified,
    int propertyCount,
    int memberCount,
    long teamAgeDays,
    int contractCount,
    int contactCount,
    int photoCount,
    int documentCount,
    int expenseCount,
    int paymentCount,
    int calendarFeedCount,
    boolean isTeamScope,
    boolean isUserScope,
    Optional<String> teamName,
    Optional<String> teamAdminEmail,
    Optional<String> teamOwnerEmail,
    Optional<String> teamCurrency,
    Optional<String> teamDefaultCountry,
    Optional<String> teamTimezone) {

  public static SegmentContext global() {
    return new SegmentContext(
        false,
        false,
        Optional.empty(),
        Optional.empty(),
        false,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        false,
        false,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }
}
