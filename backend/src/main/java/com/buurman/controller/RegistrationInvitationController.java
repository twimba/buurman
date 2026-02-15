package com.buurman.controller;

import com.buurman.dto.response.RegistrationConfigResponse;
import com.buurman.dto.response.ValidateInvitationCodeResponse;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.RegistrationInvitationService;
import com.buurman.util.FeatureFlags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Tag(name = "Registration", description = "Public registration endpoints")
public class RegistrationInvitationController {

    private final RegistrationInvitationService invitationService;
    private final FeatureFlagService featureFlagService;

    public RegistrationInvitationController(RegistrationInvitationService invitationService,
                                             FeatureFlagService featureFlagService) {
        this.invitationService = invitationService;
        this.featureFlagService = featureFlagService;
    }

    @PostMapping("/registration-invitations/validate")
    @Operation(summary = "Validate a registration invitation code")
    public ResponseEntity<ValidateInvitationCodeResponse> validate(
            @RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "");
        ValidateInvitationCodeResponse response = invitationService.validateCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registration/config")
    @Operation(summary = "Get registration configuration")
    public ResponseEntity<RegistrationConfigResponse> getConfig() {
        boolean invitationRequired = featureFlagService.isEnabled(FeatureFlags.INVITATION_REQUIRED);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(new RegistrationConfigResponse(invitationRequired));
    }
}
