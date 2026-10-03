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

import com.buurman.domain.Contact;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.document.TenantNoticeDocumentService.FormalNoticeData;
import com.buurman.util.MoneyAmount;

/**
 * Mockito unit test mirroring {@code LeaseAgreementExporterTest}'s style. Exercises {@code
 * renderFormalNotice} directly — the shared rendering path used by both the on-demand download and
 * the FINAL-tone reminder job — focused on the thing a copy-paste mistake could silently break:
 * {@code signatureBlocks}/{@code legalVariables} being wired with this exporter's own
 * contract/team/document-type identity.
 */
class PaymentFormalNoticeExporterTest {

  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final PaymentReceivalRepository receivalRepository =
      mock(PaymentReceivalRepository.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final ContactRepository contactRepository = mock(ContactRepository.class);
  private final ContractPartyService contractPartyService = mock(ContractPartyService.class);
  private final ContractPaymentInstructionRepository cpiRepository =
      mock(ContractPaymentInstructionRepository.class);
  private final PaymentInstructionRepository paymentInstructionRepository =
      mock(PaymentInstructionRepository.class);
  private final RentRegulationRepository rentRegulationRepository =
      mock(RentRegulationRepository.class);
  private final LetterExporterHelper helper = mock(LetterExporterHelper.class);
  private final LetterTemplateService documentTemplateService = mock(LetterTemplateService.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  private PaymentFormalNoticeExporter exporter;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();

  private Contract contract;
  private Payment payment;
  private Contact contact;

  @BeforeEach
  void setUp() {
    exporter =
        new PaymentFormalNoticeExporter(
            paymentRepository,
            receivalRepository,
            contractRepository,
            propertyRepository,
            contactRepository,
            contractPartyService,
            cpiRepository,
            paymentInstructionRepository,
            rentRegulationRepository,
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

    payment =
        Payment.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(Sid.of("pay_01JTEST000000000000000001")))
            .teamId(TEAM_ID)
            .contractId(CONTRACT_ID)
            .amount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
            .dueDate(LocalDate.of(2026, 1, 1))
            .status(Payment.PaymentStatus.OVERDUE)
            .build();

    contact =
        Contact.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .contactType(ContactType.INDIVIDUAL)
            .displayName("Jan de Vries")
            .build();

    Property property = Property.builder().id(PROPERTY_ID).street("Keizersgracht 12").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);

    when(helper.addressee(contact, TEAM_ID))
        .thenReturn(new LetterExporterHelper.Addressee(Optional.of(contact), Optional.empty()));

    Unit unit = Unit.builder().unitNumber("1").build();
    when(helper.premisesInfo(eq(contract), eq(property), eq(messageSource), any(Locale.class)))
        .thenReturn(new LetterExporterHelper.PremisesInfo(unit, 1, "unit "));

    when(cpiRepository.findCurrentByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(Optional.empty());

    when(documentTemplateService.renderToPdf(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF-1.7\nstub".getBytes(UTF_8));
  }

  @Test
  @DisplayName(
      "signatureBlocks and legalVariables are wired with this contract/team and the"
          + " payment-formal-notice document type, and their results flow into the rendered"
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
            eq("notice.legal."),
            eq("payment-formal-notice"),
            eq(contract),
            any(Locale.class)))
        .thenReturn(Map.of("countryCode", "NL", "legalClauses", List.of(Map.of("body", "x"))));

    FormalNoticeData data =
        new FormalNoticeData(payment, contract, contact, new BigDecimal("1200.00"), 14, "en");
    exporter.renderFormalNotice(data);

    verify(helper)
        .signatureBlocks(CONTRACT_ID, TEAM_ID, messageSource, "letter.signature", Locale.ENGLISH);
    verify(helper)
        .legalVariables(
            messageSource, "notice.legal.", "payment-formal-notice", contract, Locale.ENGLISH);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdf(eq("payment-formal-notice"), any(Locale.class), captor.capture());

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
