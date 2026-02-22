package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.CreateTenantAddressRequest;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.LinkTenantToPropertyRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateTenantAddressRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyTenantHistoryResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantAddressResponse;
import com.buurman.dto.response.TenantResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tenants")
@Tag(name = "Tenants", description = "Tenant management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class TenantController {

  private final TenantService tenantService;

  @Operation(summary = "Create tenant", description = "Create a new tenant (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public TenantResponse createTenant(
      @Valid @RequestBody CreateTenantRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.createTenant(request, principal);
  }

  @Operation(
      summary = "List tenants",
      description = "Get all tenants with optional search and pagination")
  @GetMapping
  public PageResponse<TenantResponse> getTenants(
      @RequestParam(required = false) @Nullable String search,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam(required = false) @Nullable String sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return tenantService.getTenantsPaginated(principal, search, pageRequest);
  }

  @Operation(summary = "Get tenant details", description = "Get details of a specific tenant")
  @GetMapping("/{identifier}")
  public TenantResponse getTenant(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getTenant(identifier, principal);
  }

  @Operation(summary = "Update tenant", description = "Update tenant information (Admin/Editor)")
  @PutMapping("/{identifier}")
  public TenantResponse updateTenant(
      @PathVariable String identifier,
      @Valid @RequestBody UpdateTenantRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.updateTenant(identifier, request, principal);
  }

  @Operation(summary = "Delete tenant", description = "Soft delete a tenant (Admin only)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteTenant(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    tenantService.deleteTenant(identifier, principal);
  }

  @Operation(
      summary = "Link tenant to property",
      description = "Assign tenant to a property (Admin/Editor)")
  @PostMapping("/{identifier}/link-property")
  public TenantResponse linkTenantToProperty(
      @PathVariable String identifier,
      @Valid @RequestBody LinkTenantToPropertyRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.linkTenantToProperty(identifier, request, principal);
  }

  @Operation(
      summary = "Unlink tenant from property",
      description = "Remove tenant from current property (Admin/Editor)")
  @PostMapping("/{identifier}/unlink-property")
  public TenantResponse unlinkTenantFromProperty(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.unlinkTenantFromProperty(identifier, principal);
  }

  @Operation(
      summary = "Get tenant history",
      description = "Get property assignment history for a tenant")
  @GetMapping("/{identifier}/history")
  public List<PropertyTenantHistoryResponse> getTenantHistory(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getTenantHistory(identifier, principal);
  }

  @Operation(summary = "Get audit log", description = "Get audit history for a tenant")
  @GetMapping("/{identifier}/audit-log")
  public List<RecentActivityResponse> getTenantAuditLog(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getAuditLog(identifier, principal);
  }

  @Operation(
      summary = "Upload document",
      description = "Upload a document for a tenant (Admin/Editor)")
  @PostMapping("/{identifier}/documents")
  @ResponseStatus(CREATED)
  public DocumentResponse uploadDocument(
      @PathVariable String identifier,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) @Nullable String title,
      @RequestParam(required = false) @Nullable String notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.uploadDocument(identifier, file, title, notes, principal);
  }

  @Operation(summary = "List documents", description = "Get all documents for a tenant")
  @GetMapping("/{identifier}/documents")
  public List<DocumentResponse> getDocuments(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getDocuments(identifier, principal);
  }

  @Operation(
      summary = "Get download URL",
      description = "Get presigned download URL for a document")
  @GetMapping("/documents/{documentIdentifier}/download")
  public Map<String, String> getDownloadUrl(
      @PathVariable String documentIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getDownloadUrl(documentIdentifier, principal);
  }

  @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
  @DeleteMapping("/documents/{documentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDocument(
      @PathVariable String documentIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    tenantService.deleteDocument(documentIdentifier, principal);
  }

  @Operation(summary = "List photos", description = "Get all photos for a tenant")
  @GetMapping("/{identifier}/photos")
  public List<PhotoResponse> getPhotos(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getPhotos(identifier, principal);
  }

  @Operation(summary = "Upload photo", description = "Upload a photo for a tenant (Admin/Editor)")
  @PostMapping("/{identifier}/photos")
  @ResponseStatus(CREATED)
  public PhotoResponse uploadPhoto(
      @PathVariable String identifier,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) @Nullable String title,
      @RequestParam(required = false) @Nullable String notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.uploadPhoto(identifier, file, title, notes, principal);
  }

  @Operation(
      summary = "Set main photo",
      description = "Set a photo as the main photo for a tenant (Admin/Editor)")
  @PutMapping("/{identifier}/photos/{photoIdentifier}/set-main")
  public PhotoResponse setMainPhoto(
      @PathVariable String identifier,
      @PathVariable String photoIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.setMainPhoto(identifier, photoIdentifier, principal);
  }

  @Operation(
      summary = "Create address",
      description = "Create a new address for a tenant (Admin/Editor)")
  @PostMapping("/{tenantIdentifier}/addresses")
  @ResponseStatus(CREATED)
  public TenantAddressResponse createAddress(
      @PathVariable String tenantIdentifier,
      @Valid @RequestBody CreateTenantAddressRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.createAddress(tenantIdentifier, request, principal);
  }

  @Operation(summary = "List addresses", description = "Get all addresses for a tenant")
  @GetMapping("/{tenantIdentifier}/addresses")
  public List<TenantAddressResponse> getAddresses(
      @PathVariable String tenantIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getAddresses(tenantIdentifier, principal);
  }

  @Operation(summary = "Get address", description = "Get a specific address by identifier")
  @GetMapping("/{tenantIdentifier}/addresses/{addressIdentifier}")
  public TenantAddressResponse getAddress(
      @PathVariable String tenantIdentifier,
      @PathVariable String addressIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.getAddress(tenantIdentifier, addressIdentifier, principal);
  }

  @Operation(summary = "Update address", description = "Update an existing address (Admin/Editor)")
  @PutMapping("/{tenantIdentifier}/addresses/{addressIdentifier}")
  public TenantAddressResponse updateAddress(
      @PathVariable String tenantIdentifier,
      @PathVariable String addressIdentifier,
      @Valid @RequestBody UpdateTenantAddressRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return tenantService.updateAddress(tenantIdentifier, addressIdentifier, request, principal);
  }

  @Operation(summary = "Delete address", description = "Soft delete an address (Admin/Editor)")
  @DeleteMapping("/{tenantIdentifier}/addresses/{addressIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteAddress(
      @PathVariable String tenantIdentifier,
      @PathVariable String addressIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    tenantService.deleteAddress(tenantIdentifier, addressIdentifier, principal);
  }
}
