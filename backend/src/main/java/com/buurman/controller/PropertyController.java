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
  public PropertyResponse getProperty(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getProperty(PropertyIdentifier.of(identifier), principal);
  }

  @Override
  public PropertyResponse updateProperty(
      String identifier, UpdatePropertyRequest updatePropertyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.updateProperty(PropertyIdentifier.of(identifier), updatePropertyRequest, principal);
  }

  @Override
  public void deleteProperty(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    propertyService.deleteProperty(PropertyIdentifier.of(identifier), principal);
  }

  @Override
  @SuppressWarnings(
      "NullAway") // Generated interface uses UploadPhotoRequest instead of MultipartFile
  public DocumentResponse uploadPropertyDocument(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.uploadDocument(PropertyIdentifier.of(identifier), null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getPropertyDocuments(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getDocuments(PropertyIdentifier.of(identifier), principal);
  }

  @Override
  public Map<String, String> getPropertyDocumentDownloadUrl(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getDownloadUrl(DocumentIdentifier.of(documentIdentifier), principal);
  }

  @Override
  public void deletePropertyDocument(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    propertyService.deleteDocument(DocumentIdentifier.of(documentIdentifier), principal);
  }

  @Override
  public List<RecentActivityResponse> getPropertyAuditLog(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getAuditLog(PropertyIdentifier.of(identifier), principal);
  }

  @Override
  public List<PhotoResponse> getPropertyPhotos(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.getPhotos(PropertyIdentifier.of(identifier), principal);
  }

  @Override
  @SuppressWarnings(
      "NullAway") // Generated interface uses UploadPhotoRequest instead of MultipartFile
  public PhotoResponse uploadPropertyPhoto(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.uploadPhoto(PropertyIdentifier.of(identifier), null, title, notes, principal);
  }

  @Override
  public PhotoResponse setMainPropertyPhoto(String identifier, String photoIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyService.setMainPhoto(PropertyIdentifier.of(identifier), PhotoIdentifier.of(photoIdentifier), principal);
  }
}
