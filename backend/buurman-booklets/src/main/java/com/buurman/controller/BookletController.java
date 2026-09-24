package com.buurman.controller;

import static com.buurman.util.FeatureFlags.GOOGLE_SHEETS_EXPORT;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.GenerateExtensionDocumentsRequest;
import com.buurman.dto.request.GenerateRentChangeDocumentsRequest;
import com.buurman.dto.request.GoogleSheetExportRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.GoogleSheetExportResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.BookletsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.GoogleSheetExportService;
import com.buurman.service.GoogleSheetTitleResolver;
import com.buurman.service.export.ContractExtensionAddendumExporter;
import com.buurman.service.export.DepositStatementExporter;
import com.buurman.service.export.ExtensionDocumentGenerationService;
import com.buurman.service.export.PaymentFormalNoticeExporter;
import com.buurman.service.export.RentChangeDocumentExporter;
import com.buurman.service.export.RentChangeDocumentGenerationService;
import com.buurman.service.export.RentIncreaseLetterExporter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookletController implements BookletsApi {

  private static final Set<String> SUPPORTED_LOCALES =
      Set.of("en", "nl", "de", "es", "fr", "pt", "it", "sv", "fi", "el", "pl", "da", "nb");

  private final ExportService exportService;
  private final GoogleSheetExportService googleSheetExportService;
  private final ContractExtensionAddendumExporter addendumExporter;
  private final RentIncreaseLetterExporter rentIncreaseLetterExporter;
  private final PaymentFormalNoticeExporter paymentFormalNoticeExporter;
  private final DepositStatementExporter depositStatementExporter;
  private final RentChangeDocumentExporter rentChangeDocumentExporter;
  private final ExtensionDocumentGenerationService extensionDocumentGenerationService;
  private final RentChangeDocumentGenerationService rentChangeDocumentGenerationService;
  private final FeatureFlagService featureFlagService;
  private final GoogleSheetTitleResolver titleResolver;
  private final HttpServletResponse httpServletResponse;

  @Override
  public Resource exportPropertyBooklet(
      PropertyIdentifier propertyIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Locale locale = resolveLocale(lang.orElse("en"));
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=property-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return new ByteArrayResource(
        exportService.generatePropertyBrochurePDF(
            propertyIdentifier, principal.requireTeamId(), locale));
  }

  @Override
  public Resource exportContactBooklet(ContactIdentifier contactIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Locale locale = resolveLocale(lang.orElse("en"));
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=contact-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return new ByteArrayResource(
        exportService.generateContactReportPDF(
            contactIdentifier, principal.requireTeamId(), locale));
  }

  @Override
  public Resource exportContactsCsv() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=contacts.csv");
    httpServletResponse.setContentType("text/csv");
    return new ByteArrayResource(exportService.generateContactsCSV(principal.requireTeamId()));
  }

  @Override
  public GoogleSheetExportResponse exportContactsGoogleSheet(GoogleSheetExportRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    if (featureFlagService.isDisabled(GOOGLE_SHEETS_EXPORT, principal)) {
      throw new ForbiddenException("Google Sheets export feature is not available");
    }
    GoogleSheetExport result =
        googleSheetExportService.generateContactsGoogleSheet(
            new GoogleAccessToken(request.getAccessToken()),
            principal.requireTeamId(),
            titleResolver.resolve(principal, "Contacts"));
    return new GoogleSheetExportResponse(result.spreadsheetId(), result.url());
  }

  @Override
  public Resource exportContactsXlsx() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=contacts.xlsx");
    httpServletResponse.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    return new ByteArrayResource(exportService.generateContactsExcel(principal.requireTeamId()));
  }

  // ── Properties ──────────────────────────────────────────────────────────
  @Override
  public Resource exportPropertiesCsv() {
    return downloadCsv(
        "properties.csv",
        () ->
            exportService.generatePropertiesCSV(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public Resource exportPropertiesXlsx() {
    return downloadXlsx(
        "properties.xlsx",
        () ->
            exportService.generatePropertiesExcel(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public GoogleSheetExportResponse exportPropertiesGoogleSheet(GoogleSheetExportRequest request) {
    return runGoogleSheetExport(
        request,
        "Properties",
        (token, principal, title) ->
            googleSheetExportService.generatePropertiesGoogleSheet(
                token, principal.requireTeamId(), title));
  }

  // ── Deposits ────────────────────────────────────────────────────────────
  @Override
  public Resource exportDepositsCsv() {
    return downloadCsv(
        "deposits.csv",
        () ->
            exportService.generateDepositsCSV(SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public Resource exportDepositsXlsx() {
    return downloadXlsx(
        "deposits.xlsx",
        () ->
            exportService.generateDepositsExcel(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public GoogleSheetExportResponse exportDepositsGoogleSheet(GoogleSheetExportRequest request) {
    return runGoogleSheetExport(
        request,
        "Deposits",
        (token, principal, title) ->
            googleSheetExportService.generateDepositsGoogleSheet(
                token, principal.requireTeamId(), title));
  }

  @Override
  public Resource getDepositStatement(
      ContractIdentifier contractIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    String language = lang.orElse("en");
    byte[] pdf =
        depositStatementExporter.generate(contractIdentifier, principal.requireTeamId(), language);
    return pdfResource("deposit-statement-" + language + ".pdf", pdf);
  }

  @Override
  public Resource getPaymentFormalNotice(PaymentIdentifier identifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    String language = lang.orElse("en");
    byte[] pdf =
        paymentFormalNoticeExporter.generate(identifier, principal.requireTeamId(), language);
    return pdfResource("formal-notice-" + identifier.value() + "-" + language + ".pdf", pdf);
  }

  private Resource pdfResource(String filename, byte[] pdf) {
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return new ByteArrayResource(pdf);
  }

  // ── Payments ────────────────────────────────────────────────────────────
  @Override
  public Resource exportPaymentsCsv() {
    return downloadCsv(
        "payments.csv",
        () ->
            exportService.generatePaymentsCSV(SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public Resource exportPaymentsXlsx() {
    return downloadXlsx(
        "payments.xlsx",
        () ->
            exportService.generatePaymentsExcel(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public GoogleSheetExportResponse exportPaymentsGoogleSheet(GoogleSheetExportRequest request) {
    return runGoogleSheetExport(
        request,
        "Payments",
        (token, principal, title) ->
            googleSheetExportService.generatePaymentsGoogleSheet(
                token, principal.requireTeamId(), title));
  }

  // ── Expenses ────────────────────────────────────────────────────────────
  @Override
  public Resource exportExpensesCsv() {
    return downloadCsv(
        "expenses.csv",
        () ->
            exportService.generateExpensesCSV(SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public Resource exportExpensesXlsx() {
    return downloadXlsx(
        "expenses.xlsx",
        () ->
            exportService.generateExpensesExcel(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public GoogleSheetExportResponse exportExpensesGoogleSheet(GoogleSheetExportRequest request) {
    return runGoogleSheetExport(
        request,
        "Expenses",
        (token, principal, title) ->
            googleSheetExportService.generateExpensesGoogleSheet(
                token, principal.requireTeamId(), title));
  }

  // ── Contracts ───────────────────────────────────────────────────────────
  @Override
  public Resource exportContractsCsv() {
    return downloadCsv(
        "contracts.csv",
        () ->
            exportService.generateContractsCSV(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public Resource exportContractsXlsx() {
    return downloadXlsx(
        "contracts.xlsx",
        () ->
            exportService.generateContractsExcel(
                SecurityUtils.getCurrentPrincipal().requireTeamId()));
  }

  @Override
  public GoogleSheetExportResponse exportContractsGoogleSheet(GoogleSheetExportRequest request) {
    return runGoogleSheetExport(
        request,
        "Contracts",
        (token, principal, title) ->
            googleSheetExportService.generateContractsGoogleSheet(
                token, principal.requireTeamId(), title));
  }

  // ── Helpers ────────────────────────────────────────────────────────────
  private Resource downloadCsv(String filename, java.util.function.Supplier<byte[]> generator) {
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);
    httpServletResponse.setContentType("text/csv");
    return new ByteArrayResource(generator.get());
  }

  private Resource downloadXlsx(String filename, java.util.function.Supplier<byte[]> generator) {
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + filename);
    httpServletResponse.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    return new ByteArrayResource(generator.get());
  }

  @FunctionalInterface
  private interface GoogleSheetExporterFn {
    GoogleSheetExport run(GoogleAccessToken token, UserPrincipal principal, String title);
  }

  private GoogleSheetExportResponse runGoogleSheetExport(
      GoogleSheetExportRequest request, String entityLabel, GoogleSheetExporterFn fn) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    if (featureFlagService.isDisabled(GOOGLE_SHEETS_EXPORT, principal)) {
      throw new ForbiddenException("Google Sheets export feature is not available");
    }
    GoogleSheetExport result =
        fn.run(
            new GoogleAccessToken(request.getAccessToken()),
            principal,
            titleResolver.resolve(principal, entityLabel));
    return new GoogleSheetExportResponse(result.spreadsheetId(), result.url());
  }

  @Override
  public Resource exportContractBooklet(
      ContractIdentifier contractIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Locale locale = resolveLocale(lang.orElse("en"));
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=contract-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return new ByteArrayResource(
        exportService.generateContractReportPDF(
            contractIdentifier, principal.requireTeamId(), locale));
  }

  private static Locale resolveLocale(String lang) {
    if (SUPPORTED_LOCALES.contains(lang)) {
      return Locale.forLanguageTag(lang);
    }
    return Locale.ENGLISH;
  }

  // ── One-page summary cards (A4 landscape) ─────────────────────────────────
  @Override
  public Resource getContractSummary(ContractIdentifier contractId, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        exportService.generateContractSummaryPDF(
            contractId, principal.requireTeamId(), resolveLocale(lang.orElse("en")));
    return summaryResource("contract-" + contractId.value(), pdf);
  }

  @Override
  public Resource getPropertySummary(PropertyIdentifier propertyId, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        exportService.generatePropertySummaryPDF(
            propertyId, principal.requireTeamId(), resolveLocale(lang.orElse("en")));
    return summaryResource("property-" + propertyId.value(), pdf);
  }

  @Override
  public Resource getContactSummary(ContactIdentifier contactId, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        exportService.generateContactSummaryPDF(
            contactId, principal.requireTeamId(), resolveLocale(lang.orElse("en")));
    return summaryResource("contact-" + contactId.value(), pdf);
  }

  private Resource summaryResource(String name, byte[] pdf) {
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=" + name + "-summary.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return new ByteArrayResource(pdf);
  }

  @GetMapping("/contracts/{contractId}/extensions/{extensionId}/addendum")
  public ResponseEntity<byte[]> getExtensionAddendum(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId,
      @RequestParam(defaultValue = "en") String lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        addendumExporter.generate(
            Optional.of(contractId), extensionId, principal.requireTeamId(), lang);
    String filename = "extension-addendum-" + lang + ".pdf";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
        .body(pdf);
  }

  @GetMapping("/contracts/{contractId}/extensions/{extensionId}/rent-increase-letter")
  public ResponseEntity<byte[]> getRentIncreaseLetter(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId,
      @RequestParam(defaultValue = "en") String lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        rentIncreaseLetterExporter.generate(
            Optional.of(contractId), extensionId, principal.requireTeamId(), lang);
    String filename = "rent-increase-letter-" + lang + ".pdf";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
        .body(pdf);
  }

  @GetMapping("/contracts/{contractId}/rent-periods/{periodId}/document")
  public ResponseEntity<byte[]> getRentChangeDocument(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractRentPeriodIdentifier periodId,
      @RequestParam(defaultValue = "en") String lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        rentChangeDocumentExporter.generate(contractId, periodId, principal.requireTeamId(), lang);
    String filename = "rent-change-" + periodId.value() + "-" + lang + ".pdf";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
        .body(pdf);
  }

  @PostMapping("/contracts/{contractId}/extensions/{extensionId}/generate-documents")
  public List<DocumentResponse> generateExtensionDocuments(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId,
      @RequestBody GenerateExtensionDocumentsRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return extensionDocumentGenerationService.generateAndPersist(
        contractId, extensionId, request, principal);
  }

  @PostMapping("/contracts/{contractId}/rent-periods/{periodId}/generate-documents")
  public List<DocumentResponse> generateRentChangeDocuments(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractRentPeriodIdentifier periodId,
      @RequestBody GenerateRentChangeDocumentsRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentChangeDocumentGenerationService.generateAndPersist(
        contractId, periodId, request, principal);
  }
}
