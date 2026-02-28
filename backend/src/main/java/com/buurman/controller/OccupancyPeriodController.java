package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CreateOccupancyPeriodRequest;
import com.buurman.dto.request.EndOccupancyPeriodRequest;
import com.buurman.dto.request.UpdateOccupancyPeriodRequest;
import com.buurman.dto.response.OccupancyPeriodResponse;
import com.buurman.dto.response.PropertyTimelineResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.OccupancyPeriodService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/occupancy-periods")
@Tag(name = "Occupancy Periods", description = "Self-occupancy period management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class OccupancyPeriodController {

  private final OccupancyPeriodService occupancyPeriodService;

  @Operation(
      summary = "Create occupancy period",
      description = "Record a new self-occupancy period for a property (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public OccupancyPeriodResponse create(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreateOccupancyPeriodRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return occupancyPeriodService.create(propertyIdentifier, request, principal);
  }

  @Operation(
      summary = "List occupancy periods",
      description = "Get all self-occupancy periods for a property")
  @GetMapping
  public List<OccupancyPeriodResponse> list(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return occupancyPeriodService.findByProperty(propertyIdentifier, principal);
  }

  @Operation(
      summary = "Get occupancy period",
      description = "Get details of a specific self-occupancy period")
  @GetMapping("/{identifier}")
  public OccupancyPeriodResponse get(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Occupancy period ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return occupancyPeriodService.get(propertyIdentifier, identifier, principal);
  }

  @Operation(
      summary = "Update occupancy period",
      description = "Update self-occupancy period details (Admin/Editor)")
  @PutMapping("/{identifier}")
  public OccupancyPeriodResponse update(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Occupancy period ULID identifier") @PathVariable String identifier,
      @Valid @RequestBody UpdateOccupancyPeriodRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return occupancyPeriodService.update(propertyIdentifier, identifier, request, principal);
  }

  @Operation(
      summary = "End occupancy period",
      description = "End a self-occupancy period (Admin/Editor)")
  @PostMapping("/{identifier}/end")
  public OccupancyPeriodResponse end(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Occupancy period ULID identifier") @PathVariable String identifier,
      @Valid @RequestBody EndOccupancyPeriodRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return occupancyPeriodService.end(propertyIdentifier, identifier, request, principal);
  }

  @Operation(
      summary = "Delete occupancy period",
      description = "Soft delete a self-occupancy period (Admin only)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void delete(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Occupancy period ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    occupancyPeriodService.delete(propertyIdentifier, identifier, principal);
  }

  @Operation(
      summary = "Get property timeline",
      description = "Get unified timeline of contracts and self-occupancy periods")
  @GetMapping("/timeline")
  public PropertyTimelineResponse getTimeline(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return occupancyPeriodService.getTimeline(propertyIdentifier, principal);
  }
}
