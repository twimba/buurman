package com.buurman.service;

import static com.buurman.util.UlidGenerator.newTenantId;

import java.net.URL;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Contract;
import com.buurman.domain.Photo;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyTenantHistory;
import com.buurman.domain.Tenant;
import com.buurman.domain.TenantAddress;
import com.buurman.dto.request.CreateTenantAddressRequest;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.LinkTenantToPropertyRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateTenantAddressRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.PropertyTenantHistoryResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.dto.response.TenantAddressResponse;
import com.buurman.dto.response.TenantPropertyAssignment;
import com.buurman.dto.response.TenantResponse;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyTenantHistoryRepository;
import com.buurman.repository.TenantAddressRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantService {

  private final TenantRepository tenantRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyTenantHistoryRepository historyRepository;
  private final TenantMapper tenantMapper;
  private final AuditService auditService;
  private final UserRepository userRepository;
  private final DocumentService documentService;
  private final PhotoService photoService;
  private final PhotoRepository photoRepository;
  private final S3StorageService s3StorageService;
  private final ContractRepository contractRepository;
  private final ContractPartyRepository contractPartyRepository;
  private final TenantAddressService addressService;
  private final TenantAddressRepository addressRepository;
  private final MetricsService metricsService;
  private final Clock clock;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public TenantResponse createTenant(CreateTenantRequest request, UserPrincipal principal) {
    tenantRepository
        .findByEmailAndTeamId(request.email(), principal.getTeamId())
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException(
                  "Tenant with email " + request.email() + " already exists");
            });

    Tenant tenant = tenantMapper.toEntity(request);
    tenant.setIdentifier(newTenantId().value());
    tenant.setTeamId(principal.getTeamId());
    tenant.setCreatedBy(principal.getUserId());
    tenant.setUpdatedBy(principal.getUserId());

    Tenant savedTenant = tenantRepository.save(tenant);

    metricsService.incrementCounter("tenant.total");

    log.info("Tenant created: {} for team {}", savedTenant.getIdentifier(), principal.getTeamId());

    auditService.logCreate(
        principal.getTeamId(), "TENANT", savedTenant.getId(), principal.getUserId(), savedTenant);

    return toResponse(savedTenant, principal.getTeamId());
  }

  public List<TenantResponse> getAllTenants(UserPrincipal principal) {
    List<Tenant> tenants = tenantRepository.findAllByTeamId(principal.getTeamId());
    return tenants.stream().map(tenant -> toResponse(tenant, principal.getTeamId())).toList();
  }

  public List<TenantResponse> searchTenants(String searchTerm, UserPrincipal principal) {
    List<Tenant> tenants = tenantRepository.searchByTeamId(principal.getTeamId(), searchTerm);
    return tenants.stream().map(tenant -> toResponse(tenant, principal.getTeamId())).toList();
  }

  public PageResponse<TenantResponse> getTenantsPaginated(
      UserPrincipal principal, String search, PageRequest pageRequest) {
    PaginatedResult<Tenant> result =
        tenantRepository.findAllByTeamIdPaginated(principal.getTeamId(), search, pageRequest);
    List<TenantResponse> responses =
        result.items().stream().map(tenant -> toResponse(tenant, principal.getTeamId())).toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public TenantResponse getTenant(String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    return toResponse(tenant, principal.getTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public TenantResponse updateTenant(
      String identifier, UpdateTenantRequest request, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    if (!tenant.getEmail().equals(request.email())) {
      tenantRepository
          .findByEmailAndTeamId(request.email(), principal.getTeamId())
          .ifPresent(
              existing -> {
                if (!existing.getId().equals(tenant.getId())) {
                  throw new IllegalArgumentException(
                      "Tenant with email " + request.email() + " already exists");
                }
              });
    }

    Tenant oldTenant = cloneTenant(tenant);
    tenantMapper.updateEntity(tenant, request);
    tenant.setUpdatedBy(principal.getUserId());

    Tenant updatedTenant = tenantRepository.save(tenant);
    log.info(
        "Tenant updated: {} for team {}", updatedTenant.getIdentifier(), principal.getTeamId());

    auditService.logUpdate(
        principal.getTeamId(),
        "TENANT",
        updatedTenant.getId(),
        principal.getUserId(),
        oldTenant,
        updatedTenant,
        auditService.getChangedFields(oldTenant, updatedTenant));

    return toResponse(updatedTenant, principal.getTeamId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteTenant(String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    if (tenant.getCurrentPropertyId() != null) {
      unlinkTenantInternal(tenant.getId(), principal);
    }

    tenantRepository.softDeleteByIdAndTeamId(tenant.getId(), principal.getTeamId());
    log.info("Tenant deleted: {} for team {}", identifier, principal.getTeamId());

    auditService.logDelete(
        principal.getTeamId(), "TENANT", tenant.getId(), principal.getUserId(), tenant);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public TenantResponse linkTenantToProperty(
      String identifier, LinkTenantToPropertyRequest request, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    Property property =
        propertyRepository.getByIdentifierAndTeamId(
            request.propertyIdentifier(), principal.getTeamId());

    if (tenant.getCurrentPropertyId() != null) {
      throw new IllegalArgumentException("Tenant is already linked to a property. Unlink first.");
    }

    List<Tenant> existingTenants =
        tenantRepository.findByCurrentPropertyId(property.getId(), principal.getTeamId());
    if (!existingTenants.isEmpty()) {
      throw new IllegalArgumentException("Property already has a tenant assigned");
    }

    tenant.setCurrentPropertyId(property.getId());
    tenant.setUpdatedBy(principal.getUserId());
    Tenant updatedTenant = tenantRepository.save(tenant);

    PropertyTenantHistory history = new PropertyTenantHistory();
    history.setTeamId(principal.getTeamId());
    history.setPropertyId(property.getId());
    history.setTenantId(tenant.getId());
    history.setMovedInAt(request.movedInAt() != null ? request.movedInAt() : clock.instant());
    history.setActionType(PropertyTenantHistory.ActionType.LINKED);
    history.setPerformedBy(principal.getUserId());
    history.setPerformedAt(clock.instant());
    historyRepository.save(history);

    metricsService.incrementCounter("tenant.linked.total");

    log.info(
        "Tenant {} linked to property {} for team {}",
        identifier,
        property.getIdentifier(),
        principal.getTeamId());

    auditService.logCreate(
        principal.getTeamId(),
        "PROPERTY_TENANT_LINK",
        history.getId(),
        principal.getUserId(),
        history);

    return toResponse(updatedTenant, principal.getTeamId());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public TenantResponse unlinkTenantFromProperty(String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    if (tenant.getCurrentPropertyId() == null) {
      throw new IllegalArgumentException("Tenant is not linked to any property");
    }

    UUID propertyId = tenant.getCurrentPropertyId();

    tenant.setCurrentPropertyId(null);
    tenant.setUpdatedBy(principal.getUserId());
    Tenant updatedTenant = tenantRepository.save(tenant);

    PropertyTenantHistory history = new PropertyTenantHistory();
    history.setTeamId(principal.getTeamId());
    history.setPropertyId(propertyId);
    history.setTenantId(tenant.getId());
    history.setMovedOutAt(clock.instant());
    history.setActionType(PropertyTenantHistory.ActionType.UNLINKED);
    history.setPerformedBy(principal.getUserId());
    history.setPerformedAt(clock.instant());
    historyRepository.save(history);

    metricsService.incrementCounter("tenant.unlinked.total");

    log.info(
        "Tenant {} unlinked from property {} for team {}",
        identifier,
        propertyId,
        principal.getTeamId());

    auditService.logCreate(
        principal.getTeamId(),
        "PROPERTY_TENANT_UNLINK",
        history.getId(),
        principal.getUserId(),
        history);

    return toResponse(updatedTenant, principal.getTeamId());
  }

  public List<PropertyTenantHistoryResponse> getTenantHistory(
      String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());

    List<PropertyTenantHistory> history =
        historyRepository.findByTenantId(tenant.getId(), principal.getTeamId());
    return history.stream().map(h -> toHistoryResponse(h, principal.getTeamId())).toList();
  }

  public List<RecentActivityResponse> getAuditLog(String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return auditService.getEntityAuditLog(principal.getTeamId(), "TENANT", tenant.getId());
  }

  public DocumentResponse uploadDocument(
      String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return documentService.uploadDocument(
        file, "TENANT", tenant.getId(), tenant.getIdentifier(), title, notes, principal);
  }

  public List<DocumentResponse> getDocuments(String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return documentService.getDocuments("TENANT", tenant.getId(), principal);
  }

  public Map<String, String> getDownloadUrl(String documentIdentifier, UserPrincipal principal) {
    URL url = documentService.getDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  public void deleteDocument(String documentIdentifier, UserPrincipal principal) {
    documentService.deleteDocument(documentIdentifier, principal);
  }

  public List<PhotoResponse> getPhotos(String identifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return photoService.getPhotos("TENANT", tenant.getId(), principal);
  }

  public PhotoResponse uploadPhoto(
      String identifier, MultipartFile file, String title, String notes, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    return photoService.uploadPhoto(
        file, "TENANT", tenant.getId(), tenant.getIdentifier(), title, notes, principal);
  }

  public PhotoResponse setMainPhoto(
      String identifier, String photoIdentifier, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdentifierAndTeamId(identifier, principal.getTeamId());
    Photo photo = photoRepository.getByIdentifierAndTeamId(photoIdentifier, principal.getTeamId());
    return photoService.setMainPhoto(photo.getId(), "TENANT", tenant.getId(), principal);
  }

  public TenantAddressResponse createAddress(
      String tenantIdentifier, CreateTenantAddressRequest request, UserPrincipal principal) {
    Tenant tenant =
        tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, principal.getTeamId());
    return addressService.createAddress(tenant.getId(), request, principal);
  }

  public List<TenantAddressResponse> getAddresses(
      String tenantIdentifier, UserPrincipal principal) {
    Tenant tenant =
        tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, principal.getTeamId());
    return addressService.getAddresses(tenant.getId(), principal);
  }

  public TenantAddressResponse getAddress(
      String tenantIdentifier, String addressIdentifier, UserPrincipal principal) {
    Tenant tenant =
        tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, principal.getTeamId());
    TenantAddress address =
        addressRepository.getByIdentifierAndTeamId(addressIdentifier, principal.getTeamId());
    return addressService.getAddress(tenant.getId(), address.getId(), principal);
  }

  public TenantAddressResponse updateAddress(
      String tenantIdentifier,
      String addressIdentifier,
      UpdateTenantAddressRequest request,
      UserPrincipal principal) {
    Tenant tenant =
        tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, principal.getTeamId());
    TenantAddress address =
        addressRepository.getByIdentifierAndTeamId(addressIdentifier, principal.getTeamId());
    return addressService.updateAddress(tenant.getId(), address.getId(), request, principal);
  }

  public void deleteAddress(
      String tenantIdentifier, String addressIdentifier, UserPrincipal principal) {
    Tenant tenant =
        tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, principal.getTeamId());
    TenantAddress address =
        addressRepository.getByIdentifierAndTeamId(addressIdentifier, principal.getTeamId());
    addressService.deleteAddress(tenant.getId(), address.getId(), principal);
  }

  private TenantResponse toResponse(Tenant tenant, UUID teamId) {
    TenantResponse response = tenantMapper.toResponse(tenant);

    List<Photo> photos = photoRepository.findByEntityAndTeamId("TENANT", tenant.getId(), teamId);

    Optional<Photo> mainPhoto =
        photos.stream().filter(photo -> Boolean.TRUE.equals(photo.getIsMainPhoto())).findFirst();

    String mainPhotoUrl =
        mainPhoto
            .map(photo -> s3StorageService.generatePresignedUrl(photo.getFileKey()).toString())
            .orElse(null);

    String mainPhotoThumbnailUrl =
        mainPhoto
            .map(
                photo -> {
                  String key =
                      photo.getThumbnailFileKey() != null
                          ? photo.getThumbnailFileKey()
                          : photo.getFileKey();
                  return s3StorageService.generatePresignedUrl(key).toString();
                })
            .orElse(null);

    List<Contract> activeContracts =
        contractRepository.findByTenantIdViaParties(tenant.getId(), teamId).stream()
            .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
            .toList();

    List<TenantPropertyAssignment> activeProperties = new java.util.ArrayList<>();
    for (Contract contract : activeContracts) {
      Property property =
          propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
      if (property != null) {
        String role =
            contractPartyRepository
                .findByTenantIdAndContractIdAndTeamId(tenant.getId(), contract.getId(), teamId)
                .map(party -> party.getRole().name())
                .orElse(null);
        PropertySummary summary =
            new PropertySummary(
                property.getIdentifier(),
                property.getStreet(),
                property.getCity(),
                property.getPostalCode(),
                property.getPropertyType(),
                property.getStatus());
        activeProperties.add(new TenantPropertyAssignment(summary, role));
      }
    }

    return new TenantResponse(
        response.identifier(),
        response.firstName(),
        response.lastName(),
        response.email(),
        response.phone(),
        response.taxNumber(),
        response.idNumber(),
        response.additionalInfo(),
        mainPhotoUrl,
        mainPhotoThumbnailUrl,
        activeProperties,
        response.createdAt(),
        response.updatedAt());
  }

  private PropertyTenantHistoryResponse toHistoryResponse(
      PropertyTenantHistory history, UUID teamId) {
    Property property = propertyRepository.getByIdAndTeamId(history.getPropertyId(), teamId);

    PropertySummary propertySummary =
        new PropertySummary(
            property.getIdentifier(),
            property.getStreet(),
            property.getCity(),
            property.getPostalCode(),
            property.getPropertyType(),
            property.getStatus());

    String userName =
        userRepository
            .findById(history.getPerformedBy())
            .map(user -> user.getFirstName() + " " + user.getLastName())
            .orElse("Unknown User");

    return new PropertyTenantHistoryResponse(
        propertySummary,
        history.getMovedInAt(),
        history.getMovedOutAt(),
        history.getActionType(),
        userName,
        history.getPerformedAt());
  }

  /** Internal method for unlinking by UUID (used during delete). */
  private void unlinkTenantInternal(UUID tenantId, UserPrincipal principal) {
    Tenant tenant = tenantRepository.getByIdAndTeamId(tenantId, principal.getTeamId());

    if (tenant.getCurrentPropertyId() == null) {
      return;
    }

    UUID propertyId = tenant.getCurrentPropertyId();

    tenant.setCurrentPropertyId(null);
    tenant.setUpdatedBy(principal.getUserId());
    tenantRepository.save(tenant);

    PropertyTenantHistory history = new PropertyTenantHistory();
    history.setTeamId(principal.getTeamId());
    history.setPropertyId(propertyId);
    history.setTenantId(tenantId);
    history.setMovedOutAt(clock.instant());
    history.setActionType(PropertyTenantHistory.ActionType.UNLINKED);
    history.setPerformedBy(principal.getUserId());
    history.setPerformedAt(clock.instant());
    historyRepository.save(history);

    auditService.logCreate(
        principal.getTeamId(),
        "PROPERTY_TENANT_UNLINK",
        history.getId(),
        principal.getUserId(),
        history);
  }

  private Tenant cloneTenant(Tenant tenant) {
    return new Tenant(
        tenant.getId(),
        tenant.getIdentifier(),
        tenant.getTeamId(),
        tenant.getFirstName(),
        tenant.getLastName(),
        tenant.getEmail(),
        tenant.getPhone(),
        tenant.getTaxNumber(),
        tenant.getIdNumber(),
        tenant.getAdditionalInfo(),
        tenant.getCurrentPropertyId(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt(),
        tenant.getCreatedBy(),
        tenant.getUpdatedBy(),
        tenant.getDeletedAt());
  }
}
