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
  public TenantResponse getTenant(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getTenant(identifier, principal);
  }

  @Override
  public TenantResponse updateTenant(
      TenantIdentifier identifier, UpdateTenantRequest updateTenantRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.updateTenant(identifier, updateTenantRequest, principal);
  }

  @Override
  public void deleteTenant(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    tenantService.deleteTenant(identifier, principal);
  }

  @Override
  public TenantResponse linkTenantToProperty(
      TenantIdentifier identifier, LinkTenantToPropertyRequest linkTenantToPropertyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.linkTenantToProperty(identifier, linkTenantToPropertyRequest, principal);
  }

  @Override
  public TenantResponse unlinkTenantFromProperty(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.unlinkTenantFromProperty(identifier, principal);
  }

  @Override
  public List<PropertyTenantHistoryResponse> getTenantHistory(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getTenantHistory(identifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getTenantAuditLog(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getAuditLog(identifier, principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public DocumentResponse uploadDocument(
      TenantIdentifier identifier,
      String title,
      String notes,
      UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return tenantService.uploadDocument(identifier, null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getDocuments(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getDownloadUrl(documentIdentifier, principal);
  }

  @Override
  public void deleteTenantDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    tenantService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<PhotoResponse> getPhotos(TenantIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getPhotos(identifier, principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public PhotoResponse uploadPhoto(
      TenantIdentifier identifier,
      String title,
      String notes,
      UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return tenantService.uploadPhoto(identifier, null, title, notes, principal);
  }

  @Override
  public PhotoResponse setMainPhoto(TenantIdentifier identifier, PhotoIdentifier photoIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.setMainPhoto(identifier, photoIdentifier, principal);
  }

  @Override
  public TenantAddressResponse createAddress(
      TenantIdentifier tenantIdentifier, CreateTenantAddressRequest createTenantAddressRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.createAddress(tenantIdentifier, createTenantAddressRequest, principal);
  }

  @Override
  public List<TenantAddressResponse> getAddresses(TenantIdentifier tenantIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getAddresses(tenantIdentifier, principal);
  }

  @Override
  public TenantAddressResponse getAddress(
      TenantIdentifier tenantIdentifier, TenantAddressIdentifier addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.getAddress(tenantIdentifier, addressIdentifier, principal);
  }

  @Override
  public TenantAddressResponse updateAddress(
      TenantIdentifier tenantIdentifier,
      TenantAddressIdentifier addressIdentifier,
      UpdateTenantAddressRequest updateTenantAddressRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return tenantService.updateAddress(
        tenantIdentifier, addressIdentifier, updateTenantAddressRequest, principal);
  }

  @Override
  public void deleteAddress(
      TenantIdentifier tenantIdentifier, TenantAddressIdentifier addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    tenantService.deleteAddress(tenantIdentifier, addressIdentifier, principal);
  }
}
