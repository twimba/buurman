package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
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
import static com.buurman.service.export.BookletHelper.appendSummaryCard;
import static com.buurman.service.export.BookletHelper.escapeHtml;
import static com.buurman.service.export.BookletHelper.fmt;
import static com.buurman.service.export.BookletHelper.formatEnumValue;
import static com.buurman.service.export.BookletHelper.sanitizeRichText;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.EffectiveEndDateHelper;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContactBookletExporter {

  private final ContactRepository contactRepository;
  private final ContactAddressRepository contactAddressRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final PaymentRepository paymentRepository;
  private final PropertyRepository propertyRepository;
  private final ContractPartyService contractPartyService;
  private final PdfRenderer pdfRenderer;
  private final Clock clock;

  public byte[] generate(ContactIdentifier contactIdentifier, UUID teamId) {
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);

    List<ContactAddress> addresses =
        contactAddressRepository.findByContactId(contact.getId(), teamId);
    List<Contract> contracts =
        contractRepository.findByContactIdViaParties(contact.getId(), teamId);

    List<Payment> allPayments = new ArrayList<>();
    for (Contract c : contracts) {
      allPayments.addAll(paymentRepository.findByContractId(c.getId(), teamId));
    }

    Map<UUID, Property> propertyMap = new HashMap<>();
    for (Contract c : contracts) {
      if (!propertyMap.containsKey(c.getPropertyId())) {
        propertyRepository
            .findByIdAndTeamId(c.getPropertyId(), teamId)
            .ifPresent(p -> propertyMap.put(p.getId(), p));
      }
    }

    Map<UUID, ContractPartyRole> contractRoles = new HashMap<>();
    for (Contract c : contracts) {
      contractPartyService.getPartiesForContract(c.getId(), teamId).stream()
          .filter(p -> p.getContactId().map(id -> id.equals(contact.getId())).orElse(false))
          .findFirst()
          .ifPresent(p -> contractRoles.put(c.getId(), p.getRole()));
    }

    // Bulk-load extensions and group by contract ID
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    List<ContractExtension> allExtensions =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream().collect(Collectors.groupingBy(ContractExtension::getContractId));

    String html =
        buildHtml(
            contact,
            addresses,
            contracts,
            allPayments,
            propertyMap,
            contractRoles,
            extensionsByContract);
    return pdfRenderer.renderHtml(html);
  }

  // -- HTML building ---

  private String buildHtml(
      Contact contact,
      List<ContactAddress> addresses,
      List<Contract> contracts,
      List<Payment> allPayments,
      Map<UUID, Property> propertyMap,
      Map<UUID, ContractPartyRole> contractRoles,
      Map<UUID, List<ContractExtension>> extensionsByContract) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
    String generatedDate = LocalDate.now(clock).format(dateFmt);

    String fullName = escapeHtml(contact.getDisplayName());

    BigDecimal totalPaid =
        allPayments.stream()
            .filter(p -> p.getStatus() == PAID)
            .map(p -> p.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPending =
        allPayments.stream()
            .filter(p -> p.getStatus() == PENDING || p.getStatus() == OVERDUE)
            .map(p -> p.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long activeContracts =
        contracts.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().name().equals("ACTIVE"))
            .count();

    String currentPropertyName = resolveCurrentPropertyName(contracts, propertyMap);

    String css =
        BookletCss.base()
            + BookletCss.summaryGrid()
            + BookletCss.paymentTable()
            + BookletCss.paymentStatusBadges();

    StringBuilder html = new StringBuilder(8192);
    appendDocumentStart(html, css);
    appendRunningFooter(html, generatedDate);

    appendCoverPage(
        html,
        contact,
        fullName,
        generatedDate,
        activeContracts,
        contracts.size(),
        currentPropertyName);
    appendProfilePage(
        html,
        contact,
        fullName,
        currentPropertyName,
        totalPaid,
        totalPending,
        activeContracts,
        contracts.size());
    appendAddressesPage(html, addresses);
    appendRentalHistoryPage(
        html, contracts, propertyMap, contractRoles, extensionsByContract, shortFmt);
    appendPaymentHistoryPage(html, allPayments, shortFmt);

    appendDocumentEnd(html);
    return html.toString();
  }

  // -- Page: Cover ---

  private void appendCoverPage(
      StringBuilder html,
      Contact contact,
      String fullName,
      String generatedDate,
      long activeContracts,
      int totalContracts,
      String currentPropertyName) {
    appendCoverStart(html, "CONTACT BOOKLET", fullName, generatedDate);

    html.append("<table class='cover-summary'>");
    html.append("<tr>");
    appendCoverCell(html, "Email", contact.getEmail().map(BookletHelper::escapeHtml).orElse("—"));
    appendCoverCell(html, "Phone", contact.getPhone().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(html, "Active Contracts", String.valueOf(activeContracts));
    appendCoverCell(html, "Total Contracts", String.valueOf(totalContracts));
    html.append("</tr><tr>");
    appendCoverCell(html, "Current Property", currentPropertyName);
    appendCoverCell(html, "Reference", contact.getIdentifier().orElseThrow().value());
    html.append("</tr>");
    html.append("</table>");

    appendCoverEnd(html);
  }

  // -- Page: Contact Profile ---

  private void appendProfilePage(
      StringBuilder html,
      Contact contact,
      String fullName,
      String currentPropertyName,
      BigDecimal totalPaid,
      BigDecimal totalPending,
      long activeContracts,
      int totalContracts) {
    appendPageStart(html, "Contact Profile");

    appendSectionTitle(html, "Personal Information");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Full Name", fullName);
    appendField(html, "Email", contact.getEmail().orElse(null));
    html.append("</tr><tr>");
    appendField(html, "Phone", contact.getPhone().orElse(null));
    appendField(html, "Reference", "#" + contact.getIdentifier().orElseThrow().value());
    html.append("</tr>");
    if (contact.getTaxNumber().isPresent() || contact.getIdNumber().isPresent()) {
      html.append("<tr>");
      appendField(html, "Tax Number", contact.getTaxNumber().orElse(null));
      appendField(html, "Government ID", contact.getIdNumber().orElse(null));
      html.append("</tr>");
    }
    html.append("<tr>");
    appendField(html, "Current Property", currentPropertyName);
    appendField(html, "", null);
    html.append("</tr>");
    html.append("</table>");

    contact
        .getNotes()
        .filter(s -> !s.isBlank())
        .ifPresent(
            notes ->
                html.append("<div class='text-block'><strong>Additional Information</strong><br/>")
                    .append(sanitizeRichText(notes))
                    .append("</div>"));

    appendSectionTitle(html, "Financial Summary");
    html.append("<table class='summary-grid'><tr>");
    appendSummaryCard(html, "Total Paid", fmt(totalPaid), null, "#f0fdf4", "#166534", "#059669");
    appendSummaryCard(
        html,
        "Outstanding",
        fmt(totalPending),
        null,
        totalPending.compareTo(BigDecimal.ZERO) > 0 ? "#fef2f2" : "#f0fdf4",
        totalPending.compareTo(BigDecimal.ZERO) > 0 ? "#991b1b" : "#166534",
        totalPending.compareTo(BigDecimal.ZERO) > 0 ? "#dc2626" : "#059669");
    appendSummaryCard(
        html,
        "Active Contracts",
        String.valueOf(activeContracts),
        null,
        "#f0f9ff",
        "#0c4a6e",
        "#0284c7");
    appendSummaryCard(
        html,
        "Total Contracts",
        String.valueOf(totalContracts),
        null,
        "#f9fafb",
        "#374151",
        "#292524");
    html.append("</tr></table>");

    appendPageEnd(html);
  }

  // -- Page: Addresses ---

  private void appendAddressesPage(StringBuilder html, List<ContactAddress> addresses) {
    if (addresses.isEmpty()) {
      return;
    }

    appendPageStart(html, "Addresses");
    html.append("<p style='font-size:13px;color:#78716c;margin-bottom:16px;'>")
        .append(addresses.size())
        .append(" address(es) on file</p>");

    for (ContactAddress addr : addresses) {
      String type =
          addr.getAddressType() != null ? formatEnumValue(addr.getAddressType().name()) : "Other";
      boolean isActive = addr.getStatus() != null && addr.getStatus().name().equals("ACTIVE");
      String borderColor = isActive ? "#0284c7" : "#a8a29e";

      html.append(
              "<div style='background-color:#fff;border:1px solid #e7e5e4;border-left:3px solid ")
          .append(borderColor)
          .append(";border-radius:6px;padding:16px 20px;margin-bottom:12px;'>");

      html.append(
              "<span"
                  + " style='display:inline-block;font-size:10px;font-weight:600;letter-spacing:0.8px;text-transform:uppercase;padding:2px"
                  + " 10px;"
                  + "border-radius:3px;margin-bottom:6px;color:#0284c7;background-color:#f0f9ff;'>")
          .append(escapeHtml(type))
          .append("</span>");
      if (isActive) {
        html.append(
            "<span style='display:inline-block;font-size:10px;font-weight:600;"
                + "letter-spacing:0.8px;text-transform:uppercase;padding:2px 10px;"
                + "border-radius:3px;margin-left:8px;color:#166534;background-color:#f0fdf4;'>"
                + "Active</span>");
      }

      html.append("<div style='font-size:15px;font-weight:600;color:#292524;margin-top:8px;'>")
          .append(escapeHtml(addr.getStreet()))
          .append("</div>");
      html.append("<div style='font-size:13px;color:#57534e;margin-top:2px;'>")
          .append(escapeHtml(addr.getCity()));
      if (addr.getPostalCode() != null) {
        html.append(", ").append(escapeHtml(addr.getPostalCode()));
      }
      html.append("</div>");
      if (addr.getCountryCode() != null) {
        html.append("<div style='font-size:13px;color:#57534e;'>")
            .append(escapeHtml(addr.getCountryCode()))
            .append("</div>");
      }
      html.append("</div>");
    }

    appendPageEnd(html);
  }

  // -- Page: Rental History ---

  private void appendRentalHistoryPage(
      StringBuilder html,
      List<Contract> contracts,
      Map<UUID, Property> propertyMap,
      Map<UUID, ContractPartyRole> contractRoles,
      Map<UUID, List<ContractExtension>> extensionsByContract,
      DateTimeFormatter shortFmt) {
    if (contracts.isEmpty()) {
      return;
    }

    appendPageStart(html, "Rental History");
    html.append("<p style='font-size:13px;color:#78716c;margin-bottom:16px;'>")
        .append(contracts.size())
        .append(" contract(s) on record</p>");

    for (Contract contract : contracts) {
      Property property = propertyMap.get(contract.getPropertyId());
      String propertyName =
          property != null
              ? escapeHtml(property.getStreet()) + ", " + escapeHtml(property.getCity())
              : "Unknown";
      ContractPartyRole role = contractRoles.get(contract.getId());
      String roleLabel = role != null ? formatEnumValue(role.name()) : "—";
      String statusName = contract.getStatus() != null ? contract.getStatus().name() : "DRAFT";
      boolean isActive = statusName.equals("ACTIVE");
      String borderColor = isActive ? "#0284c7" : "#a8a29e";

      html.append(
              "<div style='background-color:#fff;border:1px solid #e7e5e4;border-left:3px solid ")
          .append(borderColor)
          .append(";border-radius:6px;padding:20px 24px;margin-bottom:14px;'>");

      html.append(
              "<span style='display:inline-block;font-size:10px;font-weight:600;"
                  + "letter-spacing:0.8px;text-transform:uppercase;padding:2px 10px;"
                  + "border-radius:3px;color:#0284c7;background-color:#f0f9ff;'>")
          .append(roleLabel)
          .append("</span>");

      String statusBg = isActive ? "#f0fdf4" : "#f9fafb";
      String statusColor = isActive ? "#166534" : "#374151";
      html.append(
              "<span style='display:inline-block;font-size:10px;font-weight:600;"
                  + "letter-spacing:0.8px;text-transform:uppercase;padding:2px 10px;"
                  + "border-radius:3px;margin-left:8px;color:")
          .append(statusColor)
          .append(";background-color:")
          .append(statusBg)
          .append(";'>")
          .append(formatEnumValue(statusName))
          .append("</span>");

      html.append(
              "<div style='font-size:17px;font-weight:700;color:#292524;margin-top:10px;"
                  + "margin-bottom:10px;padding-bottom:8px;border-bottom:1px solid #f5f5f4;'>")
          .append(propertyName)
          .append("</div>");

      html.append("<table class='detail-grid'><tr>");
      appendField(html, "Contract ID", "#" + contract.getIdentifier().orElseThrow().value());
      appendField(
          html,
          "Type",
          contract.getContractType() != null
              ? formatEnumValue(contract.getContractType().name())
              : "—");
      html.append("</tr><tr>");
      appendField(
          html,
          "Start Date",
          contract.getStartDate() != null ? contract.getStartDate().format(shortFmt) : "—");
      List<ContractExtension> extensions =
          extensionsByContract.getOrDefault(contract.getId(), List.of());
      Optional<LocalDate> effectiveEndDate =
          EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);
      appendField(
          html, "End Date", effectiveEndDate.map(d -> d.format(shortFmt)).orElse("Ongoing"));
      html.append("</tr><tr>");
      appendField(
          html,
          "Rent Amount",
          CurrencyUtils.formatCurrency(
              contract.getRentAmount().value(), contract.getRentAmount().currency()));
      appendField(
          html,
          "Payment Frequency",
          contract.getPaymentFrequency() != null
              ? formatEnumValue(contract.getPaymentFrequency().name())
              : "—");
      html.append("</tr></table>");

      html.append("</div>");
    }

    appendPageEnd(html);
  }

  // -- Page: Payment History ---

  private void appendPaymentHistoryPage(
      StringBuilder html, List<Payment> allPayments, DateTimeFormatter shortFmt) {
    if (allPayments.isEmpty()) {
      return;
    }

    appendPageStart(html, "Payment History");

    // Year summary
    Map<Integer, BigDecimal[]> yearPayments = new TreeMap<>(Comparator.reverseOrder());
    for (Payment payment : allPayments) {
      LocalDate dateRef = payment.getPaymentDate().orElse(payment.getDueDate());
      if (dateRef == null) {
        continue;
      }
      int year = dateRef.getYear();
      yearPayments.computeIfAbsent(year, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
      BigDecimal[] amounts = yearPayments.get(year);
      if (payment.getStatus() == PAID) {
        amounts[0] = amounts[0].add(payment.getAmount().value());
      } else {
        amounts[1] = amounts[1].add(payment.getAmount().value());
      }
    }

    if (!yearPayments.isEmpty()) {
      html.append("<table class='summary-grid'><tr>");
      for (Map.Entry<Integer, BigDecimal[]> entry : yearPayments.entrySet()) {
        html.append(
            "<td style='background-color:#fafaf9;border-radius:8px;padding:16px;"
                + "text-align:center;vertical-align:top;'>");
        html.append(
                "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;"
                    + "font-weight:600;color:#57534e;margin-bottom:6px;'>")
            .append(entry.getKey())
            .append("</div>");
        html.append("<div style='font-size:18px;font-weight:700;color:#059669;'>")
            .append(String.format("%,.2f", entry.getValue()[0]))
            .append("</div>");
        if (entry.getValue()[1].compareTo(BigDecimal.ZERO) > 0) {
          html.append("<div style='font-size:11px;color:#dc2626;margin-top:4px;'>Outstanding: ")
              .append(String.format("%,.2f", entry.getValue()[1]))
              .append("</div>");
        }
        html.append("</td>");
      }
      html.append("</tr></table>");
    }

    // Recent payments table
    appendSectionTitle(html, "Recent Payments");
    List<Payment> sortedPayments =
        allPayments.stream()
            .sorted(
                (a, b) -> {
                  LocalDate da =
                      a.getDueDate() != null ? a.getDueDate() : a.getPaymentDate().orElse(null);
                  LocalDate db =
                      b.getDueDate() != null ? b.getDueDate() : b.getPaymentDate().orElse(null);
                  if (da == null && db == null) {
                    return 0;
                  }
                  if (da == null) {
                    return 1;
                  }
                  if (db == null) {
                    return -1;
                  }
                  return db.compareTo(da);
                })
            .limit(50)
            .toList();

    html.append("<table class='payment-table'><thead><tr>");
    html.append("<th>Due Date</th><th>Amount</th><th>Payment Date</th><th>Status</th>");
    html.append("</tr></thead><tbody>");

    for (Payment payment : sortedPayments) {
      html.append("<tr>");
      html.append("<td>")
          .append(payment.getDueDate() != null ? payment.getDueDate().format(shortFmt) : "—")
          .append("</td>");
      html.append("<td style='font-variant-numeric:tabular-nums;'>")
          .append(
              CurrencyUtils.formatCurrency(
                  payment.getAmount().value(), payment.getAmount().currency()))
          .append("</td>");
      html.append("<td>")
          .append(payment.getPaymentDate().map(d -> d.format(shortFmt)).orElse("—"))
          .append("</td>");

      String payStatus = payment.getStatus() != null ? payment.getStatus().name() : "";
      String payCssClass =
          switch (payStatus) {
            case "PAID" -> "pay-paid";
            case "PENDING" -> "pay-pending";
            case "OVERDUE" -> "pay-overdue";
            case "PARTIALLY_PAID" -> "pay-partially_paid";
            case "CANCELLED" -> "pay-cancelled";
            default -> "";
          };
      html.append("<td><span class='pay-status ")
          .append(payCssClass)
          .append("'>")
          .append(formatEnumValue(payStatus))
          .append("</span></td>");
      html.append("</tr>");
    }

    html.append("</tbody></table>");
    appendPageEnd(html);
  }

  // -- Helpers ---

  private String resolveCurrentPropertyName(
      List<Contract> contracts, Map<UUID, Property> propertyMap) {
    return contracts.stream()
        .filter(c -> c.getStatus() != null && c.getStatus().name().equals("ACTIVE"))
        .findFirst()
        .map(c -> propertyMap.get(c.getPropertyId()))
        .map(p -> escapeHtml(p.getStreet()) + ", " + escapeHtml(p.getCity()))
        .orElse("—");
  }
}
