package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.GeocodeRequest;
import com.buurman.dto.response.GeocodeResponse;
import com.buurman.generated.api.GeocodingApi;
import com.buurman.service.GeocodingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GeocodingController implements GeocodingApi {

  private final GeocodingService geocodingService;

  @Override
  public GeocodeResponse geocode(GeocodeRequest geocodeRequest) {
    return geocodingService
        .geocode(
            geocodeRequest.street(),
            geocodeRequest.city(),
            geocodeRequest.postalCode(),
            geocodeRequest.countryCode())
        .map(
            result ->
                new GeocodeResponse(
                    Optional.of(result.latitude()),
                    Optional.of(result.longitude()),
                    Optional.of(result.accuracy())))
        .orElse(new GeocodeResponse(Optional.empty(), Optional.empty(), Optional.empty()));
  }
}
