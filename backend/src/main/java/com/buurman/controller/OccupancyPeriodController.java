package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Ulid;
import com.buurman.dto.request.CreateOccupancyPeriodRequest;
import com.buurman.dto.request.EndOccupancyPeriodRequest;
import com.buurman.dto.request.UpdateOccupancyPeriodRequest;
import com.buurman.dto.response.OccupancyPeriodResponse;
import com.buurman.dto.response.PropertyTimelineResponse;
import com.buurman.generated.api.OccupancyPeriodsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.OccupancyPeriodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OccupancyPeriodController implements OccupancyPeriodsApi {

  private final OccupancyPeriodService occupancyPeriodService;

  @Override
  public OccupancyPeriodResponse create(
      String propertyIdentifier, CreateOccupancyPeriodRequest createOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.create(
        Ulid.of(propertyIdentifier), createOccupancyPeriodRequest, principal);
  }

  @Override
  public List<OccupancyPeriodResponse> callList(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.findByProperty(Ulid.of(propertyIdentifier), principal);
  }

  @Override
  public OccupancyPeriodResponse get(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.get(Ulid.of(propertyIdentifier), Ulid.of(identifier), principal);
  }

  @Override
  public OccupancyPeriodResponse update(
      String propertyIdentifier,
      String identifier,
      UpdateOccupancyPeriodRequest updateOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.update(
        Ulid.of(propertyIdentifier), Ulid.of(identifier), updateOccupancyPeriodRequest, principal);
  }

  @Override
  public OccupancyPeriodResponse end(
      String propertyIdentifier,
      String identifier,
      EndOccupancyPeriodRequest endOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.end(
        Ulid.of(propertyIdentifier), Ulid.of(identifier), endOccupancyPeriodRequest, principal);
  }

  @Override
  public void delete(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    occupancyPeriodService.delete(Ulid.of(propertyIdentifier), Ulid.of(identifier), principal);
  }

  @Override
  public PropertyTimelineResponse getTimeline(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.getTimeline(Ulid.of(propertyIdentifier), principal);
  }
}
