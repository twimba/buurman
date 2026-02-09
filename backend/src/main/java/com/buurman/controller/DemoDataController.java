package com.buurman.controller;

import com.buurman.dto.response.DemoDataResponse;
import com.buurman.service.demo.DemoDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/admin/demo-data")
@Tag(name = "Demo Data", description = "Demo data generation and cleanup")
@SecurityRequirement(name = "demo-api-key")
public class DemoDataController {

    private final DemoDataService demoDataService;

    public DemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate demo data", description = "Cleans up existing demo data and generates fresh demo data")
    public ResponseEntity<DemoDataResponse> generate() {
        DemoDataResponse response = demoDataService.generate();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Clean up demo data", description = "Removes all demo data from database and Keycloak")
    public ResponseEntity<Void> cleanup() {
        demoDataService.cleanup();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Demo data status", description = "Returns current demo data status")
    public ResponseEntity<Map<String, Object>> status() {
        Instant lastGenerated = demoDataService.getLastGeneratedAt();
        return ResponseEntity.ok(Map.of(
                "enabled", demoDataService.isEnabled(),
                "lastGeneratedAt", lastGenerated != null ? lastGenerated.toString() : "never"
        ));
    }
}
