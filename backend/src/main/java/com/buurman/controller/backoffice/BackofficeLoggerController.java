package com.buurman.controller.backoffice;

import com.buurman.dto.request.backoffice.SetLogLevelRequest;
import com.buurman.dto.response.backoffice.LoggerConfigurationResponse;
import com.buurman.service.backoffice.BackofficeLoggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/backoffice/loggers")
@Tag(name = "Backoffice - Loggers", description = "Runtime log level management")
@SecurityRequirement(name = "bearer-jwt")
public class BackofficeLoggerController {

    private final BackofficeLoggerService loggerService;

    public BackofficeLoggerController(BackofficeLoggerService loggerService) {
        this.loggerService = loggerService;
    }

    @Operation(summary = "List all loggers")
    @GetMapping
    public List<LoggerConfigurationResponse> listLoggers(
            @RequestParam(required = false) String search) {
        return loggerService.listLoggers(search);
    }

    @Operation(summary = "Set log level")
    @PostMapping("/{loggerName}/level")
    public LoggerConfigurationResponse setLogLevel(
            @PathVariable String loggerName,
            @RequestBody SetLogLevelRequest request) {
        return loggerService.setLogLevel(loggerName, request.level());
    }

    @Operation(summary = "Reset all log levels to defaults")
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetAll() {
        loggerService.resetAll();
    }
}
