package com.buurman.controller;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.GenerateExtensionDocumentsRequest;
import com.buurman.dto.request.GenerateRentChangeDocumentsRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.generated.api.BookletsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.export.ContractExtensionAddendumExporter;
import com.buurman.service.export.ExtensionDocumentGenerationService;
import com.buurman.service.export.RentChangeDocumentExporter;
import com.buurman.service.export.RentChangeDocumentGenerationService;
import com.buurman.service.export.RentIncreaseLetterExporter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookletController implements BookletsApi {

  private static final Set<String> SUPPORTED_LOCALES =
      Set.of("en", "nl", "de", "es", "fr", "pt", "it", "sv", "fi", "el", "pl", "da", "nb");

  private final ExportService exportService;
  private final ContractExtensionAddendumExporter addendumExporter;
  private final RentIncreaseLetterExporter rentIncreaseLetterExporter;
  private final RentChangeDocumentExporter rentChangeDocumentExporter;
  private final ExtensionDocumentGenerationService extensionDocumentGenerationService;
  private final RentChangeDocumentGenerationService rentChangeDocumentGenerationService;
  private final HttpServletRequest httpServletRequest;
  private final HttpServletResponse httpServletResponse;

  @Override
  public byte[] exportPropertyBooklet(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Locale locale = resolveLocale(
        Optional.ofNullable(httpServletRequest.getParameter("lang")).orElse("en"));
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=property-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generatePropertyBrochurePDF(
        propertyIdentifier, principal.requireTeamId(), locale);
  }

  @Override
  public byte[] exportContactBooklet(ContactIdentifier contactIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Locale locale = resolveLocale(
        Optional.ofNullable(httpServletRequest.getParameter("lang")).orElse("en"));
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=contact-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generateContactReportPDF(
        contactIdentifier, principal.requireTeamId(), locale);
  }

  @Override
  public byte[] exportContactsCsv() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=contacts.csv");
    httpServletResponse.setContentType("text/csv");
    return exportService.generateContactsCSV(principal.requireTeamId());
  }

  @Override
  public byte[] exportContactsXlsx() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=contacts.xlsx");
    httpServletResponse.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    return exportService.generateContactsExcel(principal.requireTeamId());
  }

  @Override
  public byte[] exportContractBooklet(ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Locale locale = resolveLocale(
        Optional.ofNullable(httpServletRequest.getParameter("lang")).orElse("en"));
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=contract-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generateContractReportPDF(
        contractIdentifier, principal.requireTeamId(), locale);
  }

  private static Locale resolveLocale(String lang) {
    if (SUPPORTED_LOCALES.contains(lang)) {
      return Locale.forLanguageTag(lang);
    }
    return Locale.ENGLISH;
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
