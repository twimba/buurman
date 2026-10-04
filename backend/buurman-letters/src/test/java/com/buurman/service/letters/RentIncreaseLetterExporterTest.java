package com.buurman.service.letters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.util.MoneyAmount;

/**
 * Mockito unit test mirroring {@code LeaseAgreementExporterTest}'s style. Focused on the thing a
 * copy-paste mistake could silently break: {@code signatureBlocks}/{@code legalVariables} being
 * wired with this exporter's own contract/team/document-type identity.
 */
class RentIncreaseLetterExporterTest {

  private final ContractExtensionRepository extensionRepository =
      mock(ContractExtensionRepository.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final LetterExporterHelper helper = mock(LetterExporterHelper.class);
  private final LetterTemplateService documentTemplateService = mock(LetterTemplateService.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  private RentIncreaseLetterExporter exporter;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final ContractExtensionIdentifier EXTENSION_IDENTIFIER =
      ContractExtensionIdentifier.of("CXT00000000000000000000001");

  private Contract contract;

  @BeforeEach
  void setUp() {
    exporter =
        new RentIncreaseLetterExporter(
            extensionRepository,
            contractRepository,
            propertyRepository,
            helper,
            documentTemplateService,
            messageSource,
            clock);

    contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .propertyId(PROPERTY_ID)
            .identifier(Optional.of(Sid.of("CON00000000000000000000001")))
            .build();

    ContractExtension extension =
        ContractExtension.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(Sid.of(EXTENSION_IDENTIFIER.value())))
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .extensionNumber(1)
            .previousEndDate(LocalDate.of(2026, 1, 1))
            .previousRentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
            .newRentAmount(MoneyAmount.of(new BigDecimal("1050.00"), "EUR"))
            .rentAdjustmentType(ContractExtension.RentAdjustmentType.FIXED_PERCENTAGE)
            .rentAdjustmentValue(Optional.of(new BigDecimal("5")))
            .status(ContractExtension.ExtensionStatus.ACTIVE)
            .triggerType(ContractExtension.TriggerType.MANUAL)
            .build();
    when(extensionRepository.getByIdentifierAndTeamId(EXTENSION_IDENTIFIER, TEAM_ID))
        .thenReturn(extension);
    when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(contract);

    Property property = Property.builder().id(PROPERTY_ID).street("Keizersgracht 12").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);

    when(helper.addressee(CONTRACT_ID, TEAM_ID))
        .thenReturn(new LetterExporterHelper.Addressee(Optional.empty(), Optional.empty()));

    Unit unit = Unit.builder().unitNumber("1").build();
    when(helper.premisesInfo(eq(contract), eq(property), eq(messageSource), any(Locale.class)))
        .thenReturn(new LetterExporterHelper.PremisesInfo(unit, 1, "unit "));

    when(helper.buildAdjustmentBasis(
            eq(messageSource), eq("letter.body.adjustment."), eq(extension), any(Locale.class)))
        .thenReturn("a 5% increase");

    when(documentTemplateService.renderToPdf(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF-1.7\nstub".getBytes(UTF_8));
  }

  @Test
  @DisplayName(
      "signatureBlocks and legalVariables are wired with this contract/team and the"
          + " rent-increase-letter document type, and their results flow into the rendered"
          + " variables")
  void wiresSignatureAndLegalVariablesForThisContract() {
    when(helper.signatureBlocks(
            eq(CONTRACT_ID),
            eq(TEAM_ID),
            eq(messageSource),
            eq("letter.signature"),
            any(Locale.class)))
        .thenReturn(List.of(Map.of("label", "Landlord", "placeholder", "signature-landlord")));
    when(helper.legalVariables(
            eq(messageSource),
            eq("legal."),
            eq("rent-increase-letter"),
            eq(contract),
            any(Locale.class)))
        .thenReturn(Map.of("countryCode", "NL", "legalClauses", List.of(Map.of("body", "x"))));

    exporter.generate(EXTENSION_IDENTIFIER, TEAM_ID);

    verify(helper)
        .signatureBlocks(CONTRACT_ID, TEAM_ID, messageSource, "letter.signature", Locale.ENGLISH);
    verify(helper)
        .legalVariables(messageSource, "legal.", "rent-increase-letter", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdf(eq("rent-increase-letter"), any(Locale.class), captor.capture());

    Map<String, Object> vars = captor.getValue();
    assertThat(vars.get("countryCode")).isEqualTo("NL");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> signatureBlocks =
        (List<Map<String, String>>) vars.get("signatureBlocks");
    assertThat(signatureBlocks)
        .extracting(b -> b.get("placeholder"))
        .containsExactly("signature-landlord");
  }
}
