package com.buurman.service.export;

import static com.buurman.service.export.BookletHelper.appendDocumentEnd;
import static com.buurman.service.export.BookletHelper.appendDocumentStart;
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
import com.buurman.domain.TenantAddress;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantAddressRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RentIncreaseLetterExporter {

  private final ContractExtensionRepository extensionRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final TenantRepository tenantRepository;
  private final TenantAddressRepository tenantAddressRepository;
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

    // Find primary tenant and their mailing/current address
    Optional<Tenant> primaryTenant = findPrimaryTenant(parties, tenantMap);
    Optional<TenantAddress> tenantAddress =
        primaryTenant.flatMap(t -> findMailingAddress(t.getId(), teamId));

    String html = buildHtml(extension, contract, property, primaryTenant, tenantAddress);
    return pdfRenderer.renderHtml(html);
  }

  // ── HTML building ───────────────────────────────────────────────

  private String buildHtml(
      ContractExtension extension,
      Contract contract,
      Property property,
      Optional<Tenant> primaryTenant,
      Optional<TenantAddress> tenantAddress) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = extension.getNewRentAmount().currency();

    String css = letterCss();

    StringBuilder html = new StringBuilder(4096);
    appendDocumentStart(html, css);

    appendLetterContent(
        html,
        extension,
        contract,
        property,
        primaryTenant,
        tenantAddress,
        ccy,
        dateFmt,
        generatedDate);

    appendDocumentEnd(html);
    return html.toString();
  }

  // ── Letter content ──────────────────────────────────────────────

  private void appendLetterContent(
      StringBuilder html,
      ContractExtension extension,
      Contract contract,
      Property property,
      Optional<Tenant> primaryTenant,
      Optional<TenantAddress> tenantAddress,
      String ccy,
      DateTimeFormatter dateFmt,
      String generatedDate) {
    html.append("<div class='letter'>");

    // Header with date and reference
    html.append("<div class='letter-header'>");
    html.append("<div class='letter-date'>").append(generatedDate).append("</div>");
    html.append("<div class='letter-ref'>Ref: ")
        .append(escapeHtml(extension.getIdentifier().orElseThrow().value()))
        .append("</div>");
    html.append("</div>");

    // Addressee
    html.append("<div class='letter-addressee'>");
    primaryTenant.ifPresentOrElse(
        tenant -> {
          html.append("<div class='addressee-name'>");
          html.append(escapeHtml(tenant.getFirstName()));
          tenant.getLastName().ifPresent(n -> html.append(" ").append(escapeHtml(n)));
          html.append("</div>");
          tenantAddress.ifPresent(
              addr -> {
                html.append("<div>").append(escapeHtml(addr.getStreet())).append("</div>");
                html.append("<div>")
                    .append(escapeHtml(addr.getPostalCode()))
                    .append(" ")
                    .append(escapeHtml(addr.getCity()))
                    .append("</div>");
                html.append("<div>").append(escapeHtml(addr.getCountryCode())).append("</div>");
              });
        },
        () -> html.append("<div class='addressee-name'>Tenant</div>"));
    html.append("</div>");

    // Subject line
    html.append("<div class='letter-subject'>Re: Notice of Rent Adjustment</div>");

    // Salutation
    String salutation =
        primaryTenant
            .map(
                t ->
                    "Dear "
                        + escapeHtml(t.getFirstName())
                        + t.getLastName().map(n -> " " + escapeHtml(n)).orElse(""))
            .orElse("Dear Tenant");
    html.append("<div class='letter-body'>");
    html.append("<p>").append(salutation).append(",</p>");

    // Body paragraph 1 — notification
    String propertyAddress =
        escapeHtml(property.getStreet())
            + ", "
            + escapeHtml(property.getPostalCode())
            + " "
            + escapeHtml(property.getCity());
    String oldRent = CurrencyUtils.formatCurrency(extension.getPreviousRentAmount().value(), ccy);
    String newRent = CurrencyUtils.formatCurrency(extension.getNewRentAmount().value(), ccy);
    String effectiveDate =
        extension
            .getNewEndDate()
            .map(d -> formatDate(extension.getPreviousEndDate(), dateFmt))
            .orElse(formatDate(extension.getPreviousEndDate(), dateFmt));
    // The effective date for rent change is the day after previous end date
    // (i.e., the start of the new extension period)
    String rentEffectiveDate = formatDate(extension.getPreviousEndDate().plusDays(1), dateFmt);

    html.append(
            "<p>This letter is to inform you that the monthly rent for the property at <strong>")
        .append(propertyAddress)
        .append("</strong> will be adjusted from <strong>")
        .append(oldRent)
        .append("</strong> to <strong>")
        .append(newRent)
        .append("</strong>, effective <strong>")
        .append(rentEffectiveDate)
        .append("</strong>.</p>");

    // Body paragraph 2 — adjustment basis
    String adjustmentBasis = buildAdjustmentBasis(extension);
    html.append("<p>").append(adjustmentBasis).append("</p>");

    // Body paragraph 3 — contract extension details
    html.append("<p>This adjustment is part of contract extension #")
        .append(extension.getExtensionNumber());
    extension
        .getNewEndDate()
        .ifPresentOrElse(
            endDate ->
                html.append(", extending your lease until <strong>")
                    .append(formatDate(endDate, dateFmt))
                    .append("</strong>."),
            () -> html.append(", for an indefinite contract period."));
    html.append("</p>");

    // Summary table
    html.append("<table class='summary-table'>");
    html.append("<tr><td class='st-label'>Contract Reference</td><td class='st-value'>")
        .append(contract.getIdentifier().orElseThrow().value())
        .append("</td></tr>");
    html.append("<tr><td class='st-label'>Property</td><td class='st-value'>")
        .append(propertyAddress)
        .append("</td></tr>");
    html.append("<tr><td class='st-label'>Previous Rent</td><td class='st-value'>")
        .append(oldRent)
        .append("</td></tr>");
    html.append("<tr><td class='st-label'>New Rent</td><td class='st-value'><strong>")
        .append(newRent)
        .append("</strong></td></tr>");
    html.append("<tr><td class='st-label'>Adjustment Type</td><td class='st-value'>")
        .append(formatEnumValue(extension.getRentAdjustmentType().name()))
        .append("</td></tr>");
    extension
        .getRentAdjustmentValue()
        .ifPresent(
            v ->
                html.append("<tr><td class='st-label'>Adjustment Value</td><td class='st-value'>")
                    .append(formatAdjustmentDisplay(v, extension.getRentAdjustmentType()))
                    .append("</td></tr>"));
    html.append("<tr><td class='st-label'>Effective Date</td><td class='st-value'>")
        .append(rentEffectiveDate)
        .append("</td></tr>");
    extension
        .getNewEndDate()
        .ifPresent(
            d ->
                html.append("<tr><td class='st-label'>New Lease End Date</td><td class='st-value'>")
                    .append(formatDate(d, dateFmt))
                    .append("</td></tr>"));
    html.append("</table>");

    // Closing
    html.append("<p>Should you have any questions regarding this adjustment, please do not ")
        .append("hesitate to contact us.</p>");
    html.append("<p>Kind regards,</p>");

    html.append("</div>");

    // Signature block
    html.append("<div class='letter-signature'>");
    html.append("<div class='sig-line'></div>");
    html.append("<div class='sig-label'>Landlord / Property Manager</div>");
    html.append("<div class='sig-date'>Date: ____________________________</div>");
    html.append("</div>");

    // Disclaimer
    html.append("<div class='letter-disclaimer'>");
    html.append(
        "This document is informational and does not constitute a legally binding agreement. "
            + "The rent adjustment is subject to the terms and conditions of the existing lease "
            + "agreement and applicable local regulations.");
    html.append("</div>");

    // Generated footer
    html.append("<div class='letter-footer'>");
    html.append("Document generated on ").append(generatedDate);
    html.append(" &mdash; Buurman Property Management");
    html.append("</div>");

    html.append("</div>");
  }

  // ── Helpers ─────────────────────────────────────────────────────

  private Optional<Tenant> findPrimaryTenant(
      List<ContractParty> parties, Map<UUID, Tenant> tenantMap) {
    return parties.stream()
        .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
        .findFirst()
        .flatMap(p -> p.getTenantId().map(tenantMap::get));
  }

  private Optional<TenantAddress> findMailingAddress(UUID tenantId, UUID teamId) {
    List<TenantAddress> addresses = tenantAddressRepository.findByTenantId(tenantId, teamId);
    // Prefer MAILING address, fall back to CURRENT
    return addresses.stream()
        .filter(
            a ->
                a.getAddressType() == TenantAddress.AddressType.MAILING
                    && a.getStatus() == TenantAddress.AddressStatus.ACTIVE)
        .findFirst()
        .or(
            () ->
                addresses.stream()
                    .filter(
                        a ->
                            a.getAddressType() == TenantAddress.AddressType.CURRENT
                                && a.getStatus() == TenantAddress.AddressStatus.ACTIVE)
                    .findFirst());
  }

  private String buildAdjustmentBasis(ContractExtension extension) {
    return switch (extension.getRentAdjustmentType()) {
      case FIXED_PERCENTAGE -> {
        String pct =
            extension
                .getRentAdjustmentValue()
                .map(v -> v.stripTrailingZeros().toPlainString() + "%")
                .orElse("a percentage");
        yield "The adjustment is based on a fixed percentage increase of " + pct + ".";
      }
      case FIXED_AMOUNT -> {
        String amt =
            extension
                .getRentAdjustmentValue()
                .map(v -> CurrencyUtils.formatCurrency(v, extension.getNewRentAmount().currency()))
                .orElse("a fixed amount");
        yield "The adjustment is based on a fixed increase of " + amt + ".";
      }
      case MANUAL -> "The rent has been manually adjusted as part of the contract extension.";
      case NONE -> "No rent adjustment has been applied for this extension period.";
    };
  }

  private String formatAdjustmentDisplay(
      java.math.BigDecimal value, ContractExtension.RentAdjustmentType type) {
    return switch (type) {
      case FIXED_PERCENTAGE -> value.stripTrailingZeros().toPlainString() + "%";
      case FIXED_AMOUNT -> value.stripTrailingZeros().toPlainString();
      default -> value.stripTrailingZeros().toPlainString();
    };
  }

  // ── CSS ─────────────────────────────────────────────────────────

  private static String letterCss() {
    return """
    @page { margin: 60px 60px 70px 60px; size: A4; }
    body { font-family: 'Satoshi', 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0; \
    padding: 0; color: #292524; font-size: 13px; line-height: 1.6; }

    .letter { max-width: 100%; }

    .letter-header { margin-bottom: 40px; }
    .letter-date { font-size: 14px; color: #292524; font-weight: 500; }
    .letter-ref { font-size: 12px; color: #78716c; margin-top: 4px; letter-spacing: 0.5px; }

    .letter-addressee { margin-bottom: 30px; line-height: 1.5; font-size: 14px; color: #292524; }
    .addressee-name { font-weight: 600; font-size: 15px; }

    .letter-subject { font-size: 16px; font-weight: 700; color: #0c4a6e; \
    margin-bottom: 24px; padding-bottom: 12px; border-bottom: 2px solid #0c4a6e; }

    .letter-body { font-size: 13px; color: #292524; line-height: 1.7; }
    .letter-body p { margin: 0 0 14px 0; }
    .letter-body strong { font-weight: 600; }

    .summary-table { width: 100%; border-collapse: collapse; margin: 20px 0 24px 0; \
    border: 1px solid #e7e5e4; border-radius: 6px; }
    .summary-table tr { border-bottom: 1px solid #e7e5e4; }
    .summary-table tr:last-child { border-bottom: none; }
    .st-label { padding: 10px 16px; font-size: 11px; text-transform: uppercase; \
    letter-spacing: 0.8px; color: #78716c; font-weight: 600; width: 40%; \
    background-color: #fafaf9; vertical-align: top; }
    .st-value { padding: 10px 16px; font-size: 13px; color: #292524; vertical-align: top; }

    .letter-signature { margin-top: 50px; width: 50%; }
    .sig-line { border-bottom: 1px solid #292524; margin-bottom: 8px; height: 40px; }
    .sig-label { font-size: 13px; color: #57534e; font-weight: 500; }
    .sig-date { font-size: 12px; color: #57534e; margin-top: 16px; }

    .letter-disclaimer { margin-top: 40px; padding: 14px 16px; background-color: #fafaf9; \
    border: 1px solid #e7e5e4; border-radius: 6px; font-size: 11px; color: #78716c; \
    line-height: 1.5; font-style: italic; }

    .letter-footer { text-align: center; margin-top: 30px; font-size: 10px; color: #a8a29e; }
    """;
  }
}
