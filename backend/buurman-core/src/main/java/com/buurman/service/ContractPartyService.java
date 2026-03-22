package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractStatus.EXPIRED;
import static com.buurman.domain.Contract.ContractStatus.TERMINATED;
import static com.buurman.util.SidGenerator.newContractPartyId;

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

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPartyIdentifier;
import com.buurman.dto.request.AddContractPartyRequest;
import com.buurman.dto.request.ChangePrimaryContactRequest;
import com.buurman.dto.request.ContractPartyRequest;
import com.buurman.dto.request.CreateContactRequest;
import com.buurman.dto.response.ContactResponse;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.ContractPartyMapper;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractPartyService {

  private final ContractPartyRepository contractPartyRepository;
  private final ContractRepository contractRepository;
  private final ContactRepository contactRepository;
  private final ContactService contactService;
  private final ContractPartyMapper contractPartyMapper;
  private final ContactMapper contactMapper;
  private final AuditService auditService;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractPartyResponse addParty(
      ContractIdentifier contractIdentifier,
      AddContractPartyRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    validateContractEditable(contract);

    if (request.role() == ContractPartyRole.PRIMARY_TENANT) {
      throw new IllegalArgumentException(
          "Cannot add PRIMARY_TENANT directly. Use change primary contact instead.");
    }

    Contact contact =
        resolveOrCreateContact(
            request.contactIdentifier().map(ContactIdentifier::of).orElse(null),
            request.newContact().orElse(null),
            principal);

    validateContactTypeForRole(contact, request.role());

    if (contractPartyRepository.existsByContractIdAndContactIdAndTeamId(
        contract.getId(), contact.getId(), teamId)) {
      throw new IllegalArgumentException("Contact is already a party to this contract");
    }

    ContractParty party = new ContractParty();
    party.setIdentifier(Optional.of(newContractPartyId()));
    party.setTeamId(teamId);
    party.setContractId(contract.getId());
    party.setContactId(Optional.of(contact.getId()));
    party.setRole(request.role());
    party.setCreatedAt(clock.instant());
    party.setUpdatedAt(clock.instant());
    party.setCreatedBy(principal.getUserId());
    party.setUpdatedBy(principal.getUserId());

    ContractParty saved = contractPartyRepository.save(party);
    log.info(
        "Party added to contract {}: contact {} as {}",
        contractIdentifier,
        contact.getIdentifier().orElseThrow(),
        request.role());

    // Audit log
    String contactName = contact.getDisplayName();
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("partyAdded", contactName);
    changedFields.put("role", request.role().name());
    auditService.logUpdate(
        teamId,
        "CONTRACT",
        contract.getId(),
        principal.getUserId(),
        Map.of("parties", "unchanged"),
        Map.of("parties", "updated"),
        changedFields);

    ContactSummary contactSummary = contactMapper.toSummary(contact);
    return contractPartyMapper.toResponse(saved, contactSummary);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void removeParty(
      ContractIdentifier contractIdentifier,
      ContractPartyIdentifier partyIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    validateContractEditable(contract);

    ContractParty party = contractPartyRepository.getByIdentifierAndTeamId(partyIdentifier, teamId);

    if (!party.getContractId().equals(contract.getId())) {
      throw new IllegalArgumentException("Party does not belong to this contract");
    }

    if (party.getRole() == ContractPartyRole.PRIMARY_TENANT) {
      throw new IllegalArgumentException(
          "Cannot remove primary contact. Use change primary contact instead.");
    }

    contractPartyRepository.softDeleteByIdAndTeamId(party.getId(), teamId);
    log.info("Party removed from contract {}: {}", contractIdentifier, partyIdentifier);

    // Audit log
    String contactName =
        party
            .getContactId()
            .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId))
            .map(Contact::getDisplayName)
            .orElse("Unknown");
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("partyRemoved", contactName);
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
  public ContractPartyResponse changePrimaryContact(
      ContractIdentifier contractIdentifier,
      ChangePrimaryContactRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    validateContractEditable(contract);

    Contact newContact =
        resolveOrCreateContact(
            request.contactIdentifier().map(ContactIdentifier::of).orElse(null),
            request.newContact().orElse(null),
            principal);

    validateContactTypeForRole(newContact, ContractPartyRole.PRIMARY_TENANT);

    // Find current primary contact
    ContractParty currentPrimary =
        contractPartyRepository.getPrimaryContactByContractIdAndTeamId(contract.getId(), teamId);

    if (currentPrimary.getContactId().filter(newContact.getId()::equals).isPresent()) {
      throw new IllegalArgumentException("This contact is already the primary contact");
    }

    String oldContactName =
        currentPrimary
            .getContactId()
            .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId))
            .map(Contact::getDisplayName)
            .orElse("Unknown");
    String newContactName = newContact.getDisplayName();

    // If new contact already exists as a different role on this contract, remove that entry
    List<ContractParty> existingParties =
        contractPartyRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    existingParties.stream()
        .filter(p -> p.getContactId().filter(newContact.getId()::equals).isPresent())
        .findFirst()
        .ifPresent(
            existing -> contractPartyRepository.softDeleteByIdAndTeamId(existing.getId(), teamId));

    // Soft delete old primary
    contractPartyRepository.softDeleteByIdAndTeamId(currentPrimary.getId(), teamId);

    // Create new primary
    ContractParty newPrimary = new ContractParty();
    newPrimary.setIdentifier(Optional.of(newContractPartyId()));
    newPrimary.setTeamId(teamId);
    newPrimary.setContractId(contract.getId());
    newPrimary.setContactId(Optional.of(newContact.getId()));
    newPrimary.setRole(ContractPartyRole.PRIMARY_TENANT);
    newPrimary.setCreatedAt(clock.instant());
    newPrimary.setUpdatedAt(clock.instant());
    newPrimary.setCreatedBy(principal.getUserId());
    newPrimary.setUpdatedBy(principal.getUserId());

    ContractParty saved = contractPartyRepository.save(newPrimary);
    log.info(
        "Primary contact changed on contract {}: {} -> {}",
        contractIdentifier,
        oldContactName,
        newContactName);

    // Audit log
    Map<String, Object> changedFields = new HashMap<>();
    changedFields.put("primaryContactChanged", newContactName);
    changedFields.put("previousPrimaryContact", oldContactName);
    auditService.logUpdate(
        teamId,
        "CONTRACT",
        contract.getId(),
        principal.getUserId(),
        Map.of("primaryContact", oldContactName),
        Map.of("primaryContact", newContactName),
        changedFields);

    ContactSummary contactSummary = contactMapper.toSummary(newContact);
    return contractPartyMapper.toResponse(saved, contactSummary);
  }

  /** Create parties for a new contract (called during contract creation). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void createPartiesForContract(
      UUID contractId, List<ContractPartyRequest> parties, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    UUID userId = principal.getUserId();
    Set<UUID> seenContactIds = new HashSet<>();

    for (ContractPartyRequest partyReq : parties) {
      Contact contact =
          resolveOrCreateContact(
              partyReq.contactIdentifier().map(ContactIdentifier::of).orElse(null),
              partyReq.newContact().orElse(null),
              principal);

      validateContactTypeForRole(contact, partyReq.role());

      if (!seenContactIds.add(contact.getId())) {
        String contactName = contact.getDisplayName();
        throw new IllegalArgumentException(
            "Contact \"" + contactName + "\" is listed more than once in the contract parties");
      }

      ContractParty party = new ContractParty();
      party.setIdentifier(Optional.of(newContractPartyId()));
      party.setTeamId(teamId);
      party.setContractId(contractId);
      party.setContactId(Optional.of(contact.getId()));
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
      copy.setIdentifier(Optional.of(newContractPartyId()));
      copy.setTeamId(teamId);
      copy.setContractId(targetContractId);
      copy.setContactId(source.getContactId());
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

  /** Get the primary contact for a contract. */
  public Contact getPrimaryContactForContract(UUID contractId, UUID teamId) {
    ContractParty primary =
        contractPartyRepository.getPrimaryContactByContractIdAndTeamId(contractId, teamId);
    UUID contactId =
        primary
            .getContactId()
            .orElseThrow(
                () -> new IllegalStateException("Primary contact party has no contact ID"));
    return contactRepository.getByIdAndTeamId(contactId, teamId);
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

  /** Build ContractPartyResponse list from parties, batch-loading contacts. */
  public List<ContractPartyResponse> buildPartyResponses(List<ContractParty> parties, UUID teamId) {
    if (parties.isEmpty()) {
      return List.of();
    }

    List<UUID> contactIds =
        parties.stream()
            .map(ContractParty::getContactId)
            .flatMap(Optional::stream)
            .distinct()
            .toList();
    List<Contact> contacts = contactRepository.findByIdsAndTeamId(contactIds, teamId);
    Map<UUID, Contact> contactMap =
        contacts.stream().collect(Collectors.toMap(Contact::getId, c -> c));

    return parties.stream()
        .map(
            party -> {
              ContactSummary summary =
                  party
                      .getContactId()
                      .map(contactMap::get)
                      .map(contactMapper::toSummary)
                      .orElse(null);
              return contractPartyMapper.toResponse(party, summary);
            })
        .toList();
  }

  /** Find primary contact for a contract (null-safe version). */
  public Optional<Contact> findPrimaryContactForContract(UUID contractId, UUID teamId) {
    return contractPartyRepository
        .findPrimaryContactByContractIdAndTeamId(contractId, teamId)
        .flatMap(
            party ->
                party
                    .getContactId()
                    .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId)));
  }

  /** Batch-load primary contacts for multiple contracts. Returns contractId -> Contact map. */
  public Map<UUID, Contact> getPrimaryContactsForContracts(
      Collection<UUID> contractIds, UUID teamId) {
    if (contractIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, List<ContractParty>> partiesByContract = getPartiesForContracts(contractIds, teamId);
    Map<UUID, UUID> contractToContactId = new HashMap<>();
    Set<UUID> contactIds = new HashSet<>();
    partiesByContract.forEach(
        (cId, parties) ->
            parties.stream()
                .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
                .findFirst()
                .ifPresent(
                    p ->
                        p.getContactId()
                            .ifPresent(
                                cid -> {
                                  contractToContactId.put(cId, cid);
                                  contactIds.add(cid);
                                })));
    if (contactIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Contact> contactsById =
        contactRepository.findByIdsAndTeamId(contactIds, teamId).stream()
            .collect(Collectors.toMap(Contact::getId, c -> c));
    Map<UUID, Contact> result = new HashMap<>();
    contractToContactId.forEach(
        (contractId, contactId) -> {
          Contact contact = contactsById.get(contactId);
          if (contact != null) {
            result.put(contractId, contact);
          }
        });
    return result;
  }

  private Contact resolveOrCreateContact(
      @Nullable ContactIdentifier contactIdentifier,
      @Nullable CreateContactRequest newContact,
      UserPrincipal principal) {
    if (contactIdentifier != null) {
      return contactRepository.getByIdentifierAndTeamId(
          contactIdentifier, principal.requireTeamId());
    }
    if (newContact != null) {
      ContactResponse created = contactService.createContact(newContact, principal);
      return contactRepository.getByIdentifierAndTeamId(
          created.identifier(), principal.requireTeamId());
    }
    throw new IllegalArgumentException("Either contactIdentifier or newContact must be provided");
  }

  private void validateContactTypeForRole(Contact contact, ContractPartyRole role) {
    switch (contact.getContactType()) {
      case SERVICE_PROVIDER ->
          throw new IllegalArgumentException(
              "Service providers cannot be added as contract parties");
      case INDIVIDUAL -> {
        if (role == ContractPartyRole.CORPORATE_TENANT
            || role == ContractPartyRole.AUTHORIZED_REPRESENTATIVE) {
          throw new IllegalArgumentException(
              "Individual contacts cannot have role: " + role.getDisplayName());
        }
      }
      case COMPANY -> {
        if (role == ContractPartyRole.PRIMARY_TENANT || role == ContractPartyRole.EXTRA_TENANT) {
          throw new IllegalArgumentException(
              "Company contacts cannot have role: "
                  + role.getDisplayName()
                  + ". Use CORPORATE_TENANT instead.");
        }
      }
    }
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
