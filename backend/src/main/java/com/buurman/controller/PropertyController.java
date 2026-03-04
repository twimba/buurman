package com.buurman.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.api.PropertiesApi;
import com.buurman.generated.model.UploadPhotoRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyController implements PropertiesApi {

  private final PropertyService propertyService;

  @Override
  public PropertyResponse createProperty(CreatePropertyRequest createPropertyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.createProperty(createPropertyRequest, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getProperties(
      String status,
      String category,
      String query,
      Integer page,
      Integer size,
      String sort,
      String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return propertyService.getPropertiesPaginated(principal, status, category, query, pageRequest);
  }

  @Override
  public PropertyResponse getProperty(PropertyIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getProperty(identifier, principal);
  }

  @Override
  public PropertyResponse updateProperty(
      PropertyIdentifier identifier, UpdatePropertyRequest updatePropertyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.updateProperty(identifier, updatePropertyRequest, principal);
  }

  @Override
  public void deleteProperty(PropertyIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    propertyService.deleteProperty(identifier, principal);
  }

  @Override
  @SuppressWarnings(
      "NullAway") // Generated interface uses UploadPhotoRequest instead of MultipartFile
  public DocumentResponse uploadPropertyDocument(
      PropertyIdentifier identifier,
      String title,
      String notes,
      UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.uploadDocument(identifier, null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getPropertyDocuments(PropertyIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getPropertyDocumentDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getDownloadUrl(documentIdentifier, principal);
  }

  @Override
  public void deletePropertyDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    propertyService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getPropertyAuditLog(PropertyIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getAuditLog(identifier, principal);
  }

  @Override
  public List<PhotoResponse> getPropertyPhotos(PropertyIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getPhotos(identifier, principal);
  }

  @Override
  @SuppressWarnings(
      "NullAway") // Generated interface uses UploadPhotoRequest instead of MultipartFile
  public PhotoResponse uploadPropertyPhoto(
      PropertyIdentifier identifier,
      String title,
      String notes,
      UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.uploadPhoto(identifier, null, title, notes, principal);
  }

  @Override
  public PhotoResponse setMainPropertyPhoto(
      PropertyIdentifier identifier, PhotoIdentifier photoIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.setMainPhoto(identifier, photoIdentifier, principal);
  }
}
