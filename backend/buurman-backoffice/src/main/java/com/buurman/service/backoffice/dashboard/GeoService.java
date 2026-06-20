package com.buurman.service.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.GeoResponse;
import com.buurman.dto.response.backoffice.dashboard.PropertyLocationsResponse;
import com.buurman.dto.response.backoffice.dashboard.PropertyLocationsResponse.PropertyPoint;
import com.buurman.repository.backoffice.DashboardAggregateRepository;

import lombok.RequiredArgsConstructor;

/** Geographic distribution of teams + properties, and the world-map property point cloud. */
@Service
@RequiredArgsConstructor
public class GeoService {

  /** Hard cap on map points returned to the browser. */
  private static final int MAX_MAP_POINTS = 50_000;

  private final DashboardAggregateRepository aggregateRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public GeoResponse getGeo() {
    return new GeoResponse(
        PanelStatus.LIVE,
        Optional.empty(),
        Optional.empty(),
        aggregateRepository.teamsByCountry(),
        aggregateRepository.propertiesByCountry(),
        aggregateRepository.teamsWithoutCountry());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public PropertyLocationsResponse getPropertyLocations() {
    List<double[]> coords = aggregateRepository.propertyCoordinates(MAX_MAP_POINTS + 1);
    boolean capped = coords.size() > MAX_MAP_POINTS;
    List<PropertyPoint> points =
        coords.stream().limit(MAX_MAP_POINTS).map(c -> new PropertyPoint(c[0], c[1])).toList();
    return new PropertyLocationsResponse(points.size(), capped, points);
  }
}
