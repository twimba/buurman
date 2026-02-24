package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.backoffice.SetLogLevelRequest;
import com.buurman.dto.response.backoffice.LoggerConfigurationResponse;
import com.buurman.service.backoffice.BackofficeLoggerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/loggers")
@Tag(name = "Backoffice - Loggers", description = "Runtime log level management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeLoggerController {

  private final BackofficeLoggerService loggerService;

  @Operation(summary = "List all loggers")
  @GetMapping
  public List<LoggerConfigurationResponse> listLoggers(@RequestParam Optional<String> search) {
    return loggerService.listLoggers(search.orElse(null));
  }

  @Operation(summary = "Set log level")
  @PostMapping("/{loggerName}/level")
  public LoggerConfigurationResponse setLogLevel(
      @PathVariable String loggerName, @RequestBody SetLogLevelRequest request) {
    return loggerService.setLogLevel(loggerName, request.level().orElse(null));
  }

  @Operation(summary = "Reset all log levels to defaults")
  @PostMapping("/reset")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetAll() {
    loggerService.resetAll();
  }
}
