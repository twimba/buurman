package com.buurman.service;

import com.buurman.domain.*;
import com.buurman.repository.*;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final PropertyRepository propertyRepository;
    private final ContractRepository contractRepository;
    private final TenantRepository tenantRepository;

    public ExportService(
            PaymentRepository paymentRepository,
            ExpenseRepository expenseRepository,
            PropertyRepository propertyRepository,
            ContractRepository contractRepository,
            TenantRepository tenantRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.expenseRepository = expenseRepository;
        this.propertyRepository = propertyRepository;
        this.contractRepository = contractRepository;
        this.tenantRepository = tenantRepository;
    }

    public byte[] generateTransactionHistoryCSV(LocalDate startDate, LocalDate endDate, UUID teamId) {
        List<TransactionDTO> transactions = getTransactionHistory(startDate, endDate, teamId);

        try (StringWriter sw = new StringWriter();
             CSVWriter writer = new CSVWriter(sw)) {

            // Header
            String[] header = {"Date", "Type", "Description", "Property", "Category", "Amount", "Currency"};
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

            return sw.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    public byte[] generateTransactionHistoryPDF(LocalDate startDate, LocalDate endDate, UUID teamId) {
        List<TransactionDTO> transactions = getTransactionHistory(startDate, endDate, teamId);

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.type))
                .map(t -> t.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.type))
                .map(t -> t.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netTotal = totalIncome.subtract(totalExpenses);

        String html = buildTransactionHistoryHTML(transactions, startDate, endDate, totalIncome, totalExpenses, netTotal);

        return convertHTMLToPDF(html);
    }

    public byte[] generatePropertyBrochurePDF(UUID propertyId, UUID teamId) {
        Property property = propertyRepository.findByIdAndTeamId(propertyId, teamId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Get contracts for this property
        List<Contract> contracts = contractRepository.findByPropertyId(propertyId, teamId);

        // Get payments for this property
        List<Payment> payments = paymentRepository.findAllByTeamId(teamId).stream()
                .filter(p -> p.getContractId() != null &&
                           contracts.stream().anyMatch(c -> c.getId().equals(p.getContractId())))
                .collect(Collectors.toList());

        // Get expenses for this property
        List<Expense> expenses = expenseRepository.findByPropertyId(propertyId, teamId);

        // Calculate financial summary by year
        Map<Integer, FinancialYearSummary> yearSummaries = calculateYearSummaries(payments, expenses);

        String html = buildPropertyBrochureHTML(property, contracts, payments, expenses, yearSummaries, teamId);

        return convertHTMLToPDF(html);
    }

    public byte[] generateContractReportPDF(UUID contractId, UUID teamId) {
        Contract contract = contractRepository.findByIdAndTeamId(contractId, teamId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        Property property = propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Tenant tenant = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        List<Payment> payments = paymentRepository.findByContractId(contractId, teamId);

        String html = buildContractReportHTML(contract, property, tenant, payments);

        return convertHTMLToPDF(html);
    }

    private List<TransactionDTO> getTransactionHistory(LocalDate startDate, LocalDate endDate, UUID teamId) {
        List<TransactionDTO> transactions = new ArrayList<>();

        // Add payments as income
        List<Payment> payments = (startDate != null && endDate != null)
                ? paymentRepository.findByDateRange(startDate, endDate, teamId)
                : paymentRepository.findAllByTeamId(teamId);

        for (Payment payment : payments) {
            if (payment.getStatus() == Payment.PaymentStatus.PAID && payment.getPaymentDate() != null) {
                Contract contract = contractRepository.findByIdAndTeamId(payment.getContractId(), teamId).orElse(null);
                Property property = contract != null
                        ? propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null)
                        : null;

                String propertyName = property != null
                        ? property.getStreet() + ", " + property.getCity()
                        : "Unknown Property";

                transactions.add(new TransactionDTO(
                        payment.getId().toString(),
                        payment.getPaymentDate(),
                        "INCOME",
                        "Rent payment - " + propertyName,
                        propertyName,
                        null,
                        payment.getAmount(),
                        payment.getCurrency()
                ));
            }
        }

        // Add expenses
        List<Expense> expenses = (startDate != null && endDate != null)
                ? expenseRepository.findByDateRange(startDate, endDate, teamId)
                : expenseRepository.findAllByTeamId(teamId);

        for (Expense expense : expenses) {
            Property property = propertyRepository.findByIdAndTeamId(expense.getPropertyId(), teamId).orElse(null);

            String propertyName = property != null
                    ? property.getStreet() + ", " + property.getCity()
                    : "Unknown Property";

            transactions.add(new TransactionDTO(
                    expense.getId().toString(),
                    expense.getExpenseDate(),
                    "EXPENSE",
                    expense.getDescription(),
                    propertyName,
                    expense.getCategory().name(),
                    expense.getAmount(),
                    expense.getCurrency()
            ));
        }

        // Sort by date descending
        transactions.sort((a, b) -> b.date.compareTo(a.date));

        return transactions;
    }

    private Map<Integer, FinancialYearSummary> calculateYearSummaries(List<Payment> payments, List<Expense> expenses) {
        Map<Integer, FinancialYearSummary> summaries = new TreeMap<>(Comparator.reverseOrder());

        // Process payments
        for (Payment payment : payments) {
            if (payment.getStatus() == Payment.PaymentStatus.PAID && payment.getPaymentDate() != null) {
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
            BigDecimal netTotal
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String periodText = startDate != null && endDate != null
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
        html.append("th { background-color: #f3f4f6; padding: 12px; text-align: left; font-weight: 600; border-bottom: 2px solid #d1d5db; }");
        html.append("td { padding: 10px; border-bottom: 1px solid #e5e7eb; }");
        html.append("tr:hover { background-color: #f9fafb; }");
        html.append(".income-badge { background-color: #d1fae5; color: #065f46; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }");
        html.append(".expense-badge { background-color: #fee2e2; color: #991b1b; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }");
        html.append(".amount-income { color: #059669; font-weight: 600; }");
        html.append(".amount-expense { color: #dc2626; font-weight: 600; }");
        html.append(".footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #d1d5db; text-align: center; color: #6b7280; font-size: 12px; }");
        html.append("</style></head><body>");

        html.append("<h1>Transaction History Report</h1>");
        html.append("<p style='color: #6b7280; font-size: 14px;'>Period: ").append(periodText).append("</p>");
        html.append("<p style='color: #6b7280; font-size: 12px;'>Generated on: ").append(LocalDate.now().format(formatter)).append("</p>");

        html.append("<div class='summary'>");
        html.append("<div class='summary-card income'><h3>Total Income</h3><p>EUR ").append(String.format("%.2f", totalIncome)).append("</p></div>");
        html.append("<div class='summary-card expense'><h3>Total Expenses</h3><p>EUR ").append(String.format("%.2f", totalExpenses)).append("</p></div>");
        html.append("<div class='summary-card net'><h3>Net Total</h3><p>EUR ").append(String.format("%.2f", netTotal)).append("</p></div>");
        html.append("</div>");

        html.append("<h2>Transactions (").append(transactions.size()).append(")</h2>");
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>Date</th><th>Type</th><th>Description</th><th>Property</th><th>Category</th><th style='text-align: right;'>Amount</th>");
        html.append("</tr></thead><tbody>");

        for (TransactionDTO transaction : transactions) {
            html.append("<tr>");
            html.append("<td>").append(transaction.date.format(formatter)).append("</td>");
            html.append("<td><span class='").append(transaction.type.equals("INCOME") ? "income-badge" : "expense-badge")
                    .append("'>").append(transaction.type).append("</span></td>");
            html.append("<td>").append(escapeHtml(transaction.description)).append("</td>");
            html.append("<td>").append(escapeHtml(transaction.property)).append("</td>");
            html.append("<td>").append(transaction.category != null ? escapeHtml(transaction.category) : "-").append("</td>");
            html.append("<td style='text-align: right;' class='").append(transaction.type.equals("INCOME") ? "amount-income" : "amount-expense")
                    .append("'>").append(transaction.type.equals("INCOME") ? "+" : "-")
                    .append(transaction.currency).append(" ").append(String.format("%.2f", transaction.amount)).append("</td>");
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
            UUID teamId
    ) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; color: #333; }");
        html.append(".cover { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 60px 40px; text-align: center; page-break-after: always; }");
        html.append(".cover h1 { font-size: 48px; margin: 0 0 20px 0; }");
        html.append(".cover p { font-size: 24px; margin: 10px 0; }");
        html.append(".section { padding: 40px; page-break-inside: avoid; }");
        html.append(".section h2 { color: #1e40af; border-bottom: 3px solid #3b82f6; padding-bottom: 10px; margin-bottom: 20px; }");
        html.append(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin: 20px 0; }");
        html.append(".info-item { margin-bottom: 10px; }");
        html.append(".info-label { font-weight: 600; color: #6b7280; font-size: 14px; }");
        html.append(".info-value { font-size: 18px; margin-top: 5px; }");
        html.append(".financial-year { background-color: #f3f4f6; border-radius: 8px; padding: 20px; margin: 15px 0; }");
        html.append(".financial-year h3 { margin: 0 0 15px 0; color: #1e40af; }");
        html.append(".financial-metrics { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; }");
        html.append(".metric { text-align: center; }");
        html.append(".metric-label { font-size: 12px; color: #6b7280; text-transform: uppercase; }");
        html.append(".metric-value { font-size: 24px; font-weight: bold; margin-top: 5px; }");
        html.append(".metric-income { color: #059669; }");
        html.append(".metric-expense { color: #dc2626; }");
        html.append(".metric-profit { color: #3b82f6; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        html.append("th { background-color: #f3f4f6; padding: 10px; text-align: left; font-size: 12px; }");
        html.append("td { padding: 8px; border-bottom: 1px solid #e5e7eb; font-size: 14px; }");
        html.append(".footer { text-align: center; color: #6b7280; font-size: 12px; padding: 20px; }");
        html.append("</style></head><body>");

        // Cover page
        html.append("<div class='cover'>");
        html.append("<h1>").append(escapeHtml(property.getStreet())).append("</h1>");
        html.append("<p>").append(escapeHtml(property.getCity())).append(", ").append(escapeHtml(property.getPostalCode())).append("</p>");
        html.append("<p style='font-size: 18px; margin-top: 40px;'>Property Brochure</p>");
        html.append("<p style='font-size: 14px;'>").append(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))).append("</p>");
        html.append("</div>");

        // Property Details
        html.append("<div class='section'>");
        html.append("<h2>Property Details</h2>");
        html.append("<div class='info-grid'>");
        html.append("<div class='info-item'><div class='info-label'>Type</div><div class='info-value'>").append(property.getPropertyType()).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Status</div><div class='info-value'>").append(property.getStatus()).append("</div></div>");
        if (property.getBedrooms() != null) {
            html.append("<div class='info-item'><div class='info-label'>Bedrooms</div><div class='info-value'>").append(property.getBedrooms()).append("</div></div>");
        }
        if (property.getBathrooms() != null) {
            html.append("<div class='info-item'><div class='info-label'>Bathrooms</div><div class='info-value'>").append(property.getBathrooms()).append("</div></div>");
        }
        if (property.getSquareMeters() != null) {
            html.append("<div class='info-item'><div class='info-label'>Area</div><div class='info-value'>").append(property.getSquareMeters()).append(" m²</div></div>");
        }
        html.append("<div class='info-item'><div class='info-label'>Property ID</div><div class='info-value'>#").append(property.getIdentifier()).append("</div></div>");
        html.append("</div></div>");

        // Financial Overview
        html.append("<div class='section'>");
        html.append("<h2>Financial Overview</h2>");

        for (Map.Entry<Integer, FinancialYearSummary> entry : yearSummaries.entrySet()) {
            FinancialYearSummary summary = entry.getValue();
            html.append("<div class='financial-year'>");
            html.append("<h3>Year ").append(entry.getKey()).append("</h3>");
            html.append("<div class='financial-metrics'>");
            html.append("<div class='metric'><div class='metric-label'>Income</div><div class='metric-value metric-income'>EUR ").append(String.format("%.2f", summary.income)).append("</div></div>");
            html.append("<div class='metric'><div class='metric-label'>Expenses</div><div class='metric-value metric-expense'>EUR ").append(String.format("%.2f", summary.expenses)).append("</div></div>");
            html.append("<div class='metric'><div class='metric-label'>Net Profit</div><div class='metric-value metric-profit'>EUR ").append(String.format("%.2f", summary.getNetProfit())).append("</div></div>");
            html.append("</div></div>");
        }

        html.append("</div>");

        // Contracts
        html.append("<div class='section'>");
        html.append("<h2>Contracts (").append(contracts.size()).append(")</h2>");
        html.append("<table><thead><tr><th>Contract ID</th><th>Tenant</th><th>Start Date</th><th>End Date</th><th>Rent</th><th>Status</th></tr></thead><tbody>");

        for (Contract contract : contracts) {
            Tenant tenant = tenantRepository.findByIdAndTeamId(contract.getTenantId(), teamId).orElse(null);
            String tenantName = tenant != null ? tenant.getFirstName() + " " + (tenant.getLastName() != null ? tenant.getLastName() : "") : "Unknown";

            html.append("<tr>");
            html.append("<td>#").append(contract.getIdentifier()).append("</td>");
            html.append("<td>").append(escapeHtml(tenantName)).append("</td>");
            html.append("<td>").append(contract.getStartDate()).append("</td>");
            html.append("<td>").append(contract.getEndDate() != null ? contract.getEndDate().toString() : "Ongoing").append("</td>");
            html.append("<td>").append(contract.getCurrency()).append(" ").append(String.format("%.2f", contract.getRentAmount())).append("</td>");
            html.append("<td>").append(contract.getStatus()).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table></div>");

        html.append("<div class='footer'>Generated by Buurman Property Management</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String buildContractReportHTML(Contract contract, Property property, Tenant tenant, List<Payment> payments) {
        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; color: #333; }");
        html.append("h1 { color: #1e40af; border-bottom: 3px solid #3b82f6; padding-bottom: 10px; }");
        html.append("h2 { color: #1e40af; margin-top: 30px; }");
        html.append(".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }");
        html.append(".info-card { background-color: #f9fafb; border-radius: 8px; padding: 20px; }");
        html.append(".info-card h3 { margin: 0 0 15px 0; color: #1e40af; font-size: 16px; }");
        html.append(".info-item { margin-bottom: 12px; }");
        html.append(".info-label { font-weight: 600; color: #6b7280; font-size: 12px; text-transform: uppercase; }");
        html.append(".info-value { font-size: 16px; margin-top: 3px; }");
        html.append(".summary { display: flex; gap: 20px; margin: 20px 0; }");
        html.append(".summary-card { flex: 1; padding: 15px; border-radius: 8px; }");
        html.append(".paid { background-color: #d1fae5; border: 2px solid #10b981; }");
        html.append(".pending { background-color: #fef3c7; border: 2px solid #f59e0b; }");
        html.append(".summary-card h4 { margin: 0 0 5px 0; font-size: 14px; color: #666; }");
        html.append(".summary-card p { margin: 0; font-size: 24px; font-weight: bold; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        html.append("th { background-color: #f3f4f6; padding: 12px; text-align: left; font-weight: 600; border-bottom: 2px solid #d1d5db; }");
        html.append("td { padding: 10px; border-bottom: 1px solid #e5e7eb; }");
        html.append(".status-paid { background-color: #d1fae5; color: #065f46; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }");
        html.append(".status-pending { background-color: #fef3c7; color: #92400e; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }");
        html.append(".footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #d1d5db; text-align: center; color: #6b7280; font-size: 12px; }");
        html.append("</style></head><body>");

        html.append("<h1>Contract Report</h1>");
        html.append("<p style='color: #6b7280; font-size: 14px;'>Contract #").append(contract.getIdentifier()).append("</p>");
        html.append("<p style='color: #6b7280; font-size: 12px;'>Generated on: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))).append("</p>");

        html.append("<div class='info-grid'>");

        // Contract Details
        html.append("<div class='info-card'><h3>Contract Details</h3>");
        html.append("<div class='info-item'><div class='info-label'>Contract ID</div><div class='info-value'>#").append(contract.getIdentifier()).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Status</div><div class='info-value'>").append(contract.getStatus()).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Start Date</div><div class='info-value'>").append(contract.getStartDate()).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>End Date</div><div class='info-value'>").append(contract.getEndDate() != null ? contract.getEndDate().toString() : "Ongoing").append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Rent Amount</div><div class='info-value'>").append(contract.getCurrency()).append(" ").append(String.format("%.2f", contract.getRentAmount())).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Deposit</div><div class='info-value'>").append(contract.getCurrency()).append(" ").append(String.format("%.2f", contract.getDepositAmount())).append("</div></div>");
        html.append("</div>");

        // Property Details
        html.append("<div class='info-card'><h3>Property</h3>");
        html.append("<div class='info-item'><div class='info-label'>Address</div><div class='info-value'>").append(escapeHtml(property.getStreet())).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>City</div><div class='info-value'>").append(escapeHtml(property.getCity())).append(", ").append(escapeHtml(property.getPostalCode())).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Type</div><div class='info-value'>").append(property.getPropertyType()).append("</div></div>");
        html.append("<div class='info-item'><div class='info-label'>Property ID</div><div class='info-value'>#").append(property.getIdentifier()).append("</div></div>");
        html.append("</div>");

        // Tenant Details
        html.append("<div class='info-card'><h3>Tenant</h3>");
        html.append("<div class='info-item'><div class='info-label'>Name</div><div class='info-value'>").append(escapeHtml(tenant.getFirstName())).append(" ").append(tenant.getLastName() != null ? escapeHtml(tenant.getLastName()) : "").append("</div></div>");
        if (tenant.getEmail() != null) {
            html.append("<div class='info-item'><div class='info-label'>Email</div><div class='info-value'>").append(escapeHtml(tenant.getEmail())).append("</div></div>");
        }
        if (tenant.getPhone() != null) {
            html.append("<div class='info-item'><div class='info-label'>Phone</div><div class='info-value'>").append(escapeHtml(tenant.getPhone())).append("</div></div>");
        }
        html.append("<div class='info-item'><div class='info-label'>Tenant ID</div><div class='info-value'>#").append(tenant.getIdentifier()).append("</div></div>");
        html.append("</div>");

        html.append("</div>");

        // Payment Summary
        html.append("<h2>Payment Summary</h2>");
        html.append("<div class='summary'>");
        html.append("<div class='summary-card paid'><h4>Total Paid</h4><p>").append(contract.getCurrency()).append(" ").append(String.format("%.2f", totalPaid)).append("</p></div>");
        html.append("<div class='summary-card pending'><h4>Total Pending</h4><p>").append(contract.getCurrency()).append(" ").append(String.format("%.2f", totalPending)).append("</p></div>");
        html.append("</div>");

        // Payments Table
        html.append("<h2>Payments (").append(payments.size()).append(")</h2>");
        html.append("<table><thead><tr><th>Due Date</th><th>Amount</th><th>Payment Date</th><th>Status</th></tr></thead><tbody>");

        for (Payment payment : payments) {
            html.append("<tr>");
            html.append("<td>").append(payment.getDueDate()).append("</td>");
            html.append("<td>").append(payment.getCurrency()).append(" ").append(String.format("%.2f", payment.getAmount())).append("</td>");
            html.append("<td>").append(payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "-").append("</td>");
            html.append("<td><span class='status-").append(payment.getStatus().name().toLowerCase()).append("'>").append(payment.getStatus()).append("</span></td>");
            html.append("</tr>");
        }

        html.append("</tbody></table>");
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
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
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

        public TransactionDTO(String id, LocalDate date, String type, String description, String property, String category, BigDecimal amount, String currency) {
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
