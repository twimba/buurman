package com.buurman.service.letters;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.document.DocumentFormatting;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.Property;
import com.buurman.repository.PropertyRepository;
import com.buurman.service.ContractTerminationLetterGenerator;

@Component
public class ContractTerminationLetterExporter implements ContractTerminationLetterGenerator {

  private final PropertyRepository propertyRepository;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public ContractTerminationLetterExporter(
      PropertyRepository propertyRepository,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.propertyRepository = propertyRepository;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  @Override
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public byte[] generate(Contract contract, ContractTermination termination, UUID teamId) {
    return generate(contract, termination, teamId, "en");
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public byte[] generate(
      Contract contract, ContractTermination termination, UUID teamId, String lang) {
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    LetterExporterHelper.Addressee addressee = helper.addressee(contract.getId(), teamId);

    Locale locale = LetterTemplateService.resolveLocale(lang);
    Map<String, Object> variables =
        buildTemplateVariables(contract, termination, property, addressee, teamId, locale);

    return documentTemplateService.renderToPdf("contract-termination-notice", locale, variables);
  }

  private Map<String, Object> buildTemplateVariables(
      Contract contract,
      ContractTermination termination,
      Property property,
      LetterExporterHelper.Addressee addressee,
      UUID teamId,
      Locale locale) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);

    Map<String, Object> vars =
        LetterExporterHelper.headerVariables(contract, LocalDate.now(clock), dateFmt);

    vars.putAll(addressee.variables());
    LetterExporterHelper.PremisesInfo premisesInfo =
        helper.premisesInfo(contract, property, messageSource, locale);
    vars.put("propertyAddress", LetterExporterHelper.premisesAddress(property, premisesInfo));
    vars.put("hasMultipleUnits", premisesInfo.hasMultipleUnits());
    vars.put(
        "unitDesignation", premisesInfo.hasMultipleUnits() ? premisesInfo.unitDesignation() : null);

    vars.put(
        "givenByLabel",
        messageSource.getMessage(
            "termination.givenBy." + termination.getGivenBy().name(), null, locale));
    vars.put("noticeDate", DocumentFormatting.formatDate(termination.getNoticeDate(), dateFmt));
    vars.put("groundLabel", termination.getGroundCode().orElse(null));
    vars.put(
        "effectiveEndDate",
        DocumentFormatting.formatDate(termination.getEffectiveEndDate(), dateFmt));
    vars.put("overrideReason", termination.getOverrideReason().orElse(null));

    // "termination.legal." is disjoint from the bare "legal." prefix rent-increase-letter and
    // the extension addendum use — without this, a country match there (e.g. legal.NL) would
    // leak into this unrelated letter type via the shared merged MessageSource.
    vars.putAll(
        helper.legalVariables(
            messageSource, "termination.legal.", "contract-termination-notice", contract, locale));

    vars.put(
        "signatureBlocks",
        helper.signatureBlocks(
            contract.getId(), teamId, messageSource, "letter.signature", locale));

    return vars;
  }
}
