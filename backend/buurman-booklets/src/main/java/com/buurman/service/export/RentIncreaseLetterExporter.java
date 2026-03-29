package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.formatEnumValue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.util.CurrencyUtils;

@Component
public class RentIncreaseLetterExporter {

  private final ContractExtensionRepository extensionRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final DocumentExporterHelper helper;
  private final DocumentTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public RentIncreaseLetterExporter(
      ContractExtensionRepository extensionRepository,
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      DocumentExporterHelper helper,
      DocumentTemplateService documentTemplateService,
      @Qualifier("documentMessageSource") MessageSource messageSource,
      Clock clock) {
    this.extensionRepository = extensionRepository;
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(ContractExtensionIdentifier extensionIdentifier, UUID teamId) {
    return generate(Optional.empty(), extensionIdentifier, teamId, "en");
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(
      Optional<ContractIdentifier> contractIdentifier,
      ContractExtensionIdentifier extensionIdentifier,
      UUID teamId,
      String lang) {
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);
    Contract contract = contractRepository.getByIdAndTeamId(extension.getContractId(), teamId);
    DocumentExporterHelper.validateContractOwnership(
        contractIdentifier, contract, "Extension");
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    DocumentExporterHelper.PartyData partyData =
        helper.loadPartyData(contract.getId(), teamId);

    Optional<Contact> primaryContact =
        helper.findPrimaryContact(partyData.parties(), partyData.contactMap());
    Optional<ContactAddress> address =
        primaryContact.flatMap(c -> helper.findMailingAddress(c.getId(), teamId));

    Locale locale = DocumentTemplateService.resolveLocale(lang);
    Map<String, Object> variables =
        buildTemplateVariables(extension, contract, property, primaryContact, address, locale);

    return documentTemplateService.renderToPdf("rent-increase-letter", locale, variables);
  }

  private Map<String, Object> buildTemplateVariables(
      ContractExtension extension,
      Contract contract,
      Property property,
      Optional<Contact> primaryContact,
      Optional<ContactAddress> contactAddress,
      Locale locale) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = extension.getNewRentAmount().currency();

    Map<String, Object> vars = new HashMap<>();

    vars.put("generatedDate", generatedDate);
    vars.put(
        "extensionIdentifier",
        extension
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Extension missing identifier"))
            .value());
    vars.put("extensionNumber", extension.getExtensionNumber());

    // Addressee
    vars.put(
        "primaryContactName", primaryContact.map(Contact::getDisplayName).orElse(null));
    vars.put("contactAddress", helper.buildAddressMap(contactAddress).orElse(null));

    // Property
    String propertyAddress =
        property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();
    vars.put("propertyAddress", propertyAddress);

    // Rent
    vars.put(
        "previousRent",
        CurrencyUtils.formatCurrency(extension.getPreviousRentAmount().value(), ccy));
    vars.put("newRent", CurrencyUtils.formatCurrency(extension.getNewRentAmount().value(), ccy));

    // Effective date: day after previous end date
    String rentEffectiveDate =
        BookletHelper.formatDate(extension.getPreviousEndDate().plusDays(1), dateFmt);
    vars.put("rentEffectiveDate", rentEffectiveDate);

    // Adjustment
    vars.put("adjustmentType", formatEnumValue(extension.getRentAdjustmentType().name()));
    vars.put(
        "adjustmentBasis",
        helper.buildAdjustmentBasis(
            messageSource, "letter.body.adjustment.", extension, locale));
    vars.put(
        "adjustmentValue",
        extension
            .getRentAdjustmentValue()
            .map(
                v ->
                    DocumentExporterHelper.formatAdjustmentDisplay(
                        v, extension.getRentAdjustmentType()))
            .orElse(null));

    // End date
    vars.put(
        "newEndDate",
        extension.getNewEndDate().map(d -> BookletHelper.formatDate(d, dateFmt)).orElse(null));

    // Contract
    vars.put(
        "contractIdentifier",
        contract
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Contract missing identifier"))
            .value());

    // Country-specific legal clause
    Optional<String> countryCode = contract.getCountryCode();
    vars.put("countryCode", countryCode.orElse(null));
    vars.put(
        "legalClause",
        helper.resolveLegalClause(messageSource, "legal.", countryCode, locale).orElse(null));

    return vars;
  }
}
