package com.buurman.service.backoffice;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Service;

import com.buurman.dto.response.backoffice.LoggerConfigurationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeLoggerService {

  private final LoggingSystem loggingSystem;

  public List<LoggerConfigurationResponse> listLoggers(@Nullable String search) {
    return loggingSystem.getLoggerConfigurations().stream()
        .filter(config -> config.getEffectiveLevel() != null)
        .filter(
            config ->
                search == null
                    || search.isBlank()
                    || config
                        .getName()
                        .toLowerCase(Locale.ROOT)
                        .contains(search.toLowerCase(Locale.ROOT)))
        .map(this::toResponse)
        .toList();
  }

  public LoggerConfigurationResponse setLogLevel(String loggerName, @Nullable String level) {
    LogLevel logLevel = level != null ? LogLevel.valueOf(level.toUpperCase(Locale.ROOT)) : null;
    loggingSystem.setLogLevel(loggerName, logLevel);
    log.info("Set log level for '{}' to '{}'", loggerName, level);

    LoggerConfiguration config = loggingSystem.getLoggerConfiguration(loggerName);
    return toResponse(java.util.Objects.requireNonNull(config));
  }

  public void resetAll() {
    loggingSystem.getLoggerConfigurations().stream()
        .filter(config -> config.getConfiguredLevel() != null)
        .forEach(config -> loggingSystem.setLogLevel(config.getName(), null));
    log.info("Reset all logger levels to defaults");
  }

  private LoggerConfigurationResponse toResponse(LoggerConfiguration config) {
    return new LoggerConfigurationResponse(
        config.getName(),
        Optional.ofNullable(
            config.getConfiguredLevel() != null ? config.getConfiguredLevel().name() : null),
        config.getEffectiveLevel().name());
  }
}
