package com.buurman.controller;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Ulid;
import com.buurman.dto.request.UpsertPropertyAcquisitionRequest;
import com.buurman.dto.response.PropertyAcquisitionResponse;
import com.buurman.generated.api.PropertyAcquisitionsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyAcquisitionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyAcquisitionController implements PropertyAcquisitionsApi {

  private final PropertyAcquisitionService acquisitionService;

  @Override
  public @Nullable PropertyAcquisitionResponse getAcquisition(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return acquisitionService.getByProperty(Ulid.of(propertyIdentifier), principal).orElse(null);
  }

  @Override
  public PropertyAcquisitionResponse upsertAcquisition(
      String propertyIdentifier,
      UpsertPropertyAcquisitionRequest upsertPropertyAcquisitionRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return acquisitionService.upsert(
        Ulid.of(propertyIdentifier), upsertPropertyAcquisitionRequest, principal);
  }
}
