package com.buurman.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.identifier.ContactAddressIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.dto.request.CreateContactAddressRequest;
import com.buurman.dto.request.CreateContactRequest;
import com.buurman.dto.request.LinkContactToPropertyRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContactAddressRequest;
import com.buurman.dto.request.UpdateContactRequest;
import com.buurman.dto.response.ContactAddressResponse;
import com.buurman.dto.response.ContactResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyContactHistoryResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.api.TenantsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContactService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContactController implements TenantsApi {

  private final ContactService contactService;

  @Override
  public ContactResponse createTenant(CreateContactRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.createContact(request, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getTenants(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return contactService.getContactsPaginated(principal, search.orElse(null), pageRequest);
  }

  @Override
  public ContactResponse getTenant(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getContact(identifier, principal);
  }

  @Override
  public ContactResponse updateTenant(
      ContactIdentifier identifier, UpdateContactRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.updateContact(identifier, request, principal);
  }

  @Override
  public void deleteTenant(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.deleteContact(identifier, principal);
  }

  @Override
  public List<PropertyContactHistoryResponse> getTenantHistory(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getContactHistory(identifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getTenantAuditLog(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getAuditLog(identifier, principal);
  }

  @Override
  public DocumentResponse uploadDocument(
      ContactIdentifier identifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.uploadDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public List<DocumentResponse> getDocuments(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getDownloadUrl(documentIdentifier, principal);
  }

  @Override
  public void deleteTenantDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<PhotoResponse> getPhotos(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getPhotos(identifier, principal);
  }

  @Override
  public PhotoResponse uploadPhoto(
      ContactIdentifier identifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.uploadPhoto(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public PhotoResponse setMainPhoto(
      ContactIdentifier identifier, PhotoIdentifier photoIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.setMainPhoto(identifier, photoIdentifier, principal);
  }

  @Override
  public ContactAddressResponse createAddress(
      ContactIdentifier contactIdentifier, CreateContactAddressRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.createAddress(contactIdentifier, request, principal);
  }

  @Override
  public List<ContactAddressResponse> getAddresses(ContactIdentifier contactIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getAddresses(contactIdentifier, principal);
  }

  @Override
  public ContactAddressResponse getAddress(
      ContactIdentifier contactIdentifier, ContactAddressIdentifier addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getAddress(contactIdentifier, addressIdentifier, principal);
  }

  @Override
  public ContactAddressResponse updateAddress(
      ContactIdentifier contactIdentifier,
      ContactAddressIdentifier addressIdentifier,
      UpdateContactAddressRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.updateAddress(
        contactIdentifier, addressIdentifier, request, principal);
  }

  @Override
  public void deleteAddress(
      ContactIdentifier contactIdentifier, ContactAddressIdentifier addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.deleteAddress(contactIdentifier, addressIdentifier, principal);
  }

  /**
   * Removed in BUUR-77 contacts rework. Will be deleted from the OpenAPI spec in Phase 4a.
   *
   * @deprecated Use contract parties to associate contacts with properties.
   */
  @Deprecated(forRemoval = true)
  @Override
  public ContactResponse linkTenantToProperty(
      ContactIdentifier identifier, LinkContactToPropertyRequest request) {
    throw new UnsupportedOperationException(
        "linkTenantToProperty has been removed. Use contract parties instead.");
  }

  /**
   * Removed in BUUR-77 contacts rework. Will be deleted from the OpenAPI spec in Phase 4a.
   *
   * @deprecated Use contract parties to associate contacts with properties.
   */
  @Deprecated(forRemoval = true)
  @Override
  public ContactResponse unlinkTenantFromProperty(ContactIdentifier identifier) {
    throw new UnsupportedOperationException(
        "unlinkTenantFromProperty has been removed. Use contract parties instead.");
  }
}
