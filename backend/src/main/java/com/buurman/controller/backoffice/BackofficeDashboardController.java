package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.backoffice.BackofficeDashboardResponse;
import com.buurman.generated.backoffice.api.BackofficeDashboardApi;
import com.buurman.service.backoffice.BackofficeDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeDashboardController implements BackofficeDashboardApi {

  private final BackofficeDashboardService backofficeDashboardService;

  @Override
  public BackofficeDashboardResponse getDashboardStats() {
    return backofficeDashboardService.getStats();
  }
}
