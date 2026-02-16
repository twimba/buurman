package com.buurman.service;

import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.buurman.domain.Amenity;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.Expense;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyAmenity;
import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.domain.Tenant;
import com.buurman.domain.TenantAddress;
import com.buurman.exception.ExternalServiceException;
import com.buurman.repository.AmenityRepository;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyAmenityRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TenantAddressRepository;
import com.buurman.repository.TenantRepository;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportService {

  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final TenantRepository tenantRepository;
  private final ContractPartyService contractPartyService;
  private final PropertyAmenityRepository propertyAmenityRepository;
  private final PropertyOutdoorAreaRepository propertyOutdoorAreaRepository;
  private final AmenityRepository amenityRepository;
  private final TenantAddressRepository tenantAddressRepository;
  private final PaymentReceivalRepository paymentReceivalRepository;
  private final ContractPaymentInstructionRepository contractPaymentInstructionRepository;
  private final PaymentInstructionRepository paymentInstructionRepository;
  private final MetricsService metricsService;
  private final Clock clock;

  public byte[] generateTransactionHistoryCSV(LocalDate startDate, LocalDate endDate, UUID teamId) {
    Instant start = clock.instant();
    try {
      List<TransactionDTO> transactions = getTransactionHistory(startDate, endDate, teamId);

      try (StringWriter sw = new StringWriter();
          CSVWriter writer = new CSVWriter(sw)) {

        // Header
        String[] header = {
          "Date", "Type", "Description", "Property", "Category", "Amount", "Currency"
        };
        writer.writeNext(header);

        // Data rows
        for (TransactionDTO transaction : transactions) {
          String[] row = {
            transaction.date.toString(),
            transaction.type,
            transaction.description,
            transaction.property,
            transaction.category != null ? transaction.category : "",
            transaction.amount.toString(),
            transaction.currency
          };
          writer.writeNext(row);
        }

        byte[] result = sw.toString().getBytes();
        metricsService.recordTimer(
            "export.generation.seconds",
            Duration.between(start, clock.instant()),
            "type",
            "transaction_csv",
            "result",
            "success");
        metricsService.incrementCounter(
            "export.generation.total", "type", "transaction_csv", "result", "success");
        metricsService.recordHistogram(
            "export.size.bytes", result.length, "type", "transaction_csv");
        return result;
      }
    } catch (Exception e) {
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "transaction_csv",
          "result",
          "failure");
      metricsService.incrementCounter(
          "export.generation.total", "type", "transaction_csv", "result", "failure");
      throw new ExternalServiceException("Failed to generate CSV", e);
    }
  }

  public byte[] generateTransactionHistoryPDF(LocalDate startDate, LocalDate endDate, UUID teamId) {
    Instant start = clock.instant();
    try {
      List<TransactionDTO> transactions = getTransactionHistory(startDate, endDate, teamId);

      BigDecimal totalIncome =
          transactions.stream()
              .filter(t -> "INCOME".equals(t.type))
              .map(t -> t.amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalExpenses =
          transactions.stream()
              .filter(t -> "EXPENSE".equals(t.type))
              .map(t -> t.amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal netTotal = totalIncome.subtract(totalExpenses);

      String html =
          buildTransactionHistoryHTML(
              transactions, startDate, endDate, totalIncome, totalExpenses, netTotal);

      byte[] result = convertHTMLToPDF(html);
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "transaction_pdf",
          "result",
          "success");
      metricsService.incrementCounter(
          "export.generation.total", "type", "transaction_pdf", "result", "success");
      metricsService.recordHistogram("export.size.bytes", result.length, "type", "transaction_pdf");
      return result;
    } catch (Exception e) {
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "transaction_pdf",
          "result",
          "failure");
      metricsService.incrementCounter(
          "export.generation.total", "type", "transaction_pdf", "result", "failure");
      throw e;
    }
  }

  public byte[] generatePropertyBrochurePDF(String propertyIdentifier, UUID teamId) {
    Instant start = clock.instant();
    try {
      Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

      UUID propertyId = property.getId();

      // Get contracts for this property
      List<Contract> contracts = contractRepository.findByPropertyId(propertyId, teamId);

      // Get payments for this property
      List<Payment> payments =
          paymentRepository.findAllByTeamId(teamId).stream()
              .filter(
                  p ->
                      p.getContractId() != null
                          && contracts.stream().anyMatch(c -> c.getId().equals(p.getContractId())))
              .toList();

      // Get expenses for this property
      List<Expense> expenses = expenseRepository.findByPropertyId(propertyId, teamId);

      // Get outdoor areas, amenities
      List<PropertyOutdoorArea> outdoorAreas =
          propertyOutdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId);
      List<PropertyAmenity> propertyAmenities =
          propertyAmenityRepository.findByPropertyIdAndTeamId(property.getId(), teamId);
      List<Amenity> allAmenities = amenityRepository.findAll();

      // Calculate financial summary by year
      Map<Integer, FinancialYearSummary> yearSummaries = calculateYearSummaries(payments, expenses);

      String html =
          buildPropertyBrochureHTML(
              property,
              contracts,
              payments,
              expenses,
              yearSummaries,
              teamId,
              outdoorAreas,
              propertyAmenities,
              allAmenities);

      byte[] result = convertHTMLToPDF(html);
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "property_brochure",
          "result",
          "success");
      metricsService.incrementCounter(
          "export.generation.total", "type", "property_brochure", "result", "success");
      metricsService.recordHistogram(
          "export.size.bytes", result.length, "type", "property_brochure");
      return result;
    } catch (Exception e) {
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "property_brochure",
          "result",
          "failure");
      metricsService.incrementCounter(
          "export.generation.total", "type", "property_brochure", "result", "failure");
      throw e;
    }
  }

  public byte[] generateContractReportPDF(String contractIdentifier, UUID teamId) {
    Instant start = clock.instant();
    try {
      Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

      Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

      List<ContractParty> parties =
          contractPartyService.getPartiesForContract(contract.getId(), teamId);
      Set<UUID> tenantIds = new HashSet<>();
      for (ContractParty party : parties) {
        tenantIds.add(party.getTenantId());
      }
      Map<UUID, Tenant> tenantMap =
          tenantRepository.findByIdsAndTeamId(tenantIds, teamId).stream()
              .collect(toMap(Tenant::getId, t -> t));

      List<Payment> payments = paymentRepository.findByContractId(contract.getId(), teamId);

      // Batch-load receivals to compute received amounts and balances
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

      // Load all payment instructions for this contract, batch-resolve templates
      List<ContractPaymentInstruction> allCpis =
          contractPaymentInstructionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
      Set<UUID> piIds = new HashSet<>();
      for (ContractPaymentInstruction cpi : allCpis) {
        if (!Boolean.TRUE.equals(cpi.getIsCustom()) && cpi.getPaymentInstructionId() != null) {
          piIds.add(cpi.getPaymentInstructionId());
        }
      }
      Map<UUID, PaymentInstruction> piMap =
          piIds.isEmpty()
              ? Map.of()
              : paymentInstructionRepository.findAllByTeamId(teamId).stream()
                  .filter(pi -> piIds.contains(pi.getId()))
                  .collect(toMap(PaymentInstruction::getId, pi -> pi));

      String html =
          buildContractReportHTML(
              contract, property, parties, tenantMap, payments, receivedByPayment, allCpis, piMap);

      byte[] result = convertHTMLToPDF(html);
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "contract_report",
          "result",
          "success");
      metricsService.incrementCounter(
          "export.generation.total", "type", "contract_report", "result", "success");
      metricsService.recordHistogram("export.size.bytes", result.length, "type", "contract_report");
      return result;
    } catch (Exception e) {
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "contract_report",
          "result",
          "failure");
      metricsService.incrementCounter(
          "export.generation.total", "type", "contract_report", "result", "failure");
      throw e;
    }
  }

  public byte[] generateTenantReportPDF(String tenantIdentifier, UUID teamId) {
    Instant start = clock.instant();
    try {
      Tenant tenant = tenantRepository.getByIdentifierAndTeamId(tenantIdentifier, teamId);

      List<TenantAddress> addresses =
          tenantAddressRepository.findByTenantId(tenant.getId(), teamId);
      List<Contract> contracts =
          contractRepository.findByTenantIdViaParties(tenant.getId(), teamId);

      // Collect all payments across all contracts
      List<Payment> allPayments = new ArrayList<>();
      for (Contract contract : contracts) {
        allPayments.addAll(paymentRepository.findByContractId(contract.getId(), teamId));
      }

      // Resolve properties for contracts
      Map<UUID, Property> propertyMap = new HashMap<>();
      for (Contract contract : contracts) {
        if (!propertyMap.containsKey(contract.getPropertyId())) {
          propertyRepository
              .findByIdAndTeamId(contract.getPropertyId(), teamId)
              .ifPresent(p -> propertyMap.put(p.getId(), p));
        }
      }

      String html = buildTenantReportHTML(tenant, addresses, contracts, allPayments, propertyMap);

      byte[] result = convertHTMLToPDF(html);
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "tenant_report",
          "result",
          "success");
      metricsService.incrementCounter(
          "export.generation.total", "type", "tenant_report", "result", "success");
      metricsService.recordHistogram("export.size.bytes", result.length, "type", "tenant_report");
      return result;
    } catch (Exception e) {
      metricsService.recordTimer(
          "export.generation.seconds",
          Duration.between(start, clock.instant()),
          "type",
          "tenant_report",
          "result",
          "failure");
      metricsService.incrementCounter(
          "export.generation.total", "type", "tenant_report", "result", "failure");
      throw e;
    }
  }

  private List<TransactionDTO> getTransactionHistory(
      LocalDate startDate, LocalDate endDate, UUID teamId) {
    List<TransactionDTO> transactions = new ArrayList<>();

    // Add payments as income
    List<Payment> payments =
        (startDate != null && endDate != null)
            ? paymentRepository.findByDateRange(startDate, endDate, teamId)
            : paymentRepository.findAllByTeamId(teamId);

    for (Payment payment : payments) {
      if (payment.getStatus() == PAID && payment.getPaymentDate() != null) {
        Contract contract =
            contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
        Property property =
            contract != null
                ? propertyRepository
                    .findByIdAndTeamId(contract.getPropertyId(), teamId)
                    .orElse(null)
                : null;

        String propertyName =
            property != null
                ? property.getStreet() + ", " + property.getCity()
                : "Unknown Property";

        transactions.add(
            new TransactionDTO(
                payment.getId().toString(),
                payment.getPaymentDate(),
                "INCOME",
                "Rent payment - " + propertyName,
                propertyName,
                null,
                payment.getAmount(),
                payment.getCurrency()));
      }
    }

    // Add expenses
    List<Expense> expenses =
        (startDate != null && endDate != null)
            ? expenseRepository.findByDateRange(startDate, endDate, teamId)
            : expenseRepository.findAllByTeamId(teamId);

    for (Expense expense : expenses) {
      Property property =
          propertyRepository.findByIdAndTeamId(expense.getPropertyId(), teamId).orElse(null);

      String propertyName =
          property != null ? property.getStreet() + ", " + property.getCity() : "Unknown Property";

      transactions.add(
          new TransactionDTO(
              expense.getId().toString(),
              expense.getExpenseDate(),
              "EXPENSE",
              expense.getDescription(),
              propertyName,
              expense.getCategory().name(),
              expense.getAmount(),
              expense.getCurrency()));
    }

    // Sort by date descending
    transactions.sort((a, b) -> b.date.compareTo(a.date));

    return transactions;
  }

  private Map<Integer, FinancialYearSummary> calculateYearSummaries(
      List<Payment> payments, List<Expense> expenses) {
    Map<Integer, FinancialYearSummary> summaries = new TreeMap<>(Comparator.reverseOrder());

    // Process payments
    for (Payment payment : payments) {
      if (payment.getStatus() == PAID && payment.getPaymentDate() != null) {
        int year = payment.getPaymentDate().getYear();
        summaries.computeIfAbsent(year, FinancialYearSummary::new);
        summaries.get(year).addIncome(payment.getAmount());
      }
    }

    // Process expenses
    for (Expense expense : expenses) {
      int year = expense.getExpenseDate().getYear();
      summaries.computeIfAbsent(year, FinancialYearSummary::new);
      summaries.get(year).addExpense(expense.getAmount());
    }

    return summaries;
  }

  private String buildTransactionHistoryHTML(
      List<TransactionDTO> transactions,
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal totalIncome,
      BigDecimal totalExpenses,
      BigDecimal netTotal) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
    String periodText =
        startDate != null && endDate != null
            ? startDate.format(formatter) + " to " + endDate.format(formatter)
            : "All Time";

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
    html.append("<style>");
    html.append("body { font-family: Arial, sans-serif; margin: 40px; color: #333; }");
    html.append("h1 { color: #1e40af; border-bottom: 3px solid #3b82f6; padding-bottom: 10px; }");
    html.append("h2 { color: #1e40af; margin-top: 30px; }");
    html.append(".summary { display: flex; gap: 20px; margin: 20px 0; }");
    html.append(".summary-card { flex: 1; padding: 15px; border-radius: 8px; }");
    html.append(".income { background-color: #d1fae5; border: 2px solid #10b981; }");
    html.append(".expense { background-color: #fee2e2; border: 2px solid #ef4444; }");
    html.append(".net { background-color: #dbeafe; border: 2px solid #3b82f6; }");
    html.append(".summary-card h3 { margin: 0 0 5px 0; font-size: 14px; color: #666; }");
    html.append(".summary-card p { margin: 0; font-size: 24px; font-weight: bold; }");
    html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
    html.append(
        "th { background-color: #f3f4f6; padding: 12px; text-align: left; font-weight: 600;"
            + " border-bottom: 2px solid #d1d5db; }");
    html.append("td { padding: 10px; border-bottom: 1px solid #e5e7eb; }");
    html.append("tr:hover { background-color: #f9fafb; }");
    html.append(
        ".income-badge { background-color: #d1fae5; color: #065f46; padding: 4px 8px;"
            + " border-radius: 4px; font-size: 12px; font-weight: 600; }");
    html.append(
        ".expense-badge { background-color: #fee2e2; color: #991b1b; padding: 4px 8px;"
            + " border-radius: 4px; font-size: 12px; font-weight: 600; }");
    html.append(".amount-income { color: #059669; font-weight: 600; }");
    html.append(".amount-expense { color: #dc2626; font-weight: 600; }");
    html.append(
        ".footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #d1d5db; text-align:"
            + " center; color: #6b7280; font-size: 12px; }");
    html.append("</style></head><body>");

    html.append("<h1>Transaction History Report</h1>");
    html.append("<p style='color: #6b7280; font-size: 14px;'>Period: ")
        .append(periodText)
        .append("</p>");
    html.append("<p style='color: #6b7280; font-size: 12px;'>Generated on: ")
        .append(LocalDate.now(clock).format(formatter))
        .append("</p>");

    html.append("<div class='summary'>");
    html.append("<div class='summary-card income'><h3>Total Income</h3><p>EUR ")
        .append(String.format("%.2f", totalIncome))
        .append("</p></div>");
    html.append("<div class='summary-card expense'><h3>Total Expenses</h3><p>EUR ")
        .append(String.format("%.2f", totalExpenses))
        .append("</p></div>");
    html.append("<div class='summary-card net'><h3>Net Total</h3><p>EUR ")
        .append(String.format("%.2f", netTotal))
        .append("</p></div>");
    html.append("</div>");

    html.append("<h2>Transactions (").append(transactions.size()).append(")</h2>");
    html.append("<table>");
    html.append("<thead><tr>");
    html.append(
        "<th>Date</th><th>Type</th><th>Description</th><th>Property</th><th>Category</th><th"
            + " style='text-align: right;'>Amount</th>");
    html.append("</tr></thead><tbody>");

    for (TransactionDTO transaction : transactions) {
      html.append("<tr>");
      html.append("<td>").append(transaction.date.format(formatter)).append("</td>");
      html.append("<td><span class='")
          .append(transaction.type.equals("INCOME") ? "income-badge" : "expense-badge")
          .append("'>")
          .append(transaction.type)
          .append("</span></td>");
      html.append("<td>").append(escapeHtml(transaction.description)).append("</td>");
      html.append("<td>").append(escapeHtml(transaction.property)).append("</td>");
      html.append("<td>")
          .append(transaction.category != null ? escapeHtml(transaction.category) : "-")
          .append("</td>");
      html.append("<td style='text-align: right;' class='")
          .append(transaction.type.equals("INCOME") ? "amount-income" : "amount-expense")
          .append("'>")
          .append(transaction.type.equals("INCOME") ? "+" : "-")
          .append(transaction.currency)
          .append(" ")
          .append(String.format("%.2f", transaction.amount))
          .append("</td>");
      html.append("</tr>");
    }

    html.append("</tbody></table>");
    html.append("<div class='footer'>Generated by Buurman Property Management</div>");
    html.append("</body></html>");

    return html.toString();
  }

  private String buildPropertyBrochureHTML(
      Property property,
      List<Contract> contracts,
      List<Payment> payments,
      List<Expense> expenses,
      Map<Integer, FinancialYearSummary> yearSummaries,
      UUID teamId,
      List<PropertyOutdoorArea> outdoorAreas,
      List<PropertyAmenity> propertyAmenities,
      List<Amenity> allAmenities) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    Map<UUID, Amenity> amenityMap = allAmenities.stream().collect(toMap(Amenity::getId, a -> a));

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
    html.append("<style>");

    // Global styles - magazine feel
    html.append(
        "body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0; padding: 0;"
            + " color: #1a202c; }");

    // Cover page
    html.append(
        ".cover { background: linear-gradient(135deg, #1a365d 0%, #2b6cb0 50%, #2c7a7b 100%); ");
    html.append(
        "color: white; padding: 120px 50px 80px 50px; text-align: center; page-break-after: always;"
            + " min-height: 700px; }");
    html.append(
        ".cover-subtitle { font-size: 14px; letter-spacing: 6px; text-transform: uppercase; color:"
            + " #bee3f8; margin-bottom: 60px; }");
    html.append(
        ".cover h1 { font-size: 44px; margin: 0 0 12px 0; font-weight: 700; letter-spacing: 1px;"
            + " }");
    html.append(
        ".cover-location { font-size: 20px; color: #e2e8f0; margin: 8px 0 0 0; font-weight: 300;"
            + " }");
    html.append(
        ".cover-divider { width: 80px; height: 2px; background-color: #63b3ed; margin: 40px auto;"
            + " }");
    html.append(
        ".cover-date { font-size: 13px; color: #a0aec0; margin-top: 40px; letter-spacing: 2px; }");
    html.append(
        ".cover-id { font-size: 11px; color: #718096; margin-top: 8px; letter-spacing: 1px; }");

    // Section pages
    html.append(".page { padding: 45px 50px; page-break-before: always; }");
    html.append(".page-first { padding: 45px 50px; }");
    html.append(
        ".section-title { font-size: 26px; font-weight: 700; color: #1a365d; margin: 0 0 25px 0; ");
    html.append("padding-left: 16px; border-left: 4px solid #2b6cb0; }");
    html.append(
        ".section-subtitle { font-size: 18px; font-weight: 600; color: #2d3748; margin: 28px 0 14px"
            + " 0; }");

    // Property detail grid (using tables for iText)
    html.append(".detail-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }");
    html.append(".detail-table td { padding: 10px 14px; vertical-align: top; }");
    html.append(
        ".detail-label { font-size: 10px; font-weight: 600; text-transform: uppercase;"
            + " letter-spacing: 1.2px; color: #718096; margin: 0 0 3px 0; }");
    html.append(".detail-value { font-size: 16px; color: #1a202c; font-weight: 500; margin: 0; }");
    html.append(
        ".detail-cell { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 4px;"
            + " padding: 12px 14px; }");

    // Energy badge
    html.append(
        ".energy-badge { display: inline-block; padding: 8px 20px; border-radius: 6px; color:"
            + " white; ");
    html.append("font-size: 22px; font-weight: 700; letter-spacing: 1px; }");

    // Cards
    html.append(
        ".card { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding:"
            + " 16px 18px; margin-bottom: 10px; }");

    // Amenity tags
    html.append(
        ".tag { display: inline-block; background-color: #ebf4ff; border: 1px solid #bee3f8; ");
    html.append(
        "color: #2b6cb0; padding: 5px 12px; border-radius: 14px; font-size: 12px; font-weight: 500;"
            + " margin: 3px 4px; }");

    // Category header
    html.append(
        ".category-header { font-size: 13px; font-weight: 600; text-transform: uppercase;"
            + " letter-spacing: 1.5px; ");
    html.append(
        "color: #4a5568; margin: 20px 0 8px 0; padding-bottom: 6px; border-bottom: 1px solid"
            + " #e2e8f0; }");

    // Checkmark list
    html.append(".check-item { padding: 6px 0; font-size: 14px; color: #2d3748; }");
    html.append(".check-icon { color: #38a169; font-weight: bold; margin-right: 8px; }");
    html.append(".cross-icon { color: #a0aec0; font-weight: bold; margin-right: 8px; }");

    // Outdoor area card
    html.append(
        ".outdoor-card { border: 1px solid #e2e8f0; border-radius: 6px; padding: 14px 18px;"
            + " margin-bottom: 8px; ");
    html.append("background-color: #f0fff4; }");
    html.append(".outdoor-type { font-size: 16px; font-weight: 600; color: #276749; }");
    html.append(".outdoor-area { font-size: 14px; color: #4a5568; margin-top: 2px; }");

    // Financial section
    html.append(
        ".financial-year { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius:"
            + " 6px; padding: 20px; margin: 15px 0; }");
    html.append(".financial-year h3 { margin: 0 0 15px 0; color: #1a365d; font-size: 18px; }");
    html.append(".metrics-table { width: 100%; border-collapse: collapse; }");
    html.append(".metrics-table td { text-align: center; padding: 10px; }");
    html.append(
        ".metric-label { font-size: 10px; color: #718096; text-transform: uppercase;"
            + " letter-spacing: 1px; }");
    html.append(".metric-value { font-size: 22px; font-weight: 700; margin-top: 4px; }");
    html.append(".metric-income { color: #059669; }");
    html.append(".metric-expense { color: #dc2626; }");
    html.append(".metric-profit { color: #2b6cb0; }");

    // Contracts table
    html.append("table.data-table { width: 100%; border-collapse: collapse; margin-top: 15px; }");
    html.append(
        "table.data-table th { background-color: #1a365d; color: white; padding: 10px 12px;"
            + " text-align: left; font-size: 11px; ");
    html.append("text-transform: uppercase; letter-spacing: 0.5px; }");
    html.append(
        "table.data-table td { padding: 9px 12px; border-bottom: 1px solid #e2e8f0; font-size:"
            + " 13px; }");
    html.append("table.data-table tr:nth-child(even) td { background-color: #f7fafc; }");

    // Notes box
    html.append(
        ".notes-box { background-color: #fffff0; border: 1px solid #fefcbf; border-radius: 4px;"
            + " padding: 12px 16px; ");
    html.append("font-size: 13px; color: #744210; margin-top: 8px; }");

    // Footer
    html.append(
        ".footer { text-align: center; color: #a0aec0; font-size: 11px; padding: 30px 0 20px 0;"
            + " letter-spacing: 1px; }");

    html.append("</style></head><body>");

    // ========== COVER PAGE ==========
    html.append("<div class='cover'>");
    html.append("<div class='cover-subtitle'>Property Portfolio</div>");
    html.append("<h1>").append(escapeHtml(property.getStreet())).append("</h1>");
    html.append("<p class='cover-location'>").append(escapeHtml(property.getCity()));
    if (property.getPostalCode() != null) {
      html.append(" &middot; ").append(escapeHtml(property.getPostalCode()));
    }
    if (property.getCountry() != null) {
      html.append(" &middot; ").append(escapeHtml(property.getCountry()));
    }
    html.append("</p>");
    html.append("<div class='cover-divider'></div>");
    html.append("<p class='cover-date'>")
        .append(LocalDate.now(clock).format(dateFormatter).toUpperCase())
        .append("</p>");
    html.append("<p class='cover-id'>REF #").append(property.getIdentifier()).append("</p>");
    html.append("</div>");

    // ========== PAGE 2: PROPERTY OVERVIEW ==========
    html.append("<div class='page'>");
    html.append("<h2 class='section-title'>Property Overview</h2>");

    html.append("<table class='detail-table'><tbody>");

    // Row 1: Type + Status
    html.append("<tr>");
    appendDetailCell(
        html,
        "Property Type",
        formatEnumValue(
            property.getPropertyType() != null ? property.getPropertyType().name() : null));
    appendDetailCell(
        html,
        "Status",
        formatEnumValue(property.getStatus() != null ? property.getStatus().name() : null));
    html.append("</tr>");

    // Row 2: Bedrooms + Bathrooms
    if (property.getBedrooms() != null || property.getBathrooms() != null) {
      html.append("<tr>");
      appendDetailCell(
          html,
          "Bedrooms",
          property.getBedrooms() != null ? property.getBedrooms().toString() : null);
      appendDetailCell(
          html,
          "Bathrooms",
          property.getBathrooms() != null ? property.getBathrooms().toString() : null);
      html.append("</tr>");
    }

    // Row 3: Area + Floors
    if (property.getAreaValue() != null || property.getNumberOfFloors() != null) {
      html.append("<tr>");
      String areaText = null;
      if (property.getAreaValue() != null) {
        String unit = property.getAreaUnit() != null ? property.getAreaUnit() : "sqm";
        areaText = property.getAreaValue() + " " + unit;
      }
      appendDetailCell(html, "Total Area", areaText);
      appendDetailCell(
          html,
          "Number of Floors",
          property.getNumberOfFloors() != null ? property.getNumberOfFloors().toString() : null);
      html.append("</tr>");
    }

    // Row 4: Year Built + Last Renovated
    if (property.getYearBuilt() != null || property.getYearLastRenovated() != null) {
      html.append("<tr>");
      appendDetailCell(
          html,
          "Year Built",
          property.getYearBuilt() != null ? property.getYearBuilt().toString() : null);
      appendDetailCell(
          html,
          "Last Renovated",
          property.getYearLastRenovated() != null
              ? property.getYearLastRenovated().toString()
              : null);
      html.append("</tr>");
    }

    // Row 5: Construction + Foundation
    if (property.getConstructionType() != null || property.getFoundationType() != null) {
      html.append("<tr>");
      appendDetailCell(html, "Construction Type", formatEnumValue(property.getConstructionType()));
      appendDetailCell(html, "Foundation", formatEnumValue(property.getFoundationType()));
      html.append("</tr>");
    }

    // Row 6: Roof + Windows
    if (property.getRoofType() != null || property.getWindowType() != null) {
      html.append("<tr>");
      appendDetailCell(html, "Roof Type", formatEnumValue(property.getRoofType()));
      appendDetailCell(html, "Window Type", formatEnumValue(property.getWindowType()));
      html.append("</tr>");
    }

    // Row 7: Wall + Flooring
    if (property.getWallConstruction() != null || property.getFlooringType() != null) {
      html.append("<tr>");
      appendDetailCell(html, "Wall Construction", formatEnumValue(property.getWallConstruction()));
      appendDetailCell(html, "Flooring", formatEnumValue(property.getFlooringType()));
      html.append("</tr>");
    }

    html.append("</tbody></table>");

    // Structural notes
    if (property.getStructuralNotes() != null && !property.getStructuralNotes().isBlank()) {
      html.append("<div class='notes-box'><strong>Structural Notes:</strong> ")
          .append(property.getStructuralNotes())
          .append("</div>");
    }

    html.append("</div>");

    // ========== PAGE 3: BUILDING SPECIFICATIONS ==========
    boolean hasEnergyData =
        property.getEnergyEfficiencyRating() != null
            || property.getHeatingType() != null
            || property.getCoolingType() != null
            || property.getHotWaterSystem() != null
            || property.getEnergyCertificateExpiryDate() != null
            || property.getInsulationNotes() != null;
    boolean hasUtilitiesData =
        property.getElectricityConnectionType() != null
            || property.getWaterConnectionType() != null
            || property.getHasGasConnection() != null
            || property.getSewageType() != null
            || property.getInternetConnectionType() != null;
    boolean hasParkingData =
        property.getParkingType() != null || property.getParkingSpaces() != null;

    if (hasEnergyData || hasUtilitiesData || hasParkingData) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Building Specifications</h2>");

      // Energy & Climate
      if (hasEnergyData) {
        html.append("<h3 class='section-subtitle'>Energy &amp; Climate</h3>");

        // Energy rating badge
        if (property.getEnergyEfficiencyRating() != null) {
          String ratingColor = getEnergyRatingColor(property.getEnergyEfficiencyRating());
          html.append("<div style='margin-bottom: 16px;'>");
          html.append("<span class='energy-badge' style='background-color: ")
              .append(ratingColor)
              .append(";'>");
          html.append(escapeHtml(property.getEnergyEfficiencyRating()));
          html.append("</span>");
          html.append(
              "<span style='margin-left: 12px; font-size: 13px; color: #718096;'>Energy Efficiency"
                  + " Rating</span>");
          html.append("</div>");
        }

        html.append("<table class='detail-table'><tbody>");
        if (property.getHeatingType() != null || property.getCoolingType() != null) {
          html.append("<tr>");
          appendDetailCell(html, "Heating System", formatEnumValue(property.getHeatingType()));
          appendDetailCell(html, "Cooling System", formatEnumValue(property.getCoolingType()));
          html.append("</tr>");
        }
        if (property.getHotWaterSystem() != null
            || property.getEnergyCertificateExpiryDate() != null) {
          html.append("<tr>");
          appendDetailCell(html, "Hot Water System", formatEnumValue(property.getHotWaterSystem()));
          appendDetailCell(
              html,
              "Certificate Expiry",
              property.getEnergyCertificateExpiryDate() != null
                  ? property
                      .getEnergyCertificateExpiryDate()
                      .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                  : null);
          html.append("</tr>");
        }
        html.append("</tbody></table>");

        if (property.getInsulationNotes() != null && !property.getInsulationNotes().isBlank()) {
          html.append("<div class='notes-box'><strong>Insulation Notes:</strong> ")
              .append(property.getInsulationNotes())
              .append("</div>");
        }
      }

      // Utilities & Infrastructure
      if (hasUtilitiesData) {
        html.append("<h3 class='section-subtitle'>Utilities &amp; Infrastructure</h3>");
        html.append("<table class='detail-table'><tbody>");

        if (property.getElectricityConnectionType() != null
            || property.getElectricityCapacityAmps() != null) {
          html.append("<tr>");
          appendDetailCell(
              html, "Electricity", formatEnumValue(property.getElectricityConnectionType()));
          appendDetailCell(
              html,
              "Capacity",
              property.getElectricityCapacityAmps() != null
                  ? property.getElectricityCapacityAmps() + " Amps"
                  : null);
          html.append("</tr>");
        }
        if (property.getWaterConnectionType() != null
            || isTrue(property.getHasGasConnection())
            || Boolean.FALSE.equals(property.getHasGasConnection())) {
          html.append("<tr>");
          appendDetailCell(html, "Water", formatEnumValue(property.getWaterConnectionType()));
          String gasText =
              property.getHasGasConnection() != null
                  ? (isTrue(property.getHasGasConnection()) ? "Connected" : "Not Connected")
                  : null;
          appendDetailCell(html, "Gas Connection", gasText);
          html.append("</tr>");
        }
        if (property.getSewageType() != null || property.getInternetConnectionType() != null) {
          html.append("<tr>");
          appendDetailCell(html, "Sewage", formatEnumValue(property.getSewageType()));
          appendDetailCell(html, "Internet", formatEnumValue(property.getInternetConnectionType()));
          html.append("</tr>");
        }
        if (property.getInternetMaxSpeedMbps() != null || property.getInternetStatus() != null) {
          html.append("<tr>");
          appendDetailCell(
              html,
              "Max Speed",
              property.getInternetMaxSpeedMbps() != null
                  ? property.getInternetMaxSpeedMbps() + " Mbps"
                  : null);
          appendDetailCell(html, "Internet Status", formatEnumValue(property.getInternetStatus()));
          html.append("</tr>");
        }
        html.append("</tbody></table>");
      }

      // Parking
      if (hasParkingData) {
        html.append("<h3 class='section-subtitle'>Parking</h3>");
        html.append("<table class='detail-table'><tbody>");
        html.append("<tr>");
        appendDetailCell(html, "Parking Type", formatEnumValue(property.getParkingType()));
        appendDetailCell(
            html,
            "Parking Spaces",
            property.getParkingSpaces() != null ? property.getParkingSpaces().toString() : null);
        html.append("</tr>");
        html.append("</tbody></table>");
      }

      html.append("</div>");
    }

    // ========== PAGE 4: FEATURES & OUTDOOR SPACES ==========
    boolean hasAmenities = !propertyAmenities.isEmpty();
    boolean hasOutdoorAreas = !outdoorAreas.isEmpty();

    if (hasAmenities || hasOutdoorAreas) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Features &amp; Outdoor Spaces</h2>");

      // Amenities grouped by category
      if (hasAmenities) {
        html.append("<h3 class='section-subtitle'>Amenities</h3>");

        // Group amenities by category
        Map<String, List<Amenity>> grouped = new LinkedHashMap<>();
        for (PropertyAmenity pa : propertyAmenities) {
          Amenity amenity = amenityMap.get(pa.getAmenityId());
          if (amenity != null) {
            String category = amenity.getCategory() != null ? amenity.getCategory() : "Other";
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(amenity);
          }
        }

        for (Map.Entry<String, List<Amenity>> entry : grouped.entrySet()) {
          html.append("<div class='category-header'>")
              .append(escapeHtml(formatEnumValue(entry.getKey())))
              .append("</div>");
          html.append("<div style='margin-bottom: 12px;'>");
          for (Amenity amenity : entry.getValue()) {
            html.append("<span class='tag'>")
                .append(escapeHtml(amenity.getName()))
                .append("</span>");
          }
          html.append("</div>");
        }
      }

      // Outdoor Areas
      if (hasOutdoorAreas) {
        html.append("<h3 class='section-subtitle'>Outdoor Spaces</h3>");
        for (PropertyOutdoorArea area : outdoorAreas) {
          html.append("<div class='outdoor-card'>");
          html.append("<div class='outdoor-type'>")
              .append(escapeHtml(formatEnumValue(area.getType())))
              .append("</div>");
          if (area.getAreaValue() != null) {
            String unit = area.getAreaUnit() != null ? area.getAreaUnit() : "sqm";
            html.append("<div class='outdoor-area'>")
                .append(area.getAreaValue())
                .append(" ")
                .append(unit)
                .append("</div>");
          }
          html.append("</div>");
        }
      }

      html.append("</div>");
    }

    // ========== PAGE 5: SAFETY & ACCESSIBILITY ==========
    boolean hasSafetyData =
        isTrue(property.getHasSmokeDetectors())
            || isTrue(property.getHasCoDetectors())
            || isTrue(property.getHasFireExtinguisher())
            || isTrue(property.getHasSprinklerSystem())
            || isTrue(property.getHasAlarmSystem())
            || isTrue(property.getHasSecurityCameras())
            || isTrue(property.getHasSecureEntry())
            || (property.getSafetyNotes() != null && !property.getSafetyNotes().isBlank());
    boolean hasAccessibilityData =
        isTrue(property.getIsWheelchairAccessible())
            || isTrue(property.getHasElevator())
            || isTrue(property.getHasStepFreeEntrance())
            || isTrue(property.getHasAdaptedBathroom())
            || (property.getAccessibilityNotes() != null
                && !property.getAccessibilityNotes().isBlank());

    if (hasSafetyData || hasAccessibilityData) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Safety &amp; Accessibility</h2>");

      if (hasSafetyData) {
        html.append("<h3 class='section-subtitle'>Safety &amp; Security</h3>");
        html.append("<div class='card'>");
        appendCheckItem(html, "Smoke Detectors", property.getHasSmokeDetectors());
        appendCheckItem(html, "CO Detectors", property.getHasCoDetectors());
        appendCheckItem(html, "Fire Extinguisher", property.getHasFireExtinguisher());
        appendCheckItem(html, "Sprinkler System", property.getHasSprinklerSystem());
        appendCheckItem(html, "Alarm System", property.getHasAlarmSystem());
        appendCheckItem(html, "Security Cameras", property.getHasSecurityCameras());
        appendCheckItem(html, "Secure Entry", property.getHasSecureEntry());
        html.append("</div>");

        if (property.getSafetyNotes() != null && !property.getSafetyNotes().isBlank()) {
          html.append("<div class='notes-box'><strong>Safety Notes:</strong> ")
              .append(property.getSafetyNotes())
              .append("</div>");
        }
      }

      if (hasAccessibilityData) {
        html.append("<h3 class='section-subtitle'>Accessibility</h3>");
        html.append("<div class='card'>");
        appendCheckItem(html, "Wheelchair Accessible", property.getIsWheelchairAccessible());
        appendCheckItem(html, "Elevator", property.getHasElevator());
        appendCheckItem(html, "Step-Free Entrance", property.getHasStepFreeEntrance());
        appendCheckItem(html, "Adapted Bathroom", property.getHasAdaptedBathroom());
        html.append("</div>");

        if (property.getAccessibilityNotes() != null
            && !property.getAccessibilityNotes().isBlank()) {
          html.append("<div class='notes-box'><strong>Accessibility Notes:</strong> ")
              .append(property.getAccessibilityNotes())
              .append("</div>");
        }
      }

      html.append("</div>");
    }

    // ========== FINANCIAL OVERVIEW ==========
    if (!yearSummaries.isEmpty()) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Financial Overview</h2>");

      for (Map.Entry<Integer, FinancialYearSummary> entry : yearSummaries.entrySet()) {
        FinancialYearSummary summary = entry.getValue();
        html.append("<div class='financial-year'>");
        html.append("<h3>Year ").append(entry.getKey()).append("</h3>");
        html.append("<table class='metrics-table'><tr>");
        html.append(
                "<td><div class='metric-label'>Income</div><div class='metric-value"
                    + " metric-income'>EUR ")
            .append(String.format("%.2f", summary.income))
            .append("</div></td>");
        html.append(
                "<td><div class='metric-label'>Expenses</div><div class='metric-value"
                    + " metric-expense'>EUR ")
            .append(String.format("%.2f", summary.expenses))
            .append("</div></td>");
        html.append(
                "<td><div class='metric-label'>Net Profit</div><div class='metric-value"
                    + " metric-profit'>EUR ")
            .append(String.format("%.2f", summary.getNetProfit()))
            .append("</div></td>");
        html.append("</tr></table>");
        html.append("</div>");
      }

      html.append("</div>");
    }

    // ========== CONTRACTS ==========
    if (!contracts.isEmpty()) {
      List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
      Map<UUID, Tenant> primaryTenants =
          contractPartyService.getPrimaryTenantsForContracts(contractIds, teamId);

      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Contracts</h2>");
      html.append("<p style='font-size: 13px; color: #718096; margin-bottom: 12px;'>")
          .append(contracts.size())
          .append(" contract(s) on record</p>");
      html.append("<table class='data-table'><thead><tr>");
      html.append(
          "<th>Contract ID</th><th>Tenant</th><th>Start Date</th><th>End"
              + " Date</th><th>Rent</th><th>Status</th>");
      html.append("</tr></thead><tbody>");

      for (Contract contract : contracts) {
        Tenant tenant = primaryTenants.get(contract.getId());
        String tenantName =
            tenant != null
                ? tenant.getFirstName()
                    + " "
                    + (tenant.getLastName() != null ? tenant.getLastName() : "")
                : "Unknown";

        html.append("<tr>");
        html.append("<td>#").append(contract.getIdentifier()).append("</td>");
        html.append("<td>").append(escapeHtml(tenantName)).append("</td>");
        html.append("<td>").append(contract.getStartDate()).append("</td>");
        html.append("<td>")
            .append(contract.getEndDate() != null ? contract.getEndDate().toString() : "Ongoing")
            .append("</td>");
        html.append("<td>")
            .append(contract.getCurrency())
            .append(" ")
            .append(String.format("%.2f", contract.getRentAmount()))
            .append("</td>");
        html.append("<td>")
            .append(
                formatEnumValue(
                    contract.getStatus() != null ? contract.getStatus().toString() : null))
            .append("</td>");
        html.append("</tr>");
      }

      html.append("</tbody></table>");
      html.append("</div>");
    }

    html.append("<div class='footer'>Generated by Buurman Property Management</div>");
    html.append("</body></html>");

    return html.toString();
  }

  private String buildContractReportHTML(
      Contract contract,
      Property property,
      List<ContractParty> parties,
      Map<UUID, Tenant> tenantMap,
      List<Payment> payments,
      Map<UUID, BigDecimal> receivedByPayment,
      List<ContractPaymentInstruction> allCpis,
      Map<UUID, PaymentInstruction> piMap) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    String ccy = contract.getCurrency() != null ? contract.getCurrency() : "EUR";

    // Find primary tenant
    Tenant primaryTenant =
        parties.stream()
            .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
            .findFirst()
            .map(p -> tenantMap.get(p.getTenantId()))
            .orElse(null);
    String primaryName =
        primaryTenant != null
            ? escapeHtml(primaryTenant.getFirstName())
                + " "
                + (primaryTenant.getLastName() != null
                    ? escapeHtml(primaryTenant.getLastName())
                    : "")
            : "—";

    // Payment aggregations
    BigDecimal totalPaid = BigDecimal.ZERO;
    BigDecimal totalPending = BigDecimal.ZERO;
    BigDecimal totalOverdue = BigDecimal.ZERO;
    BigDecimal totalCancelled = BigDecimal.ZERO;
    long countPaid = 0, countPending = 0, countOverdue = 0, countCancelled = 0, countPartial = 0;
    for (Payment p : payments) {
      BigDecimal received = receivedByPayment.getOrDefault(p.getId(), BigDecimal.ZERO);
      switch (p.getStatus()) {
        case PAID -> {
          totalPaid = totalPaid.add(p.getAmount());
          countPaid++;
        }
        case PENDING -> {
          totalPending = totalPending.add(p.getAmount());
          countPending++;
        }
        case OVERDUE -> {
          totalOverdue = totalOverdue.add(p.getAmount());
          countOverdue++;
        }
        case CANCELLED -> {
          totalCancelled = totalCancelled.add(p.getAmount());
          countCancelled++;
        }
        case PARTIALLY_PAID -> {
          totalPaid = totalPaid.add(received);
          totalPending = totalPending.add(p.getAmount().subtract(received));
          countPartial++;
        }
      }
    }

    StringBuilder html = new StringBuilder(8192);
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
    html.append("<style>");
    appendContractReportCSS(html);
    html.append("</style></head><body>");

    // Running footer (placed into @page @bottom-center via CSS position:running)
    html.append("<div class='running-footer'>");
    html.append("<table style='width:100%;border-collapse:collapse;'><tr>");
    html.append("<td style='text-align:left;font-size:9px;color:#a0aec0;width:33%;'>")
        .append(generatedDate)
        .append("</td>");
    html.append(
        "<td style='text-align:center;font-size:9px;color:#a0aec0;width:34%;'>Confidential &mdash;"
            + " Buurman Property Management</td>");
    html.append("<td style='text-align:right;font-size:9px;color:#a0aec0;width:33%;'></td>");
    html.append("</tr></table></div>");

    // ═══════════════════════════════════════════════════════════════
    // PAGE 1 — COVER
    // ═══════════════════════════════════════════════════════════════
    html.append("<div class='cover'>");
    html.append("<div class='cover-header'>");
    html.append("<div class='cover-title'>CONTRACT REPORT</div>");
    html.append("<div class='cover-subtitle'>")
        .append(escapeHtml(contract.getIdentifier()))
        .append("</div>");
    html.append("<div class='cover-date'>Generated ").append(generatedDate).append("</div>");
    html.append("</div>");

    // Status badge
    String statusStr = contract.getStatus() != null ? contract.getStatus().name() : "DRAFT";
    html.append("<div style='text-align:center;margin:30px 0;'>");
    html.append("<span class='status-badge status-").append(statusStr.toLowerCase()).append("'>");
    html.append(formatEnumValue(statusStr));
    html.append("</span></div>");

    // Summary grid
    html.append("<table class='cover-summary'>");
    html.append("<tr>");
    appendCoverCell(
        html, "Property", escapeHtml(property.getStreet()) + ", " + escapeHtml(property.getCity()));
    appendCoverCell(html, "Primary Tenant", primaryName);
    html.append("</tr><tr>");
    appendCoverCell(html, "Monthly Rent", ccy + " " + fmt(contract.getRentAmount()));
    String period =
        formatDate(contract.getStartDate(), dateFmt)
            + " — "
            + (contract.getEndDate() != null
                ? formatDate(contract.getEndDate(), dateFmt)
                : "Indefinite");
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

    html.append(
        "<div class='cover-footer'>Confidential &mdash; Generated by Buurman Property"
            + " Management</div>");
    html.append("</div>");

    // ═══════════════════════════════════════════════════════════════
    // PAGE 2 — CONTRACT DETAILS + PROPERTY
    // ═══════════════════════════════════════════════════════════════
    html.append("<div class='page'>");
    html.append("<div class='page-header'>Contract Details</div>");

    html.append("<h2 class='section-title'>Contract Information</h2>");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Contract ID", contract.getIdentifier());
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
        contract.getSignedDate() != null
            ? formatDate(contract.getSignedDate(), dateFmt)
            : "Not signed");
    html.append("</tr><tr>");
    appendField(html, "Start Date", formatDate(contract.getStartDate(), dateFmt));
    appendField(
        html,
        "End Date",
        contract.getEndDate() != null ? formatDate(contract.getEndDate(), dateFmt) : "Indefinite");
    html.append("</tr><tr>");
    appendField(html, "Rent Amount", ccy + " " + fmt(contract.getRentAmount()));
    appendField(html, "Deposit Amount", ccy + " " + fmt(contract.getDepositAmount()));
    html.append("</tr><tr>");
    appendField(html, "Security Deposit", ccy + " " + fmt(contract.getSecurityDeposit()));
    appendField(html, "Currency", ccy);
    html.append("</tr><tr>");
    appendField(
        html,
        "Payment Frequency",
        formatEnumValue(
            contract.getPaymentFrequency() != null ? contract.getPaymentFrequency().name() : ""));
    appendField(
        html,
        "Payment Due Day",
        contract.getPaymentDueDay() != null
            ? "Day " + contract.getPaymentDueDay() + " of month"
            : "—");
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
    appendField(
        html,
        "Late Fee",
        contract.getLateFeePercentage() != null ? contract.getLateFeePercentage() + "%" : "—");
    html.append("</tr>");
    html.append("</table>");

    // Terms & Conditions (rich text HTML from editor)
    if (contract.getTermsAndConditions() != null && !contract.getTermsAndConditions().isBlank()) {
      html.append("<h2 class='section-title'>Terms &amp; Conditions</h2>");
      html.append("<div class='text-block'>")
          .append(sanitizeRichText(contract.getTermsAndConditions()))
          .append("</div>");
    }

    // Notes (rich text HTML from editor)
    if (contract.getNotes() != null && !contract.getNotes().isBlank()) {
      html.append("<h2 class='section-title'>Notes</h2>");
      html.append("<div class='text-block'>")
          .append(sanitizeRichText(contract.getNotes()))
          .append("</div>");
    }

    // Property section
    html.append("<h2 class='section-title'>Property</h2>");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Address", escapeHtml(property.getStreet()));
    appendField(
        html, "City", escapeHtml(property.getCity()) + " " + escapeHtml(property.getPostalCode()));
    html.append("</tr><tr>");
    appendField(
        html, "Country", property.getCountry() != null ? escapeHtml(property.getCountry()) : "—");
    appendField(
        html,
        "Property Type",
        formatEnumValue(
            property.getPropertyType() != null ? property.getPropertyType().name() : ""));
    html.append("</tr><tr>");
    appendField(
        html,
        "Bedrooms",
        property.getBedrooms() != null ? String.valueOf(property.getBedrooms()) : "—");
    appendField(
        html,
        "Bathrooms",
        property.getBathrooms() != null ? String.valueOf(property.getBathrooms()) : "—");
    html.append("</tr><tr>");
    String area =
        property.getAreaValue() != null
            ? property.getAreaValue()
                + " "
                + (property.getAreaUnit() != null ? property.getAreaUnit() : "m²")
            : "—";
    appendField(html, "Area", area);
    appendField(html, "Property ID", property.getIdentifier());
    html.append("</tr>");
    html.append("</table>");

    html.append("</div>");

    // ═══════════════════════════════════════════════════════════════
    // PAGE 3 — CONTRACT PARTIES
    // ═══════════════════════════════════════════════════════════════
    html.append("<div class='page'>");
    html.append("<div class='page-header'>Contract Parties</div>");

    html.append("<h2 class='section-title'>Parties (").append(parties.size()).append(")</h2>");

    // Sort: primary tenant first, then by role
    List<ContractParty> sortedParties = new ArrayList<>(parties);
    sortedParties.sort(
        (a, b) -> {
          if (a.getRole() == ContractPartyRole.PRIMARY_TENANT) return -1;
          if (b.getRole() == ContractPartyRole.PRIMARY_TENANT) return 1;
          return a.getRole().compareTo(b.getRole());
        });

    for (ContractParty party : sortedParties) {
      Tenant t = tenantMap.get(party.getTenantId());
      if (t == null) continue;
      String roleColor = getPartyRoleColor(party.getRole());
      String roleBg = getPartyRoleBgColor(party.getRole());

      html.append("<div class='party-card' style='border-left-color:")
          .append(roleColor)
          .append(";'>");

      // Role label
      html.append("<div class='party-role' style='color:")
          .append(roleColor)
          .append(";background-color:")
          .append(roleBg)
          .append(";'>");
      html.append(party.getRole().getDisplayName());
      html.append("</div>");

      // Name
      html.append("<div class='party-name'>");
      html.append(escapeHtml(t.getFirstName()));
      if (t.getLastName() != null) html.append(" ").append(escapeHtml(t.getLastName()));
      html.append("</div>");

      // Contact details in a 2-column layout
      html.append("<table class='party-details'>");
      if (t.getEmail() != null) {
        html.append("<tr><td class='pd-label'>Email</td><td class='pd-value'>")
            .append(escapeHtml(t.getEmail()))
            .append("</td>");
        if (t.getPhone() != null) {
          html.append("<td class='pd-label'>Phone</td><td class='pd-value'>")
              .append(escapeHtml(t.getPhone()))
              .append("</td>");
        } else {
          html.append("<td></td><td></td>");
        }
        html.append("</tr>");
      } else if (t.getPhone() != null) {
        html.append("<tr><td class='pd-label'>Phone</td><td class='pd-value'>")
            .append(escapeHtml(t.getPhone()))
            .append("</td><td></td><td></td></tr>");
      }
      boolean hasTax = t.getTaxNumber() != null && !t.getTaxNumber().isBlank();
      boolean hasId = t.getIdNumber() != null && !t.getIdNumber().isBlank();
      if (hasTax || hasId) {
        html.append("<tr>");
        if (hasTax) {
          html.append("<td class='pd-label'>Tax Number</td><td class='pd-value'>")
              .append(escapeHtml(t.getTaxNumber()))
              .append("</td>");
        } else {
          html.append("<td></td><td></td>");
        }
        if (hasId) {
          html.append("<td class='pd-label'>ID Number</td><td class='pd-value'>")
              .append(escapeHtml(t.getIdNumber()))
              .append("</td>");
        } else {
          html.append("<td></td><td></td>");
        }
        html.append("</tr>");
      }
      html.append(
              "<tr><td class='pd-label'>Reference</td><td class='pd-value'"
                  + " style='color:#a0aec0;font-size:11px;'>")
          .append(t.getIdentifier())
          .append("</td><td></td><td></td></tr>");
      html.append("</table></div>");
    }

    html.append("</div>");

    // ═══════════════════════════════════════════════════════════════
    // PAGE 4 — PAYMENT INSTRUCTIONS
    // ═══════════════════════════════════════════════════════════════
    if (!allCpis.isEmpty()) {
      html.append("<div class='page'>");
      html.append("<div class='page-header'>Payment Instructions</div>");

      // Sort by effectiveFrom descending (most recent first)
      List<ContractPaymentInstruction> sortedCpis = new ArrayList<>(allCpis);
      sortedCpis.sort(
          (a, b) -> {
            LocalDate aDate = a.getEffectiveFrom();
            LocalDate bDate = b.getEffectiveFrom();
            if (aDate == null && bDate == null) return 0;
            if (aDate == null) return 1;
            if (bDate == null) return -1;
            return bDate.compareTo(aDate);
          });

      for (ContractPaymentInstruction cpi : sortedCpis) {
        boolean isCustom = Boolean.TRUE.equals(cpi.getIsCustom());
        PaymentInstruction tpl =
            (!isCustom && cpi.getPaymentInstructionId() != null)
                ? piMap.get(cpi.getPaymentInstructionId())
                : null;

        String piName = isCustom ? cpi.getCustomName() : (tpl != null ? tpl.getName() : null);
        String piMethod =
            isCustom
                ? cpi.getCustomPaymentMethod()
                : (tpl != null && tpl.getPaymentMethod() != null
                    ? tpl.getPaymentMethod().name()
                    : null);
        String piBankName =
            isCustom ? cpi.getCustomBankName() : (tpl != null ? tpl.getBankName() : null);
        String piAccountHolder =
            isCustom
                ? cpi.getCustomAccountHolderName()
                : (tpl != null ? tpl.getAccountHolderName() : null);
        String piIban = isCustom ? cpi.getCustomIban() : (tpl != null ? tpl.getIban() : null);
        String piBicSwift =
            isCustom ? cpi.getCustomBicSwift() : (tpl != null ? tpl.getBicSwift() : null);
        String piAccountNumber =
            isCustom ? cpi.getCustomAccountNumber() : (tpl != null ? tpl.getAccountNumber() : null);
        String piRoutingNumber =
            isCustom ? cpi.getCustomRoutingNumber() : (tpl != null ? tpl.getRoutingNumber() : null);
        String piReference =
            isCustom
                ? cpi.getCustomPaymentReference()
                : (tpl != null ? tpl.getPaymentReference() : null);
        String piDetails =
            isCustom
                ? cpi.getCustomAdditionalDetails()
                : (tpl != null ? tpl.getAdditionalDetails() : null);

        // Current (no effectiveTo) gets a blue accent, expired gets gray
        boolean isCurrent = cpi.getEffectiveTo() == null;
        String accentColor = isCurrent ? "#2b6cb0" : "#a0aec0";

        html.append("<div class='pi-card' style='border-left-color:")
            .append(accentColor)
            .append(";'>");

        // Header: name, method badge, date range
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

        // Effective period
        String fromStr =
            cpi.getEffectiveFrom() != null ? formatDate(cpi.getEffectiveFrom(), dateFmt) : "—";
        String toStr =
            cpi.getEffectiveTo() != null ? formatDate(cpi.getEffectiveTo(), dateFmt) : "Present";
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
        if (cpi.getNotes() != null && !cpi.getNotes().isBlank()) {
          html.append("<div class='pi-details'><span class='fg-label'>Notes</span><br/>");
          html.append("<span style='font-size:13px;color:#2d3748;'>")
              .append(escapeHtml(cpi.getNotes()))
              .append("</span></div>");
        }

        html.append("</div>");
      }

      html.append("</div>");
    }

    // ═══════════════════════════════════════════════════════════════
    // PAGE 5 — PAYMENT OVERVIEW
    // ═══════════════════════════════════════════════════════════════
    html.append("<div class='page'>");
    html.append("<div class='page-header'>Payment Overview</div>");

    // Summary cards
    html.append("<h2 class='section-title'>Summary</h2>");
    html.append("<table class='summary-grid'><tr>");
    appendSummaryCard(
        html, "Paid", countPaid, ccy + " " + fmt(totalPaid), "#f0fdf4", "#16a34a", "#166534");
    appendSummaryCard(
        html,
        "Pending",
        countPending,
        ccy + " " + fmt(totalPending),
        "#fefce8",
        "#ca8a04",
        "#854d0e");
    appendSummaryCard(
        html,
        "Partial",
        countPartial,
        ccy
            + " "
            + fmt(
                payments.stream()
                    .filter(p -> p.getStatus() == PARTIALLY_PAID)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)),
        "#eff6ff",
        "#3b82f6",
        "#1e40af");
    appendSummaryCard(
        html,
        "Overdue",
        countOverdue,
        ccy + " " + fmt(totalOverdue),
        "#fef2f2",
        "#dc2626",
        "#991b1b");
    html.append("</tr></table>");

    // Payments table
    html.append("<h2 class='section-title'>Payment History (")
        .append(payments.size())
        .append(")</h2>");

    if (payments.isEmpty()) {
      html.append(
          "<p style='color:#718096;font-style:italic;'>No payments recorded for this"
              + " contract.</p>");
    } else {
      // Sort by due date descending
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
            .append(payment.getCurrency())
            .append(" ")
            .append(fmt(payment.getAmount()))
            .append("</td>");
        html.append("<td style='text-align:right;font-variant-numeric:tabular-nums;'>")
            .append(
                received.compareTo(BigDecimal.ZERO) > 0
                    ? payment.getCurrency() + " " + fmt(received)
                    : "—")
            .append("</td>");
        html.append("<td style='text-align:right;font-variant-numeric:tabular-nums;'>")
            .append(
                balance.compareTo(BigDecimal.ZERO) > 0 && payment.getStatus() != PAID
                    ? payment.getCurrency() + " " + fmt(balance)
                    : "—")
            .append("</td>");
        html.append("<td>");
        String payStatus = payment.getStatus().name();
        html.append("<span class='pay-status pay-").append(payStatus.toLowerCase()).append("'>");
        html.append(formatEnumValue(payStatus)).append("</span>");
        html.append("</td>");
        html.append("<td>")
            .append(
                payment.getPaymentDate() != null
                    ? formatDate(payment.getPaymentDate(), dateFmt)
                    : "—")
            .append("</td>");
        html.append("</tr>");
      }
      html.append("</tbody></table>");
    }

    html.append("</div>");

    html.append("</body></html>");
    return html.toString();
  }

  private void appendContractReportCSS(StringBuilder css) {
    // Page margins + running footer + page numbers
    css.append("@page { margin: 40px 50px 70px 50px; ");
    css.append("@bottom-center { content: element(running-footer); } ");
    css.append(
        "@bottom-right { content: 'Page ' counter(page) ' of ' counter(pages); font-size: 9px;"
            + " color: #a0aec0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; } }");
    css.append(
        "@page:first { margin: 0; @bottom-center { content: none; } @bottom-right { content: none;"
            + " } }");
    css.append(
        ".running-footer { position: running(running-footer); width: 100%; border-top: 1px solid"
            + " #e2e8f0; padding-top: 8px; font-size: 9px; color: #a0aec0; display: flex;"
            + " justify-content: space-between; }");
    css.append(".running-footer .rf-left { }");
    css.append(".running-footer .rf-center { text-align: center; }");
    css.append(".running-footer .rf-right { text-align: right; }");
    css.append(
        "body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0; padding: 0;"
            + " color: #1a202c; font-size: 13px; line-height: 1.5; }");

    // Cover page
    css.append(
        ".cover { page-break-after: always; padding: 0; height: 100vh; display: flex;"
            + " flex-direction: column; justify-content: center; align-items: center; }");
    css.append(".cover-header { text-align: center; margin-bottom: 40px; }");
    css.append(
        ".cover-title { font-size: 44px; font-weight: 700; color: #1a365d; letter-spacing: 3px;"
            + " margin-bottom: 12px; }");
    css.append(
        ".cover-subtitle { font-size: 16px; color: #718096; letter-spacing: 2px; margin-bottom:"
            + " 6px; }");
    css.append(".cover-date { font-size: 12px; color: #a0aec0; }");
    css.append(
        ".cover-summary { width: 80%; max-width: 520px; border-collapse: collapse; margin-top:"
            + " 20px; }");
    css.append(".cover-summary td { padding: 14px 20px; }");
    css.append(
        ".cs-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1.2px; color:"
            + " #718096; font-weight: 600; margin-bottom: 4px; }");
    css.append(".cs-value { font-size: 15px; color: #1a202c; font-weight: 500; }");
    css.append(
        ".cover-footer { position: absolute; bottom: 40px; text-align: center; font-size: 10px;"
            + " color: #a0aec0; width: 100%; }");

    // Status badges (cover)
    css.append(
        ".status-badge { display: inline-block; padding: 8px 28px; border-radius: 20px; font-size:"
            + " 14px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; }");
    css.append(
        ".status-active { background-color: #f0fdf4; color: #166534; border: 2px solid #bbf7d0; }");
    css.append(
        ".status-draft { background-color: #f9fafb; color: #374151; border: 2px solid #e5e7eb; }");
    css.append(
        ".status-expired { background-color: #fef2f2; color: #991b1b; border: 2px solid #fecaca;"
            + " }");
    css.append(
        ".status-terminated { background-color: #fef2f2; color: #991b1b; border: 2px solid #fecaca;"
            + " }");
    css.append(
        ".status-pending_signature { background-color: #fefce8; color: #854d0e; border: 2px solid"
            + " #fef08a; }");

    // Content pages (no extra padding — @page margins handle it)
    css.append(".page { page-break-before: always; }");
    css.append(
        ".page-header { border-bottom: 2px solid #1a365d; padding-bottom: 10px; margin-bottom:"
            + " 30px; font-size: 20px; font-weight: 700; color: #1a365d; }");

    // Section titles
    css.append(
        ".section-title { font-size: 16px; font-weight: 700; color: #1a365d; margin: 28px 0 14px 0;"
            + " padding-left: 12px; border-left: 4px solid #2b6cb0; }");

    // Detail grid (2-column)
    css.append(".detail-grid { width: 100%; border-collapse: collapse; }");
    css.append(".detail-grid td { padding: 10px 16px; vertical-align: top; width: 50%; }");
    css.append(
        ".fg-label { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color:"
            + " #718096; font-weight: 600; margin-bottom: 3px; }");
    css.append(".fg-value { font-size: 14px; color: #1a202c; }");

    // Text blocks (rich HTML content)
    css.append(
        ".text-block { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 6px;"
            + " padding: 16px; font-size: 13px; color: #2d3748; line-height: 1.6; }");
    css.append(".text-block p { margin: 0 0 8px 0; }");
    css.append(".text-block ul, .text-block ol { margin: 4px 0; padding-left: 24px; }");
    css.append(".text-block li { margin-bottom: 4px; }");
    css.append(".text-block strong, .text-block b { font-weight: 600; }");
    css.append(
        ".text-block h1, .text-block h2, .text-block h3, .text-block h4 { color: #1a365d; margin:"
            + " 12px 0 6px 0; }");

    // Party cards
    css.append(
        ".party-card { background-color: #fff; border: 1px solid #e2e8f0; border-left: 3px solid;"
            + " border-radius: 6px; padding: 20px 24px; margin-bottom: 14px; }");
    css.append(
        ".party-role { display: inline-block; font-size: 10px; font-weight: 600; letter-spacing:"
            + " 0.8px; text-transform: uppercase; padding: 2px 10px; border-radius: 3px;"
            + " margin-bottom: 6px; }");
    css.append(
        ".party-name { font-size: 20px; font-weight: 700; color: #1a202c; margin-bottom: 12px;"
            + " padding-bottom: 10px; border-bottom: 1px solid #edf2f7; }");
    css.append(".party-details { border-collapse: collapse; width: 100%; }");
    css.append(
        ".pd-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.8px; color:"
            + " #a0aec0; font-weight: 600; padding: 4px 8px 4px 0; width: 90px; vertical-align:"
            + " top; }");
    css.append(
        ".pd-value { font-size: 13px; color: #2d3748; padding: 4px 20px 4px 0; vertical-align: top;"
            + " }");

    // Payment instruction card
    css.append(
        ".pi-card { background-color: #f7fafc; border: 1px solid #e2e8f0; border-left: 3px solid"
            + " #2b6cb0; border-radius: 6px; padding: 20px 24px; margin-bottom: 20px; }");
    css.append(
        ".pi-header { margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #edf2f7;"
            + " }");
    css.append(
        ".pi-name { font-size: 17px; font-weight: 700; color: #1a202c; margin-right: 12px; }");
    css.append(
        ".pi-method { display: inline-block; font-size: 10px; font-weight: 600; letter-spacing:"
            + " 0.8px; text-transform: uppercase; padding: 2px 10px; border-radius: 3px; color:"
            + " #2b6cb0; background-color: #eff6ff; vertical-align: middle; }");
    css.append(
        ".pi-current { display: inline-block; font-size: 10px; font-weight: 600; letter-spacing:"
            + " 0.8px; text-transform: uppercase; padding: 2px 10px; border-radius: 3px; color:"
            + " #166534; background-color: #f0fdf4; vertical-align: middle; margin-left: 8px; }");
    css.append(".pi-period { font-size: 12px; color: #718096; margin-bottom: 12px; }");
    css.append(
        ".pi-details { margin-top: 10px; padding-top: 8px; border-top: 1px solid #edf2f7; }");

    // Summary cards
    css.append(".summary-grid { width: 100%; border-collapse: separate; border-spacing: 10px 0; }");
    css.append(
        ".summary-grid td { border-radius: 8px; padding: 16px; text-align: center; vertical-align:"
            + " top; width: 25%; }");
    css.append(
        ".sc-label { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; font-weight:"
            + " 600; margin-bottom: 6px; }");
    css.append(".sc-amount { font-size: 20px; font-weight: 700; margin-bottom: 2px; }");
    css.append(".sc-count { font-size: 11px; }");

    // Payment table
    css.append(".payment-table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
    css.append(
        ".payment-table thead th { background-color: #edf2f7; padding: 10px 12px; text-align: left;"
            + " font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: #4a5568;"
            + " font-weight: 700; border-bottom: 2px solid #cbd5e0; }");
    css.append(
        ".payment-table tbody td { padding: 9px 12px; border-bottom: 1px solid #e2e8f0; font-size:"
            + " 13px; }");
    css.append(".payment-table tbody tr:nth-child(even) { background-color: #f7fafc; }");

    // Payment status badges
    css.append(
        ".pay-status { display: inline-block; padding: 3px 10px; border-radius: 4px; font-size:"
            + " 11px; font-weight: 600; letter-spacing: 0.3px; }");
    css.append(".pay-paid { background-color: #f0fdf4; color: #166534; }");
    css.append(".pay-pending { background-color: #fefce8; color: #854d0e; }");
    css.append(".pay-partially_paid { background-color: #fefce8; color: #854d0e; }");
    css.append(".pay-overdue { background-color: #fef2f2; color: #991b1b; }");
    css.append(".pay-cancelled { background-color: #f9fafb; color: #6b7280; }");
  }

  private void appendCoverCell(StringBuilder html, String label, String value) {
    html.append("<td><div class='cs-label'>").append(escapeHtml(label)).append("</div>");
    html.append("<div class='cs-value'>").append(value).append("</div></td>");
  }

  private void appendField(StringBuilder html, String label, String value) {
    html.append("<td><div class='fg-label'>").append(escapeHtml(label)).append("</div>");
    html.append("<div class='fg-value'>").append(value != null ? value : "—").append("</div></td>");
  }

  private void appendSummaryCard(
      StringBuilder html,
      String label,
      long count,
      String amount,
      String bgColor,
      String accentColor,
      String textColor) {
    html.append("<td style='background-color:").append(bgColor).append(";'>");
    html.append("<div class='sc-label' style='color:")
        .append(accentColor)
        .append(";'>")
        .append(label)
        .append("</div>");
    html.append("<div class='sc-amount' style='color:")
        .append(textColor)
        .append(";'>")
        .append(amount)
        .append("</div>");
    html.append("<div class='sc-count' style='color:")
        .append(accentColor)
        .append(";'>")
        .append(count)
        .append(count == 1 ? " payment" : " payments")
        .append("</div>");
    html.append("</td>");
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

  private String fmt(BigDecimal value) {
    return value != null ? String.format("%,.2f", value) : "0.00";
  }

  private String formatDate(LocalDate date, DateTimeFormatter fmt) {
    return date != null ? date.format(fmt) : "—";
  }

  private String buildTenantReportHTML(
      Tenant tenant,
      List<TenantAddress> addresses,
      List<Contract> contracts,
      List<Payment> allPayments,
      Map<UUID, Property> propertyMap) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

    BigDecimal totalPaid =
        allPayments.stream()
            .filter(p -> p.getStatus() == PAID)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPending =
        allPayments.stream()
            .filter(p -> p.getStatus() == PENDING || p.getStatus() == OVERDUE)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long activeContracts =
        contracts.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().name().equals("ACTIVE"))
            .count();

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
    html.append("<style>");

    // Global styles
    html.append(
        "body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 0; padding: 0;"
            + " color: #1a202c; }");

    // Cover
    html.append(
        ".cover { background: linear-gradient(135deg, #2d3748 0%, #4a5568 50%, #718096 100%); ");
    html.append(
        "color: white; padding: 120px 50px 80px 50px; text-align: center; page-break-after: always;"
            + " min-height: 700px; }");
    html.append(
        ".cover-subtitle { font-size: 14px; letter-spacing: 6px; text-transform: uppercase; color:"
            + " #e2e8f0; margin-bottom: 60px; }");
    html.append(
        ".cover h1 { font-size: 44px; margin: 0 0 12px 0; font-weight: 700; letter-spacing: 1px;"
            + " }");
    html.append(".cover-detail { font-size: 16px; color: #cbd5e0; margin: 6px 0; }");
    html.append(
        ".cover-divider { width: 80px; height: 2px; background-color: #a0aec0; margin: 40px auto;"
            + " }");
    html.append(
        ".cover-date { font-size: 13px; color: #a0aec0; margin-top: 40px; letter-spacing: 2px; }");
    html.append(
        ".cover-id { font-size: 11px; color: #718096; margin-top: 8px; letter-spacing: 1px; }");

    // Pages
    html.append(".page { padding: 45px 50px; page-break-before: always; }");
    html.append(
        ".section-title { font-size: 26px; font-weight: 700; color: #2d3748; margin: 0 0 25px 0; ");
    html.append("padding-left: 16px; border-left: 4px solid #4a5568; }");
    html.append(
        ".section-subtitle { font-size: 18px; font-weight: 600; color: #2d3748; margin: 28px 0 14px"
            + " 0; }");

    // Detail table
    html.append(".detail-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }");
    html.append(".detail-table td { padding: 10px 14px; vertical-align: top; }");
    html.append(
        ".detail-label { font-size: 10px; font-weight: 600; text-transform: uppercase;"
            + " letter-spacing: 1.2px; color: #718096; margin: 0 0 3px 0; }");
    html.append(".detail-value { font-size: 16px; color: #1a202c; font-weight: 500; margin: 0; }");
    html.append(
        ".detail-cell { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 4px;"
            + " padding: 12px 14px; }");

    // Summary cards
    html.append(".summary-table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
    html.append(".summary-table td { padding: 10px; text-align: center; }");
    html.append(".summary-card { border: 1px solid #e2e8f0; border-radius: 6px; padding: 16px; }");
    html.append(
        ".summary-label { font-size: 10px; color: #718096; text-transform: uppercase;"
            + " letter-spacing: 1px; }");
    html.append(".summary-value { font-size: 24px; font-weight: 700; margin-top: 4px; }");

    // Address card
    html.append(
        ".address-card { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 6px;"
            + " padding: 16px 18px; margin-bottom: 10px; }");
    html.append(
        ".address-type { font-size: 11px; font-weight: 600; text-transform: uppercase;"
            + " letter-spacing: 1px; color: #4a5568; margin-bottom: 6px; }");
    html.append(".address-line { font-size: 14px; color: #1a202c; margin: 2px 0; }");
    html.append(
        ".address-status { display: inline-block; font-size: 10px; font-weight: 600;"
            + " text-transform: uppercase; letter-spacing: 0.5px; ");
    html.append("padding: 2px 8px; border-radius: 10px; margin-top: 6px; }");
    html.append(".status-active { background-color: #c6f6d5; color: #276749; }");
    html.append(".status-inactive { background-color: #e2e8f0; color: #4a5568; }");

    // Data table
    html.append("table.data-table { width: 100%; border-collapse: collapse; margin-top: 15px; }");
    html.append(
        "table.data-table th { background-color: #2d3748; color: white; padding: 10px 12px;"
            + " text-align: left; font-size: 11px; ");
    html.append("text-transform: uppercase; letter-spacing: 0.5px; }");
    html.append(
        "table.data-table td { padding: 9px 12px; border-bottom: 1px solid #e2e8f0; font-size:"
            + " 13px; }");
    html.append("table.data-table tr:nth-child(even) td { background-color: #f7fafc; }");

    // Notes
    html.append(
        ".notes-box { background-color: #f7fafc; border: 1px solid #e2e8f0; border-radius: 4px;"
            + " padding: 12px 16px; ");
    html.append("font-size: 13px; color: #2d3748; margin-top: 16px; }");

    // Footer
    html.append(
        ".footer { text-align: center; color: #a0aec0; font-size: 11px; padding: 30px 0 20px 0;"
            + " letter-spacing: 1px; }");

    html.append("</style></head><body>");

    // ========== COVER PAGE ==========
    html.append("<div class='cover'>");
    html.append("<div class='cover-subtitle'>Tenant Report</div>");
    String fullName =
        escapeHtml(tenant.getFirstName())
            + (tenant.getLastName() != null ? " " + escapeHtml(tenant.getLastName()) : "");
    html.append("<h1>").append(fullName).append("</h1>");
    if (tenant.getEmail() != null) {
      html.append("<p class='cover-detail'>").append(escapeHtml(tenant.getEmail())).append("</p>");
    }
    if (tenant.getPhone() != null) {
      html.append("<p class='cover-detail'>").append(escapeHtml(tenant.getPhone())).append("</p>");
    }
    html.append("<div class='cover-divider'></div>");
    html.append("<p class='cover-date'>")
        .append(LocalDate.now(clock).format(dateFormatter).toUpperCase())
        .append("</p>");
    html.append("<p class='cover-id'>REF #").append(tenant.getIdentifier()).append("</p>");
    html.append("</div>");

    // ========== PAGE 2: TENANT PROFILE ==========
    html.append("<div class='page'>");
    html.append("<h2 class='section-title'>Tenant Profile</h2>");

    html.append("<table class='detail-table'><tbody>");
    html.append("<tr>");
    appendDetailCell(html, "Full Name", fullName);
    appendDetailCell(html, "Email", tenant.getEmail());
    html.append("</tr>");
    html.append("<tr>");
    appendDetailCell(html, "Phone", tenant.getPhone());
    appendDetailCell(html, "Tenant ID", "#" + tenant.getIdentifier());
    html.append("</tr>");
    if (tenant.getTaxNumber() != null || tenant.getIdNumber() != null) {
      html.append("<tr>");
      appendDetailCell(html, "Tax Number", tenant.getTaxNumber());
      appendDetailCell(html, "ID Number", tenant.getIdNumber());
      html.append("</tr>");
    }
    html.append("</tbody></table>");

    // Additional info (rich text)
    if (tenant.getAdditionalInfo() != null && !tenant.getAdditionalInfo().isBlank()) {
      html.append("<div class='notes-box'><strong>Additional Information:</strong><br/>")
          .append(tenant.getAdditionalInfo())
          .append("</div>");
    }

    // Summary cards
    html.append("<h3 class='section-subtitle'>Overview</h3>");
    html.append("<table class='summary-table'><tr>");
    html.append(
            "<td><div class='summary-card'><div class='summary-label'>Total Contracts</div><div"
                + " class='summary-value'>")
        .append(contracts.size())
        .append("</div></div></td>");
    html.append(
            "<td><div class='summary-card'><div class='summary-label'>Active Contracts</div><div"
                + " class='summary-value' style='color: #059669;'>")
        .append(activeContracts)
        .append("</div></div></td>");
    html.append(
            "<td><div class='summary-card'><div class='summary-label'>Total Paid</div><div"
                + " class='summary-value' style='color: #059669;'>EUR ")
        .append(String.format("%.2f", totalPaid))
        .append("</div></div></td>");
    html.append(
            "<td><div class='summary-card'><div class='summary-label'>Outstanding</div><div"
                + " class='summary-value' style='color: ")
        .append(totalPending.compareTo(BigDecimal.ZERO) > 0 ? "#dc2626" : "#059669")
        .append(";'>EUR ")
        .append(String.format("%.2f", totalPending))
        .append("</div></div></td>");
    html.append("</tr></table>");

    html.append("</div>");

    // ========== PAGE 3: ADDRESSES ==========
    if (!addresses.isEmpty()) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Addresses</h2>");

      for (TenantAddress addr : addresses) {
        html.append("<div class='address-card'>");
        String type =
            addr.getAddressType() != null ? formatEnumValue(addr.getAddressType().name()) : "Other";
        html.append("<div class='address-type'>").append(escapeHtml(type)).append("</div>");
        html.append("<div class='address-line'>")
            .append(escapeHtml(addr.getStreet()))
            .append("</div>");
        html.append("<div class='address-line'>").append(escapeHtml(addr.getCity()));
        if (addr.getPostalCode() != null) {
          html.append(", ").append(escapeHtml(addr.getPostalCode()));
        }
        html.append("</div>");
        if (addr.getCountry() != null) {
          html.append("<div class='address-line'>")
              .append(escapeHtml(addr.getCountry()))
              .append("</div>");
        }
        String statusClass =
            addr.getStatus() != null && addr.getStatus().name().equals("ACTIVE")
                ? "status-active"
                : "status-inactive";
        String statusLabel =
            addr.getStatus() != null ? formatEnumValue(addr.getStatus().name()) : "Unknown";
        html.append("<span class='address-status ")
            .append(statusClass)
            .append("'>")
            .append(statusLabel)
            .append("</span>");
        html.append("</div>");
      }

      html.append("</div>");
    }

    // ========== PAGE 4: CONTRACTS ==========
    if (!contracts.isEmpty()) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Rental History</h2>");
      html.append("<p style='font-size: 13px; color: #718096; margin-bottom: 12px;'>")
          .append(contracts.size())
          .append(" contract(s) on record</p>");

      html.append("<table class='data-table'><thead><tr>");
      html.append(
          "<th>Contract</th><th>Property</th><th>Type</th><th>Start</th><th>End</th><th>Rent</th><th>Status</th>");
      html.append("</tr></thead><tbody>");

      for (Contract contract : contracts) {
        Property property = propertyMap.get(contract.getPropertyId());
        String propertyName =
            property != null ? property.getStreet() + ", " + property.getCity() : "Unknown";

        html.append("<tr>");
        html.append("<td>#").append(contract.getIdentifier()).append("</td>");
        html.append("<td>").append(escapeHtml(propertyName)).append("</td>");
        html.append("<td>")
            .append(
                contract.getContractType() != null
                    ? formatEnumValue(contract.getContractType().name())
                    : "-")
            .append("</td>");
        html.append("<td>")
            .append(
                contract.getStartDate() != null
                    ? contract.getStartDate().format(shortDateFormatter)
                    : "-")
            .append("</td>");
        html.append("<td>")
            .append(
                contract.getEndDate() != null
                    ? contract.getEndDate().format(shortDateFormatter)
                    : "Ongoing")
            .append("</td>");
        html.append("<td>")
            .append(contract.getCurrency())
            .append(" ")
            .append(String.format("%.2f", contract.getRentAmount()))
            .append("</td>");
        html.append("<td>")
            .append(
                contract.getStatus() != null ? formatEnumValue(contract.getStatus().name()) : "-")
            .append("</td>");
        html.append("</tr>");
      }

      html.append("</tbody></table>");
      html.append("</div>");
    }

    // ========== PAGE 5: PAYMENT HISTORY ==========
    if (!allPayments.isEmpty()) {
      html.append("<div class='page'>");
      html.append("<h2 class='section-title'>Payment History</h2>");

      // Payment summary by year
      Map<Integer, BigDecimal[]> yearPayments = new TreeMap<>(Comparator.reverseOrder());
      for (Payment payment : allPayments) {
        LocalDate dateRef =
            payment.getPaymentDate() != null ? payment.getPaymentDate() : payment.getDueDate();
        if (dateRef == null) {
          continue;
        }
        int year = dateRef.getYear();
        yearPayments.computeIfAbsent(
            year, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal[] amounts = yearPayments.get(year);
        if (payment.getStatus() == PAID) {
          amounts[0] = amounts[0].add(payment.getAmount());
        } else {
          amounts[1] = amounts[1].add(payment.getAmount());
        }
      }

      if (!yearPayments.isEmpty()) {
        html.append("<table class='summary-table'><tr>");
        for (Map.Entry<Integer, BigDecimal[]> entry : yearPayments.entrySet()) {
          html.append("<td><div class='summary-card'>");
          html.append("<div class='summary-label'>").append(entry.getKey()).append("</div>");
          html.append("<div class='summary-value' style='color: #059669; font-size: 18px;'>EUR ")
              .append(String.format("%.2f", entry.getValue()[0]))
              .append("</div>");
          if (entry.getValue()[1].compareTo(BigDecimal.ZERO) > 0) {
            html.append(
                    "<div style='font-size: 12px; color: #dc2626; margin-top: 4px;'>Outstanding:"
                        + " EUR ")
                .append(String.format("%.2f", entry.getValue()[1]))
                .append("</div>");
          }
          html.append("</div></td>");
        }
        html.append("</tr></table>");
      }

      // Recent payments table (last 50)
      html.append("<h3 class='section-subtitle'>Recent Payments</h3>");
      List<Payment> sortedPayments =
          allPayments.stream()
              .sorted(
                  (a, b) -> {
                    LocalDate da = a.getDueDate() != null ? a.getDueDate() : a.getPaymentDate();
                    LocalDate db = b.getDueDate() != null ? b.getDueDate() : b.getPaymentDate();
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

      html.append("<table class='data-table'><thead><tr>");
      html.append("<th>Due Date</th><th>Amount</th><th>Payment Date</th><th>Status</th>");
      html.append("</tr></thead><tbody>");

      for (Payment payment : sortedPayments) {
        html.append("<tr>");
        html.append("<td>")
            .append(
                payment.getDueDate() != null
                    ? payment.getDueDate().format(shortDateFormatter)
                    : "-")
            .append("</td>");
        html.append("<td>")
            .append(payment.getCurrency())
            .append(" ")
            .append(String.format("%.2f", payment.getAmount()))
            .append("</td>");
        html.append("<td>")
            .append(
                payment.getPaymentDate() != null
                    ? payment.getPaymentDate().format(shortDateFormatter)
                    : "-")
            .append("</td>");
        String statusStyle = "";
        if (payment.getStatus() == PAID) {
          statusStyle = "color: #059669; font-weight: 600;";
        } else if (payment.getStatus() == OVERDUE) {
          statusStyle = "color: #dc2626; font-weight: 600;";
        } else if (payment.getStatus() == PENDING) {
          statusStyle = "color: #d97706; font-weight: 600;";
        }
        html.append("<td style='")
            .append(statusStyle)
            .append("'>")
            .append(payment.getStatus() != null ? formatEnumValue(payment.getStatus().name()) : "-")
            .append("</td>");
        html.append("</tr>");
      }

      html.append("</tbody></table>");
      html.append("</div>");
    }

    html.append("<div class='footer'>Generated by Buurman Property Management</div>");
    html.append("</body></html>");

    return html.toString();
  }

  private byte[] convertHTMLToPDF(String html) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      PdfWriter writer = new PdfWriter(baos);
      PdfDocument pdf = new PdfDocument(writer);
      pdf.setDefaultPageSize(PageSize.A4);

      ConverterProperties converterProperties = new ConverterProperties();
      HtmlConverter.convertToPdf(html, pdf, converterProperties);

      return baos.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate PDF", e);
    }
  }

  private String escapeHtml(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private String sanitizeRichText(String html) {
    if (html == null) return "";
    return html.replaceAll("(?i)<script[^>]*>.*?</script>", "")
        .replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "")
        .replaceAll("(?i)<object[^>]*>.*?</object>", "")
        .replaceAll("(?i)<embed[^>]*>", "")
        .replaceAll("(?i)<link[^>]*>", "")
        .replaceAll("(?i)\\s+on\\w+\\s*=\\s*\"[^\"]*\"", "")
        .replaceAll("(?i)\\s+on\\w+\\s*=\\s*'[^']*'", "");
  }

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

  private String formatPaymentMethod(String value) {
    if (value == null) {
      return "";
    }
    return PAYMENT_METHOD_LABELS.getOrDefault(value, formatEnumValue(value));
  }

  private String formatEnumValue(String value) {
    if (value == null) {
      return "";
    }
    return Arrays.stream(value.split("_"))
        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
        .collect(joining(" "));
  }

  private String getEnergyRatingColor(String rating) {
    if (rating == null) {
      return "#6b7280";
    }
    return switch (rating) {
      case "A++" -> "#065f46";
      case "A+" -> "#047857";
      case "A" -> "#059669";
      case "B" -> "#65a30d";
      case "C" -> "#ca8a04";
      case "D" -> "#ea580c";
      case "E" -> "#dc2626";
      case "F" -> "#b91c1c";
      case "G" -> "#991b1b";
      default -> "#6b7280";
    };
  }

  private boolean isTrue(Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  private void appendDetailCell(StringBuilder html, String label, String value) {
    html.append("<td width='50%'>");
    if (value != null && !value.isBlank()) {
      html.append("<div class='detail-cell'>");
      html.append("<p class='detail-label'>").append(escapeHtml(label)).append("</p>");
      html.append("<p class='detail-value'>").append(escapeHtml(value)).append("</p>");
      html.append("</div>");
    } else {
      html.append("&nbsp;");
    }
    html.append("</td>");
  }

  private void appendCheckItem(StringBuilder html, String label, Boolean value) {
    if (isTrue(value)) {
      html.append("<div class='check-item'><span class='check-icon'>&#10003;</span>")
          .append(escapeHtml(label))
          .append("</div>");
    }
  }

  private static class TransactionDTO {
    String id;
    LocalDate date;
    String type;
    String description;
    String property;
    String category;
    BigDecimal amount;
    String currency;

    public TransactionDTO(
        String id,
        LocalDate date,
        String type,
        String description,
        String property,
        String category,
        BigDecimal amount,
        String currency) {
      this.id = id;
      this.date = date;
      this.type = type;
      this.description = description;
      this.property = property;
      this.category = category;
      this.amount = amount;
      this.currency = currency;
    }
  }

  private static class FinancialYearSummary {
    private final int year;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expenses = BigDecimal.ZERO;

    public FinancialYearSummary(int year) {
      this.year = year;
    }

    public void addIncome(BigDecimal amount) {
      this.income = this.income.add(amount);
    }

    public void addExpense(BigDecimal amount) {
      this.expenses = this.expenses.add(amount);
    }

    public BigDecimal getNetProfit() {
      return income.subtract(expenses);
    }
  }
}
