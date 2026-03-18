package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.appendCoverCell;
import static com.buurman.service.export.BookletHelper.appendCoverEnd;
import static com.buurman.service.export.BookletHelper.appendCoverStart;
import static com.buurman.service.export.BookletHelper.appendDocumentEnd;
import static com.buurman.service.export.BookletHelper.appendDocumentStart;
import static com.buurman.service.export.BookletHelper.appendField;
import static com.buurman.service.export.BookletHelper.appendPageEnd;
import static com.buurman.service.export.BookletHelper.appendPageStart;
import static com.buurman.service.export.BookletHelper.appendRunningFooter;
import static com.buurman.service.export.BookletHelper.appendSectionTitle;
import static com.buurman.service.export.BookletHelper.appendStatusBadge;
import static com.buurman.service.export.BookletHelper.escapeHtml;
import static com.buurman.service.export.BookletHelper.formatDate;
import static com.buurman.service.export.BookletHelper.formatEnumValue;
import static java.util.stream.Collectors.toMap;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractExtensionAddendumExporter {

  private final ContractExtensionRepository extensionRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final TenantRepository tenantRepository;
  private final ContractPartyService contractPartyService;
  private final PdfRenderer pdfRenderer;
  private final Clock clock;

  public byte[] generate(ContractExtensionIdentifier extensionIdentifier, UUID teamId) {
    ContractExtension extension =
        extensionRepository.getByIdentifierAndTeamId(extensionIdentifier, teamId);
    Contract contract = contractRepository.getByIdAndTeamId(extension.getContractId(), teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    List<ContractParty> parties =
        contractPartyService.getPartiesForContract(contract.getId(), teamId);
    Set<UUID> tenantIds = new HashSet<>();
    for (ContractParty party : parties) {
      party.getTenantId().ifPresent(tenantIds::add);
    }
    Map<UUID, Tenant> tenantMap =
        tenantRepository.findByIdsAndTeamId(tenantIds, teamId).stream()
            .collect(toMap(Tenant::getId, t -> t));

    String html = buildHtml(extension, contract, property, parties, tenantMap);
    return pdfRenderer.renderHtml(html);
  }

  // ── HTML building ───────────────────────────────────────────────

  private String buildHtml(
      ContractExtension extension,
      Contract contract,
      Property property,
      List<ContractParty> parties,
      Map<UUID, Tenant> tenantMap) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = extension.getNewRentAmount().currency();

    String primaryName = findPrimaryTenantName(parties, tenantMap);
    String tenantNames = buildTenantNamesList(parties, tenantMap);

    String css = BookletCss.base() + BookletCss.contractStatusBadges() + signatureBlockCss();

    StringBuilder html = new StringBuilder(4096);
    appendDocumentStart(html, css);
    appendRunningFooter(html, generatedDate);

    appendCoverPage(html, extension, contract, property, primaryName, ccy, dateFmt, generatedDate);
    appendDetailsPage(
        html, extension, contract, property, tenantNames, ccy, dateFmt, generatedDate);

    appendDocumentEnd(html);
    return html.toString();
  }

  // ── Page: Cover ─────────────────────────────────────────────────

  private void appendCoverPage(
      StringBuilder html,
      ContractExtension extension,
      Contract contract,
      Property property,
      String primaryName,
      String ccy,
      DateTimeFormatter dateFmt,
      String generatedDate) {
    appendCoverStart(
        html,
        "CONTRACT EXTENSION ADDENDUM #" + extension.getExtensionNumber(),
        escapeHtml(extension.getIdentifier().orElseThrow().value()),
        generatedDate);

    appendStatusBadge(html, extension.getStatus().name());

    html.append("<table class='cover-summary'>");
    html.append("<tr>");
    appendCoverCell(
        html, "Property", escapeHtml(property.getStreet()) + ", " + escapeHtml(property.getCity()));
    appendCoverCell(html, "Primary Tenant", primaryName);
    html.append("</tr><tr>");
    appendCoverCell(html, "Contract", escapeHtml(contract.getIdentifier().orElseThrow().value()));
    appendCoverCell(html, "Trigger", formatEnumValue(extension.getTriggerType().name()));
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        "Previous Rent",
        CurrencyUtils.formatCurrency(extension.getPreviousRentAmount().value(), ccy));
    appendCoverCell(
        html, "New Rent", CurrencyUtils.formatCurrency(extension.getNewRentAmount().value(), ccy));
    html.append("</tr>");
    html.append("</table>");

    appendCoverEnd(html);
  }

  // ── Page: Extension Details ──────────────────────────────────────

  private void appendDetailsPage(
      StringBuilder html,
      ContractExtension extension,
      Contract contract,
      Property property,
      String tenantNames,
      String ccy,
      DateTimeFormatter dateFmt,
      String generatedDate) {
    appendPageStart(html, "Extension Details");

    // Property & Tenant Info
    appendSectionTitle(html, "Property &amp; Tenant");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        "Property Address",
        escapeHtml(property.getStreet())
            + ", "
            + escapeHtml(property.getPostalCode())
            + " "
            + escapeHtml(property.getCity()));
    appendField(html, "Tenant(s)", tenantNames);
    html.append("</tr>");
    html.append("</table>");

    // Original Contract
    appendSectionTitle(html, "Original Contract");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Contract Reference", contract.getIdentifier().orElseThrow().value());
    appendField(html, "Start Date", formatDate(contract.getStartDate(), dateFmt));
    html.append("</tr><tr>");
    appendField(
        html,
        "Original End Date",
        contract.getEndDate().map(d -> formatDate(d, dateFmt)).orElse("Indefinite"));
    appendField(
        html,
        "Contract Type",
        formatEnumValue(
            contract.getContractType() != null ? contract.getContractType().name() : ""));
    html.append("</tr>");
    html.append("</table>");

    // Extension Details
    appendSectionTitle(html, "Extension #" + extension.getExtensionNumber());
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Extension Number", String.valueOf(extension.getExtensionNumber()));
    appendField(html, "Status", formatEnumValue(extension.getStatus().name()));
    html.append("</tr><tr>");
    appendField(html, "Previous End Date", formatDate(extension.getPreviousEndDate(), dateFmt));
    appendField(
        html,
        "New End Date",
        extension.getNewEndDate().map(d -> formatDate(d, dateFmt)).orElse("Indefinite"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Previous Rent",
        CurrencyUtils.formatCurrency(extension.getPreviousRentAmount().value(), ccy));
    appendField(
        html, "New Rent", CurrencyUtils.formatCurrency(extension.getNewRentAmount().value(), ccy));
    html.append("</tr><tr>");
    appendField(
        html, "Rent Adjustment Type", formatEnumValue(extension.getRentAdjustmentType().name()));
    appendField(
        html,
        "Adjustment Value",
        extension
            .getRentAdjustmentValue()
            .map(v -> formatAdjustmentValue(v, extension.getRentAdjustmentType()))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(html, "Trigger Type", formatEnumValue(extension.getTriggerType().name()));
    appendField(
        html,
        "Activated",
        extension
            .getActivatedAt()
            .map(i -> formatDate(i.atZone(java.time.ZoneOffset.UTC).toLocalDate(), dateFmt))
            .orElse("—"));
    html.append("</tr>");
    html.append("</table>");

    // Notes
    extension
        .getNotes()
        .filter(n -> !n.isBlank())
        .ifPresent(
            notes -> {
              appendSectionTitle(html, "Notes");
              html.append("<div class='text-block'>").append(escapeHtml(notes)).append("</div>");
            });

    // Signature blocks
    appendSectionTitle(html, "Signatures");
    html.append("<div class='signature-container'>");
    appendSignatureBlock(html, "Landlord");
    appendSignatureBlock(html, "Tenant");
    html.append("</div>");

    // Disclaimer
    html.append("<div class='disclaimer'>");
    html.append(
        "This document is informational and does not constitute a legally binding agreement. "
            + "Both parties should consult with legal counsel before entering into any "
            + "contractual obligations.");
    html.append("</div>");

    // Generated date
    html.append("<div style='text-align:center;margin-top:30px;font-size:11px;color:#a8a29e;'>");
    html.append("Document generated on ").append(generatedDate);
    html.append("</div>");

    appendPageEnd(html);
  }

  // ── Helpers ─────────────────────────────────────────────────────

  private String findPrimaryTenantName(List<ContractParty> parties, Map<UUID, Tenant> tenantMap) {
    return parties.stream()
        .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
        .findFirst()
        .flatMap(p -> p.getTenantId().map(tenantMap::get))
        .map(
            t ->
                escapeHtml(t.getFirstName())
                    + t.getLastName().map(n -> " " + escapeHtml(n)).orElse(""))
        .orElse("—");
  }

  private String buildTenantNamesList(List<ContractParty> parties, Map<UUID, Tenant> tenantMap) {
    List<String> names =
        parties.stream()
            .filter(p -> p.getTenantId().isPresent())
            .map(p -> p.getTenantId().map(tenantMap::get))
            .flatMap(Optional::stream)
            .map(
                t ->
                    escapeHtml(t.getFirstName())
                        + t.getLastName().map(n -> " " + escapeHtml(n)).orElse(""))
            .toList();
    if (names.isEmpty()) {
      return "—";
    }
    return String.join(", ", names);
  }

  private String formatAdjustmentValue(
      java.math.BigDecimal value, ContractExtension.RentAdjustmentType type) {
    return switch (type) {
      case FIXED_PERCENTAGE -> value.stripTrailingZeros().toPlainString() + "%";
      case FIXED_AMOUNT -> value.stripTrailingZeros().toPlainString();
      default -> value.stripTrailingZeros().toPlainString();
    };
  }

  private void appendSignatureBlock(StringBuilder html, String role) {
    html.append("<div class='signature-block'>");
    html.append("<div class='signature-role'>").append(escapeHtml(role)).append("</div>");
    html.append("<div class='signature-line'></div>");
    html.append("<div class='signature-fields'>");
    html.append("<div class='signature-field'>Name: ____________________________</div>");
    html.append("<div class='signature-field'>Date: ____________________________</div>");
    html.append("</div>");
    html.append("</div>");
  }

  private static String signatureBlockCss() {
    return """
    .signature-container { display: flex; justify-content: space-between; margin-top: 20px; }
    .signature-block { width: 45%; padding: 20px; }
    .signature-role { font-size: 14px; font-weight: 700; color: #0c4a6e; \
    margin-bottom: 60px; text-transform: uppercase; letter-spacing: 1px; }
    .signature-line { border-bottom: 1px solid #292524; margin-bottom: 8px; }
    .signature-fields { margin-top: 16px; }
    .signature-field { font-size: 12px; color: #57534e; margin-bottom: 12px; }
    .disclaimer { margin-top: 40px; padding: 16px; background-color: #fafaf9; \
    border: 1px solid #e7e5e4; border-radius: 6px; font-size: 11px; color: #78716c; \
    line-height: 1.5; font-style: italic; }
    """;
  }
}
