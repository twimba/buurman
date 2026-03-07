package com.buurman.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Tenant;
import com.buurman.domain.TenantAddress;
import com.buurman.domain.TenantAddress.AddressStatus;
import com.buurman.domain.TenantAddress.AddressType;
import com.buurman.dto.request.CreateTenantAddressRequest;
import com.buurman.dto.request.UpdateTenantAddressRequest;
import com.buurman.dto.response.TenantAddressResponse;
import com.buurman.mapper.TenantAddressMapper;
import com.buurman.repository.TenantAddressRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantAddressService {

  private final TenantAddressRepository addressRepository;
  private final TenantRepository tenantRepository;
  private final TenantAddressMapper addressMapper;
  private final AuditService auditService;
  private final GeocodingService geocodingService;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public TenantAddressResponse createAddress(
      UUID tenantId, CreateTenantAddressRequest request, UserPrincipal principal) {
    // Verify tenant exists and belongs to user's team
    Tenant tenant = tenantRepository.getByIdAndTeamId(tenantId, principal.requireTeamId());

    // Check for unique ACTIVE CURRENT address constraint
    if (request.addressType() == AddressType.CURRENT
        && (request.status().isEmpty()
            || request
                    .status()
                    .orElseThrow(
                        () -> new IllegalStateException("Status is empty after isEmpty check"))
                == AddressStatus.ACTIVE)) {
      List<TenantAddress> existingAddresses =
          addressRepository.findByTenantId(tenantId, principal.requireTeamId());
      boolean hasActiveCurrent =
          existingAddresses.stream()
              .anyMatch(
                  addr ->
                      addr.getAddressType() == AddressType.CURRENT
                          && addr.getStatus() == AddressStatus.ACTIVE);
      if (hasActiveCurrent) {
        throw new IllegalArgumentException(
            "Tenant already has an active CURRENT address. Please set existing address to INACTIVE"
                + " first.");
      }
    }

    TenantAddress address = addressMapper.toEntity(request);
    address.setTenantId(tenantId);
    address.setTeamId(principal.requireTeamId());
    address.setCreatedBy(principal.getUserId());
    address.setUpdatedBy(principal.getUserId());
    address.setCreatedAt(clock.instant());
    address.setUpdatedAt(clock.instant());
    address.setStatus(request.status().orElse(AddressStatus.ACTIVE));

    if (address.getLatitude().isEmpty() || address.getLongitude().isEmpty()) {
      geocodingService
          .geocode(
              request.street(), request.city(),
              request.postalCode(), request.countryCode())
          .ifPresent(
              result -> {
                address.setLatitude(Optional.of(result.latitude().doubleValue()));
                address.setLongitude(Optional.of(result.longitude().doubleValue()));
                address.setGeocodeAccuracy(Optional.of(result.accuracy()));
              });
    }

    TenantAddress savedAddress = addressRepository.save(address);
    log.info(
        "Address created for tenant {} in team {}: {} - {}, {}",
        tenantId,
        principal.requireTeamId(),
        savedAddress.getStreet(),
        savedAddress.getCity(),
        savedAddress.getCountryCode());

    // Log to audit trail
    auditService.logCreate(
        principal.requireTeamId(),
        "TENANT_ADDRESS",
        savedAddress.getId(),
        principal.getUserId(),
        savedAddress);

    return addressMapper.toResponse(savedAddress);
  }

  public List<TenantAddressResponse> getAddresses(UUID tenantId, UserPrincipal principal) {
    // Verify tenant exists and belongs to user's team
    tenantRepository.getByIdAndTeamId(tenantId, principal.requireTeamId());

    List<TenantAddress> addresses =
        addressRepository.findByTenantId(tenantId, principal.requireTeamId());
    return addresses.stream().map(addressMapper::toResponse).toList();
  }

  public TenantAddressResponse getAddress(UUID tenantId, UUID addressId, UserPrincipal principal) {
    TenantAddress address =
        addressRepository.getByIdAndTeamId(addressId, principal.requireTeamId());
    if (!address.getTenantId().equals(tenantId)) {
      throw new IllegalArgumentException("Address does not belong to the specified tenant");
    }
    return addressMapper.toResponse(address);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public TenantAddressResponse updateAddress(
      UUID tenantId, UUID addressId, UpdateTenantAddressRequest request, UserPrincipal principal) {
    TenantAddress address =
        addressRepository.getByIdAndTeamId(addressId, principal.requireTeamId());
    if (!address.getTenantId().equals(tenantId)) {
      throw new IllegalArgumentException("Address does not belong to the specified tenant");
    }

    // Store old values for audit
    TenantAddress oldAddress =
        TenantAddress.builder()
            .id(address.getId())
            .identifier(address.getIdentifier())
            .tenantId(address.getTenantId())
            .teamId(address.getTeamId())
            .street(address.getStreet())
            .city(address.getCity())
            .postalCode(address.getPostalCode())
            .countryCode(address.getCountryCode())
            .addressType(address.getAddressType())
            .status(address.getStatus())
            .latitude(address.getLatitude())
            .longitude(address.getLongitude())
            .geocodeAccuracy(address.getGeocodeAccuracy())
            .createdAt(address.getCreatedAt())
            .updatedAt(address.getUpdatedAt())
            .createdBy(address.getCreatedBy())
            .updatedBy(address.getUpdatedBy())
            .deletedAt(address.getDeletedAt())
            .build();

    // Check for unique ACTIVE CURRENT address constraint
    if (request.addressType() == AddressType.CURRENT && request.status() == AddressStatus.ACTIVE) {
      // If this address is being changed to CURRENT ACTIVE, check for conflicts
      if (address.getAddressType() != AddressType.CURRENT
          || address.getStatus() != AddressStatus.ACTIVE) {
        List<TenantAddress> existingAddresses =
            addressRepository.findByTenantId(address.getTenantId(), principal.requireTeamId());
        boolean hasOtherActiveCurrent =
            existingAddresses.stream()
                .anyMatch(
                    addr ->
                        !addr.getId().equals(addressId)
                            && addr.getAddressType() == AddressType.CURRENT
                            && addr.getStatus() == AddressStatus.ACTIVE);
        if (hasOtherActiveCurrent) {
          throw new IllegalArgumentException(
              "Tenant already has an active CURRENT address. Please set existing address to"
                  + " INACTIVE first.");
        }
      }
    }

    // Capture old address before update
    String oldStreet = address.getStreet();
    String oldCity = address.getCity();
    String oldPostalCode = address.getPostalCode();
    String oldCountryCode = address.getCountryCode();

    // Update address fields
    addressMapper.updateEntity(address, request);
    address.setUpdatedBy(principal.getUserId());
    address.setUpdatedAt(clock.instant());

    boolean addressChanged =
        !java.util.Objects.equals(oldStreet, address.getStreet())
            || !java.util.Objects.equals(oldCity, address.getCity())
            || !java.util.Objects.equals(oldPostalCode, address.getPostalCode())
            || !java.util.Objects.equals(oldCountryCode, address.getCountryCode());

    if (addressChanged && (address.getLatitude().isEmpty() || address.getLongitude().isEmpty())) {
      geocodingService
          .geocode(
              address.getStreet(), address.getCity(),
              Optional.ofNullable(address.getPostalCode()), address.getCountryCode())
          .ifPresent(
              result -> {
                address.setLatitude(Optional.of(result.latitude().doubleValue()));
                address.setLongitude(Optional.of(result.longitude().doubleValue()));
                address.setGeocodeAccuracy(Optional.ofNullable(result.accuracy()));
              });
    }

    TenantAddress updatedAddress = addressRepository.save(address);
    log.info(
        "Address updated: {} for tenant {} in team {}",
        addressId,
        address.getTenantId(),
        principal.requireTeamId());

    // Determine changed fields for audit
    Map<String, Object> changedFields = new HashMap<>();
    if (!oldAddress.getStreet().equals(updatedAddress.getStreet())) {
      changedFields.put("street", updatedAddress.getStreet());
    }
    if (!oldAddress.getCity().equals(updatedAddress.getCity())) {
      changedFields.put("city", updatedAddress.getCity());
    }
    if (!java.util.Objects.equals(oldAddress.getPostalCode(), updatedAddress.getPostalCode())) {
      changedFields.put("postalCode", updatedAddress.getPostalCode());
    }
    if (!oldAddress.getCountryCode().equals(updatedAddress.getCountryCode())) {
      changedFields.put("countryCode", updatedAddress.getCountryCode());
    }
    if (!oldAddress.getAddressType().equals(updatedAddress.getAddressType())) {
      changedFields.put("addressType", updatedAddress.getAddressType());
    }
    if (!oldAddress.getStatus().equals(updatedAddress.getStatus())) {
      changedFields.put("status", updatedAddress.getStatus());
    }
    if (!java.util.Objects.equals(oldAddress.getLatitude(), updatedAddress.getLatitude())) {
      changedFields.put("latitude", updatedAddress.getLatitude());
    }
    if (!java.util.Objects.equals(oldAddress.getLongitude(), updatedAddress.getLongitude())) {
      changedFields.put("longitude", updatedAddress.getLongitude());
    }

    // Log to audit trail
    auditService.logUpdate(
        principal.requireTeamId(),
        "TENANT_ADDRESS",
        updatedAddress.getId(),
        principal.getUserId(),
        oldAddress,
        updatedAddress,
        changedFields);

    return addressMapper.toResponse(updatedAddress);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteAddress(UUID tenantId, UUID addressId, UserPrincipal principal) {
    TenantAddress address =
        addressRepository.getByIdAndTeamId(addressId, principal.requireTeamId());
    if (!address.getTenantId().equals(tenantId)) {
      throw new IllegalArgumentException("Address does not belong to the specified tenant");
    }

    addressRepository.softDeleteByIdAndTeamId(addressId, principal.requireTeamId());
    log.info(
        "Address soft deleted: {} for tenant {} in team {}",
        addressId,
        address.getTenantId(),
        principal.requireTeamId());

    // Log to audit trail
    auditService.logDelete(
        principal.requireTeamId(), "TENANT_ADDRESS", addressId, principal.getUserId(), address);
  }
}
