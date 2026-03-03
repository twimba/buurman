package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
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
import static com.buurman.service.export.BookletHelper.appendSummaryCard;
import static com.buurman.service.export.BookletHelper.escapeHtml;
import static com.buurman.service.export.BookletHelper.formatDate;
import static com.buurman.service.export.BookletHelper.formatEnumValue;
import static com.buurman.service.export.BookletHelper.sanitizeRichText;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Ulid;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Property;
import com.buurman.domain.Tenant;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractBookletExporter {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final TenantRepository tenantRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository paymentReceivalRepository;
  private final ContractPaymentInstructionRepository contractPaymentInstructionRepository;
  private final PaymentInstructionRepository paymentInstructionRepository;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final ContractPartyService contractPartyService;
  private final PdfRenderer pdfRenderer;
  private final Clock clock;

  private static final Map<String, String> PAYMENT_METHOD_LABELS =
      Map.of(
          "BANK_TRANSFER", "Bank Transfer",
          "PAYPAL", "PayPal",
          "CASH", "Cash",
          "CHECK", "Check",
          "DIRECT_DEBIT", "Direct Debit",
          "IDEAL_WERO", "iDEAL / Wero",
          "ZELLE", "Zelle",
          "OTHER", "Other");

  public byte[] generate(Ulid contractIdentifier, UUID teamId) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    // Load parties + tenants
    List<ContractParty> parties =
        contractPartyService.getPartiesForContract(contract.getId(), teamId);
    Set<UUID> tenantIds = new HashSet<>();
    for (ContractParty party : parties) {
      party.getTenantId().ifPresent(tenantIds::add);
    }
    Map<UUID, Tenant> tenantMap =
        tenantRepository.findByIdsAndTeamId(tenantIds, teamId).stream()
            .collect(toMap(Tenant::getId, t -> t));

    // Load payments + receivals
    List<Payment> payments = paymentRepository.findByContractId(contract.getId(), teamId);
    Set<UUID> paymentIds = new HashSet<>();
    for (Payment p : payments) paymentIds.add(p.getId());
    List<PaymentReceival> allReceivals =
        paymentIds.isEmpty()
            ? List.of()
            : paymentReceivalRepository.findByPaymentIdsAndTeamId(paymentIds, teamId);
    Map<UUID, BigDecimal> receivedByPayment = new HashMap<>();
    for (PaymentReceival r : allReceivals) {
      receivedByPayment.merge(r.getPaymentId(), r.getAmount(), BigDecimal::add);
    }

    // Load rent periods
    List<ContractRentPeriod> rentPeriods =
        rentPeriodRepository.findByContractIdAndTeamId(contract.getId(), teamId);

    // Load payment instructions
    List<ContractPaymentInstruction> allCpis =
        contractPaymentInstructionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    Set<UUID> piIds = new HashSet<>();
    for (ContractPaymentInstruction cpi : allCpis) {
      if (!cpi.getIsCustom()) {
        cpi.getPaymentInstructionId().ifPresent(piIds::add);
      }
    }
    Map<UUID, PaymentInstruction> piMap =
        piIds.isEmpty()
            ? Map.of()
            : paymentInstructionRepository.findAllByTeamId(teamId).stream()
                .filter(pi -> piIds.contains(pi.getId()))
                .collect(toMap(PaymentInstruction::getId, pi -> pi));

    String html =
        buildHtml(
            contract,
            property,
            parties,
            tenantMap,
            payments,
            receivedByPayment,
            rentPeriods,
            allCpis,
            piMap);
    return pdfRenderer.renderHtml(html);
  }

  // ── HTML building ───────────────────────────────────────────────

  private String buildHtml(
      Contract contract,
      Property property,
      List<ContractParty> parties,
      Map<UUID, Tenant> tenantMap,
      List<Payment> payments,
      Map<UUID, BigDecimal> receivedByPayment,
      List<ContractRentPeriod> rentPeriods,
      List<ContractPaymentInstruction> allCpis,
      Map<UUID, PaymentInstruction> piMap) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = contract.getRentAmountCurrency();

    String primaryName =
        findPrimaryTenant(parties, tenantMap)
            .map(
                t ->
                    escapeHtml(t.getFirstName())
                        + t.getLastName().map(n -> " " + escapeHtml(n)).orElse(""))
            .orElse("—");

    PaymentAggregation agg = aggregatePayments(payments, receivedByPayment);

    String css =
        BookletCss.base()
            + BookletCss.contractStatusBadges()
            + BookletCss.partyCards()
            + BookletCss.paymentInstructionCards()
            + BookletCss.summaryGrid()
            + BookletCss.paymentTable()
            + BookletCss.paymentStatusBadges();

    StringBuilder html = new StringBuilder(8192);
    appendDocumentStart(html, css);
    appendRunningFooter(html, generatedDate);

    appendCoverPage(html, contract, property, primaryName, ccy, dateFmt, generatedDate);
    appendContractDetailsPage(html, contract, property, rentPeriods, dateFmt, ccy);
    appendPartiesPage(html, parties, tenantMap);
    appendPaymentInstructionsPage(html, allCpis, piMap, dateFmt);
    appendPaymentOverviewPage(html, payments, receivedByPayment, agg, ccy, dateFmt);

    appendDocumentEnd(html);
    return html.toString();
  }

  // ── Page: Cover ─────────────────────────────────────────────────

  private void appendCoverPage(
      StringBuilder html,
      Contract contract,
      Property property,
      String primaryName,
      String ccy,
      DateTimeFormatter dateFmt,
      String generatedDate) {
    appendCoverStart(html, "CONTRACT REPORT", escapeHtml(contract.getIdentifier().orElseThrow().value()), generatedDate);

    String statusStr = contract.getStatus() != null ? contract.getStatus().name() : "DRAFT";
    appendStatusBadge(html, statusStr);

    html.append("<table class='cover-summary'>");
    html.append("<tr>");
    appendCoverCell(
        html, "Property", escapeHtml(property.getStreet()) + ", " + escapeHtml(property.getCity()));
    appendCoverCell(html, "Primary Tenant", primaryName);
    html.append("</tr><tr>");
    appendCoverCell(
        html, "Current Rent", CurrencyUtils.formatCurrency(contract.getRentAmount(), ccy));
    String period =
        formatDate(contract.getStartDate(), dateFmt)
            + " — "
            + contract.getEndDate().map(d -> formatDate(d, dateFmt)).orElse("Indefinite");
    appendCoverCell(html, "Contract Period", period);
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        "Contract Type",
        formatEnumValue(
            contract.getContractType() != null ? contract.getContractType().name() : ""));
    appendCoverCell(
        html,
        "Payment Frequency",
        formatEnumValue(
            contract.getPaymentFrequency() != null ? contract.getPaymentFrequency().name() : ""));
    html.append("</tr>");
    html.append("</table>");

    appendCoverEnd(html);
  }

  // ── Page: Contract Details ──────────────────────────────────────

  private void appendContractDetailsPage(
      StringBuilder html,
      Contract contract,
      Property property,
      List<ContractRentPeriod> rentPeriods,
      DateTimeFormatter dateFmt,
      String ccy) {
    appendPageStart(html, "Contract Details");

    appendSectionTitle(html, "Contract Information");
    String statusStr = contract.getStatus() != null ? contract.getStatus().name() : "DRAFT";
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Contract ID", contract.getIdentifier().orElseThrow().value());
    appendField(html, "Status", formatEnumValue(statusStr));
    html.append("</tr><tr>");
    appendField(
        html,
        "Contract Type",
        formatEnumValue(
            contract.getContractType() != null ? contract.getContractType().name() : ""));
    appendField(
        html,
        "Signed Date",
        contract.getSignedDate().map(d -> formatDate(d, dateFmt)).orElse("Not signed"));
    html.append("</tr><tr>");
    appendField(html, "Start Date", formatDate(contract.getStartDate(), dateFmt));
    appendField(
        html,
        "End Date",
        contract.getEndDate().map(d -> formatDate(d, dateFmt)).orElse("Indefinite"));
    html.append("</tr><tr>");
    appendField(html, "Current Rent", CurrencyUtils.formatCurrency(contract.getRentAmount(), ccy));
    appendField(
        html,
        "Deposit Amount",
        CurrencyUtils.formatCurrency(contract.getDepositAmount().orElse(null), ccy));
    html.append("</tr><tr>");
    appendField(
        html,
        "Security Deposit",
        CurrencyUtils.formatCurrency(contract.getSecurityDeposit().orElse(null), ccy));
    appendField(html, "Currency", CurrencyUtils.getCurrencySymbol(ccy) + " (" + ccy + ")");
    html.append("</tr><tr>");
    appendField(
        html,
        "Payment Frequency",
        formatEnumValue(
            contract.getPaymentFrequency() != null ? contract.getPaymentFrequency().name() : ""));
    appendField(
        html,
        "Payment Due Day",
        contract.getPaymentDueDay().map(d -> "Day " + d + " of month").orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Auto-Renewal",
        contract.getAutoRenewal() != null && contract.getAutoRenewal() ? "Yes" : "No");
    appendField(
        html,
        "Renewal Notice",
        contract.getRenewalNoticeDays() != null ? contract.getRenewalNoticeDays() + " days" : "—");
    html.append("</tr><tr>");
    appendField(
        html,
        "Termination Notice",
        contract.getTerminationNoticeDays() != null
            ? contract.getTerminationNoticeDays() + " days"
            : "—");
    appendField(html, "Late Fee", contract.getLateFeePercentage().map(p -> p + "%").orElse("—"));
    html.append("</tr>");
    html.append("</table>");

    // Rich text sections
    contract
        .getTermsAndConditions()
        .filter(s -> !s.isBlank())
        .ifPresent(
            tc -> {
              appendSectionTitle(html, "Terms &amp; Conditions");
              html.append("<div class='text-block'>").append(sanitizeRichText(tc)).append("</div>");
            });
    contract
        .getNotes()
        .filter(s -> !s.isBlank())
        .ifPresent(
            notes -> {
              appendSectionTitle(html, "Notes");
              html.append("<div class='text-block'>")
                  .append(sanitizeRichText(notes))
                  .append("</div>");
            });

    // Rent History section
    if (rentPeriods.size() > 1) {
      appendRentHistorySection(html, rentPeriods, ccy, dateFmt);
    }

    // Property section
    appendSectionTitle(html, "Property");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Address", escapeHtml(property.getStreet()));
    appendField(
        html, "City", escapeHtml(property.getCity()) + " " + escapeHtml(property.getPostalCode()));
    html.append("</tr><tr>");
    appendField(html, "Country", escapeHtml(property.getCountry()));
    appendField(html, "Property Type", formatEnumValue(property.getPropertyType().name()));
    html.append("</tr><tr>");
    appendField(html, "Category", formatEnumValue(property.getPropertyCategory().name()));
    appendField(html, "Property ID", property.getIdentifier().orElseThrow().value());
    html.append("</tr><tr>");
    appendField(
        html,
        "Area",
        property
            .getAreaValue()
            .map(av -> "%s %s".formatted(av, property.getAreaUnit().orElse("")))
            .orElse("–"));
    html.append("</tr>");
    html.append("</table>");

    appendPageEnd(html);
  }

  // ── Page: Contract Parties ──────────────────────────────────────

  private void appendPartiesPage(
      StringBuilder html, List<ContractParty> parties, Map<UUID, Tenant> tenantMap) {
    appendPageStart(html, "Contract Parties");

    appendSectionTitle(html, "Parties (" + parties.size() + ")");

    List<ContractParty> sortedParties = new ArrayList<>(parties);
    sortedParties.sort(
        (a, b) -> {
          if (a.getRole() == ContractPartyRole.PRIMARY_TENANT) {
            return -1;
          }
          if (b.getRole() == ContractPartyRole.PRIMARY_TENANT) {
            return 1;
          }
          return a.getRole().compareTo(b.getRole());
        });

    for (ContractParty party : sortedParties) {
      Tenant t = party.getTenantId().map(tenantMap::get).orElse(null);
      if (t == null) {
        continue;
      }
      String roleColor = getPartyRoleColor(party.getRole());
      String roleBg = getPartyRoleBgColor(party.getRole());

      html.append("<div class='party-card' style='border-left-color:")
          .append(roleColor)
          .append(";'>");

      html.append("<div class='party-role' style='color:")
          .append(roleColor)
          .append(";background-color:")
          .append(roleBg)
          .append(";'>");
      html.append(party.getRole().getDisplayName());
      html.append("</div>");

      html.append("<div class='party-name'>");
      html.append(escapeHtml(t.getFirstName()));
      t.getLastName().ifPresent(n -> html.append(" ").append(escapeHtml(n)));
      html.append("</div>");

      html.append("<table class='party-details'>");
      String phone = t.getPhone().orElse(null);
      if (t.getEmail().isPresent()) {
        html.append("<tr><td class='pd-label'>Email</td><td class='pd-value'>")
            .append(escapeHtml(t.getEmail().get()))
            .append("</td>");
        if (phone != null) {
          html.append("<td class='pd-label'>Phone</td><td class='pd-value'>")
              .append(escapeHtml(phone))
              .append("</td>");
        } else {
          html.append("<td></td><td></td>");
        }
        html.append("</tr>");
      } else if (phone != null) {
        html.append("<tr><td class='pd-label'>Phone</td><td class='pd-value'>")
            .append(escapeHtml(phone))
            .append("</td><td></td><td></td></tr>");
      }
      String taxNumber = t.getTaxNumber().orElse(null);
      String idNumber = t.getIdNumber().orElse(null);
      boolean hasTax = taxNumber != null && !taxNumber.isBlank();
      boolean hasId = idNumber != null && !idNumber.isBlank();
      if (hasTax || hasId) {
        html.append("<tr>");
        if (hasTax) {
          html.append("<td class='pd-label'>Tax Number</td><td class='pd-value'>")
              .append(escapeHtml(taxNumber))
              .append("</td>");
        } else {
          html.append("<td></td><td></td>");
        }
        if (hasId) {
          html.append("<td class='pd-label'>ID Number</td><td class='pd-value'>")
              .append(escapeHtml(idNumber))
              .append("</td>");
        } else {
          html.append("<td></td><td></td>");
        }
        html.append("</tr>");
      }
      html.append(
              "<tr><td class='pd-label'>Reference</td><td class='pd-value'"
                  + " style='color:#a0aec0;font-size:11px;'>")
          .append(t.getIdentifier().orElseThrow().value())
          .append("</td><td></td><td></td></tr>");
      html.append("</table></div>");
    }

    appendPageEnd(html);
  }

  // ── Section: Rent History ──────────────────────────────────────

  private void appendRentHistorySection(
      StringBuilder html,
      List<ContractRentPeriod> rentPeriods,
      String ccy,
      DateTimeFormatter dateFmt) {
    appendSectionTitle(html, "Rent History (" + rentPeriods.size() + " periods)");

    // Periods are already ordered by effective_from DESC from the repository
    List<ContractRentPeriod> sorted = new ArrayList<>(rentPeriods);
    sorted.sort((a, b) -> b.getEffectiveFrom().compareTo(a.getEffectiveFrom()));

    html.append("<table class='payment-table'>");
    html.append("<thead><tr>");
    html.append(
        "<th>Effective From</th><th>Effective To</th><th style='text-align:right'>Rent"
            + " Amount</th><th style='text-align:right'>Change</th>");
    html.append("</tr></thead><tbody>");

    for (int i = 0; i < sorted.size(); i++) {
      ContractRentPeriod period = sorted.get(i);
      boolean isCurrent = period.getEffectiveTo().isEmpty();

      html.append("<tr>");
      html.append("<td>").append(formatDate(period.getEffectiveFrom(), dateFmt)).append("</td>");
      html.append("<td>")
          .append(
              period.getEffectiveTo().isPresent()
                  ? formatDate(period.getEffectiveTo().get(), dateFmt)
                  : "<span style='color:#166534;font-weight:600;'>Current</span>")
          .append("</td>");
      html.append("<td style='text-align:right;font-variant-numeric:tabular-nums;")
          .append(isCurrent ? "font-weight:600;" : "")
          .append("'>")
          .append(CurrencyUtils.formatCurrency(period.getRentAmount(), ccy))
          .append("</td>");

      // Percentage change vs next older period
      html.append("<td style='text-align:right;'>");
      if (i < sorted.size() - 1) {
        BigDecimal previousAmount = sorted.get(i + 1).getRentAmount();
        if (previousAmount.compareTo(BigDecimal.ZERO) > 0) {
          BigDecimal change =
              period
                  .getRentAmount()
                  .subtract(previousAmount)
                  .multiply(new BigDecimal("100"))
                  .divide(previousAmount, 1, java.math.RoundingMode.HALF_UP);
          String color = change.compareTo(BigDecimal.ZERO) > 0 ? "#dc2626" : "#166534";
          String prefix = change.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
          html.append("<span style='color:")
              .append(color)
              .append(";font-weight:600;font-size:12px;'>")
              .append(prefix)
              .append(change)
              .append("%</span>");
        }
      } else {
        html.append("<span style='color:#718096;font-size:12px;'>Initial</span>");
      }
      html.append("</td>");

      html.append("</tr>");
    }

    html.append("</tbody></table>");
  }

  // ── Page: Payment Instructions ──────────────────────────────────

  private void appendPaymentInstructionsPage(
      StringBuilder html,
      List<ContractPaymentInstruction> allCpis,
      Map<UUID, PaymentInstruction> piMap,
      DateTimeFormatter dateFmt) {
    if (allCpis.isEmpty()) {
      return;
    }

    appendPageStart(html, "Payment Instructions");

    List<ContractPaymentInstruction> sortedCpis = new ArrayList<>(allCpis);
    sortedCpis.sort(
        (a, b) -> {
          LocalDate aDate = a.getEffectiveFrom();
          LocalDate bDate = b.getEffectiveFrom();
          if (aDate == null && bDate == null) {
            return 0;
          } else if (aDate == null) {
            return 1;
          } else if (bDate == null) {
            return -1;
          } else {
            return bDate.compareTo(aDate);
          }
        });

    for (ContractPaymentInstruction cpi : sortedCpis) {
      boolean isCustom = cpi.getIsCustom();
      PaymentInstruction tpl =
          !isCustom ? cpi.getPaymentInstructionId().map(piMap::get).orElse(null) : null;

      String piName =
          isCustom ? cpi.getCustomName().orElse(null) : (tpl != null ? tpl.getName() : null);
      String piMethod =
          isCustom
              ? cpi.getCustomPaymentMethod().orElse(null)
              : (tpl != null && tpl.getPaymentMethod() != null
                  ? tpl.getPaymentMethod().name()
                  : null);
      String piBankName =
          isCustom
              ? cpi.getCustomBankName().orElse(null)
              : (tpl != null ? tpl.getBankName().orElse(null) : null);
      String piAccountHolder =
          isCustom
              ? cpi.getCustomAccountHolderName().orElse(null)
              : (tpl != null ? tpl.getAccountHolderName().orElse(null) : null);
      String piIban =
          isCustom
              ? cpi.getCustomIban().orElse(null)
              : (tpl != null ? tpl.getIban().orElse(null) : null);
      String piBicSwift =
          isCustom
              ? cpi.getCustomBicSwift().orElse(null)
              : (tpl != null ? tpl.getBicSwift().orElse(null) : null);
      String piAccountNumber =
          isCustom
              ? cpi.getCustomAccountNumber().orElse(null)
              : (tpl != null ? tpl.getAccountNumber().orElse(null) : null);
      String piRoutingNumber =
          isCustom
              ? cpi.getCustomRoutingNumber().orElse(null)
              : (tpl != null ? tpl.getRoutingNumber().orElse(null) : null);
      String piReference =
          isCustom
              ? cpi.getCustomPaymentReference().orElse(null)
              : (tpl != null ? tpl.getPaymentReference().orElse(null) : null);
      String piDetails =
          isCustom
              ? cpi.getCustomAdditionalDetails().orElse(null)
              : (tpl != null ? tpl.getAdditionalDetails().orElse(null) : null);

      boolean isCurrent = cpi.getEffectiveTo().isEmpty();
      String accentColor = isCurrent ? "#2b6cb0" : "#a0aec0";

      html.append("<div class='pi-card' style='border-left-color:")
          .append(accentColor)
          .append(";'>");

      // Header
      html.append("<div class='pi-header'>");
      if (piName != null && !piName.isBlank()) {
        html.append("<span class='pi-name'>").append(escapeHtml(piName)).append("</span>");
      }
      if (piMethod != null && !piMethod.isBlank()) {
        html.append("<span class='pi-method'>")
            .append(formatPaymentMethod(piMethod))
            .append("</span>");
      }
      if (isCurrent) {
        html.append("<span class='pi-current'>Current</span>");
      }
      html.append("</div>");

      // Period
      String fromStr =
          cpi.getEffectiveFrom() != null ? formatDate(cpi.getEffectiveFrom(), dateFmt) : "—";
      String toStr = cpi.getEffectiveTo().map(d -> formatDate(d, dateFmt)).orElse("Present");
      html.append("<div class='pi-period'>")
          .append(fromStr)
          .append(" — ")
          .append(toStr)
          .append("</div>");

      // Details grid
      html.append("<table class='detail-grid'>");
      boolean hasBank = piBankName != null && !piBankName.isBlank();
      boolean hasHolder = piAccountHolder != null && !piAccountHolder.isBlank();
      if (hasBank || hasHolder) {
        html.append("<tr>");
        appendField(html, "Bank Name", hasBank ? escapeHtml(piBankName) : null);
        appendField(html, "Account Holder", hasHolder ? escapeHtml(piAccountHolder) : null);
        html.append("</tr>");
      }
      boolean hasIban = piIban != null && !piIban.isBlank();
      boolean hasBic = piBicSwift != null && !piBicSwift.isBlank();
      if (hasIban || hasBic) {
        html.append("<tr>");
        appendField(html, "IBAN", hasIban ? escapeHtml(piIban) : null);
        appendField(html, "BIC / SWIFT", hasBic ? escapeHtml(piBicSwift) : null);
        html.append("</tr>");
      }
      boolean hasAccNum = piAccountNumber != null && !piAccountNumber.isBlank();
      boolean hasRouting = piRoutingNumber != null && !piRoutingNumber.isBlank();
      if (hasAccNum || hasRouting) {
        html.append("<tr>");
        appendField(html, "Account Number", hasAccNum ? escapeHtml(piAccountNumber) : null);
        appendField(html, "Routing Number", hasRouting ? escapeHtml(piRoutingNumber) : null);
        html.append("</tr>");
      }
      if (piReference != null && !piReference.isBlank()) {
        html.append("<tr>");
        appendField(html, "Payment Reference", escapeHtml(piReference));
        html.append("<td></td>");
        html.append("</tr>");
      }
      html.append("</table>");

      if (piDetails != null && !piDetails.isBlank()) {
        html.append(
            "<div class='pi-details'><span class='fg-label'>Additional Details</span><br/>");
        html.append("<span style='font-size:13px;color:#2d3748;'>")
            .append(escapeHtml(piDetails))
            .append("</span></div>");
      }
      cpi.getNotes()
          .filter(n -> !n.isBlank())
          .ifPresent(
              n -> {
                html.append("<div class='pi-details'><span class='fg-label'>Notes</span><br/>");
                html.append("<span style='font-size:13px;color:#2d3748;'>")
                    .append(escapeHtml(n))
                    .append("</span></div>");
              });

      html.append("</div>");
    }

    appendPageEnd(html);
  }

  // ── Page: Payment Overview ──────────────────────────────────────

  private void appendPaymentOverviewPage(
      StringBuilder html,
      List<Payment> payments,
      Map<UUID, BigDecimal> receivedByPayment,
      PaymentAggregation agg,
      String ccy,
      DateTimeFormatter dateFmt) {
    appendPageStart(html, "Payment Overview");

    // Summary cards
    appendSectionTitle(html, "Summary");
    html.append("<table class='summary-grid'><tr>");
    appendSummaryCard(
        html,
        "Paid",
        agg.countPaid,
        CurrencyUtils.formatCurrency(agg.totalPaid, ccy),
        "#f0fdf4",
        "#16a34a",
        "#166534");
    appendSummaryCard(
        html,
        "Pending",
        agg.countPending,
        CurrencyUtils.formatCurrency(agg.totalPending, ccy),
        "#fefce8",
        "#ca8a04",
        "#854d0e");
    appendSummaryCard(
        html,
        "Partial",
        agg.countPartial,
        CurrencyUtils.formatCurrency(
            payments.stream()
                .filter(p -> p.getStatus() == PARTIALLY_PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            ccy),
        "#eff6ff",
        "#3b82f6",
        "#1e40af");
    appendSummaryCard(
        html,
        "Overdue",
        agg.countOverdue,
        CurrencyUtils.formatCurrency(agg.totalOverdue, ccy),
        "#fef2f2",
        "#dc2626",
        "#991b1b");
    html.append("</tr></table>");

    // Payment table
    html.append("<h2 class='section-title'>Payment History (")
        .append(payments.size())
        .append(")</h2>");

    if (payments.isEmpty()) {
      html.append(
          "<p style='color:#718096;font-style:italic;'>No payments recorded for this"
              + " contract.</p>");
    } else {
      List<Payment> sortedPayments = new ArrayList<>(payments);
      sortedPayments.sort((a, b) -> b.getDueDate().compareTo(a.getDueDate()));

      html.append("<table class='payment-table'>");
      html.append("<thead><tr>");
      html.append(
          "<th>Due Date</th><th style='text-align:right'>Amount</th><th"
              + " style='text-align:right'>Paid</th><th"
              + " style='text-align:right'>Balance</th><th>Status</th><th>Payment Date</th>");
      html.append("</tr></thead><tbody>");

      for (Payment payment : sortedPayments) {
        BigDecimal received = receivedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO);
        BigDecimal balance = payment.getAmount().subtract(received);
        html.append("<tr>");
        html.append("<td>").append(formatDate(payment.getDueDate(), dateFmt)).append("</td>");
        html.append("<td style='text-align:right;font-variant-numeric:tabular-nums;'>")
            .append(CurrencyUtils.formatCurrency(payment.getAmount(), payment.getCurrency()))
            .append("</td>");
        html.append("<td style='text-align:right;font-variant-numeric:tabular-nums;'>")
            .append(
                received.compareTo(BigDecimal.ZERO) > 0
                    ? CurrencyUtils.formatCurrency(received, payment.getCurrency())
                    : "—")
            .append("</td>");
        html.append("<td style='text-align:right;font-variant-numeric:tabular-nums;'>")
            .append(
                balance.compareTo(BigDecimal.ZERO) > 0 && payment.getStatus() != PAID
                    ? CurrencyUtils.formatCurrency(balance, payment.getCurrency())
                    : "—")
            .append("</td>");
        html.append("<td>");
        String payStatus = payment.getStatus().name();
        html.append("<span class='pay-status pay-")
            .append(payStatus.toLowerCase(Locale.ROOT))
            .append("'>");
        html.append(formatEnumValue(payStatus)).append("</span>");
        html.append("</td>");
        html.append("<td>")
            .append(payment.getPaymentDate().map(d -> formatDate(d, dateFmt)).orElse("—"))
            .append("</td>");
        html.append("</tr>");
      }
      html.append("</tbody></table>");
    }

    appendPageEnd(html);
  }

  // ── Helpers ─────────────────────────────────────────────────────

  private Optional<Tenant> findPrimaryTenant(
      List<ContractParty> parties, Map<UUID, Tenant> tenantMap) {
    return parties.stream()
        .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
        .findFirst()
        .flatMap(p -> p.getTenantId().map(tenantMap::get));
  }

  private PaymentAggregation aggregatePayments(
      List<Payment> payments, Map<UUID, BigDecimal> receivedByPayment) {
    PaymentAggregation agg = new PaymentAggregation();
    for (Payment p : payments) {
      BigDecimal received = receivedByPayment.getOrDefault(p.getId(), BigDecimal.ZERO);
      switch (p.getStatus()) {
        case PAID -> {
          agg.totalPaid = agg.totalPaid.add(p.getAmount());
          agg.countPaid++;
        }
        case PENDING -> {
          agg.totalPending = agg.totalPending.add(p.getAmount());
          agg.countPending++;
        }
        case OVERDUE -> {
          agg.totalOverdue = agg.totalOverdue.add(p.getAmount());
          agg.countOverdue++;
        }
        case CANCELLED -> {
          agg.totalCancelled = agg.totalCancelled.add(p.getAmount());
          agg.countCancelled++;
        }
        case PARTIALLY_PAID -> {
          agg.totalPaid = agg.totalPaid.add(received);
          agg.totalPending = agg.totalPending.add(p.getAmount().subtract(received));
          agg.countPartial++;
        }
      }
    }
    return agg;
  }

  private String formatPaymentMethod(@Nullable String value) {
    if (value == null) {
      return "";
    }
    return PAYMENT_METHOD_LABELS.getOrDefault(value, formatEnumValue(value));
  }

  private String getPartyRoleColor(ContractPartyRole role) {
    return switch (role) {
      case PRIMARY_TENANT -> "#2b6cb0";
      case GUARANTOR -> "#b45309";
      case COSIGNER -> "#6d28d9";
      case EXTRA_TENANT -> "#0f766e";
    };
  }

  private String getPartyRoleBgColor(ContractPartyRole role) {
    return switch (role) {
      case PRIMARY_TENANT -> "#eff6ff";
      case GUARANTOR -> "#fffbeb";
      case COSIGNER -> "#f5f3ff";
      case EXTRA_TENANT -> "#f0fdfa";
    };
  }

  // ── Inner types ─────────────────────────────────────────────────

  private static class PaymentAggregation {
    BigDecimal totalPaid = BigDecimal.ZERO;
    BigDecimal totalPending = BigDecimal.ZERO;
    BigDecimal totalOverdue = BigDecimal.ZERO;
    BigDecimal totalCancelled = BigDecimal.ZERO;
    long countPaid, countPending, countOverdue, countCancelled, countPartial;
  }
}
