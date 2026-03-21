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

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactAddress.AddressStatus;
import com.buurman.domain.ContactAddress.AddressType;
import com.buurman.dto.request.CreateContactAddressRequest;
import com.buurman.dto.request.UpdateContactAddressRequest;
import com.buurman.dto.response.ContactAddressResponse;
import com.buurman.mapper.ContactAddressMapper;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactAddressService {

  private final ContactAddressRepository addressRepository;
  private final ContactRepository contactRepository;
  private final ContactAddressMapper addressMapper;
  private final AuditService auditService;
  private final GeocodingService geocodingService;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactAddressResponse createAddress(
      UUID contactId, CreateContactAddressRequest request, UserPrincipal principal) {
    // Verify contact exists and belongs to user's team
    Contact contact = contactRepository.getByIdAndTeamId(contactId, principal.requireTeamId());

    // Check for unique ACTIVE CURRENT address constraint
    if (request.addressType() == AddressType.CURRENT
        && (request.status().isEmpty()
            || request
                    .status()
                    .orElseThrow(
                        () -> new IllegalStateException("Status is empty after isEmpty check"))
                == AddressStatus.ACTIVE)) {
      List<ContactAddress> existingAddresses =
          addressRepository.findByContactId(contactId, principal.requireTeamId());
      boolean hasActiveCurrent =
          existingAddresses.stream()
              .anyMatch(
                  addr ->
                      addr.getAddressType() == AddressType.CURRENT
                          && addr.getStatus() == AddressStatus.ACTIVE);
      if (hasActiveCurrent) {
        throw new IllegalArgumentException(
            "Contact already has an active CURRENT address. Please set existing address to INACTIVE"
                + " first.");
      }
    }

    ContactAddress address = addressMapper.toEntity(request);
    address.setContactId(contactId);
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

    ContactAddress savedAddress = addressRepository.save(address);
    log.info(
        "Address created for contact {} in team {}: {} - {}, {}",
        contactId,
        principal.requireTeamId(),
        savedAddress.getStreet(),
        savedAddress.getCity(),
        savedAddress.getCountryCode());

    // Log to audit trail
    auditService.logCreate(
        principal.requireTeamId(),
        "CONTACT_ADDRESS",
        savedAddress.getId(),
        principal.getUserId(),
        savedAddress);

    return addressMapper.toResponse(savedAddress);
  }

  public List<ContactAddressResponse> getAddresses(UUID contactId, UserPrincipal principal) {
    // Verify contact exists and belongs to user's team
    contactRepository.getByIdAndTeamId(contactId, principal.requireTeamId());

    List<ContactAddress> addresses =
        addressRepository.findByContactId(contactId, principal.requireTeamId());
    return addresses.stream().map(addressMapper::toResponse).toList();
  }

  public ContactAddressResponse getAddress(UUID contactId, UUID addressId, UserPrincipal principal) {
    ContactAddress address =
        addressRepository.getByIdAndTeamId(addressId, principal.requireTeamId());
    if (!address.getContactId().equals(contactId)) {
      throw new IllegalArgumentException("Address does not belong to the specified contact");
    }
    return addressMapper.toResponse(address);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContactAddressResponse updateAddress(
      UUID contactId, UUID addressId, UpdateContactAddressRequest request, UserPrincipal principal) {
    ContactAddress address =
        addressRepository.getByIdAndTeamId(addressId, principal.requireTeamId());
    if (!address.getContactId().equals(contactId)) {
      throw new IllegalArgumentException("Address does not belong to the specified contact");
    }

    // Store old values for audit
    ContactAddress oldAddress =
        ContactAddress.builder()
            .id(address.getId())
            .identifier(address.getIdentifier())
            .contactId(address.getContactId())
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
        List<ContactAddress> existingAddresses =
            addressRepository.findByContactId(address.getContactId(), principal.requireTeamId());
        boolean hasOtherActiveCurrent =
            existingAddresses.stream()
                .anyMatch(
                    addr ->
                        !addr.getId().equals(addressId)
                            && addr.getAddressType() == AddressType.CURRENT
                            && addr.getStatus() == AddressStatus.ACTIVE);
        if (hasOtherActiveCurrent) {
          throw new IllegalArgumentException(
              "Contact already has an active CURRENT address. Please set existing address to"
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
              Optional.of(address.getPostalCode()), address.getCountryCode())
          .ifPresent(
              result -> {
                address.setLatitude(Optional.of(result.latitude().doubleValue()));
                address.setLongitude(Optional.of(result.longitude().doubleValue()));
                address.setGeocodeAccuracy(Optional.of(result.accuracy()));
              });
    }

    ContactAddress updatedAddress = addressRepository.save(address);
    log.info(
        "Address updated: {} for contact {} in team {}",
        addressId,
        address.getContactId(),
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
        "CONTACT_ADDRESS",
        updatedAddress.getId(),
        principal.getUserId(),
        oldAddress,
        updatedAddress,
        changedFields);

    return addressMapper.toResponse(updatedAddress);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteAddress(UUID contactId, UUID addressId, UserPrincipal principal) {
    ContactAddress address =
        addressRepository.getByIdAndTeamId(addressId, principal.requireTeamId());
    if (!address.getContactId().equals(contactId)) {
      throw new IllegalArgumentException("Address does not belong to the specified contact");
    }

    addressRepository.softDeleteByIdAndTeamId(addressId, principal.requireTeamId());
    log.info(
        "Address soft deleted: {} for contact {} in team {}",
        addressId,
        address.getContactId(),
        principal.requireTeamId());

    // Log to audit trail
    auditService.logDelete(
        principal.requireTeamId(), "CONTACT_ADDRESS", addressId, principal.getUserId(), address);
  }
}
