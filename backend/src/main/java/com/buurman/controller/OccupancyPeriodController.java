package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.OccupancyPeriodIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
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
        PropertyIdentifier.of(propertyIdentifier), createOccupancyPeriodRequest, principal);
  }

  @Override
  public List<OccupancyPeriodResponse> callList(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.findByProperty(PropertyIdentifier.of(propertyIdentifier), principal);
  }

  @Override
  public OccupancyPeriodResponse get(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.get(PropertyIdentifier.of(propertyIdentifier), OccupancyPeriodIdentifier.of(identifier), principal);
  }

  @Override
  public OccupancyPeriodResponse update(
      String propertyIdentifier,
      String identifier,
      UpdateOccupancyPeriodRequest updateOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.update(
        PropertyIdentifier.of(propertyIdentifier), OccupancyPeriodIdentifier.of(identifier), updateOccupancyPeriodRequest, principal);
  }

  @Override
  public OccupancyPeriodResponse end(
      String propertyIdentifier,
      String identifier,
      EndOccupancyPeriodRequest endOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.end(
        PropertyIdentifier.of(propertyIdentifier), OccupancyPeriodIdentifier.of(identifier), endOccupancyPeriodRequest, principal);
  }

  @Override
  public void delete(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    occupancyPeriodService.delete(PropertyIdentifier.of(propertyIdentifier), OccupancyPeriodIdentifier.of(identifier), principal);
  }

  @Override
  public PropertyTimelineResponse getTimeline(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.getTimeline(PropertyIdentifier.of(propertyIdentifier), principal);
  }
}
