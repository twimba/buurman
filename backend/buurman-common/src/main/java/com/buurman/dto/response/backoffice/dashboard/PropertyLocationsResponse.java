package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

/**
 * Geocoded property points for the world map. Capped server-side; {@code capped} indicates the
 * result was truncated so the client can surface that.
 */
@SkipTestCoverage
public record PropertyLocationsResponse(int total, boolean capped, List<PropertyPoint> points) {

  @SkipTestCoverage
  public record PropertyPoint(double lat, double lng) {}
}
