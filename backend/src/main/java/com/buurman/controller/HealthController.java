package com.buurman.controller;

import com.buurman.config.models.AppProperties;
import com.buurman.dto.response.HealthResponse;
import com.buurman.dto.response.InfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flywaydb.core.Flyway;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@RestController
@RequestMapping("")
@Tag(name = "Health", description = "Health and info endpoints")
public class HealthController {

    private final AppProperties appProperties;
    private final Environment environment;
    private final Optional<Flyway> flyway;
    private final Clock clock;

    public HealthController(AppProperties appProperties,
                            Environment environment,
                            Optional<Flyway> flyway,
                            Clock clock) {
        this.appProperties = appProperties;
        this.environment = environment;
        this.flyway = flyway;
        this.clock = clock;
    }

    @Operation(summary = "Health check", description = "Returns the health status of the application")
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", clock.instant());
    }

    @Operation(summary = "Application info", description = "Returns application version, environment, and database migration info")
    @GetMapping("/info")
    public InfoResponse info() {
        InfoResponse.DatabaseInfo dbInfo = null;

        if (flyway.isPresent()) {
            var flywayInfo = flyway.get().info();
            var current = flywayInfo.current();

            if (current != null) {
                dbInfo = new InfoResponse.DatabaseInfo(
                        current.getVersion().toString(),
                        current.getDescription(),
                        current.getInstalledOn().toInstant().atZone(ZoneId.systemDefault()).toInstant(),
                        flywayInfo.applied().length,
                        flywayInfo.pending().length
                );
            }
        }

        String activeProfiles = String.join(",", environment.getActiveProfiles());
        return new InfoResponse(appProperties.version(), activeProfiles.isEmpty() ? "default" : activeProfiles, clock.instant(), dbInfo);
    }
}
