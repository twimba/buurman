package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse;
import com.buurman.generated.backoffice.api.BackofficeSystemApi;
import com.buurman.service.backoffice.BackofficeSystemInfoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeSystemInfoController implements BackofficeSystemApi {

  private final BackofficeSystemInfoService backofficeSystemInfoService;

  @Override
  public BackofficeSystemInfoResponse getSystemInfo() {
    return backofficeSystemInfoService.getSystemInfo();
  }
}
