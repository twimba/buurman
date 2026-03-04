package com.buurman.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.domain.identifier.TenantAddressIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;
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
import com.buurman.generated.api.TenantsApi;
import com.buurman.generated.model.UploadPhotoRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.TenantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TenantController implements TenantsApi {

  private final TenantService tenantService;

  @Override
  public TenantResponse createTenant(CreateTenantRequest createTenantRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.createTenant(createTenantRequest, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getTenants(
      String search, Integer page, Integer size, String sort, String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return tenantService.getTenantsPaginated(principal, search, pageRequest);
  }

  @Override
  public TenantResponse getTenant(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getTenant(TenantIdentifier.of(identifier), principal);
  }

  @Override
  public TenantResponse updateTenant(String identifier, UpdateTenantRequest updateTenantRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.updateTenant(TenantIdentifier.of(identifier), updateTenantRequest, principal);
  }

  @Override
  public void deleteTenant(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    tenantService.deleteTenant(TenantIdentifier.of(identifier), principal);
  }

  @Override
  public TenantResponse linkTenantToProperty(
      String identifier, LinkTenantToPropertyRequest linkTenantToPropertyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.linkTenantToProperty(TenantIdentifier.of(identifier), linkTenantToPropertyRequest, principal);
  }

  @Override
  public TenantResponse unlinkTenantFromProperty(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.unlinkTenantFromProperty(TenantIdentifier.of(identifier), principal);
  }

  @Override
  public List<PropertyTenantHistoryResponse> getTenantHistory(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getTenantHistory(TenantIdentifier.of(identifier), principal);
  }

  @Override
  public List<RecentActivityResponse> getTenantAuditLog(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getAuditLog(TenantIdentifier.of(identifier), principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public DocumentResponse uploadDocument(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return tenantService.uploadDocument(TenantIdentifier.of(identifier), null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getDocuments(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getDocuments(TenantIdentifier.of(identifier), principal);
  }

  @Override
  public Map<String, String> getDownloadUrl(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getDownloadUrl(DocumentIdentifier.of(documentIdentifier), principal);
  }

  @Override
  public void deleteTenantDocument(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    tenantService.deleteDocument(DocumentIdentifier.of(documentIdentifier), principal);
  }

  @Override
  public List<PhotoResponse> getPhotos(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getPhotos(TenantIdentifier.of(identifier), principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public PhotoResponse uploadPhoto(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return tenantService.uploadPhoto(TenantIdentifier.of(identifier), null, title, notes, principal);
  }

  @Override
  public PhotoResponse setMainPhoto(String identifier, String photoIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.setMainPhoto(TenantIdentifier.of(identifier), PhotoIdentifier.of(photoIdentifier), principal);
  }

  @Override
  public TenantAddressResponse createAddress(
      String tenantIdentifier, CreateTenantAddressRequest createTenantAddressRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.createAddress(TenantIdentifier.of(tenantIdentifier), createTenantAddressRequest, principal);
  }

  @Override
  public List<TenantAddressResponse> getAddresses(String tenantIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getAddresses(TenantIdentifier.of(tenantIdentifier), principal);
  }

  @Override
  public TenantAddressResponse getAddress(String tenantIdentifier, String addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getAddress(TenantIdentifier.of(tenantIdentifier), TenantAddressIdentifier.of(addressIdentifier), principal);
  }

  @Override
  public TenantAddressResponse updateAddress(
      String tenantIdentifier,
      String addressIdentifier,
      UpdateTenantAddressRequest updateTenantAddressRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.updateAddress(
        TenantIdentifier.of(tenantIdentifier), TenantAddressIdentifier.of(addressIdentifier), updateTenantAddressRequest, principal);
  }

  @Override
  public void deleteAddress(String tenantIdentifier, String addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    tenantService.deleteAddress(TenantIdentifier.of(tenantIdentifier), TenantAddressIdentifier.of(addressIdentifier), principal);
  }
}
