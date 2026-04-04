package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.formatEnumValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Property;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.util.CurrencyUtils;

/**
 * Generates a rent change document (PDF) for any rent period — independent of contract extensions.
 * Integrates rent regulation data when available. Handles "initial rent confirmation" for first
 * periods.
 */
@Component
public class RentChangeDocumentExporter {

  private final ContractRentPeriodRepository rentPeriodRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository extensionRepository;
  private final PropertyRepository propertyRepository;
  private final RentRegulationRepository regulationRepository;
  private final DocumentExporterHelper helper;
  private final DocumentTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public RentChangeDocumentExporter(
      ContractRentPeriodRepository rentPeriodRepository,
      ContractRepository contractRepository,
      ContractExtensionRepository extensionRepository,
      PropertyRepository propertyRepository,
      RentRegulationRepository regulationRepository,
      DocumentExporterHelper helper,
      DocumentTemplateService documentTemplateService,
      @Qualifier("documentMessageSource") MessageSource messageSource,
      Clock clock) {
    this.rentPeriodRepository = rentPeriodRepository;
    this.contractRepository = contractRepository;
    this.extensionRepository = extensionRepository;
    this.propertyRepository = propertyRepository;
    this.regulationRepository = regulationRepository;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(
      ContractIdentifier contractIdentifier,
      ContractRentPeriodIdentifier periodIdentifier,
      UUID teamId,
      String lang) {
    ContractRentPeriod period =
        rentPeriodRepository
            .findByIdentifierAndTeamId(periodIdentifier, teamId)
            .orElseThrow(
                () ->
                    new com.buurman.exception.NotFoundException(
                        "Rent period not found: " + periodIdentifier.value()));

    Contract contract = contractRepository.getByIdAndTeamId(period.getContractId(), teamId);
    DocumentExporterHelper.validateContractOwnership(
        Optional.of(contractIdentifier), contract, "Rent period");
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    // Find previous period (for delta calculation)
    Optional<ContractRentPeriod> previousPeriod =
        rentPeriodRepository.findPreviousPeriod(
            period.getContractId(), teamId, period.getEffectiveFrom());

    // Find linked extension (if any)
    Optional<ContractExtension> linkedExtension = findLinkedExtension(period, contract, teamId);

    // Load contacts
    DocumentExporterHelper.PartyData partyData = helper.loadPartyData(contract.getId(), teamId);

    Optional<Contact> primaryContact =
        helper.findPrimaryContact(partyData.parties(), partyData.contactMap());
    Optional<ContactAddress> address =
        primaryContact.flatMap(c -> helper.findMailingAddress(c.getId(), teamId));

    // Look up rent regulation data
    Optional<RentRegulationRule> regulationRule =
        findApplicableRegulationRule(contract, property, period);

    Locale locale = DocumentTemplateService.resolveLocale(lang);
    Map<String, Object> variables =
        buildTemplateVariables(
            period,
            previousPeriod,
            contract,
            property,
            primaryContact,
            address,
            linkedExtension,
            regulationRule,
            locale);

    return documentTemplateService.renderToPdf("rent-change", locale, variables);
  }

  private Map<String, Object> buildTemplateVariables(
      ContractRentPeriod currentPeriod,
      Optional<ContractRentPeriod> previousPeriod,
      Contract contract,
      Property property,
      Optional<Contact> primaryContact,
      Optional<ContactAddress> contactAddress,
      Optional<ContractExtension> linkedExtension,
      Optional<RentRegulationRule> regulationRule,
      Locale locale) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = currentPeriod.getRentAmount().currency();

    boolean isInitialRent = previousPeriod.isEmpty();

    Map<String, Object> vars = new HashMap<>();

    vars.put("generatedDate", generatedDate);
    vars.put("isInitialRent", isInitialRent);
    vars.put(
        "rentPeriodIdentifier",
        currentPeriod
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Rent period missing identifier"))
            .value());

    // Addressee
    vars.put("primaryContactName", primaryContact.map(Contact::getDisplayName).orElse(null));
    vars.put("contactAddress", helper.buildAddressMap(contactAddress).orElse(null));

    // Property
    String propertyAddress =
        property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();
    vars.put("propertyAddress", propertyAddress);

    // Rent amounts
    vars.put("newRent", CurrencyUtils.formatCurrency(currentPeriod.getRentAmount().value(), ccy));
    if (!isInitialRent) {
      vars.put(
          "previousRent",
          CurrencyUtils.formatCurrency(previousPeriod.get().getRentAmount().value(), ccy));
    } else {
      vars.put("previousRent", null);
    }

    // Effective date
    vars.put("effectiveDate", BookletHelper.formatDate(currentPeriod.getEffectiveFrom(), dateFmt));

    // Percentage change
    if (!isInitialRent) {
      BigDecimal oldAmount = previousPeriod.get().getRentAmount().value();
      BigDecimal newAmount = currentPeriod.getRentAmount().value();
      if (oldAmount.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal change =
            newAmount
                .subtract(oldAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(oldAmount, 2, RoundingMode.HALF_UP);
        vars.put("percentageChange", change);
        vars.put("percentageChangeFormatted", change.abs().stripTrailingZeros().toPlainString());
      } else {
        vars.put("percentageChange", null);
        vars.put("percentageChangeFormatted", null);
      }
    } else {
      vars.put("percentageChange", null);
      vars.put("percentageChangeFormatted", null);
    }

    // Adjustment basis
    linkedExtension.ifPresentOrElse(
        ext -> {
          vars.put("adjustmentType", formatEnumValue(ext.getRentAdjustmentType().name()));
          vars.put(
              "adjustmentBasis",
              helper.buildAdjustmentBasis(
                  messageSource, "rentchange.body.adjustment.", ext, locale));
        },
        () -> {
          vars.put("adjustmentType", null);
          if (!isInitialRent) {
            vars.put(
                "adjustmentBasis",
                messageSource.getMessage("rentchange.body.adjustment.standalone", null, locale));
          } else {
            vars.put("adjustmentBasis", null);
          }
        });

    // Extension details (if linked)
    linkedExtension.ifPresentOrElse(
        ext -> {
          vars.put("extensionNumber", ext.getExtensionNumber());
          vars.put(
              "extensionNewEndDate",
              ext.getNewEndDate().map(d -> BookletHelper.formatDate(d, dateFmt)).orElse(null));
        },
        () -> {
          vars.put("extensionNumber", null);
          vars.put("extensionNewEndDate", null);
        });

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
        helper
            .resolveLegalClause(messageSource, "rentchange.legal.", countryCode, locale)
            .orElse(null));

    // Regulation data
    regulationRule.ifPresentOrElse(
        rule -> {
          Map<String, Object> regMap = new HashMap<>();
          regMap.put("year", rule.getYear());
          regMap.put(
              "maxIncreasePercentage",
              rule.getMaxIncreasePercentage()
                  .map(v -> v.stripTrailingZeros().toPlainString())
                  .orElse(null));
          regMap.put("indexName", rule.getIndexName().orElse(null));
          regMap.put(
              "indexValue",
              rule.getIndexValue().map(v -> v.stripTrailingZeros().toPlainString()).orElse(null));
          regMap.put("noticePeriodDays", rule.getNoticePeriodDays().orElse(null));
          regMap.put("additionalConditions", rule.getAdditionalConditions().orElse(null));
          regMap.put("sourceUrl", rule.getSourceUrl().orElse(null));
          vars.put("regulation", regMap);

          // Check if increase exceeds max
          BigDecimal percentageChange = (BigDecimal) vars.get("percentageChange");
          boolean exceeds =
              percentageChange != null
                  && rule.getMaxIncreasePercentage().isPresent()
                  && percentageChange.compareTo(rule.getMaxIncreasePercentage().get()) > 0;
          vars.put("exceedsMaxIncrease", exceeds);
        },
        () -> {
          vars.put("regulation", null);
          vars.put("exceedsMaxIncrease", false);
        });

    return vars;
  }

  private Optional<ContractExtension> findLinkedExtension(
      ContractRentPeriod period, Contract contract, UUID teamId) {
    List<ContractExtension> extensions =
        extensionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    return extensions.stream()
        .filter(ext -> ext.getRentPeriodId().isPresent())
        .filter(ext -> ext.getRentPeriodId().get().equals(period.getId()))
        .findFirst();
  }

  private Optional<RentRegulationRule> findApplicableRegulationRule(
      Contract contract, Property property, ContractRentPeriod period) {
    Optional<String> countryCode = contract.getCountryCode();
    if (countryCode.isEmpty()) {
      return Optional.empty();
    }
    Optional<RentRegulationCountry> country =
        regulationRepository.findCountryByCode(countryCode.get());
    if (country.isEmpty()) {
      return Optional.empty();
    }
    int year = period.getEffectiveFrom().getYear();
    List<RentRegulationRule> rules =
        regulationRepository.findNationalRulesByCountryIdAndYear(country.get().getId(), year);
    return rules.stream().findFirst();
  }
}
