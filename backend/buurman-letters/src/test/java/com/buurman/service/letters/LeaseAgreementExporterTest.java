package com.buurman.service.letters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.service.LeaseClauseResolver;

/**
 * Mockito unit test (no Spring context, mirrors {@code SignatureServiceTest}'s style). Proves the
 * behaviors that matter most for this exporter: only clauses the resolver marked {@code included}
 * ever reach the template's variable map, regeneration is never cached (a second, different
 * resolution produces a different {@code clauses} list), and an all-excluded resolution throws
 * before the template service is ever touched.
 */
class LeaseAgreementExporterTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final ContractRentComponentRepository rentComponentRepository =
      mock(ContractRentComponentRepository.class);
  private final LeaseClauseResolver clauseResolver = mock(LeaseClauseResolver.class);
  private final LetterExporterHelper helper = mock(LetterExporterHelper.class);
  private final LetterTemplateService documentTemplateService = mock(LetterTemplateService.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  private LeaseAgreementExporter exporter;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_IDENTIFIER =
      ContractIdentifier.of("CON00000000000000000000001");

  private Contract contract;
  private Property property;

  @BeforeEach
  void setUp() {
    exporter =
        new LeaseAgreementExporter(
            contractRepository,
            propertyRepository,
            rentComponentRepository,
            clauseResolver,
            helper,
            documentTemplateService,
            messageSource,
            clock);

    contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .propertyId(PROPERTY_ID)
            .identifier(Optional.of(Sid.of(CONTRACT_IDENTIFIER.value())))
            .build();
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract);

    property =
        Property.builder()
            .id(PROPERTY_ID)
            .street("Keizersgracht 12")
            .postalCode("1015 CJ")
            .city("Amsterdam")
            .build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);

    Unit unit = Unit.builder().unitNumber("1").name(Optional.empty()).build();
    LetterExporterHelper.PremisesInfo premisesInfo =
        new LetterExporterHelper.PremisesInfo(unit, 1, "unit ");
    when(helper.premisesInfo(eq(contract), eq(property), eq(messageSource), any(Locale.class)))
        .thenReturn(premisesInfo);

    LetterExporterHelper.Addressee addressee =
        new LetterExporterHelper.Addressee(Optional.empty(), Optional.empty());
    when(helper.addressee(CONTRACT_ID, TEAM_ID)).thenReturn(addressee);

    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());

    when(documentTemplateService.renderToPdf(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF-1.7\nstub".getBytes(UTF_8));
  }

  private static ResolvedLeaseClauseResponse clause(
      String key, String title, String body, boolean included) {
    return new ResolvedLeaseClauseResponse(
        Sid.of("LCT0000000000000000000000" + (included ? "1" : "0")),
        key,
        title,
        body,
        included,
        true,
        0);
  }

  @Test
  @DisplayName("only included clauses reach the renderToPdf variable map")
  void onlyIncludedClausesReachTemplate() {
    ResolvedLeaseClauseResponse included1 =
        clause("term", "Term of Lease", "The lease runs for twelve months.", true);
    ResolvedLeaseClauseResponse excluded = clause("pets", "Pets", "Pets are not permitted.", false);
    ResolvedLeaseClauseResponse included2 =
        clause("maintenance", "Maintenance", "Tenant handles minor repairs.", true);
    when(clauseResolver.resolve(eq(contract), any(Locale.class)))
        .thenReturn(List.of(included1, excluded, included2));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdf(eq("lease-agreement"), any(Locale.class), captor.capture());

    Object clausesObj = captor.getValue().get("clauses");
    assertThat(clausesObj).isInstanceOf(List.class);
    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) clausesObj;

    assertThat(clauses).hasSize(2);
    assertThat(clauses)
        .extracting(c -> c.get("title"), c -> c.get("body"))
        .containsExactly(
            org.assertj.core.api.Assertions.tuple(
                "Term of Lease", "The lease runs for twelve months."),
            org.assertj.core.api.Assertions.tuple("Maintenance", "Tenant handles minor repairs."));
    assertThat(clauses).noneMatch(c -> c.get("title").equals("Pets"));
  }

  @Test
  @DisplayName(
      "regenerating after a clause selection change is never cached — a second, different"
          + " resolution produces a different clauses list")
  void regenerationIsNotCached() {
    ResolvedLeaseClauseResponse firstSelectionClause =
        clause("term", "Term of Lease", "The lease runs for twelve months.", true);
    when(clauseResolver.resolve(eq(contract), any(Locale.class)))
        .thenReturn(List.of(firstSelectionClause));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    // Landlord flips a clause selection, then regenerates: the resolver now returns a
    // different set of included clauses for the very same contract.
    ResolvedLeaseClauseResponse secondSelectionClause =
        clause("pets", "Pets", "Pets are permitted with a deposit.", true);
    when(clauseResolver.resolve(eq(contract), any(Locale.class)))
        .thenReturn(List.of(secondSelectionClause));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService, org.mockito.Mockito.times(2))
        .renderToPdf(eq("lease-agreement"), any(Locale.class), captor.capture());

    List<Map<String, Object>> allVariables = captor.getAllValues();
    @SuppressWarnings("unchecked")
    List<Map<String, String>> firstClauses =
        (List<Map<String, String>>) (List<?>) allVariables.get(0).get("clauses");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> secondClauses =
        (List<Map<String, String>>) (List<?>) allVariables.get(1).get("clauses");

    assertThat(firstClauses).extracting(c -> c.get("title")).containsExactly("Term of Lease");
    assertThat(secondClauses).extracting(c -> c.get("title")).containsExactly("Pets");
    assertThat(firstClauses).isNotEqualTo(secondClauses);
  }

  @Test
  @DisplayName(
      "throws BusinessRuleException before renderToPdf is ever called when no clauses are"
          + " included")
  void emptyIncludedClausesThrowsBeforeRender() {
    ResolvedLeaseClauseResponse excludedOnly =
        clause("pets", "Pets", "Pets are not permitted.", false);
    when(clauseResolver.resolve(eq(contract), any(Locale.class))).thenReturn(List.of(excludedOnly));

    assertThatThrownBy(() -> exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en"))
        .isInstanceOf(BusinessRuleException.class);

    verifyNoInteractions(documentTemplateService);
  }
}
