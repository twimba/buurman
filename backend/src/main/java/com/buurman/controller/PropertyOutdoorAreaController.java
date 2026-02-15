package com.buurman.controller;

import com.buurman.dto.request.PropertyOutdoorAreaRequest;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyOutdoorAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

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
            @PathVariable String propertyIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return outdoorAreaService.getOutdoorAreas(propertyIdentifier, principal);
    }

    @Operation(summary = "Create outdoor area", description = "Add an outdoor area to a property (Admin/Editor)")
    @PostMapping
    @ResponseStatus(CREATED)
    public PropertyOutdoorAreaResponse createOutdoorArea(
            @PathVariable String propertyIdentifier,
            @Valid @RequestBody PropertyOutdoorAreaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return outdoorAreaService.createOutdoorArea(propertyIdentifier, request, principal);
    }

    @Operation(summary = "Update outdoor area", description = "Update an outdoor area (Admin/Editor)")
    @PutMapping("/{areaIdentifier}")
    public PropertyOutdoorAreaResponse updateOutdoorArea(
            @PathVariable String propertyIdentifier,
            @PathVariable String areaIdentifier,
            @Valid @RequestBody PropertyOutdoorAreaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return outdoorAreaService.updateOutdoorArea(propertyIdentifier, areaIdentifier, request, principal);
    }

    @Operation(summary = "Delete outdoor area", description = "Soft delete an outdoor area (Admin/Editor)")
    @DeleteMapping("/{areaIdentifier}")
    @ResponseStatus(NO_CONTENT)
    public void deleteOutdoorArea(
            @PathVariable String propertyIdentifier,
            @PathVariable String areaIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        outdoorAreaService.deleteOutdoorArea(propertyIdentifier, areaIdentifier, principal);
    }
}
