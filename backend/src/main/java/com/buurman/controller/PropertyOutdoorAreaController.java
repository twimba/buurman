package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Ulid;
import com.buurman.dto.request.PropertyOutdoorAreaRequest;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.generated.api.PropertyOutdoorAreasApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyOutdoorAreaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyOutdoorAreaController implements PropertyOutdoorAreasApi {

  private final PropertyOutdoorAreaService outdoorAreaService;

  @Override
  public List<PropertyOutdoorAreaResponse> getOutdoorAreas(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return outdoorAreaService.getOutdoorAreas(Ulid.of(propertyIdentifier), principal);
  }

  @Override
  public PropertyOutdoorAreaResponse createOutdoorArea(
      String propertyIdentifier, PropertyOutdoorAreaRequest propertyOutdoorAreaRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return outdoorAreaService.createOutdoorArea(
        Ulid.of(propertyIdentifier), propertyOutdoorAreaRequest, principal);
  }

  @Override
  public PropertyOutdoorAreaResponse updateOutdoorArea(
      String propertyIdentifier,
      String areaIdentifier,
      PropertyOutdoorAreaRequest propertyOutdoorAreaRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return outdoorAreaService.updateOutdoorArea(
        Ulid.of(propertyIdentifier), Ulid.of(areaIdentifier), propertyOutdoorAreaRequest, principal);
  }

  @Override
  public void deleteOutdoorArea(String propertyIdentifier, String areaIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    outdoorAreaService.deleteOutdoorArea(Ulid.of(propertyIdentifier), Ulid.of(areaIdentifier), principal);
  }
}
