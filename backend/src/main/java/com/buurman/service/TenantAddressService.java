package com.buurman.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Tenant;
import com.buurman.domain.TenantAddress;
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
    Tenant tenant =
        tenantRepository
            .findByIdAndTeamId(tenantId, principal.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

    // Check for unique ACTIVE CURRENT address constraint
    if (request.addressType() == TenantAddress.AddressType.CURRENT
        && (request.status() == null || request.status() == TenantAddress.AddressStatus.ACTIVE)) {
      List<TenantAddress> existingAddresses =
          addressRepository.findByTenantId(tenantId, principal.getTeamId());
      boolean hasActiveCurrent =
          existingAddresses.stream()
              .anyMatch(
                  addr ->
                      addr.getAddressType() == TenantAddress.AddressType.CURRENT
                          && addr.getStatus() == TenantAddress.AddressStatus.ACTIVE);
      if (hasActiveCurrent) {
        throw new IllegalArgumentException(
            "Tenant already has an active CURRENT address. Please set existing address to INACTIVE"
                + " first.");
      }
    }

    TenantAddress address = addressMapper.toEntity(request);
    address.setTenantId(tenantId);
    address.setTeamId(principal.getTeamId());
    address.setCreatedBy(principal.getUserId());
    address.setUpdatedBy(principal.getUserId());
    address.setCreatedAt(clock.instant());
    address.setUpdatedAt(clock.instant());

    // Set default status if not provided
    if (address.getStatus() == null) {
      address.setStatus(TenantAddress.AddressStatus.ACTIVE);
    }

    if (address.getLatitude() == null || address.getLongitude() == null) {
      geocodingService
          .geocode(
              request.street(), request.city(),
              request.postalCode(), request.country())
          .ifPresent(
              result -> {
                address.setLatitude(result.latitude().doubleValue());
                address.setLongitude(result.longitude().doubleValue());
              });
    }

    TenantAddress savedAddress = addressRepository.save(address);
    log.info(
        "Address created for tenant {} in team {}: {} - {}, {}",
        tenantId,
        principal.getTeamId(),
        savedAddress.getStreet(),
        savedAddress.getCity(),
        savedAddress.getCountry());

    // Log to audit trail
    auditService.logCreate(
        principal.getTeamId(),
        "TENANT_ADDRESS",
        savedAddress.getId(),
        principal.getUserId(),
        savedAddress);

    return addressMapper.toResponse(savedAddress);
  }

  public List<TenantAddressResponse> getAddresses(UUID tenantId, UserPrincipal principal) {
    // Verify tenant exists and belongs to user's team
    tenantRepository
        .findByIdAndTeamId(tenantId, principal.getTeamId())
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

    List<TenantAddress> addresses =
        addressRepository.findByTenantId(tenantId, principal.getTeamId());
    return addresses.stream().map(addressMapper::toResponse).toList();
  }

  public TenantAddressResponse getAddress(UUID tenantId, UUID addressId, UserPrincipal principal) {
    TenantAddress address =
        addressRepository
            .findByIdAndTeamId(addressId, principal.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("Address not found or access denied"));
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
        addressRepository
            .findByIdAndTeamId(addressId, principal.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("Address not found or access denied"));
    if (!address.getTenantId().equals(tenantId)) {
      throw new IllegalArgumentException("Address does not belong to the specified tenant");
    }

    // Store old values for audit
    TenantAddress oldAddress =
        new TenantAddress(
            address.getId(),
            address.getTenantId(),
            address.getTeamId(),
            address.getStreet(),
            address.getCity(),
            address.getPostalCode(),
            address.getCountry(),
            address.getAddressType(),
            address.getStatus(),
            address.getLatitude(),
            address.getLongitude(),
            address.getCreatedAt(),
            address.getUpdatedAt(),
            address.getCreatedBy(),
            address.getUpdatedBy(),
            address.getDeletedAt());

    // Check for unique ACTIVE CURRENT address constraint
    if (request.addressType() == TenantAddress.AddressType.CURRENT
        && request.status() == TenantAddress.AddressStatus.ACTIVE) {
      // If this address is being changed to CURRENT ACTIVE, check for conflicts
      if (address.getAddressType() != TenantAddress.AddressType.CURRENT
          || address.getStatus() != TenantAddress.AddressStatus.ACTIVE) {
        List<TenantAddress> existingAddresses =
            addressRepository.findByTenantId(address.getTenantId(), principal.getTeamId());
        boolean hasOtherActiveCurrent =
            existingAddresses.stream()
                .anyMatch(
                    addr ->
                        !addr.getId().equals(addressId)
                            && addr.getAddressType() == TenantAddress.AddressType.CURRENT
                            && addr.getStatus() == TenantAddress.AddressStatus.ACTIVE);
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
    String oldCountry = address.getCountry();

    // Update address fields
    addressMapper.updateEntity(address, request);
    address.setUpdatedBy(principal.getUserId());
    address.setUpdatedAt(clock.instant());

    boolean addressChanged =
        !java.util.Objects.equals(oldStreet, address.getStreet())
            || !java.util.Objects.equals(oldCity, address.getCity())
            || !java.util.Objects.equals(oldPostalCode, address.getPostalCode())
            || !java.util.Objects.equals(oldCountry, address.getCountry());

    if (addressChanged && (address.getLatitude() == null || address.getLongitude() == null)) {
      geocodingService
          .geocode(
              address.getStreet(), address.getCity(),
              address.getPostalCode(), address.getCountry())
          .ifPresent(
              result -> {
                address.setLatitude(result.latitude().doubleValue());
                address.setLongitude(result.longitude().doubleValue());
              });
    }

    TenantAddress updatedAddress = addressRepository.save(address);
    log.info(
        "Address updated: {} for tenant {} in team {}",
        addressId,
        address.getTenantId(),
        principal.getTeamId());

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
    if (!oldAddress.getCountry().equals(updatedAddress.getCountry())) {
      changedFields.put("country", updatedAddress.getCountry());
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
        principal.getTeamId(),
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
        addressRepository
            .findByIdAndTeamId(addressId, principal.getTeamId())
            .orElseThrow(() -> new IllegalArgumentException("Address not found or access denied"));
    if (!address.getTenantId().equals(tenantId)) {
      throw new IllegalArgumentException("Address does not belong to the specified tenant");
    }

    addressRepository.softDeleteByIdAndTeamId(addressId, principal.getTeamId());
    log.info(
        "Address soft deleted: {} for tenant {} in team {}",
        addressId,
        address.getTenantId(),
        principal.getTeamId());

    // Log to audit trail
    auditService.logDelete(
        principal.getTeamId(), "TENANT_ADDRESS", addressId, principal.getUserId(), address);
  }
}
