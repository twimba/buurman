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
import com.buurman.domain.Deposit;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DepositRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.util.MoneyAmount;

/**
 * Mockito unit test mirroring {@code LeaseAgreementExporterTest}'s style. Focused on the thing a
 * copy-paste mistake could silently break: {@code signatureBlocks}/{@code legalVariables} being
 * wired with this exporter's own contract/team/document-type identity.
 */
class DepositStatementExporterTest {

  private final DepositRepository depositRepository = mock(DepositRepository.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final LetterExporterHelper helper = mock(LetterExporterHelper.class);
  private final LetterTemplateService documentTemplateService = mock(LetterTemplateService.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  private DepositStatementExporter exporter;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_IDENTIFIER =
      ContractIdentifier.of("CON00000000000000000000001");

  private Contract contract;

  @BeforeEach
  void setUp() {
    exporter =
        new DepositStatementExporter(
            depositRepository,
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
            .startDate(java.time.LocalDate.of(2025, 1, 1))
            .identifier(Optional.of(Sid.of(CONTRACT_IDENTIFIER.value())))
            .build();
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract);

    Deposit deposit =
        Deposit.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .amount(MoneyAmount.of(new BigDecimal("2000.00"), "EUR"))
            .status(Deposit.DepositStatus.HELD)
            .build();
    when(depositRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.of(deposit));
    when(depositRepository.findDeductions(deposit.getId(), TEAM_ID)).thenReturn(List.of());

    Property property = Property.builder().id(PROPERTY_ID).street("Keizersgracht 12").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);

    when(helper.addressee(CONTRACT_ID, TEAM_ID))
        .thenReturn(new LetterExporterHelper.Addressee(Optional.empty(), Optional.empty()));

    Unit unit = Unit.builder().unitNumber("1").build();
    when(helper.premisesInfo(eq(contract), eq(property), eq(messageSource), any(Locale.class)))
        .thenReturn(new LetterExporterHelper.PremisesInfo(unit, 1, "unit "));

    when(documentTemplateService.renderToPdf(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF-1.7\nstub".getBytes(UTF_8));
  }

  @Test
  @DisplayName(
      "signatureBlocks and legalVariables are wired with this contract/team and the"
          + " deposit-statement document type, and their results flow into the rendered variables")
  void wiresSignatureAndLegalVariablesForThisContract() {
    when(helper.signatureBlocks(
            eq(CONTRACT_ID),
            eq(TEAM_ID),
            eq(messageSource),
            eq("letter.signature"),
            any(Locale.class)))
        .thenReturn(
            List.of(
                Map.of("label", "Landlord", "placeholder", "signature-landlord"),
                Map.of("label", "Tenant", "placeholder", "signature-tenant-1")));
    when(helper.legalVariables(
            eq(messageSource),
            eq("deposit.legal."),
            eq("deposit-statement"),
            eq(contract),
            any(Locale.class)))
        .thenReturn(Map.of("countryCode", "NL", "legalClauses", List.of(Map.of("body", "x"))));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    verify(helper)
        .signatureBlocks(CONTRACT_ID, TEAM_ID, messageSource, "letter.signature", Locale.ENGLISH);
    verify(helper)
        .legalVariables(
            messageSource, "deposit.legal.", "deposit-statement", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdf(eq("deposit-statement"), any(Locale.class), captor.capture());

    Map<String, Object> vars = captor.getValue();
    assertThat(vars.get("countryCode")).isEqualTo("NL");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> signatureBlocks =
        (List<Map<String, String>>) vars.get("signatureBlocks");
    assertThat(signatureBlocks)
        .extracting(b -> b.get("placeholder"))
        .containsExactly("signature-landlord", "signature-tenant-1");
  }
}
