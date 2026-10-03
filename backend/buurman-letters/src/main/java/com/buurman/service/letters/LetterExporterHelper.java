package com.buurman.service.letters;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.CurrencyUtils;

/**
 * Shared helper methods for document exporters. Eliminates duplication of contact resolution,
 * address lookup, legal clause resolution, and contract validation across exporters.
 */
@Component
class LetterExporterHelper {

  private final ContractPartyService contractPartyService;
  private final ContactRepository contactRepository;
  private final ContactAddressRepository contactAddressRepository;
  private final UnitRepository unitRepository;

  LetterExporterHelper(
      ContractPartyService contractPartyService,
      ContactRepository contactRepository,
      ContactAddressRepository contactAddressRepository,
      UnitRepository unitRepository) {
    this.contractPartyService = contractPartyService;
    this.contactRepository = contactRepository;
    this.contactAddressRepository = contactAddressRepository;
    this.unitRepository = unitRepository;
  }

  /** Every letter prints dates the same way. */
  static DateTimeFormatter letterDateFormatter(Locale locale) {
    return DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
  }

  /**
   * The let premises' unit and its property's total unit count, resolved from the contract, plus
   * the localized "unit " prefix used when a unit has no name of its own. Every exporter needs all
   * three to build both {@link #premisesAddress} and the multi-unit summary-table row.
   */
  record PremisesInfo(Unit unit, int propertyUnitCount, String unitPrefix) {
    boolean hasMultipleUnits() {
      return propertyUnitCount > 1;
    }

    /** The unit's own name, or "unit " plus its number when it has none. */
    String unitDesignation() {
      return unit.getName().orElseGet(() -> unitPrefix + unit.getUnitNumber());
    }
  }

  PremisesInfo premisesInfo(
      Contract contract, Property property, MessageSource messageSource, Locale locale) {
    UUID teamId = contract.getTeamId();
    Unit unit = unitRepository.getByIdAndTeamId(contract.getUnitId(), teamId);
    int propertyUnitCount =
        unitRepository.countActiveByPropertyIdAndTeamId(property.getId(), teamId);
    String unitPrefix = messageSource.getMessage("premises.unitPrefix", null, locale);
    return new PremisesInfo(unit, propertyUnitCount, unitPrefix);
  }

  /**
   * The address of the let premises, as it must appear on a legal notice. A multi-unit building
   * names the specific dwelling — a notice naming only the building is defective when the tenant
   * rents one apartment of several.
   *
   * <p>Keys on {@code propertyUnitCount}, not on {@link Unit#isImplicit()}: a landlord who split a
   * property into units and later deleted back down to one still has a single-dwelling tenancy, and
   * "Dorpsstraat 5, unit A" would read as an error to that tenant.
   */
  static String premisesAddress(
      Property property, Unit unit, int propertyUnitCount, String unitPrefix) {
    StringBuilder address = new StringBuilder(property.getStreet());
    if (propertyUnitCount > 1) {
      address
          .append(", ")
          .append(unit.getName().orElseGet(() -> unitPrefix + unit.getUnitNumber()));
    }
    return address
        .append(", ")
        .append(property.getPostalCode())
        .append(" ")
        .append(property.getCity())
        .toString();
  }

  /** Convenience overload taking an already-resolved {@link PremisesInfo}. */
  static String premisesAddress(Property property, PremisesInfo premisesInfo) {
    return premisesAddress(
        property, premisesInfo.unit(), premisesInfo.propertyUnitCount(), premisesInfo.unitPrefix());
  }

  /** Variables every letter starts with: generation date and the contract reference. */
  static Map<String, Object> headerVariables(
      Contract contract, LocalDate today, DateTimeFormatter dateFmt) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("generatedDate", today.format(dateFmt));
    vars.put(
        "contractIdentifier",
        contract
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Contract missing identifier"))
            .value());
    return vars;
  }

  /** Country code plus the country-specific legal clauses under the given key prefix. */
  Map<String, Object> legalVariables(
      MessageSource messageSource,
      String keyPrefix,
      String documentType,
      Contract contract,
      Locale locale) {
    Map<String, Object> vars = new HashMap<>();
    Optional<String> countryCode = contract.getCountryCode();
    vars.put("countryCode", countryCode.orElse(null));
    vars.put(
        "legalClauses",
        resolveLegalClauses(messageSource, keyPrefix, documentType, countryCode, locale));
    return vars;
  }

  /**
   * Resolves the ordered legal clauses for a letter: a {@link CountryLetterClauseCatalog} entry for
   * {@code documentType}/country if one exists (each clause pre-resolved to {@code {title, body}}
   * string maps, {@code title} omitted when the catalog gives no title key), else the single legacy
   * {@code keyPrefix + COUNTRY} clause wrapped as a one-item list, else empty.
   */
  List<Map<String, String>> resolveLegalClauses(
      MessageSource messageSource,
      String keyPrefix,
      String documentType,
      Optional<String> countryCode,
      Locale locale) {
    return countryCode
        .map(
            code -> {
              List<LetterClauseKey> catalogClauses =
                  CountryLetterClauseCatalog.resolve(documentType, code);
              if (!catalogClauses.isEmpty()) {
                return catalogClauses.stream()
                    .map(
                        clause -> {
                          Map<String, String> resolved = new HashMap<>();
                          resolved.put(
                              "title", messageSource.getMessage(clause.titleKey(), null, locale));
                          resolved.put(
                              "body", messageSource.getMessage(clause.bodyKey(), null, locale));
                          return resolved;
                        })
                    .toList();
              }
              return resolveLegalClause(messageSource, keyPrefix, Optional.of(code), locale)
                  .map(body -> List.of(Map.of("body", body)))
                  .orElse(List.<Map<String, String>>of());
            })
        .orElse(List.of());
  }

  /** The tenant a letter is addressed to, with their mailing address when known. */
  record Addressee(Optional<Contact> contact, Optional<ContactAddress> address) {
    Map<String, Object> variables() {
      Map<String, Object> vars = new HashMap<>();
      vars.put("primaryContactName", contact.map(Contact::getDisplayName).orElse(null));
      vars.put("contactAddress", buildAddressMap(address).orElse(null));
      return vars;
    }
  }

  /** The contract's primary tenant as addressee. */
  Addressee addressee(UUID contractId, UUID teamId) {
    PartyData partyData = loadPartyData(contractId, teamId);
    Optional<Contact> primary = findPrimaryContact(partyData.parties(), partyData.contactMap());
    return new Addressee(primary, primary.flatMap(c -> findMailingAddress(c.getId(), teamId)));
  }

  /** A specific contact as addressee (e.g. the payer of a payment). */
  Addressee addressee(Contact contact, UUID teamId) {
    return new Addressee(Optional.of(contact), findMailingAddress(contact.getId(), teamId));
  }

  /** Loads contract parties and resolves their contacts into a map. */
  record PartyData(List<ContractParty> parties, Map<UUID, Contact> contactMap) {}

  PartyData loadPartyData(UUID contractId, UUID teamId) {
    List<ContractParty> parties = contractPartyService.getPartiesForContract(contractId, teamId);
    Set<UUID> contactIds = new HashSet<>();
    for (ContractParty party : parties) {
      party.getContactId().ifPresent(contactIds::add);
    }
    Map<UUID, Contact> contactMap =
        contactRepository.findByIdsAndTeamId(contactIds, teamId).stream()
            .collect(toMap(Contact::getId, t -> t));
    return new PartyData(parties, contactMap);
  }

  /**
   * Signature blocks for a document that may be sent for e-signature: the landlord always first
   * with a fixed placeholder, then one block per distinct tenant email (deduped, in contract-party
   * order), numbered {@code "signature-tenant-N"}.
   *
   * <p>These placeholders are matched against this exact literal text in the rendered PDF by {@code
   * DocumensoClient} to place each signer's signature field, so they must agree exactly with {@code
   * SignatureService}'s own numbering — both sides independently iterate the same contract-party
   * list in the same order, which is what keeps the two in sync without the two modules sharing a
   * dependency.
   */
  List<Map<String, String>> signatureBlocks(
      UUID contractId,
      UUID teamId,
      MessageSource messageSource,
      String landlordLabelKey,
      Locale locale) {
    List<Map<String, String>> blocks = new ArrayList<>();
    blocks.add(
        Map.of(
            "label",
            messageSource.getMessage(landlordLabelKey, null, locale),
            "placeholder",
            "signature-landlord"));

    PartyData partyData = loadPartyData(contractId, teamId);
    Set<String> seenEmails = new HashSet<>();
    int tenantIndex = 0;
    for (ContractParty party : partyData.parties()) {
      Optional<Contact> contact = party.getContactId().map(partyData.contactMap()::get);
      Optional<String> email = contact.flatMap(Contact::getEmail);
      if (email.isEmpty() || !seenEmails.add(email.get().toLowerCase(Locale.ROOT))) {
        continue;
      }
      tenantIndex++;
      String label = contact.map(Contact::getDisplayName).orElse(email.get());
      blocks.add(Map.of("label", label, "placeholder", "signature-tenant-" + tenantIndex));
    }
    return blocks;
  }

  /** Finds the primary tenant contact from a list of contract parties. */
  Optional<Contact> findPrimaryContact(List<ContractParty> parties, Map<UUID, Contact> contactMap) {
    return parties.stream()
        .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
        .findFirst()
        .flatMap(p -> p.getContactId().map(contactMap::get));
  }

  /** Finds the primary tenant's display name, or em dash if not found. */
  String findPrimaryContactName(List<ContractParty> parties, Map<UUID, Contact> contactMap) {
    return findPrimaryContact(parties, contactMap).map(Contact::getDisplayName).orElse("\u2014");
  }

  /** Builds a comma-separated list of all contact names. */
  String buildContactNamesList(List<ContractParty> parties, Map<UUID, Contact> contactMap) {
    List<String> names =
        parties.stream()
            .filter(p -> p.getContactId().isPresent())
            .map(p -> p.getContactId().map(contactMap::get))
            .flatMap(Optional::stream)
            .map(Contact::getDisplayName)
            .toList();
    if (names.isEmpty()) {
      return "\u2014";
    }
    return String.join(", ", names);
  }

  /** Finds the mailing or current address for a contact. Prefers MAILING over CURRENT. */
  Optional<ContactAddress> findMailingAddress(UUID contactId, UUID teamId) {
    List<ContactAddress> addresses = contactAddressRepository.findByContactId(contactId, teamId);
    return addresses.stream()
        .filter(
            a ->
                a.getAddressType() == ContactAddress.AddressType.MAILING
                    && a.getStatus() == ContactAddress.AddressStatus.ACTIVE)
        .findFirst()
        .or(
            () ->
                addresses.stream()
                    .filter(
                        a ->
                            a.getAddressType() == ContactAddress.AddressType.CURRENT
                                && a.getStatus() == ContactAddress.AddressStatus.ACTIVE)
                    .findFirst());
  }

  /** Builds an address map for template rendering, or empty if no address. */
  static Optional<Map<String, String>> buildAddressMap(Optional<ContactAddress> address) {
    return address.map(
        a -> {
          Map<String, String> addrMap = new HashMap<>();
          addrMap.put("street", a.getStreet());
          addrMap.put("postalCode", a.getPostalCode());
          addrMap.put("city", a.getCity());
          addrMap.put("countryCode", a.getCountryCode());
          return addrMap;
        });
  }

  /**
   * Resolves a country-specific legal clause from the message source.
   *
   * @param messageSource the message source to query
   * @param keyPrefix the message key prefix (e.g., "legal." or "rentchange.legal.")
   * @param countryCode the ISO country code
   * @param locale the locale for message resolution
   * @return the legal clause text, or empty if not found
   */
  Optional<String> resolveLegalClause(
      MessageSource messageSource, String keyPrefix, Optional<String> countryCode, Locale locale) {
    return countryCode.flatMap(
        code -> {
          String key = keyPrefix + code.toUpperCase(Locale.ROOT);
          try {
            String resolved = messageSource.getMessage(key, null, locale);
            return key.equals(resolved) ? Optional.empty() : Optional.of(resolved);
          } catch (NoSuchMessageException e) {
            return Optional.empty();
          }
        });
  }

  /**
   * Builds a human-readable adjustment basis string from a contract extension.
   *
   * @param messageSource the message source for i18n
   * @param keyPrefix the message key prefix (e.g., "letter.body.adjustment." or
   *     "rentchange.body.adjustment.")
   * @param extension the contract extension
   * @param locale the locale
   * @return the adjustment basis description
   */
  String buildAdjustmentBasis(
      MessageSource messageSource, String keyPrefix, ContractExtension extension, Locale locale) {
    return switch (extension.getRentAdjustmentType()) {
      case FIXED_PERCENTAGE -> {
        String pct =
            extension
                .getRentAdjustmentValue()
                .map(v -> v.stripTrailingZeros().toPlainString() + "%")
                .orElse("a percentage");
        yield messageSource.getMessage(keyPrefix + "fixedPercentage", new Object[] {pct}, locale);
      }
      case FIXED_AMOUNT -> {
        String amt =
            extension
                .getRentAdjustmentValue()
                .map(v -> CurrencyUtils.formatCurrency(v, extension.getNewRentAmount().currency()))
                .orElse("a fixed amount");
        yield messageSource.getMessage(keyPrefix + "fixedAmount", new Object[] {amt}, locale);
      }
      case MANUAL -> messageSource.getMessage(keyPrefix + "manual", null, locale);
      case NONE -> messageSource.getMessage(keyPrefix + "none", null, locale);
    };
  }

  /** Formats an adjustment value for display (appends % for percentage types). */
  static String formatAdjustmentDisplay(
      BigDecimal value, ContractExtension.RentAdjustmentType type) {
    return switch (type) {
      case FIXED_PERCENTAGE -> value.stripTrailingZeros().toPlainString() + "%";
      case FIXED_AMOUNT, MANUAL, NONE -> value.stripTrailingZeros().toPlainString();
    };
  }

  /**
   * Validates that a contract identifier matches the contract's actual identifier.
   *
   * @param contractIdentifier the expected identifier (empty = skip validation)
   * @param contract the contract to validate
   * @param errorPrefix error message prefix (e.g., "Extension" or "Rent period")
   */
  static void validateContractOwnership(
      Optional<ContractIdentifier> contractIdentifier, Contract contract, String errorPrefix) {
    contractIdentifier.ifPresent(
        expected ->
            contract
                .getIdentifier()
                .ifPresent(
                    actual -> {
                      if (!actual.equals(expected)) {
                        throw new BadRequestException(
                            errorPrefix + " does not belong to contract: " + expected.value());
                      }
                    }));
  }
}
