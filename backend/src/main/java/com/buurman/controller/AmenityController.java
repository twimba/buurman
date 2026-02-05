package com.buurman.controller;

import com.buurman.dto.request.PropertyAmenityRequest;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.dto.response.PropertyAmenityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyAmenityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "bearer-jwt")
public class AmenityController {

    private final PropertyAmenityService propertyAmenityService;

    public AmenityController(PropertyAmenityService propertyAmenityService) {
        this.propertyAmenityService = propertyAmenityService;
    }

    @Tag(name = "Amenities", description = "Reference amenity data")
    @Operation(summary = "List all amenities grouped by category", description = "Get all available amenities grouped by category")
    @GetMapping("/api/amenities")
    public Map<String, List<AmenityResponse>> getAllAmenities() {
        return propertyAmenityService.getAllAmenitiesGrouped();
    }

    @Tag(name = "Property Amenities", description = "Manage amenities for properties")
    @Operation(summary = "List property amenities", description = "Get amenities linked to a property")
    @GetMapping("/api/properties/{propertyIdentifier}/amenities")
    public List<PropertyAmenityResponse> getPropertyAmenities(
            @PathVariable String propertyIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return propertyAmenityService.getPropertyAmenities(propertyIdentifier, principal);
    }

    @Tag(name = "Property Amenities")
    @Operation(summary = "Add amenity to property", description = "Link an amenity to a property (Admin/Editor)")
    @PostMapping("/api/properties/{propertyIdentifier}/amenities")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyAmenityResponse addAmenity(
            @PathVariable String propertyIdentifier,
            @Valid @RequestBody PropertyAmenityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return propertyAmenityService.addAmenity(propertyIdentifier, request, principal);
    }

    @Tag(name = "Property Amenities")
    @Operation(summary = "Remove amenity from property", description = "Unlink an amenity from a property (Admin/Editor)")
    @DeleteMapping("/api/properties/{propertyIdentifier}/amenities/{amenityIdentifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAmenity(
            @PathVariable String propertyIdentifier,
            @PathVariable String amenityIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        propertyAmenityService.removeAmenity(propertyIdentifier, amenityIdentifier, principal);
    }
}
