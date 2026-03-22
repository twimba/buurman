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
      PropertyIdentifier propertyIdentifier,
      CreateOccupancyPeriodRequest createOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.create(
        propertyIdentifier, createOccupancyPeriodRequest, principal);
  }

  @Override
  public List<OccupancyPeriodResponse> listOccupancyPeriods(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.findByProperty(propertyIdentifier, principal);
  }

  @Override
  public OccupancyPeriodResponse get(
      PropertyIdentifier propertyIdentifier, OccupancyPeriodIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.get(propertyIdentifier, identifier, principal);
  }

  @Override
  public OccupancyPeriodResponse update(
      PropertyIdentifier propertyIdentifier,
      OccupancyPeriodIdentifier identifier,
      UpdateOccupancyPeriodRequest updateOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.update(
        propertyIdentifier, identifier, updateOccupancyPeriodRequest, principal);
  }

  @Override
  public OccupancyPeriodResponse end(
      PropertyIdentifier propertyIdentifier,
      OccupancyPeriodIdentifier identifier,
      EndOccupancyPeriodRequest endOccupancyPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.end(
        propertyIdentifier, identifier, endOccupancyPeriodRequest, principal);
  }

  @Override
  public void delete(PropertyIdentifier propertyIdentifier, OccupancyPeriodIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    occupancyPeriodService.delete(propertyIdentifier, identifier, principal);
  }

  @Override
  public PropertyTimelineResponse getTimeline(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return occupancyPeriodService.getTimeline(propertyIdentifier, principal);
  }
}
