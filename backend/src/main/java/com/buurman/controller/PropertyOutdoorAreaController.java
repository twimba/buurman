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

import com.buurman.dto.request.PropertyOutdoorAreaRequest;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyOutdoorAreaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/outdoor-areas")
@Tag(name = "Property Outdoor Areas", description = "Manage outdoor areas for properties")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyOutdoorAreaController {

  private final PropertyOutdoorAreaService outdoorAreaService;

  @Operation(summary = "List outdoor areas", description = "Get all outdoor areas for a property")
  @GetMapping
  public List<PropertyOutdoorAreaResponse> getOutdoorAreas(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return outdoorAreaService.getOutdoorAreas(propertyIdentifier, principal);
  }

  @Operation(
      summary = "Create outdoor area",
      description = "Add an outdoor area to a property (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyOutdoorAreaResponse createOutdoorArea(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Valid @RequestBody PropertyOutdoorAreaRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return outdoorAreaService.createOutdoorArea(propertyIdentifier, request, principal);
  }

  @Operation(summary = "Update outdoor area", description = "Update an outdoor area (Admin/Editor)")
  @PutMapping("/{areaIdentifier}")
  public PropertyOutdoorAreaResponse updateOutdoorArea(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Outdoor area ULID identifier") @PathVariable String areaIdentifier,
      @Valid @RequestBody PropertyOutdoorAreaRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return outdoorAreaService.updateOutdoorArea(
        propertyIdentifier, areaIdentifier, request, principal);
  }

  @Operation(
      summary = "Delete outdoor area",
      description = "Soft delete an outdoor area (Admin/Editor)")
  @DeleteMapping("/{areaIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteOutdoorArea(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Outdoor area ULID identifier") @PathVariable String areaIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    outdoorAreaService.deleteOutdoorArea(propertyIdentifier, areaIdentifier, principal);
  }
}
