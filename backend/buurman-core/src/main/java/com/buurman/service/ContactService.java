package com.buurman.service;

import static com.buurman.util.SidGenerator.newContactId;

import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.DataRetentionStatus;
import com.buurman.domain.Photo;
import com.buurman.domain.Property;
import com.buurman.domain.User;
import com.buurman.domain.identifier.ContactAddressIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PhotoIdentifier;
import com.buurman.dto.request.AddContactTagRequest;
import com.buurman.dto.request.CreateContactAddressRequest;
import com.buurman.dto.request.CreateContactRequest;
import com.buurman.dto.request.DuplicateCheckRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContactAddressRequest;
import com.buurman.dto.request.UpdateContactRequest;
import com.buurman.dto.response.ContactActivityItem;
import com.buurman.dto.response.ContactAddressResponse;
import com.buurman.dto.response.ContactListItemResponse;
import com.buurman.dto.response.ContactPropertyAssignment;
import com.buurman.dto.response.ContactResponse;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.DuplicateCheckResponse;
import com.buurman.dto.response.DuplicateMatch;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyContactHistoryResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.mapper.ContactMapper;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactNoteRepository;
import com.buurman.repository.ContactRelationshipRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContactRepository.ContactWithCount;
import com.buurman.repository.ContactTagRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyContactHistoryRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactService {

  private final ContactRepository contactRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyContactHistoryRepository historyRepository;
  private final ContactMapper contactMapper;
  private final AuditService auditService;
  private final UserRepository userRepository;
  private final DocumentService documentService;
  private final PhotoService photoService;
  private final PhotoRepository photoRepository;
  private final S3StorageService s3StorageService;
  private final ContractRepository contractRepository;
  private final ContractPartyRepository contractPartyRepository;
  private final ContactAddressService addressService;
  private final ContactAddressRepository addressRepository;
  private final DocumentRepository documentRepository;
  private final ContactTagRepository contactTagRepository;
  private final ContactNoteRepository contactNoteRepository;
  private final ContactRelationshipRepository contactRelationshipRepository;
  private final MetricsService metricsService;
  private final Clock clock;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactResponse createContact(CreateContactRequest request, UserPrincipal principal) {
    request
        .email()
        .filter(e -> !e.isBlank())
        .ifPresent(
            email ->
                contactRepository
                    .findByEmailAndTeamId(email, principal.requireTeamId())
                    .ifPresent(
                        existing -> {
                          throw new IllegalArgumentException(
                              "Contact with email " + email + " already exists");
                        }));

    validateContactRequest(request.contactType(), request.firstName(), request.companyName());

    Contact contact = contactMapper.toEntity(request);
    contact.setIdentifier(Optional.of(newContactId()));
    contact.setTeamId(principal.requireTeamId());
    contact.setDisplayName(computeDisplayName(request.contactType(), request));
    contact.setCreatedBy(principal.getUserId());
    contact.setUpdatedBy(principal.getUserId());

    Contact savedContact = contactRepository.save(contact);

    metricsService.incrementCounter("contact.total");

    log.info(
        "Contact created: {} for team {}",
        savedContact.getIdentifier().orElseThrow(),
        principal.requireTeamId());

    auditService.logCreate(
        principal.requireTeamId(),
        "CONTACT",
        savedContact.getId(),
        principal.getUserId(),
        savedContact);

    return toResponse(savedContact, principal.requireTeamId());
  }

  public List<ContactResponse> getAllContacts(UserPrincipal principal) {
    List<Contact> contacts = contactRepository.findAllByTeamId(principal.requireTeamId());
    return contacts.stream()
        .map(contact -> toResponse(contact, principal.requireTeamId()))
        .toList();
  }

  public List<ContactResponse> searchContacts(String searchTerm, UserPrincipal principal) {
    List<Contact> contacts =
        contactRepository.searchByTeamId(principal.requireTeamId(), searchTerm);
    return contacts.stream()
        .map(contact -> toResponse(contact, principal.requireTeamId()))
        .toList();
  }

  public PageResponse<ContactListItemResponse> getContactsPaginated(
      UserPrincipal principal,
      @Nullable String search,
      @Nullable ContactType contactType,
      @Nullable List<ContactTag> tags,
      PageRequest pageRequest) {
    UUID teamId = principal.requireTeamId();
    PaginatedResult<ContactWithCount> result =
        contactRepository.findAllByTeamIdPaginatedWithCounts(
            teamId, search, contactType, tags, pageRequest);

    // Batch-load tags for all contacts on the page
    List<UUID> contactIds = result.items().stream().map(cwc -> cwc.contact().getId()).toList();
    Map<UUID, List<ContactTag>> tagsByContactId =
        contactTagRepository.findByContactIdsGrouped(contactIds);

    // Batch-load main photo thumbnails for all contacts on the page
    Map<UUID, Optional<String>> thumbnailsByContactId =
        resolveMainPhotoThumbnails(contactIds, teamId);

    List<ContactListItemResponse> responses =
        result.items().stream()
            .map(
                cwc -> {
                  Contact contact = cwc.contact();
                  List<ContactTag> contactTags =
                      tagsByContactId.getOrDefault(contact.getId(), List.of());
                  Optional<String> thumbnailUrl =
                      thumbnailsByContactId.getOrDefault(contact.getId(), Optional.empty());
                  return contactMapper.toListItem(
                      contact, cwc.activeContractCount(), contactTags, thumbnailUrl);
                })
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  private Map<UUID, Optional<String>> resolveMainPhotoThumbnails(
      List<UUID> contactIds, UUID teamId) {
    if (contactIds.isEmpty()) {
      return Map.of();
    }
    List<Photo> allPhotos =
        photoRepository.findByEntityTypeAndEntityIdsAndTeamId("CONTACT", contactIds, teamId);

    // Group by entity, pick main photo per contact, resolve presigned URL
    Map<UUID, Optional<String>> result = new java.util.HashMap<>();
    Map<UUID, List<Photo>> photosByEntity = new java.util.HashMap<>();
    for (Photo photo : allPhotos) {
      photosByEntity.computeIfAbsent(photo.getEntityId(), k -> new ArrayList<>()).add(photo);
    }
    for (UUID contactId : contactIds) {
      List<Photo> photos = photosByEntity.getOrDefault(contactId, List.of());
      Optional<String> thumbnailUrl =
          photos.stream()
              .filter(Photo::getIsMainPhoto)
              .findFirst()
              .map(
                  photo -> {
                    String key = photo.getThumbnailFileKey().orElse(photo.getFileKey());
                    return s3StorageService.generatePresignedUrl(key).toString();
                  });
      result.put(contactId, thumbnailUrl);
    }
    return result;
  }

  public ContactResponse getContact(ContactIdentifier identifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return toResponse(contact, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactResponse updateContact(
      ContactIdentifier identifier, UpdateContactRequest request, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    request
        .email()
        .filter(e -> !e.isBlank() && contact.getEmail().filter(e::equals).isEmpty())
        .ifPresent(
            email ->
                contactRepository
                    .findByEmailAndTeamId(email, principal.requireTeamId())
                    .ifPresent(
                        existing -> {
                          if (!existing.getId().equals(contact.getId())) {
                            throw new IllegalArgumentException(
                                "Contact with email " + email + " already exists");
                          }
                        }));

    validateContactRequest(request.contactType(), request.firstName(), request.companyName());

    // Clear irrelevant fields when contact type changes
    UpdateContactRequest effectiveRequest = request;
    if (contact.getContactType() != request.contactType()) {
      effectiveRequest = clearFieldsForTypeChange(request);
    }

    Contact oldContact = cloneContact(contact);
    contactMapper.updateEntity(contact, effectiveRequest);
    contact.setDisplayName(computeDisplayName(effectiveRequest.contactType(), effectiveRequest));
    contact.setUpdatedBy(principal.getUserId());

    Contact updatedContact = contactRepository.save(contact);
    log.info(
        "Contact updated: {} for team {}",
        updatedContact.getIdentifier().orElseThrow(),
        principal.requireTeamId());

    auditService.logUpdate(
        principal.requireTeamId(),
        "CONTACT",
        updatedContact.getId(),
        principal.getUserId(),
        oldContact,
        updatedContact,
        auditService.getChangedFields(oldContact, updatedContact));

    return toResponse(updatedContact, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteContact(ContactIdentifier identifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    contactRepository.softDeleteByIdAndTeamId(contact.getId(), principal.requireTeamId());
    log.info("Contact deleted: {} for team {}", identifier, principal.requireTeamId());

    auditService.logDelete(
        principal.requireTeamId(), "CONTACT", contact.getId(), principal.getUserId(), contact);
  }

  public List<PropertyContactHistoryResponse> getContactHistory(
      ContactIdentifier identifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    List<com.buurman.domain.PropertyContactHistory> history =
        historyRepository.findByContactId(contact.getId(), principal.requireTeamId());
    return history.stream().map(h -> toHistoryResponse(h, principal.requireTeamId())).toList();
  }

  public List<RecentActivityResponse> getAuditLog(
      ContactIdentifier identifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return auditService.getEntityAuditLog(principal.requireTeamId(), "CONTACT", contact.getId());
  }

  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public PageResponse<ContactActivityItem> getActivity(
      ContactIdentifier identifier, UserPrincipal principal, PageRequest pageRequest) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(identifier, teamId);

    PaginatedResult<ContactActivityItem> result =
        contactRepository.findActivityByContactIdPaginated(contact.getId(), teamId, pageRequest);

    return PageResponse.of(
        result.items(), pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public DocumentResponse uploadDocument(
      ContactIdentifier identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return documentService.uploadDocument(
        file,
        "CONTACT",
        contact.getId(),
        contact.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public List<DocumentResponse> getDocuments(
      ContactIdentifier identifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return documentService.getDocuments("CONTACT", contact.getId(), principal);
  }

  public Map<String, String> getDownloadUrl(
      DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  public void deleteDocument(DocumentIdentifier documentIdentifier, UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }

  public List<PhotoResponse> getPhotos(ContactIdentifier identifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return photoService.getPhotos("CONTACT", contact.getId(), principal);
  }

  public PhotoResponse uploadPhoto(
      ContactIdentifier identifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return photoService.uploadPhoto(
        file,
        "CONTACT",
        contact.getId(),
        contact.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public PhotoResponse setMainPhoto(
      ContactIdentifier identifier, PhotoIdentifier photoIdentifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    Photo photo =
        photoRepository.getByIdentifierAndTeamId(photoIdentifier, principal.requireTeamId());
    return photoService.setMainPhoto(photo.getId(), "CONTACT", contact.getId(), principal);
  }

  public ContactAddressResponse createAddress(
      ContactIdentifier contactIdentifier,
      CreateContactAddressRequest request,
      UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(contactIdentifier, principal.requireTeamId());
    return addressService.createAddress(contact.getId(), request, principal);
  }

  public List<ContactAddressResponse> getAddresses(
      ContactIdentifier contactIdentifier, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(contactIdentifier, principal.requireTeamId());
    return addressService.getAddresses(contact.getId(), principal);
  }

  public ContactAddressResponse getAddress(
      ContactIdentifier contactIdentifier,
      ContactAddressIdentifier addressIdentifier,
      UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(contactIdentifier, principal.requireTeamId());
    ContactAddress address =
        addressRepository.getByIdentifierAndTeamId(addressIdentifier, principal.requireTeamId());
    return addressService.getAddress(contact.getId(), address.getId(), principal);
  }

  public ContactAddressResponse updateAddress(
      ContactIdentifier contactIdentifier,
      ContactAddressIdentifier addressIdentifier,
      UpdateContactAddressRequest request,
      UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(contactIdentifier, principal.requireTeamId());
    ContactAddress address =
        addressRepository.getByIdentifierAndTeamId(addressIdentifier, principal.requireTeamId());
    return addressService.updateAddress(contact.getId(), address.getId(), request, principal);
  }

  public void deleteAddress(
      ContactIdentifier contactIdentifier,
      ContactAddressIdentifier addressIdentifier,
      UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(contactIdentifier, principal.requireTeamId());
    ContactAddress address =
        addressRepository.getByIdentifierAndTeamId(addressIdentifier, principal.requireTeamId());
    addressService.deleteAddress(contact.getId(), address.getId(), principal);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_EDITOR')")
  public ContactResponse addTag(
      ContactIdentifier identifier, AddContactTagRequest request, UserPrincipal principal) {
    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    contactTagRepository.addTag(
        contact.getId(), principal.requireTeamId(), request.tag(), principal.getUserId());

    log.info(
        "Tag {} added to contact {} for team {}",
        request.tag(),
        identifier,
        principal.requireTeamId());

    auditService.logUpdate(
        principal.requireTeamId(),
        "CONTACT",
        contact.getId(),
        principal.getUserId(),
        null,
        Map.of("action", "ADD_TAG", "tag", request.tag().name()),
        Map.of("tags", request.tag().name()));

    return toResponse(contact, principal.requireTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_EDITOR')")
  public ContactResponse removeTag(
      ContactIdentifier identifier, String tag, UserPrincipal principal) {
    ContactTag contactTag;
    try {
      contactTag = ContactTag.valueOf(tag);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid tag: " + tag);
    }

    Contact contact =
        contactRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    contactTagRepository.removeTag(contact.getId(), contactTag);

    log.info(
        "Tag {} removed from contact {} for team {}",
        contactTag,
        identifier,
        principal.requireTeamId());

    auditService.logUpdate(
        principal.requireTeamId(),
        "CONTACT",
        contact.getId(),
        principal.getUserId(),
        null,
        Map.of("action", "REMOVE_TAG", "tag", contactTag.name()),
        Map.of("tags", contactTag.name()));

    return toResponse(contact, principal.requireTeamId());
  }

  @PreAuthorize("hasRole('TEAM_VIEWER')")
  public DuplicateCheckResponse checkDuplicates(
      DuplicateCheckRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    List<DuplicateMatch> matches = new ArrayList<>();

    request
        .email()
        .filter(e -> !e.isBlank())
        .ifPresent(
            email ->
                contactRepository
                    .findDuplicatesByEmail(email, teamId)
                    .forEach(
                        c -> {
                          ContactSummary summary = contactMapper.toSummary(c);
                          matches.add(new DuplicateMatch(summary, "email", "exact"));
                        }));

    @Nullable String firstName = request.firstName().orElse(null);
    @Nullable String lastName = request.lastName().orElse(null);
    @Nullable String companyName = request.companyName().orElse(null);

    contactRepository
        .findDuplicatesByName(firstName, lastName, companyName, teamId)
        .forEach(
            c -> {
              ContactSummary summary = contactMapper.toSummary(c);
              String matchField =
                  (companyName != null
                          && !companyName.isBlank()
                          && c.getCompanyName()
                              .filter(cn -> cn.equalsIgnoreCase(companyName))
                              .isPresent())
                      ? "companyName"
                      : "name";
              matches.add(new DuplicateMatch(summary, matchField, "exact"));
            });

    return new DuplicateCheckResponse(List.copyOf(matches));
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void eraseContactData(ContactIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contact contact = contactRepository.getByIdentifierAndTeamId(identifier, teamId);

    // Guard: skip if already anonymized
    if (contact.getDataRetentionStatus() == DataRetentionStatus.ANONYMIZED) {
      throw new BadRequestException("Contact data has already been erased");
    }

    Instant now = clock.instant();
    UUID contactId = contact.getId();

    // Anonymize contact record
    contactRepository.anonymizeContact(contactId, teamId, principal.getUserId(), now);

    // Delete associated data (relationships are preserved — the related contact shows as [ERASED])
    contactNoteRepository.anonymizeByContactId(contactId);
    contactTagRepository.deleteByContactId(contactId);
    addressRepository.hardDeleteByContactId(contactId);

    // Delete photos from S3 and soft-delete DB records
    List<Photo> photos = photoRepository.findByEntityAndTeamId("CONTACT", contactId, teamId);
    for (Photo photo : photos) {
      s3StorageService.deleteFile(photo.getFileKey());
      photo.getThumbnailFileKey().ifPresent(s3StorageService::deleteFile);
    }
    photoRepository.softDeleteByEntityAndTeamId("CONTACT", contactId, teamId);

    // Soft-delete documents (S3 files retained for audit trail; DB records marked deleted)
    documentRepository.softDeleteByEntityAndTeamId("CONTACT", contactId, teamId);

    log.info("Contact data erased: {} for team {}", identifier, teamId);

    auditService.logUpdate(
        teamId,
        "CONTACT",
        contactId,
        principal.getUserId(),
        null,
        Map.of("action", "GDPR_ERASE"),
        Map.of("dataRetentionStatus", "ANONYMIZED"));
  }

  private ContactResponse toResponse(Contact contact, UUID teamId) {
    // Reload tags from DB — the mapper initializes tags to empty list
    List<ContactTag> tags = contactTagRepository.findByContactId(contact.getId());
    contact.setTags(tags);

    ContactResponse response = contactMapper.toResponse(contact);

    List<Photo> photos = photoRepository.findByEntityAndTeamId("CONTACT", contact.getId(), teamId);

    Optional<Photo> mainPhoto = photos.stream().filter(Photo::getIsMainPhoto).findFirst();

    String mainPhotoUrl =
        mainPhoto
            .map(photo -> s3StorageService.generatePresignedUrl(photo.getFileKey()).toString())
            .orElse(null);

    String mainPhotoThumbnailUrl =
        mainPhoto
            .map(
                photo -> {
                  String key = photo.getThumbnailFileKey().orElse(photo.getFileKey());
                  return s3StorageService.generatePresignedUrl(key).toString();
                })
            .orElse(null);

    List<Contract> activeContracts =
        contractRepository.findByContactIdViaParties(contact.getId(), teamId).stream()
            .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
            .toList();

    List<ContactPropertyAssignment> activeProperties = new java.util.ArrayList<>();
    for (Contract contract2 : activeContracts) {
      Property property =
          propertyRepository.findByIdAndTeamId(contract2.getPropertyId(), teamId).orElse(null);
      if (property != null) {
        String role =
            contractPartyRepository
                .findByContactIdAndContractIdAndTeamId(contact.getId(), contract2.getId(), teamId)
                .map(party -> party.getRole().name())
                .orElse(null);
        PropertySummary summary =
            new PropertySummary(
                property.getIdentifier().orElseThrow(),
                property.getStreet(),
                property.getCity(),
                property.getPostalCode(),
                property.getPropertyCategory(),
                property.getPropertyType(),
                property.getStatus());
        activeProperties.add(new ContactPropertyAssignment(summary, Optional.ofNullable(role)));
      }
    }

    return new ContactResponse(
        response.identifier(),
        response.contactType(),
        response.displayName(),
        response.firstName(),
        response.lastName(),
        response.companyName(),
        response.tradeName(),
        response.industry(),
        response.email(),
        response.invoiceEmail(),
        response.phone(),
        response.website(),
        response.taxNumber(),
        response.idNumber(),
        response.dateOfBirth(),
        response.idExpiryDate(),
        response.notes(),
        Optional.ofNullable(mainPhotoUrl),
        Optional.ofNullable(mainPhotoThumbnailUrl),
        response.tags(),
        response.dataRetentionStatus(),
        activeProperties,
        response.createdAt(),
        response.updatedAt());
  }

  private PropertyContactHistoryResponse toHistoryResponse(
      com.buurman.domain.PropertyContactHistory history, UUID teamId) {
    Property property = propertyRepository.getByIdAndTeamId(history.getPropertyId(), teamId);

    PropertySummary propertySummary =
        new PropertySummary(
            property.getIdentifier().orElseThrow(),
            property.getStreet(),
            property.getCity(),
            property.getPostalCode(),
            property.getPropertyCategory(),
            property.getPropertyType(),
            property.getStatus());

    String userName =
        userRepository
            .findById(history.getPerformedBy())
            .map(User::getFullName)
            .orElse("Unknown User");

    return new PropertyContactHistoryResponse(
        propertySummary,
        history.getMovedInAt(),
        history.getMovedOutAt(),
        history.getActionType(),
        Optional.of(userName),
        history.getPerformedAt());
  }

  private String computeDisplayName(ContactType contactType, Object request) {
    if (request instanceof CreateContactRequest create) {
      return computeDisplayNameFromFields(
          contactType,
          create.firstName().orElse(null),
          create.lastName().orElse(null),
          create.companyName().orElse(null));
    }
    if (request instanceof UpdateContactRequest update) {
      return computeDisplayNameFromFields(
          contactType,
          update.firstName().orElse(null),
          update.lastName().orElse(null),
          update.companyName().orElse(null));
    }
    return "";
  }

  private String computeDisplayNameFromFields(
      ContactType contactType,
      @Nullable String firstName,
      @Nullable String lastName,
      @Nullable String companyName) {
    return switch (contactType) {
      case INDIVIDUAL -> {
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        yield (first + " " + last).trim();
      }
      case COMPANY, SERVICE_PROVIDER -> {
        if (companyName == null || companyName.isBlank()) {
          throw new BadRequestException("Company name is required for " + contactType);
        }
        yield companyName;
      }
    };
  }

  private UpdateContactRequest clearFieldsForTypeChange(UpdateContactRequest request) {
    return switch (request.contactType()) {
      case INDIVIDUAL ->
          new UpdateContactRequest(
              request.contactType(),
              request.firstName(),
              request.lastName(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              request.email(),
              Optional.empty(),
              request.phone(),
              Optional.empty(),
              request.taxNumber(),
              request.idNumber(),
              request.dateOfBirth(),
              request.idExpiryDate(),
              request.notes(),
              request.tags());
      case COMPANY, SERVICE_PROVIDER ->
          new UpdateContactRequest(
              request.contactType(),
              request.firstName(),
              request.lastName(),
              request.companyName(),
              request.tradeName(),
              request.industry(),
              request.email(),
              request.invoiceEmail(),
              request.phone(),
              request.website(),
              request.taxNumber(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              request.notes(),
              request.tags());
    };
  }

  private void validateContactRequest(
      ContactType contactType, Optional<String> firstName, Optional<String> companyName) {
    switch (contactType) {
      case INDIVIDUAL -> {
        if (firstName.isEmpty() || firstName.get().isBlank()) {
          throw new BadRequestException("firstName is required for INDIVIDUAL contacts");
        }
      }
      case COMPANY, SERVICE_PROVIDER -> {
        if (companyName.isEmpty() || companyName.get().isBlank()) {
          throw new BadRequestException(
              "companyName is required for COMPANY/SERVICE_PROVIDER contacts");
        }
      }
    }
  }

  private Contact cloneContact(Contact contact) {
    return Contact.builder()
        .id(contact.getId())
        .identifier(contact.getIdentifier())
        .teamId(contact.getTeamId())
        .contactType(contact.getContactType())
        .displayName(contact.getDisplayName())
        .firstName(contact.getFirstName())
        .lastName(contact.getLastName())
        .companyName(contact.getCompanyName())
        .tradeName(contact.getTradeName())
        .industry(contact.getIndustry())
        .email(contact.getEmail())
        .invoiceEmail(contact.getInvoiceEmail())
        .phone(contact.getPhone())
        .website(contact.getWebsite())
        .taxNumber(contact.getTaxNumber())
        .idNumber(contact.getIdNumber())
        .dateOfBirth(contact.getDateOfBirth())
        .idExpiryDate(contact.getIdExpiryDate())
        .notes(contact.getNotes())
        .dataRetentionStatus(contact.getDataRetentionStatus())
        .tags(contact.getTags())
        .createdAt(contact.getCreatedAt())
        .updatedAt(contact.getUpdatedAt())
        .createdBy(contact.getCreatedBy())
        .updatedBy(contact.getUpdatedBy())
        .deletedAt(contact.getDeletedAt())
        .build();
  }
}
