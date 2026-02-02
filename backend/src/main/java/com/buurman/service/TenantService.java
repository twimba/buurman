package com.buurman.service;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyTenantHistory;
import com.buurman.domain.Tenant;
import com.buurman.domain.User;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.LinkTenantToPropertyRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.PropertyTenantHistoryResponse;
import com.buurman.dto.response.TenantResponse;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyTenantHistoryRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyTenantHistoryRepository historyRepository;
    private final TenantMapper tenantMapper;
    private final PropertyMapper propertyMapper;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public TenantService(
            TenantRepository tenantRepository,
            PropertyRepository propertyRepository,
            PropertyTenantHistoryRepository historyRepository,
            TenantMapper tenantMapper,
            PropertyMapper propertyMapper,
            AuditService auditService,
            UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.propertyRepository = propertyRepository;
        this.historyRepository = historyRepository;
        this.tenantMapper = tenantMapper;
        this.propertyMapper = propertyMapper;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    public TenantResponse createTenant(CreateTenantRequest request, UserPrincipal principal) {
        // Check if email already exists
        tenantRepository.findByEmailAndTeamId(request.email(), principal.getTeamId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tenant with email " + request.email() + " already exists");
                });

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setIdentifier(UlidGenerator.generate());
        tenant.setTeamId(principal.getTeamId());
        tenant.setCreatedBy(principal.getUserId());
        tenant.setUpdatedBy(principal.getUserId());

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Tenant created: {} for team {}", savedTenant.getId(), principal.getTeamId());

        // Log to audit trail
        auditService.logCreate(
                principal.getTeamId(),
                "TENANT",
                savedTenant.getId(),
                principal.getUserId(),
                savedTenant
        );

        return toResponse(savedTenant, principal.getTeamId());
    }

    public List<TenantResponse> getAllTenants(UserPrincipal principal) {
        List<Tenant> tenants = tenantRepository.findAllByTeamId(principal.getTeamId());
        return tenants.stream()
                .map(tenant -> toResponse(tenant, principal.getTeamId()))
                .toList();
    }

    public List<TenantResponse> searchTenants(String searchTerm, UserPrincipal principal) {
        List<Tenant> tenants = tenantRepository.searchByTeamId(principal.getTeamId(), searchTerm);
        return tenants.stream()
                .map(tenant -> toResponse(tenant, principal.getTeamId()))
                .toList();
    }

    public TenantResponse getTenant(UUID tenantId, UserPrincipal principal) {
        Tenant tenant = tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        return toResponse(tenant, principal.getTeamId());
    }

    @Transactional
    public TenantResponse updateTenant(UUID tenantId, UpdateTenantRequest request, UserPrincipal principal) {
        Tenant tenant = tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        // Check if email is being changed to an existing email
        if (!tenant.getEmail().equals(request.email())) {
            tenantRepository.findByEmailAndTeamId(request.email(), principal.getTeamId())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(tenantId)) {
                            throw new IllegalArgumentException("Tenant with email " + request.email() + " already exists");
                        }
                    });
        }

        Tenant oldTenant = cloneTenant(tenant);
        tenantMapper.updateEntity(tenant, request);
        tenant.setUpdatedBy(principal.getUserId());

        Tenant updatedTenant = tenantRepository.save(tenant);
        log.info("Tenant updated: {} for team {}", updatedTenant.getId(), principal.getTeamId());

        // Log to audit trail
        auditService.logUpdate(
                principal.getTeamId(),
                "TENANT",
                updatedTenant.getId(),
                principal.getUserId(),
                oldTenant,
                updatedTenant,
                auditService.getChangedFields(oldTenant, updatedTenant)
        );

        return toResponse(updatedTenant, principal.getTeamId());
    }

    @Transactional
    public void deleteTenant(UUID tenantId, UserPrincipal principal) {
        Tenant tenant = tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        // Unlink from property if linked
        if (tenant.getCurrentPropertyId() != null) {
            unlinkTenantFromProperty(tenantId, principal);
        }

        tenantRepository.softDeleteByIdAndTeamId(tenantId, principal.getTeamId());
        log.info("Tenant deleted: {} for team {}", tenantId, principal.getTeamId());

        // Log to audit trail
        auditService.logDelete(
                principal.getTeamId(),
                "TENANT",
                tenantId,
                principal.getUserId(),
                tenant
        );
    }

    @Transactional
    public TenantResponse linkTenantToProperty(UUID tenantId, LinkTenantToPropertyRequest request, UserPrincipal principal) {
        Tenant tenant = tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        Property property = propertyRepository.findByIdAndTeamId(request.propertyId(), principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Check if tenant is already linked to a property
        if (tenant.getCurrentPropertyId() != null) {
            throw new IllegalArgumentException("Tenant is already linked to a property. Unlink first.");
        }

        // Check if property already has a tenant
        List<Tenant> existingTenants = tenantRepository.findByCurrentPropertyId(request.propertyId(), principal.getTeamId());
        if (!existingTenants.isEmpty()) {
            throw new IllegalArgumentException("Property already has a tenant assigned");
        }

        // Link tenant to property
        tenant.setCurrentPropertyId(request.propertyId());
        tenant.setUpdatedBy(principal.getUserId());
        Tenant updatedTenant = tenantRepository.save(tenant);

        // Create history record
        PropertyTenantHistory history = new PropertyTenantHistory();
        history.setTeamId(principal.getTeamId());
        history.setPropertyId(request.propertyId());
        history.setTenantId(tenantId);
        history.setMovedInAt(request.movedInAt() != null ? request.movedInAt() : Instant.now());
        history.setActionType(PropertyTenantHistory.ActionType.LINKED);
        history.setPerformedBy(principal.getUserId());
        history.setPerformedAt(Instant.now());
        historyRepository.save(history);

        log.info("Tenant {} linked to property {} for team {}", tenantId, request.propertyId(), principal.getTeamId());

        // Log to audit trail
        auditService.logCreate(
                principal.getTeamId(),
                "PROPERTY_TENANT_LINK",
                history.getId(),
                principal.getUserId(),
                history
        );

        return toResponse(updatedTenant, principal.getTeamId());
    }

    @Transactional
    public TenantResponse unlinkTenantFromProperty(UUID tenantId, UserPrincipal principal) {
        Tenant tenant = tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        if (tenant.getCurrentPropertyId() == null) {
            throw new IllegalArgumentException("Tenant is not linked to any property");
        }

        UUID propertyId = tenant.getCurrentPropertyId();

        // Unlink tenant from property
        tenant.setCurrentPropertyId(null);
        tenant.setUpdatedBy(principal.getUserId());
        Tenant updatedTenant = tenantRepository.save(tenant);

        // Create history record
        PropertyTenantHistory history = new PropertyTenantHistory();
        history.setTeamId(principal.getTeamId());
        history.setPropertyId(propertyId);
        history.setTenantId(tenantId);
        history.setMovedOutAt(Instant.now());
        history.setActionType(PropertyTenantHistory.ActionType.UNLINKED);
        history.setPerformedBy(principal.getUserId());
        history.setPerformedAt(Instant.now());
        historyRepository.save(history);

        log.info("Tenant {} unlinked from property {} for team {}", tenantId, propertyId, principal.getTeamId());

        // Log to audit trail
        auditService.logCreate(
                principal.getTeamId(),
                "PROPERTY_TENANT_UNLINK",
                history.getId(),
                principal.getUserId(),
                history
        );

        return toResponse(updatedTenant, principal.getTeamId());
    }

    public List<PropertyTenantHistoryResponse> getTenantHistory(UUID tenantId, UserPrincipal principal) {
        // Verify tenant exists and belongs to team
        tenantRepository.findByIdAndTeamId(tenantId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        List<PropertyTenantHistory> history = historyRepository.findByTenantId(tenantId, principal.getTeamId());
        return history.stream()
                .map(h -> toHistoryResponse(h, principal.getTeamId()))
                .toList();
    }

    private TenantResponse toResponse(Tenant tenant, UUID teamId) {
        TenantResponse response = tenantMapper.toResponse(tenant);

        // Load current property if linked
        if (tenant.getCurrentPropertyId() != null) {
            Property property = propertyRepository.findByIdAndTeamId(tenant.getCurrentPropertyId(), teamId)
                    .orElse(null);
            if (property != null) {
                PropertySummary propertySummary = new PropertySummary(
                        property.getId(),
                        property.getIdentifier(),
                        property.getStreet(),
                        property.getCity(),
                        property.getPostalCode(),
                        property.getPropertyType(),
                        property.getStatus()
                );
                return new TenantResponse(
                        response.id(),
                        response.identifier(),
                        response.teamId(),
                        response.name(),
                        response.email(),
                        response.phone(),
                        response.taxNumber(),
                        response.idNumber(),
                        propertySummary,
                        response.createdAt(),
                        response.updatedAt()
                );
            }
        }

        return response;
    }

    private PropertyTenantHistoryResponse toHistoryResponse(PropertyTenantHistory history, UUID teamId) {
        Property property = propertyRepository.findByIdAndTeamId(history.getPropertyId(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        PropertySummary propertySummary = new PropertySummary(
                property.getId(),
                property.getIdentifier(),
                property.getStreet(),
                property.getCity(),
                property.getPostalCode(),
                property.getPropertyType(),
                property.getStatus()
        );

        // Fetch user name
        String userName = userRepository.findById(history.getPerformedBy())
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Unknown User");

        return new PropertyTenantHistoryResponse(
                history.getId(),
                propertySummary,
                history.getMovedInAt(),
                history.getMovedOutAt(),
                history.getActionType(),
                userName,
                history.getPerformedAt()
        );
    }

    private Tenant cloneTenant(Tenant tenant) {
        return new Tenant(
                tenant.getId(),
                tenant.getIdentifier(),
                tenant.getTeamId(),
                tenant.getName(),
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
                tenant.getDeletedAt()
        );
    }
}
