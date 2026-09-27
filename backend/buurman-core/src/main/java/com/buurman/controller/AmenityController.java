package com.buurman.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.AmenityResponse;
import com.buurman.generated.api.AmenitiesApi;
import com.buurman.service.PropertyAmenityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AmenityController implements AmenitiesApi {

  private final PropertyAmenityService propertyAmenityService;

  @Override
  public Map<String, List<AmenityResponse>> getAllAmenities(Optional<String> category) {
    return propertyAmenityService.getAllAmenitiesGrouped(category.orElse(null));
  }
}
