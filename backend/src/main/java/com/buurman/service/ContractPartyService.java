package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractStatus.EXPIRED;
import static com.buurman.domain.Contract.ContractStatus.TERMINATED;
import static com.buurman.util.UlidGenerator.newContractPartyId;

import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Tenant;
import com.buurman.dto.request.AddContractPartyRequest;
import com.buurman.dto.request.ChangePrimaryTenantRequest;
import com.buurman.dto.request.ContractPartyRequest;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.TenantResponse;
import com.buurman.dto.response.TenantSummary;
import com.buurman.mapper.ContractPartyMapper;
import com.buurman.mapper.TenantMapper;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractPartyService {

  private final ContractPartyRepository contractPartyRepository;
  private final ContractRepository contractRepository;
  private final TenantRepository tenantRepository;
  private final TenantService tenantService;
  private final ContractPartyMapper contractPartyMapper;
  private final TenantMapper tenantMapper;
  private final AuditService auditService;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractPartyResponse addParty(
      String contractIdentifier, AddContractPartyRequest request, UserPrincipal principal) {
    UUID teamId = principal.getTeamId();

    Contract contract =
        contractRepository
            .findByIdentifierAndTeamId(contractIdentifier, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

    validateContractEditable(contract);

    if (request.role() == ContractPartyRole.PRIMARY_TENANT) {
      throw new IllegalArgumentException(
          "Cannot add PRIMARY_TENANT directly. Use change primary tenant instead.");
    }

    Tenant tenant =
        resolveOrCreateTenant(request.tenantIdentifier(), request.newTenant(), principal);

    if (contractPartyRepository.existsByContractIdAndTenantIdAndTeamId(
        contract.getId(), tenant.getId(), teamId)) {
      throw new IllegalArgumentException("Tenant is already a party to this contract");
    }

    ContractParty party = new ContractParty();
    party.setIdentifier(newContractPartyId().value());
    party.setTeamId(teamId);
    party.setContractId(contract.getId());
    party.setTenantId(tenant.getId());
    party.setRole(request.role());
    party.setCreatedAt(clock.instant());
    party.setUpdatedAt(clock.instant());
    party.setCreatedBy(principal.getUserId());
    party.setUpdatedBy(principal.getUserId());

    ContractParty saved = contractPartyRepository.save(party);
    log.info(
        "Party added to contract {}: tenant {} as {}",
        contractIdentifier,
        tenant.getIdentifier(),
        request.role());

    // Audit log
    String tenantName = tenant.getFirstName() + " " + tenant.getLastName();
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("partyAdded", tenantName);
    changedFields.put("role", request.role().name());
    auditService.logUpdate(
        teamId,
        "CONTRACT",
        contract.getId(),
        principal.getUserId(),
        Map.of("parties", "unchanged"),
        Map.of("parties", "updated"),
        changedFields);

    TenantSummary tenantSummary = tenantMapper.toSummary(tenant);
    return contractPartyMapper.toResponse(saved, tenantSummary);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void removeParty(
      String contractIdentifier, String partyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId();

    Contract contract =
        contractRepository
            .findByIdentifierAndTeamId(contractIdentifier, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

    validateContractEditable(contract);

    ContractParty party =
        contractPartyRepository
            .findByIdentifierAndTeamId(partyIdentifier, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Party not found or access denied"));

    if (!party.getContractId().equals(contract.getId())) {
      throw new IllegalArgumentException("Party does not belong to this contract");
    }

    if (party.getRole() == ContractPartyRole.PRIMARY_TENANT) {
      throw new IllegalArgumentException(
          "Cannot remove primary tenant. Use change primary tenant instead.");
    }

    contractPartyRepository.softDeleteByIdAndTeamId(party.getId(), teamId);
    log.info("Party removed from contract {}: {}", contractIdentifier, partyIdentifier);

    // Audit log
    Tenant tenant = tenantRepository.findByIdAndTeamId(party.getTenantId(), teamId).orElse(null);
    String tenantName =
        tenant != null ? tenant.getFirstName() + " " + tenant.getLastName() : "Unknown";
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("partyRemoved", tenantName);
    changedFields.put("role", party.getRole().name());
    auditService.logUpdate(
        teamId,
        "CONTRACT",
        contract.getId(),
        principal.getUserId(),
        Map.of("parties", "unchanged"),
        Map.of("parties", "updated"),
        changedFields);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractPartyResponse changePrimaryTenant(
      String contractIdentifier, ChangePrimaryTenantRequest request, UserPrincipal principal) {
    UUID teamId = principal.getTeamId();

    Contract contract =
        contractRepository
            .findByIdentifierAndTeamId(contractIdentifier, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found or access denied"));

    validateContractEditable(contract);

    Tenant newTenant =
        resolveOrCreateTenant(request.tenantIdentifier(), request.newTenant(), principal);

    // Find current primary tenant
    ContractParty currentPrimary =
        contractPartyRepository
            .findPrimaryTenantByContractIdAndTeamId(contract.getId(), teamId)
            .orElseThrow(() -> new IllegalStateException("Contract has no primary tenant"));

    if (currentPrimary.getTenantId().equals(newTenant.getId())) {
      throw new IllegalArgumentException("This tenant is already the primary tenant");
    }

    Tenant oldTenant =
        tenantRepository.findByIdAndTeamId(currentPrimary.getTenantId(), teamId).orElse(null);
    String oldTenantName =
        oldTenant != null ? oldTenant.getFirstName() + " " + oldTenant.getLastName() : "Unknown";
    String newTenantName = newTenant.getFirstName() + " " + newTenant.getLastName();

    // If new tenant already exists as a different role on this contract, remove that entry
    List<ContractParty> existingParties =
        contractPartyRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    existingParties.stream()
        .filter(p -> p.getTenantId().equals(newTenant.getId()))
        .findFirst()
        .ifPresent(
            existing -> contractPartyRepository.softDeleteByIdAndTeamId(existing.getId(), teamId));

    // Soft delete old primary
    contractPartyRepository.softDeleteByIdAndTeamId(currentPrimary.getId(), teamId);

    // Create new primary
    ContractParty newPrimary = new ContractParty();
    newPrimary.setIdentifier(newContractPartyId().value());
    newPrimary.setTeamId(teamId);
    newPrimary.setContractId(contract.getId());
    newPrimary.setTenantId(newTenant.getId());
    newPrimary.setRole(ContractPartyRole.PRIMARY_TENANT);
    newPrimary.setCreatedAt(clock.instant());
    newPrimary.setUpdatedAt(clock.instant());
    newPrimary.setCreatedBy(principal.getUserId());
    newPrimary.setUpdatedBy(principal.getUserId());

    ContractParty saved = contractPartyRepository.save(newPrimary);
    log.info(
        "Primary tenant changed on contract {}: {} -> {}",
        contractIdentifier,
        oldTenantName,
        newTenantName);

    // Audit log
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("primaryTenantChanged", newTenantName);
    changedFields.put("previousPrimaryTenant", oldTenantName);
    auditService.logUpdate(
        teamId,
        "CONTRACT",
        contract.getId(),
        principal.getUserId(),
        Map.of("primaryTenant", oldTenantName),
        Map.of("primaryTenant", newTenantName),
        changedFields);

    TenantSummary tenantSummary = tenantMapper.toSummary(newTenant);
    return contractPartyMapper.toResponse(saved, tenantSummary);
  }

  /** Create parties for a new contract (called during contract creation). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void createPartiesForContract(
      UUID contractId, List<ContractPartyRequest> parties, UserPrincipal principal) {
    UUID teamId = principal.getTeamId();
    UUID userId = principal.getUserId();
    Set<UUID> seenTenantIds = new HashSet<>();

    for (ContractPartyRequest partyReq : parties) {
      Tenant tenant =
          resolveOrCreateTenant(partyReq.tenantIdentifier(), partyReq.newTenant(), principal);

      if (!seenTenantIds.add(tenant.getId())) {
        String tenantName = tenant.getFirstName() + " " + tenant.getLastName();
        throw new IllegalArgumentException(
            "Tenant \"" + tenantName + "\" is listed more than once in the contract parties");
      }

      ContractParty party = new ContractParty();
      party.setIdentifier(newContractPartyId().value());
      party.setTeamId(teamId);
      party.setContractId(contractId);
      party.setTenantId(tenant.getId());
      party.setRole(partyReq.role());
      party.setCreatedAt(clock.instant());
      party.setUpdatedAt(clock.instant());
      party.setCreatedBy(userId);
      party.setUpdatedBy(userId);

      contractPartyRepository.save(party);
    }
  }

  /** Duplicate all parties from a source contract to a target contract. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void duplicateParties(
      UUID sourceContractId, UUID targetContractId, UUID teamId, UUID userId) {
    List<ContractParty> sourceParties =
        contractPartyRepository.findByContractIdAndTeamId(sourceContractId, teamId);
    for (ContractParty source : sourceParties) {
      ContractParty copy = new ContractParty();
      copy.setIdentifier(newContractPartyId().value());
      copy.setTeamId(teamId);
      copy.setContractId(targetContractId);
      copy.setTenantId(source.getTenantId());
      copy.setRole(source.getRole());
      copy.setCreatedAt(clock.instant());
      copy.setUpdatedAt(clock.instant());
      copy.setCreatedBy(userId);
      copy.setUpdatedBy(userId);
      contractPartyRepository.save(copy);
    }
  }

  /** Soft-delete all parties for a contract (called during contract deletion). */
  public void softDeletePartiesForContract(UUID contractId, UUID teamId) {
    contractPartyRepository.softDeleteByContractIdAndTeamId(contractId, teamId);
  }

  /** Get the primary tenant for a contract. */
  public Tenant getPrimaryTenantForContract(UUID contractId, UUID teamId) {
    ContractParty primary =
        contractPartyRepository
            .findPrimaryTenantByContractIdAndTeamId(contractId, teamId)
            .orElseThrow(() -> new IllegalStateException("Contract has no primary tenant"));
    return tenantRepository
        .findByIdAndTeamId(primary.getTenantId(), teamId)
        .orElseThrow(() -> new IllegalStateException("Primary tenant not found"));
  }

  /** Get all parties for a contract. */
  public List<ContractParty> getPartiesForContract(UUID contractId, UUID teamId) {
    return contractPartyRepository.findByContractIdAndTeamId(contractId, teamId);
  }

  /** Batch-load parties for multiple contracts (N+1 avoidance). */
  public Map<UUID, List<ContractParty>> getPartiesForContracts(
      Collection<UUID> contractIds, UUID teamId) {
    List<ContractParty> allParties =
        contractPartyRepository.findByContractIdsAndTeamId(contractIds, teamId);
    return allParties.stream().collect(Collectors.groupingBy(ContractParty::getContractId));
  }

  /** Build ContractPartyResponse list from parties, batch-loading tenants. */
  public List<ContractPartyResponse> buildPartyResponses(List<ContractParty> parties, UUID teamId) {
    if (parties.isEmpty()) return List.of();

    List<UUID> tenantIds = parties.stream().map(ContractParty::getTenantId).distinct().toList();
    List<Tenant> tenants = tenantRepository.findByIdsAndTeamId(tenantIds, teamId);
    Map<UUID, Tenant> tenantMap = tenants.stream().collect(Collectors.toMap(Tenant::getId, t -> t));

    return parties.stream()
        .map(
            party -> {
              Tenant tenant = tenantMap.get(party.getTenantId());
              TenantSummary summary = tenant != null ? tenantMapper.toSummary(tenant) : null;
              return contractPartyMapper.toResponse(party, summary);
            })
        .toList();
  }

  /** Find primary tenant for a contract (null-safe version). */
  public Optional<Tenant> findPrimaryTenantForContract(UUID contractId, UUID teamId) {
    return contractPartyRepository
        .findPrimaryTenantByContractIdAndTeamId(contractId, teamId)
        .flatMap(party -> tenantRepository.findByIdAndTeamId(party.getTenantId(), teamId));
  }

  /** Batch-load primary tenants for multiple contracts. Returns contractId → Tenant map. */
  public Map<UUID, Tenant> getPrimaryTenantsForContracts(
      Collection<UUID> contractIds, UUID teamId) {
    if (contractIds.isEmpty()) return Map.of();
    Map<UUID, List<ContractParty>> partiesByContract = getPartiesForContracts(contractIds, teamId);
    Map<UUID, UUID> contractToTenantId = new HashMap<>();
    Set<UUID> tenantIds = new HashSet<>();
    partiesByContract.forEach(
        (cId, parties) ->
            parties.stream()
                .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
                .findFirst()
                .ifPresent(
                    p -> {
                      contractToTenantId.put(cId, p.getTenantId());
                      tenantIds.add(p.getTenantId());
                    }));
    if (tenantIds.isEmpty()) return Map.of();
    Map<UUID, Tenant> tenantsById =
        tenantRepository.findByIdsAndTeamId(tenantIds, teamId).stream()
            .collect(Collectors.toMap(Tenant::getId, t -> t));
    Map<UUID, Tenant> result = new HashMap<>();
    contractToTenantId.forEach(
        (contractId, tenantId) -> {
          Tenant tenant = tenantsById.get(tenantId);
          if (tenant != null) result.put(contractId, tenant);
        });
    return result;
  }

  private Tenant resolveOrCreateTenant(
      String tenantIdentifier, CreateTenantRequest newTenant, UserPrincipal principal) {
    if (tenantIdentifier != null && !tenantIdentifier.isBlank()) {
      return tenantRepository
          .findByIdentifierAndTeamId(tenantIdentifier, principal.getTeamId())
          .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantIdentifier));
    }
    if (newTenant != null) {
      TenantResponse created = tenantService.createTenant(newTenant, principal);
      return tenantRepository
          .findByIdentifierAndTeamId(created.identifier(), principal.getTeamId())
          .orElseThrow(() -> new IllegalStateException("Failed to retrieve newly created tenant"));
    }
    throw new IllegalArgumentException("Either tenantIdentifier or newTenant must be provided");
  }

  private void validateContractEditable(Contract contract) {
    if (contract.getStatus() == ACTIVE) {
      throw new IllegalArgumentException("Cannot modify parties on ACTIVE contracts.");
    }
    if (contract.getStatus() == TERMINATED) {
      throw new IllegalArgumentException("Cannot modify parties on TERMINATED contracts.");
    }
    if (contract.getStatus() == EXPIRED) {
      throw new IllegalArgumentException("Cannot modify parties on EXPIRED contracts.");
    }
  }
}
