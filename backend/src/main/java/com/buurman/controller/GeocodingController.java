package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.GeocodeRequest;
import com.buurman.dto.response.GeocodeResponse;
import com.buurman.generated.api.GeocodingApi;
import com.buurman.service.GeocodingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GeocodingController implements GeocodingApi {

  private final GeocodingService geocodingService;

  @Override
  public GeocodeResponse geocode(@Valid GeocodeRequest geocodeRequest) {
    return geocodingService
        .geocode(
            geocodeRequest.street(),
            geocodeRequest.city(),
            geocodeRequest.postalCode().orElse(null),
            geocodeRequest.country())
        .map(
            result ->
                new GeocodeResponse(
                    Optional.ofNullable(result.latitude()),
                    Optional.ofNullable(result.longitude()),
                    Optional.ofNullable(result.accuracy())))
        .orElse(
            new GeocodeResponse(Optional.empty(), Optional.empty(), Optional.empty()));
  }
}
