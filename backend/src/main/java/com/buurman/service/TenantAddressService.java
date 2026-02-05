package com.buurman.service;

import com.buurman.domain.Tenant;
import com.buurman.domain.TenantAddress;
import com.buurman.dto.request.CreateTenantAddressRequest;
import com.buurman.dto.request.UpdateTenantAddressRequest;
import com.buurman.dto.response.TenantAddressResponse;
import com.buurman.mapper.TenantAddressMapper;
import com.buurman.repository.TenantAddressRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantAddressService {

    private static final Logger log = LoggerFactory.getLogger(TenantAddressService.class);

    private final TenantAddressRepository addressRepository;
    private final TenantRepository tenantRepository;
    private final TenantAddressMapper addressMapper;
    private final AuditService auditService;

    public TenantAddressService(
            TenantAddressRepository addressRepository,
            TenantRepository tenantRepository,
            TenantAddressMapper addressMapper,
            AuditService auditService) {
        this.addressRepository = addressRepository;
        this.tenantRepository = tenantRepository;
        this.addressMapper = addressMapper;
        this.auditService = auditService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public TenantAddressResponse createAddress(
            UUID tenantId,
            CreateTenantAddressRequest request,
            UserPrincipal principal) {
        // Verify tenant exists and belongs to user's team
        Tenant tenant = tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        // Check for unique ACTIVE CURRENT address constraint
        if (request.addressType() == TenantAddress.AddressType.CURRENT &&
            (request.status() == null || request.status() == TenantAddress.AddressStatus.ACTIVE)) {
            List<TenantAddress> existingAddresses = addressRepository.findByTenantId(tenantId, principal.getTeamId());
            boolean hasActiveCurrent = existingAddresses.stream()
                    .anyMatch(addr -> addr.getAddressType() == TenantAddress.AddressType.CURRENT &&
                                     addr.getStatus() == TenantAddress.AddressStatus.ACTIVE);
            if (hasActiveCurrent) {
                throw new IllegalArgumentException(
                    "Tenant already has an active CURRENT address. Please set existing address to INACTIVE first."
                );
            }
        }

        TenantAddress address = addressMapper.toEntity(request);
        address.setTenantId(tenantId);
        address.setTeamId(principal.getTeamId());
        address.setCreatedBy(principal.getUserId());
        address.setUpdatedBy(principal.getUserId());
        address.setCreatedAt(Instant.now());
        address.setUpdatedAt(Instant.now());

        // Set default status if not provided
        if (address.getStatus() == null) {
            address.setStatus(TenantAddress.AddressStatus.ACTIVE);
        }

        TenantAddress savedAddress = addressRepository.save(address);
        log.info("Address created for tenant {} in team {}: {} - {}, {}",
                tenantId, principal.getTeamId(), savedAddress.getStreet(), savedAddress.getCity(), savedAddress.getCountry());

        // Log to audit trail
        auditService.logCreate(
                principal.getTeamId(),
                "TENANT_ADDRESS",
                savedAddress.getId(),
                principal.getUserId(),
                savedAddress
        );

        return addressMapper.toResponse(savedAddress);
    }

    public List<TenantAddressResponse> getAddresses(UUID tenantId, UserPrincipal principal) {
        // Verify tenant exists and belongs to user's team
        tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found or access denied"));

        List<TenantAddress> addresses = addressRepository.findByTenantId(tenantId, principal.getTeamId());
        return addresses.stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    public TenantAddressResponse getAddress(UUID tenantId, UUID addressId, UserPrincipal principal) {
        TenantAddress address = addressRepository.findByIdAndTeamId(addressId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found or access denied"));
        if (!address.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Address does not belong to the specified tenant");
        }
        return addressMapper.toResponse(address);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public TenantAddressResponse updateAddress(
            UUID tenantId,
            UUID addressId,
            UpdateTenantAddressRequest request,
            UserPrincipal principal) {
        TenantAddress address = addressRepository.findByIdAndTeamId(addressId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found or access denied"));
        if (!address.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Address does not belong to the specified tenant");
        }

        // Store old values for audit
        TenantAddress oldAddress = new TenantAddress(
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
                address.getDeletedAt()
        );

        // Check for unique ACTIVE CURRENT address constraint
        if (request.addressType() == TenantAddress.AddressType.CURRENT &&
            request.status() == TenantAddress.AddressStatus.ACTIVE) {
            // If this address is being changed to CURRENT ACTIVE, check for conflicts
            if (address.getAddressType() != TenantAddress.AddressType.CURRENT ||
                address.getStatus() != TenantAddress.AddressStatus.ACTIVE) {
                List<TenantAddress> existingAddresses = addressRepository.findByTenantId(
                        address.getTenantId(), principal.getTeamId());
                boolean hasOtherActiveCurrent = existingAddresses.stream()
                        .anyMatch(addr -> !addr.getId().equals(addressId) &&
                                         addr.getAddressType() == TenantAddress.AddressType.CURRENT &&
                                         addr.getStatus() == TenantAddress.AddressStatus.ACTIVE);
                if (hasOtherActiveCurrent) {
                    throw new IllegalArgumentException(
                        "Tenant already has an active CURRENT address. Please set existing address to INACTIVE first."
                    );
                }
            }
        }

        // Update address fields
        addressMapper.updateEntity(address, request);
        address.setUpdatedBy(principal.getUserId());
        address.setUpdatedAt(Instant.now());

        TenantAddress updatedAddress = addressRepository.save(address);
        log.info("Address updated: {} for tenant {} in team {}",
                addressId, address.getTenantId(), principal.getTeamId());

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
                changedFields
        );

        return addressMapper.toResponse(updatedAddress);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void deleteAddress(UUID tenantId, UUID addressId, UserPrincipal principal) {
        TenantAddress address = addressRepository.findByIdAndTeamId(addressId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found or access denied"));
        if (!address.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Address does not belong to the specified tenant");
        }

        addressRepository.softDeleteByIdAndTeamId(addressId, principal.getTeamId());
        log.info("Address soft deleted: {} for tenant {} in team {}",
                addressId, address.getTenantId(), principal.getTeamId());

        // Log to audit trail
        auditService.logDelete(
                principal.getTeamId(),
                "TENANT_ADDRESS",
                addressId,
                principal.getUserId(),
                address
        );
    }
}
