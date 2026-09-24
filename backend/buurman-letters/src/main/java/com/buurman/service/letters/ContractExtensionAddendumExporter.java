package com.buurman.service.letters;

import static com.buurman.document.DocumentFormatting.formatEnumValue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.document.DocumentFormatting;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractParty;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.util.CurrencyUtils;

@Component
public class ContractExtensionAddendumExporter {

  private final ContractExtensionRepository extensionRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public ContractExtensionAddendumExporter(
      ContractExtensionRepository extensionRepository,
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
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
    LetterExporterHelper.validateContractOwnership(contractIdentifier, contract, "Extension");
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    LetterExporterHelper.PartyData partyData = helper.loadPartyData(contract.getId(), teamId);

    Locale locale = LetterTemplateService.resolveLocale(lang);
    Map<String, Object> variables =
        buildTemplateVariables(
            extension, contract, property, partyData.parties(), partyData.contactMap(), locale);

    return documentTemplateService.renderToPdf("extension-addendum", locale, variables);
  }

  private Map<String, Object> buildTemplateVariables(
      ContractExtension extension,
      Contract contract,
      Property property,
      List<ContractParty> parties,
      Map<UUID, Contact> contactMap,
      Locale locale) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = extension.getNewRentAmount().currency();

    Map<String, Object> vars = new HashMap<>();

    // Cover page
    vars.put("extensionNumber", extension.getExtensionNumber());
    vars.put(
        "extensionIdentifier",
        extension
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Extension missing identifier"))
            .value());
    vars.put("extensionStatus", extension.getStatus().name());
    vars.put("extensionStatusDisplay", formatEnumValue(extension.getStatus().name()));
    vars.put("generatedDate", generatedDate);

    vars.put("propertyAddress", property.getStreet() + ", " + property.getCity());
    vars.put(
        "propertyFullAddress",
        property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity());

    // Contacts
    vars.put("primaryContactName", helper.findPrimaryContactName(parties, contactMap));
    vars.put("contactNames", helper.buildContactNamesList(parties, contactMap));

    // Contract
    vars.put(
        "contractIdentifier",
        contract
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Contract missing identifier"))
            .value());
    vars.put("contractStartDate", DocumentFormatting.formatDate(contract.getStartDate(), dateFmt));
    vars.put(
        "contractEndDate",
        contract
            .getEndDate()
            .map(d -> DocumentFormatting.formatDate(d, dateFmt))
            .orElse(formatEnumValue("INDEFINITE")));
    vars.put(
        "contractType",
        contract.getContractType() != null
            ? formatEnumValue(contract.getContractType().name())
            : "");

    // Extension
    vars.put(
        "previousEndDate", DocumentFormatting.formatDate(extension.getPreviousEndDate(), dateFmt));
    vars.put(
        "newEndDate",
        extension
            .getNewEndDate()
            .map(d -> DocumentFormatting.formatDate(d, dateFmt))
            .orElse(formatEnumValue("INDEFINITE")));
    vars.put(
        "previousRent",
        CurrencyUtils.formatCurrency(extension.getPreviousRentAmount().value(), ccy));
    vars.put("newRent", CurrencyUtils.formatCurrency(extension.getNewRentAmount().value(), ccy));
    vars.put("triggerType", formatEnumValue(extension.getTriggerType().name()));
    vars.put("adjustmentType", formatEnumValue(extension.getRentAdjustmentType().name()));
    vars.put(
        "adjustmentValue",
        extension
            .getRentAdjustmentValue()
            .map(
                v ->
                    LetterExporterHelper.formatAdjustmentDisplay(
                        v, extension.getRentAdjustmentType()))
            .orElse("\u2014"));
    vars.put(
        "activatedDate",
        extension
            .getActivatedAt()
            .map(
                i ->
                    DocumentFormatting.formatDate(
                        i.atZone(java.time.ZoneOffset.UTC).toLocalDate(), dateFmt))
            .orElse("\u2014"));

    // Notes
    vars.put("notes", extension.getNotes().filter(n -> !n.isBlank()).orElse(null));

    // Country-specific legal clause
    Optional<String> countryCode = contract.getCountryCode();
    vars.put("countryCode", countryCode.orElse(null));
    vars.put(
        "legalClause",
        helper.resolveLegalClause(messageSource, "legal.", countryCode, locale).orElse(null));

    return vars;
  }
}
