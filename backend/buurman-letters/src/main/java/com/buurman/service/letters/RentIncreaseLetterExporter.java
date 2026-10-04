package com.buurman.service.letters;

import static com.buurman.document.DocumentFormatting.formatEnumValue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.document.DocumentFormatting;
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
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public RentIncreaseLetterExporter(
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

    LetterExporterHelper.Addressee addressee = helper.addressee(contract.getId(), teamId);

    Locale locale = LetterTemplateService.resolveLocale(lang);
    Map<String, Object> variables =
        buildTemplateVariables(extension, contract, property, addressee, teamId, locale);

    return documentTemplateService.renderToPdf("rent-increase-letter", locale, variables);
  }

  private Map<String, Object> buildTemplateVariables(
      ContractExtension extension,
      Contract contract,
      Property property,
      LetterExporterHelper.Addressee addressee,
      UUID teamId,
      Locale locale) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    String ccy = extension.getNewRentAmount().currency();

    Map<String, Object> vars =
        LetterExporterHelper.headerVariables(contract, LocalDate.now(clock), dateFmt);

    vars.put(
        "extensionIdentifier",
        extension
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Extension missing identifier"))
            .value());
    vars.put("extensionNumber", extension.getExtensionNumber());

    vars.putAll(addressee.variables());
    LetterExporterHelper.PremisesInfo premisesInfo =
        helper.premisesInfo(contract, property, messageSource, locale);
    vars.put("propertyAddress", LetterExporterHelper.premisesAddress(property, premisesInfo));
    vars.put("hasMultipleUnits", premisesInfo.hasMultipleUnits());
    vars.put(
        "unitDesignation", premisesInfo.hasMultipleUnits() ? premisesInfo.unitDesignation() : null);

    // Rent
    vars.put(
        "previousRent",
        CurrencyUtils.formatCurrency(extension.getPreviousRentAmount().value(), ccy));
    vars.put("newRent", CurrencyUtils.formatCurrency(extension.getNewRentAmount().value(), ccy));

    // Effective date: day after previous end date
    String rentEffectiveDate =
        DocumentFormatting.formatDate(extension.getPreviousEndDate().plusDays(1), dateFmt);
    vars.put("rentEffectiveDate", rentEffectiveDate);

    // Adjustment
    vars.put("adjustmentType", formatEnumValue(extension.getRentAdjustmentType().name()));
    vars.put(
        "adjustmentBasis",
        helper.buildAdjustmentBasis(messageSource, "letter.body.adjustment.", extension, locale));
    vars.put(
        "adjustmentValue",
        extension
            .getRentAdjustmentValue()
            .map(
                v ->
                    LetterExporterHelper.formatAdjustmentDisplay(
                        v, extension.getRentAdjustmentType()))
            .orElse(null));

    // End date
    vars.put(
        "newEndDate",
        extension.getNewEndDate().map(d -> DocumentFormatting.formatDate(d, dateFmt)).orElse(null));

    vars.putAll(
        helper.legalVariables(messageSource, "legal.", "rent-increase-letter", contract, locale));

    vars.put(
        "signatureBlocks",
        helper.signatureBlocks(
            contract.getId(), teamId, messageSource, "letter.signature", locale));

    return vars;
  }
}
