package com.buurman.service.backoffice;

import com.buurman.dto.response.backoffice.LoggerConfigurationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackofficeLoggerService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeLoggerService.class);

    private final LoggingSystem loggingSystem;

    public BackofficeLoggerService(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    public List<LoggerConfigurationResponse> listLoggers(String search) {
        return loggingSystem.getLoggerConfigurations().stream()
                .filter(config -> config.getEffectiveLevel() != null)
                .filter(config -> search == null || search.isBlank()
                        || config.getName().toLowerCase().contains(search.toLowerCase()))
                .map(this::toResponse)
                .toList();
    }

    public LoggerConfigurationResponse setLogLevel(String loggerName, String level) {
        LogLevel logLevel = level != null ? LogLevel.valueOf(level.toUpperCase()) : null;
        loggingSystem.setLogLevel(loggerName, logLevel);
        log.info("Set log level for '{}' to '{}'", loggerName, level);

        LoggerConfiguration config = loggingSystem.getLoggerConfiguration(loggerName);
        return toResponse(config);
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
                config.getConfiguredLevel() != null ? config.getConfiguredLevel().name() : null,
                config.getEffectiveLevel().name()
        );
    }
}
