package com.buurman.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.GeocodeRequest;
import com.buurman.dto.response.GeocodeResponse;
import com.buurman.service.GeocodingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/geocode")
@Tag(name = "Geocoding", description = "Address geocoding")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class GeocodingController {

  private final GeocodingService geocodingService;

  @Operation(
      summary = "Geocode address",
      description = "Convert an address to latitude/longitude coordinates")
  @PostMapping
  public ResponseEntity<GeocodeResponse> geocode(@Valid @RequestBody GeocodeRequest request) {
    return geocodingService
        .geocode(
            request.street(), request.city(),
            request.postalCode(), request.country())
        .map(
            result ->
                ResponseEntity.ok(
                    new GeocodeResponse(
                        Optional.ofNullable(result.latitude()),
                        Optional.ofNullable(result.longitude()),
                        Optional.ofNullable(result.accuracy()))))
        .orElse(ResponseEntity.noContent().build());
  }
}
