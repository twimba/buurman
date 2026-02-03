package com.buurman.controller;

import com.buurman.dto.request.CreateTenantAddressRequest;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.LinkTenantToPropertyRequest;
import com.buurman.dto.request.UpdateTenantAddressRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PropertyTenantHistoryResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantAddressResponse;
import com.buurman.dto.response.TenantResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import com.buurman.service.DocumentService;
import com.buurman.service.TenantAddressService;
import com.buurman.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "Tenant management")
@SecurityRequirement(name = "bearer-jwt")
public class TenantController {

    private final TenantService tenantService;
    private final TenantAddressService addressService;
    private final DocumentService documentService;
    private final AuditService auditService;

    public TenantController(TenantService tenantService, TenantAddressService addressService,
                           DocumentService documentService, AuditService auditService) {
        this.tenantService = tenantService;
        this.addressService = addressService;
        this.documentService = documentService;
        this.auditService = auditService;
    }

    @Operation(summary = "Create tenant", description = "Create a new tenant (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
    public TenantResponse updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.updateTenant(id, request, principal);
    }

    @Operation(summary = "Delete tenant", description = "Soft delete a tenant (Admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenant(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tenantService.deleteTenant(id, principal);
    }

    @Operation(summary = "Link tenant to property", description = "Assign tenant to a property (Admin/Editor)")
    @PostMapping("/{id}/link-property")
    public TenantResponse linkTenantToProperty(
            @PathVariable UUID id,
            @Valid @RequestBody LinkTenantToPropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tenantService.linkTenantToProperty(id, request, principal);
    }

    @Operation(summary = "Unlink tenant from property", description = "Remove tenant from current property (Admin/Editor)")
    @PostMapping("/{id}/unlink-property")
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

    @Operation(summary = "Get audit log", description = "Get audit history for a tenant")
    @GetMapping("/{id}/audit-log")
    public List<RecentActivityResponse> getTenantAuditLog(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return auditService.getEntityAuditLog(principal.getTeamId(), "TENANT", id);
    }

    @Operation(summary = "Upload document", description = "Upload a document for a tenant (Admin/Editor)")
    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.uploadDocument(file, "TENANT", id, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for a tenant")
    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.getDocuments("TENANT", id, principal);
    }

    @Operation(summary = "Get download URL", description = "Get presigned download URL for a document")
    @GetMapping("/documents/{documentId}/download")
    public Map<String, String> getDownloadUrl(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        URL url = documentService.getDownloadUrl(documentId, principal);
        return Map.of("url", url.toString());
    }

    @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.deleteDocument(documentId, principal);
    }

    @Operation(summary = "List photos", description = "Get all photos for a tenant")
    @GetMapping("/{id}/photos")
    public List<DocumentResponse> getPhotos(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.getPhotos("TENANT", id, principal);
    }

    @Operation(summary = "Upload photo", description = "Upload a photo for a tenant (Admin/Editor)")
    @PostMapping("/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.uploadDocument(file, "TENANT", id, title, notes, principal);
    }

    @Operation(summary = "Set main photo", description = "Set a photo as the main photo for a tenant (Admin/Editor)")
    @PutMapping("/{id}/photos/{photoId}/set-main")
    public DocumentResponse setMainPhoto(
            @PathVariable UUID id,
            @PathVariable UUID photoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.setMainPhoto(photoId, "TENANT", id, principal);
    }

    @Operation(summary = "Create address", description = "Create a new address for a tenant (Admin/Editor)")
    @PostMapping("/{tenantId}/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantAddressResponse createAddress(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateTenantAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return addressService.createAddress(tenantId, request, principal);
    }

    @Operation(summary = "List addresses", description = "Get all addresses for a tenant")
    @GetMapping("/{tenantId}/addresses")
    public List<TenantAddressResponse> getAddresses(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return addressService.getAddresses(tenantId, principal);
    }

    @Operation(summary = "Get address", description = "Get a specific address by ID")
    @GetMapping("/{tenantId}/addresses/{addressId}")
    public TenantAddressResponse getAddress(
            @PathVariable UUID tenantId,
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return addressService.getAddress(addressId, principal);
    }

    @Operation(summary = "Update address", description = "Update an existing address (Admin/Editor)")
    @PutMapping("/{tenantId}/addresses/{addressId}")
    public TenantAddressResponse updateAddress(
            @PathVariable UUID tenantId,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateTenantAddressRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return addressService.updateAddress(addressId, request, principal);
    }

    @Operation(summary = "Delete address", description = "Soft delete an address (Admin/Editor)")
    @DeleteMapping("/{tenantId}/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
            @PathVariable UUID tenantId,
            @PathVariable UUID addressId,
            @AuthenticationPrincipal UserPrincipal principal) {
        addressService.deleteAddress(addressId, principal);
    }
}
