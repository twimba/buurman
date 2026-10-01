package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import com.buurman.document.DocumentRenderer;
import com.buurman.domain.Contact;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactNoteRepository;
import com.buurman.repository.ContactRelationshipRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.MoneyAmount;

/**
 * View-model coverage of the contact booklet: the template engine is mocked so the assembled
 * variables can be asserted on directly.
 */
@DisplayName("ContactBookletExporter")
@ExtendWith(MockitoExtension.class)
class ContactBookletExporterTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final ContactIdentifier CONTACT_ID = ContactIdentifier.of("CT-5001");

  @Mock private ContactRepository contactRepository;
  @Mock private ContactAddressRepository contactAddressRepository;
  @Mock private ContactNoteRepository contactNoteRepository;
  @Mock private ContactRelationshipRepository contactRelationshipRepository;
  @Mock private UserRepository userRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private DocumentRenderer pdfRenderer;
  @Mock private TemplateEngine templateEngine;
  @Mock private MessageSource messageSource;

  private ContactBookletExporter exporter;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource labels = new ReloadableResourceBundleMessageSource();
    labels.setBasenames("classpath:messages/test-enum-labels");
    labels.setDefaultEncoding("UTF-8");
    labels.setUseCodeAsDefaultMessage(true);
    exporter =
        new ContactBookletExporter(
            contactRepository,
            contactAddressRepository,
            contactNoteRepository,
            contactRelationshipRepository,
            userRepository,
            contractRepository,
            contractExtensionRepository,
            paymentRepository,
            propertyRepository,
            contractPartyService,
            pdfRenderer,
            templateEngine,
            messageSource,
            new EnumLabelResolver(labels),
            new BookletFormatter(),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");
  }

  @Test
  @DisplayName(
      "a NOTICE_GIVEN contract is still in force: counted as active and shown as the current"
          + " property")
  void noticeGivenContractCountsAsActive() {
    UUID contactId = UUID.randomUUID();
    Property property =
        Property.builder().id(UUID.randomUUID()).street("Kerkstraat 14").city("Amsterdam").build();
    Contract underNotice =
        Contract.builder()
            .id(UUID.randomUUID())
            .propertyId(property.getId())
            .status(Contract.ContractStatus.NOTICE_GIVEN)
            .startDate(LocalDate.of(2025, 1, 1))
            .rentAmount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
            .build();
    when(contactRepository.getByIdentifierAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(
            Contact.builder()
                .id(contactId)
                .contactType(ContactType.INDIVIDUAL)
                .displayName("X")
                .build());
    when(contactAddressRepository.findByContactId(contactId, TEAM_ID)).thenReturn(List.of());
    when(contractRepository.findByContactIdViaParties(contactId, TEAM_ID))
        .thenReturn(List.of(underNotice));
    when(paymentRepository.findByContractId(underNotice.getId(), TEAM_ID)).thenReturn(List.of());
    when(propertyRepository.findByIdAndTeamId(property.getId(), TEAM_ID))
        .thenReturn(Optional.of(property));
    when(contractPartyService.getPartiesForContract(underNotice.getId(), TEAM_ID))
        .thenReturn(List.of());
    when(contractExtensionRepository.findByContractIdsAndTeamId(any(), eq(TEAM_ID)))
        .thenReturn(List.of());
    when(contactNoteRepository.findByContactIdAndTeamId(contactId, TEAM_ID)).thenReturn(List.of());
    when(contactRelationshipRepository.findByContactIdAndTeamId(contactId, TEAM_ID))
        .thenReturn(List.of());
    when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html/>");

    exporter.generate(CONTACT_ID, TEAM_ID, Locale.ENGLISH);

    ArgumentCaptor<IContext> context = ArgumentCaptor.forClass(IContext.class);
    verify(templateEngine).process(eq("contact-booklet/generic"), context.capture());
    Context model = (Context) context.getValue();
    assertThat(model.getVariable("activeContracts")).isEqualTo("1");
    assertThat(model.getVariable("currentProperty")).isEqualTo("Kerkstraat 14, Amsterdam");
  }
}
