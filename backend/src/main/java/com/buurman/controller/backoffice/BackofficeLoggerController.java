package com.buurman.controller.backoffice;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.backoffice.SetLogLevelRequest;
import com.buurman.dto.response.backoffice.LoggerConfigurationResponse;
import com.buurman.generated.backoffice.api.BackofficeLoggersApi;
import com.buurman.service.backoffice.BackofficeLoggerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeLoggerController implements BackofficeLoggersApi {

  private final BackofficeLoggerService loggerService;

  @Override
  public List<LoggerConfigurationResponse> listLoggers(String search) {
    return loggerService.listLoggers(search);
  }

  @Override
  public LoggerConfigurationResponse setLogLevel(
      String loggerName, SetLogLevelRequest setLogLevelRequest) {
    return loggerService.setLogLevel(loggerName, setLogLevelRequest.level().orElse(null));
  }

  @Override
  public void resetAll() {
    loggerService.resetAll();
  }
}
