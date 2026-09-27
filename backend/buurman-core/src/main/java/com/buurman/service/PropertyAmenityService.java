package com.buurman.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.domain.Amenity;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.repository.AmenityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyAmenityService {

  private final AmenityRepository amenityRepository;

  public Map<String, List<AmenityResponse>> getAllAmenitiesGrouped(
      @Nullable String propertyCategory) {
    List<Amenity> amenities;
    if (propertyCategory != null && !propertyCategory.isBlank()) {
      amenities = amenityRepository.findByApplicableCategory(propertyCategory);
    } else {
      amenities = amenityRepository.findAll();
    }
    return amenities.stream()
        .map(
            a ->
                new AmenityResponse(
                    a.getIdentifier().orElseThrow(),
                    a.getName(),
                    a.getCategory(),
                    a.getIcon(),
                    a.getApplicableCategories()))
        .collect(groupingBy(AmenityResponse::category));
  }
}
