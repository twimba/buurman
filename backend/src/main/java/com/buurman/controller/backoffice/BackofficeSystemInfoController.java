package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse;
import com.buurman.service.backoffice.BackofficeSystemInfoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/system")
@Tag(name = "Backoffice - System", description = "System information and health checks")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeSystemInfoController {

  private final BackofficeSystemInfoService backofficeSystemInfoService;

  @Operation(
      summary = "Get system info",
      description = "Returns build info, runtime, migrations, and service health.")
  @GetMapping("/info")
  public BackofficeSystemInfoResponse getSystemInfo() {
    return backofficeSystemInfoService.getSystemInfo();
  }
}
