package com.buurman.controller.backoffice;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateBuurmyRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BuurmyResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeBuurmyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/backoffice/buurmies")
@Tag(name = "Backoffice - Buurmies", description = "Keycloak user management for buurman realm")
@SecurityRequirement(name = "bearer-jwt")
public class BackofficeBuurmyController {

    private final BackofficeBuurmyService buurmyService;

    public BackofficeBuurmyController(BackofficeBuurmyService buurmyService) {
        this.buurmyService = buurmyService;
    }

    @Operation(summary = "List buurmies", description = "List Keycloak users with search and pagination")
    @GetMapping
    public PageResponse<BuurmyResponse> listBuurmies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction) {
        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        return buurmyService.listBuurmies(pageRequest, search);
    }

    @Operation(summary = "Get buurmy", description = "Get a Keycloak user by ID")
    @GetMapping("/{keycloakId}")
    public BuurmyResponse getBuurmy(@PathVariable String keycloakId) {
        return buurmyService.getBuurmy(keycloakId);
    }

    @Operation(summary = "Create buurmy", description = "Create a new user in the buurman Keycloak realm")
    @PostMapping
    @ResponseStatus(CREATED)
    public BuurmyResponse createBuurmy(
            @Valid @RequestBody CreateBuurmyRequest request,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        return buurmyService.createBuurmy(request, principal);
    }

    @Operation(summary = "Disable buurmy", description = "Disable a Keycloak user")
    @PostMapping("/{keycloakId}/disable")
    @ResponseStatus(NO_CONTENT)
    public void disableBuurmy(
            @PathVariable String keycloakId,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        buurmyService.disableBuurmy(keycloakId, principal);
    }

    @Operation(summary = "Enable buurmy", description = "Enable a disabled Keycloak user")
    @PostMapping("/{keycloakId}/enable")
    @ResponseStatus(NO_CONTENT)
    public void enableBuurmy(
            @PathVariable String keycloakId,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        buurmyService.enableBuurmy(keycloakId, principal);
    }

    @Operation(summary = "Delete buurmy", description = "Permanently delete a Keycloak user")
    @DeleteMapping("/{keycloakId}")
    @ResponseStatus(NO_CONTENT)
    public void deleteBuurmy(
            @PathVariable String keycloakId,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        buurmyService.deleteBuurmy(keycloakId, principal);
    }

    @Operation(summary = "Force password update", description = "Require the user to update their password on next login")
    @PostMapping("/{keycloakId}/force-password-update")
    @ResponseStatus(NO_CONTENT)
    public void forcePasswordUpdate(
            @PathVariable String keycloakId,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        buurmyService.forcePasswordUpdate(keycloakId, principal);
    }

    @Operation(summary = "Force profile update", description = "Require the user to update their profile on next login")
    @PostMapping("/{keycloakId}/force-profile-update")
    @ResponseStatus(NO_CONTENT)
    public void forceProfileUpdate(
            @PathVariable String keycloakId,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        buurmyService.forceProfileUpdate(keycloakId, principal);
    }
}
