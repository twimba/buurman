package com.buurman.controller;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.util.List;
import java.util.Optional;

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

import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.GenerateExtensionDocumentsRequest;
import com.buurman.dto.request.GenerateRentChangeDocumentsRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.generated.api.LettersApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.letters.ContractExtensionAddendumExporter;
import com.buurman.service.letters.DepositStatementExporter;
import com.buurman.service.letters.ExtensionDocumentGenerationService;
import com.buurman.service.letters.PaymentFormalNoticeExporter;
import com.buurman.service.letters.RentChangeDocumentExporter;
import com.buurman.service.letters.RentChangeDocumentGenerationService;
import com.buurman.service.letters.RentIncreaseLetterExporter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Tenant-facing letters: on-demand PDFs and generate-and-file flows. */
@RestController
@RequiredArgsConstructor
public class LetterController implements LettersApi {

  private final ContractExtensionAddendumExporter addendumExporter;
  private final RentIncreaseLetterExporter rentIncreaseLetterExporter;
  private final PaymentFormalNoticeExporter paymentFormalNoticeExporter;
  private final DepositStatementExporter depositStatementExporter;
  private final RentChangeDocumentExporter rentChangeDocumentExporter;
  private final ExtensionDocumentGenerationService extensionDocumentGenerationService;
  private final RentChangeDocumentGenerationService rentChangeDocumentGenerationService;
  private final HttpServletResponse httpServletResponse;

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

  @GetMapping("/contracts/{contractId}/extensions/{extensionId}/addendum")
  public ResponseEntity<byte[]> getExtensionAddendum(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId,
      @RequestParam(defaultValue = "en") String lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        addendumExporter.generate(
            Optional.of(contractId), extensionId, principal.requireTeamId(), lang);
    return pdfEntity("extension-addendum-" + lang + ".pdf", pdf);
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
    return pdfEntity("rent-increase-letter-" + lang + ".pdf", pdf);
  }

  @GetMapping("/contracts/{contractId}/rent-periods/{periodId}/document")
  public ResponseEntity<byte[]> getRentChangeDocument(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractRentPeriodIdentifier periodId,
      @RequestParam(defaultValue = "en") String lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf =
        rentChangeDocumentExporter.generate(contractId, periodId, principal.requireTeamId(), lang);
    return pdfEntity("rent-change-" + periodId.value() + "-" + lang + ".pdf", pdf);
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

  private static ResponseEntity<byte[]> pdfEntity(String filename, byte[] pdf) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
        .body(pdf);
  }
}
