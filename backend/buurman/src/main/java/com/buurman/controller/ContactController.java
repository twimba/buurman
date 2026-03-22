package com.buurman.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.identifier.ContactAddressIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContactNoteIdentifier;
import com.buurman.domain.identifier.ContactRelationshipIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.dto.request.AddContactTagRequest;
import com.buurman.dto.request.CreateContactAddressRequest;
import com.buurman.dto.request.CreateContactNoteRequest;
import com.buurman.dto.request.CreateContactRelationshipRequest;
import com.buurman.dto.request.CreateContactRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContactAddressRequest;
import com.buurman.dto.request.UpdateContactNoteRequest;
import com.buurman.dto.request.UpdateContactRelationshipRequest;
import com.buurman.dto.request.UpdateContactRequest;
import com.buurman.dto.response.ContactAddressResponse;
import com.buurman.dto.response.ContactNoteResponse;
import com.buurman.dto.response.ContactRelationshipResponse;
import com.buurman.dto.response.ContactListItemResponse;
import com.buurman.dto.response.ContactResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.DuplicateCheckResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyContactHistoryResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.api.ContactsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContactNoteService;
import com.buurman.service.ContactRelationshipService;
import com.buurman.service.ContactService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContactController implements ContactsApi {

  private final ContactService contactService;
  private final ContactNoteService contactNoteService;
  private final ContactRelationshipService contactRelationshipService;

  @Override
  public ContactResponse createContact(CreateContactRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.createContact(request, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getContacts(
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
  public ContactResponse getContact(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getContact(identifier, principal);
  }

  @Override
  public ContactResponse updateContact(
      ContactIdentifier identifier, UpdateContactRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.updateContact(identifier, request, principal);
  }

  @Override
  public void deleteContact(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.deleteContact(identifier, principal);
  }

  @Override
  public List<PropertyContactHistoryResponse> getContactHistory(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getContactHistory(identifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getContactAuditLog(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getAuditLog(identifier, principal);
  }

  // --- Documents ---

  @Override
  public DocumentResponse uploadContactDocument(
      ContactIdentifier identifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.uploadDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public List<DocumentResponse> getContactDocuments(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getContactDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getDownloadUrl(documentIdentifier, principal);
  }

  @Override
  public void deleteContactDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.deleteDocument(documentIdentifier, principal);
  }

  // --- Photos ---

  @Override
  public List<PhotoResponse> getContactPhotos(ContactIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getPhotos(identifier, principal);
  }

  @Override
  public PhotoResponse uploadContactPhoto(
      ContactIdentifier identifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.uploadPhoto(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public PhotoResponse setContactMainPhoto(
      ContactIdentifier identifier, PhotoIdentifier photoIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.setMainPhoto(identifier, photoIdentifier, principal);
  }

  // --- Addresses ---

  @Override
  public ContactAddressResponse createContactAddress(
      ContactIdentifier contactIdentifier, CreateContactAddressRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.createAddress(contactIdentifier, request, principal);
  }

  @Override
  public List<ContactAddressResponse> getContactAddresses(ContactIdentifier contactIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getAddresses(contactIdentifier, principal);
  }

  @Override
  public ContactAddressResponse getContactAddress(
      ContactIdentifier contactIdentifier, ContactAddressIdentifier addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.getAddress(contactIdentifier, addressIdentifier, principal);
  }

  @Override
  public ContactAddressResponse updateContactAddress(
      ContactIdentifier contactIdentifier,
      ContactAddressIdentifier addressIdentifier,
      UpdateContactAddressRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.updateAddress(
        contactIdentifier, addressIdentifier, request, principal);
  }

  @Override
  public void deleteContactAddress(
      ContactIdentifier contactIdentifier, ContactAddressIdentifier addressIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.deleteAddress(contactIdentifier, addressIdentifier, principal);
  }

  // --- Notes ---

  @Override
  public ContactNoteResponse createContactNote(
      ContactIdentifier contactIdentifier, CreateContactNoteRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactNoteService.createNote(contactIdentifier, request, principal);
  }

  @Override
  public List<ContactNoteResponse> getContactNotes(ContactIdentifier contactIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactNoteService.getNotes(contactIdentifier, principal);
  }

  @Override
  public ContactNoteResponse updateContactNote(
      ContactIdentifier contactIdentifier,
      ContactNoteIdentifier noteIdentifier,
      UpdateContactNoteRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactNoteService.updateNote(contactIdentifier, noteIdentifier, request, principal);
  }

  @Override
  public void deleteContactNote(
      ContactIdentifier contactIdentifier, ContactNoteIdentifier noteIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactNoteService.deleteNote(contactIdentifier, noteIdentifier, principal);
  }

  @Override
  public ContactNoteResponse pinContactNote(
      ContactIdentifier contactIdentifier, ContactNoteIdentifier noteIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactNoteService.pinNote(contactIdentifier, noteIdentifier, principal);
  }

  @Override
  public ContactNoteResponse unpinContactNote(
      ContactIdentifier contactIdentifier, ContactNoteIdentifier noteIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactNoteService.unpinNote(contactIdentifier, noteIdentifier, principal);
  }

  // --- Relationships ---

  @Override
  public ContactRelationshipResponse createContactRelationship(
      ContactIdentifier contactIdentifier, CreateContactRelationshipRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactRelationshipService.createRelationship(contactIdentifier, request, principal);
  }

  @Override
  public List<ContactRelationshipResponse> getContactRelationships(
      ContactIdentifier contactIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactRelationshipService.getRelationships(contactIdentifier, principal);
  }

  @Override
  public ContactRelationshipResponse updateContactRelationship(
      ContactIdentifier contactIdentifier,
      ContactRelationshipIdentifier relationshipIdentifier,
      UpdateContactRelationshipRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactRelationshipService.updateRelationship(
        contactIdentifier, relationshipIdentifier, request, principal);
  }

  @Override
  public void deleteContactRelationship(
      ContactIdentifier contactIdentifier,
      ContactRelationshipIdentifier relationshipIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactRelationshipService.deleteRelationship(
        contactIdentifier, relationshipIdentifier, principal);
  }

  // --- Tags ---

  @Override
  public ContactResponse addContactTag(
      ContactIdentifier contactIdentifier, AddContactTagRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.addTag(contactIdentifier, request, principal);
  }

  @Override
  public ContactResponse removeContactTag(ContactIdentifier contactIdentifier, String tag) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.removeTag(contactIdentifier, tag, principal);
  }

  // --- Duplicates ---

  @Override
  public DuplicateCheckResponse checkContactDuplicates(CreateContactRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contactService.checkDuplicates(request, principal);
  }

  // --- Activity ---

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getContactActivity(
      ContactIdentifier contactIdentifier,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return contactService.getActivity(contactIdentifier, principal, pageRequest);
  }

  // --- GDPR Erase ---

  @Override
  public void eraseContactData(ContactIdentifier contactIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contactService.eraseContactData(contactIdentifier, principal);
  }
}
