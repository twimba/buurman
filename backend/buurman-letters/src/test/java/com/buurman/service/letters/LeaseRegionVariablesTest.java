package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.context.MessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.Contract;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Sid;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.service.LeaseKindResolver;
import com.buurman.util.MoneyAmount;

/**
 * {@code regionCode} (nullable) and {@code countryCode} are template variables of the per-language
 * document path, so a document can branch on region, nation or state. The fixture document {@code
 * ZZ/residential/en.html} (test resources) has a {@code clause-region} fragment that does exactly
 * that, rendered here through the real exporter variables and the real shell.
 */
@DisplayName("lease template variables regionCode and countryCode")
class LeaseRegionVariablesTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  private final LeaseClauseResolver resolver = mock(LeaseClauseResolver.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private LeaseAgreementExporter exporter;
  private TemplateEngine engine;

  @BeforeEach
  void setUp() {
    exporter =
        new LeaseAgreementExporter(
            mock(ContractRepository.class),
            mock(PropertyRepository.class),
            mock(ContractRentComponentRepository.class),
            mock(ContractRentPeriodRepository.class),
            resolver,
            mock(LeaseKindResolver.class),
            new LeaseDocumentLocator(Map.of("ZZ", List.of("en"))),
            mock(TeamRepository.class),
            mock(LetterExporterHelper.class),
            mock(LetterTemplateService.class),
            messageSource,
            CLOCK);
    when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
    engine =
        DocumentTemplateSupport.templateEngine(
            DocumentTemplateSupport.messageSource(
                false,
                "classpath:messages/document-letter-chrome",
                "classpath:messages/document-lease-agreement"),
            false);
  }

  private static LeaseClauseTemplate template(String country, String key) {
    return LeaseClauseTemplate.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("LCT00000000000000000000001")))
        .countryCode(country)
        .leaseKind(LeaseKind.RESIDENTIAL)
        .clauseKey(key)
        .titleI18nKey("t")
        .bodyI18nKey("b")
        .defaultIncluded(true)
        .optional(false)
        .sortOrder(1)
        .version(1)
        .build();
  }

  private static ResolvedLeaseClauseResponse resolved(String key) {
    return new ResolvedLeaseClauseResponse(
        Sid.of("LCT00000000000000000000001"), key, "Region rules", "B", true, false, 1, false, 1);
  }

  private static LeaseRenderInput input(Optional<String> region) {
    return new LeaseRenderInput(
        "SAMPLE",
        LocalDate.of(2026, 1, 15),
        region,
        LocalDate.of(2026, 2, 1),
        Optional.empty(),
        Contract.ContractType.INDEFINITE,
        Contract.PaymentFrequency.MONTHLY,
        30,
        30,
        List.of(),
        new MoneyAmount(new BigDecimal("1000.00"), "EUR"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of("Landlord BV"),
        Optional.of("Tenant One"),
        Optional.empty(),
        Optional.empty(),
        "Street 1",
        List.of());
  }

  private LeaseAgreementExporter.AssembledLease assemble(String country, Optional<String> region) {
    when(resolver.templatesFor(Optional.of(country), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template(country, "region")));
    var plan = exporter.plan(Optional.of(country), LeaseKind.RESIDENTIAL, "en");
    return exporter.assembleResolved(
        plan, input(region), List.of(resolved("region")), List.of(resolved("region")));
  }

  private String render(LeaseAgreementExporter.AssembledLease lease) {
    Context ctx = new Context(lease.locale());
    ctx.setVariables(lease.variables());
    return engine.process(lease.templateName(), ctx);
  }

  @Test
  @DisplayName("the variable map carries the contract's region and the effective country")
  void variablesCarryRegionAndCountry() {
    Map<String, Object> variables = assemble("ZZ", Optional.of("X")).variables();

    assertThat(variables).containsEntry("regionCode", "X").containsEntry("countryCode", "ZZ");
  }

  @Test
  @DisplayName("an unknown region is a present, null variable (templates can test it directly)")
  void unknownRegionIsNullButPresent() {
    Map<String, Object> variables = assemble("ZZ", Optional.empty()).variables();

    assertThat(variables).containsKey("regionCode");
    assertThat(variables.get("regionCode")).isNull();
    assertThat(variables).containsEntry("countryCode", "ZZ");
  }

  @Test
  @DisplayName("a document branches on regionCode: the X branch renders for X and not otherwise")
  void documentBranchesOnRegion() {
    String forX = render(assemble("ZZ", Optional.of("X")));
    assertThat(forX).contains("REGION-X-RULES").doesNotContain("GENERAL-RULES");

    String forY = render(assemble("ZZ", Optional.of("Y")));
    assertThat(forY).contains("GENERAL-RULES").doesNotContain("REGION-X-RULES");

    String unknown = render(assemble("ZZ", Optional.empty()));
    assertThat(unknown).contains("GENERAL-RULES").doesNotContain("REGION-X-RULES");
  }

  @Test
  @DisplayName("a lower-case region code is upper-cased: 'x' gets the X branch")
  void lowerCaseRegionIsNormalised() {
    var assembled = assemble("ZZ", Optional.of("x"));

    assertThat(assembled.variables()).containsEntry("regionCode", "X");
    assertThat(render(assembled)).contains("REGION-X-RULES").doesNotContain("GENERAL-RULES");
  }

  @Test
  @DisplayName("a lower-case Canadian province 'on' gets the Ontario branch of the real document")
  void lowerCaseOntarioGetsOntarioBranch() {
    exporter =
        new LeaseAgreementExporter(
            mock(ContractRepository.class),
            mock(PropertyRepository.class),
            mock(ContractRentComponentRepository.class),
            mock(ContractRentPeriodRepository.class),
            resolver,
            mock(LeaseKindResolver.class),
            new LeaseDocumentLocator(Map.of("CA", List.of("en"))),
            mock(TeamRepository.class),
            mock(LetterExporterHelper.class),
            mock(LetterTemplateService.class),
            messageSource,
            CLOCK);
    when(resolver.templatesFor(Optional.of("CA"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template("CA", "premises")));
    var plan = exporter.plan(Optional.of("CA"), LeaseKind.RESIDENTIAL, "en");
    var assembled =
        exporter.assembleResolved(
            plan,
            input(Optional.of("on")),
            List.of(resolved("premises")),
            List.of(resolved("premises")));

    assertThat(assembled.variables()).containsEntry("regionCode", "ON");
    assertThat(render(assembled)).contains("THIS DOCUMENT IS NOT THE STANDARD FORM OF LEASE");
  }

  @Test
  @DisplayName("a document can print the country it is rendered for")
  void documentSeesCountryCode() {
    assertThat(render(assemble("ZZ", Optional.empty())))
        .contains("GENERAL-RULES in <span>ZZ</span>");
  }

  @Test
  @DisplayName("the preview path (assembleWithOverrides) supplies both variables too")
  void previewPathSuppliesBoth() {
    when(resolver.templatesFor(Optional.of("ZZ"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template("ZZ", "region")));
    var plan = exporter.plan(Optional.of("ZZ"), LeaseKind.RESIDENTIAL, "en");
    when(resolver.resolve(eq(plan.templates()), any(), eq(plan.country()), eq(plan.locale())))
        .thenReturn(List.of(resolved("region")));

    var assembled = exporter.assembleWithOverrides(plan, input(Optional.of("X")), List.of());

    assertThat(assembled.variables())
        .containsEntry("regionCode", "X")
        .containsEntry("countryCode", "ZZ");
    assertThat(render(assembled)).contains("REGION-X-RULES");
  }

  @Test
  @DisplayName("the legacy example-text path does not receive the per-language variables")
  void legacyPathUnchanged() {
    // BE has RESIDENTIAL documents since V082; MIXED_USE has none and takes the legacy path
    when(resolver.templatesFor(Optional.of("BE"), LeaseKind.MIXED_USE))
        .thenReturn(List.of(template("BE", "rent")));
    var plan = exporter.plan(Optional.of("BE"), LeaseKind.MIXED_USE, "en");

    var assembled =
        exporter.assembleResolved(
            plan, input(Optional.of("X")), List.of(resolved("rent")), List.of(resolved("rent")));

    assertThat(assembled.legacy()).isTrue();
    assertThat(assembled.variables()).doesNotContainKeys("regionCode", "countryCode");
  }
}
