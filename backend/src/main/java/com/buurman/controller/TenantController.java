package com.buurman.controller;

import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.LinkTenantToPropertyRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.PropertyTenantHistoryResponse;
import com.buurman.dto.response.TenantResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "Tenant management")
@SecurityRequirement(name = "bearer-jwt")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "Create tenant", description = "Create a new tenant (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public TenantResponse createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.createTenant(request, principal);
    }

    @Operation(summary = "List tenants", description = "Get all tenants with optional search")
    @GetMapping
    public List<TenantResponse> getTenants(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (search != null && !search.isBlank()) {
            return tenantService.searchTenants(search, principal);
        }
        return tenantService.getAllTenants(principal);
    }

    @Operation(summary = "Get tenant details", description = "Get details of a specific tenant")
    @GetMapping("/{id}")
    public TenantResponse getTenant(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.getTenant(id, principal);
    }

    @Operation(summary = "Update tenant", description = "Update tenant information (Admin/Editor)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public TenantResponse updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.updateTenant(id, request, principal);
    }

    @Operation(summary = "Delete tenant", description = "Soft delete a tenant (Admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteTenant(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tenantService.deleteTenant(id, principal);
    }

    @Operation(summary = "Link tenant to property", description = "Assign tenant to a property (Admin/Editor)")
    @PostMapping("/{id}/link-property")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public TenantResponse linkTenantToProperty(
            @PathVariable UUID id,
            @Valid @RequestBody LinkTenantToPropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.linkTenantToProperty(id, request, principal);
    }

    @Operation(summary = "Unlink tenant from property", description = "Remove tenant from current property (Admin/Editor)")
    @PostMapping("/{id}/unlink-property")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public TenantResponse unlinkTenantFromProperty(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.unlinkTenantFromProperty(id, principal);
    }

    @Operation(summary = "Get tenant history", description = "Get property assignment history for a tenant")
    @GetMapping("/{id}/history")
    public List<PropertyTenantHistoryResponse> getTenantHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.getTenantHistory(id, principal);
    }
}
