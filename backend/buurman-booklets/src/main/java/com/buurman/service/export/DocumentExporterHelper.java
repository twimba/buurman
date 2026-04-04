package com.buurman.service.export;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
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
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.CurrencyUtils;

/**
 * Shared helper methods for document exporters. Eliminates duplication of contact resolution,
 * address lookup, legal clause resolution, and contract validation across exporters.
 */
@Component
class DocumentExporterHelper {

  private final ContractPartyService contractPartyService;
  private final ContactRepository contactRepository;
  private final ContactAddressRepository contactAddressRepository;

  DocumentExporterHelper(
      ContractPartyService contractPartyService,
      ContactRepository contactRepository,
      ContactAddressRepository contactAddressRepository) {
    this.contractPartyService = contractPartyService;
    this.contactRepository = contactRepository;
    this.contactAddressRepository = contactAddressRepository;
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
  Optional<Map<String, String>> buildAddressMap(Optional<ContactAddress> address) {
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
