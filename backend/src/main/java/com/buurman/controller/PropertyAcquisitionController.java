package com.buurman.controller;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.UpsertPropertyAcquisitionRequest;
import com.buurman.dto.response.PropertyAcquisitionResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyAcquisitionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/acquisition")
@Tag(name = "Property Acquisitions", description = "Property acquisition data (1:1 per property)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyAcquisitionController {

  private final PropertyAcquisitionService acquisitionService;

  @Operation(summary = "Get acquisition", description = "Get acquisition data for a property")
  @GetMapping
  public Optional<PropertyAcquisitionResponse> getAcquisition(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return acquisitionService.getByProperty(propertyIdentifier, principal);
  }

  @Operation(
      summary = "Create or update acquisition",
      description = "Create or update acquisition data for a property (Admin/Editor)")
  @PutMapping
  public PropertyAcquisitionResponse upsertAcquisition(
      @PathVariable String propertyIdentifier,
      @Valid @RequestBody UpsertPropertyAcquisitionRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return acquisitionService.upsert(propertyIdentifier, request, principal);
  }
}
