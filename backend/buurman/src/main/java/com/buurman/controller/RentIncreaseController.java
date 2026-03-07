package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.ApplyRentIncreasesRequest;
import com.buurman.dto.request.RentIncreasePreviewRequest;
import com.buurman.dto.response.ApplyRentIncreasesResponse;
import com.buurman.dto.response.RentIncreasePreviewResponse;
import com.buurman.generated.api.RentIncreasesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.RentIncreaseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RentIncreaseController implements RentIncreasesApi {

  private final RentIncreaseService rentIncreaseService;

  @Override
  public RentIncreasePreviewResponse previewRentIncreases(
      RentIncreasePreviewRequest rentIncreasePreviewRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentIncreaseService.preview(rentIncreasePreviewRequest, principal);
  }

  @Override
  public ApplyRentIncreasesResponse applyRentIncreases(
      ApplyRentIncreasesRequest applyRentIncreasesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentIncreaseService.apply(applyRentIncreasesRequest, principal);
  }
}
