package com.buurman.controller;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.util.List;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
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

  @Override
  public Resource getExtensionAddendum(
      String contractIdentifier, String extensionIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    String language = lang.orElse("en");
    byte[] pdf =
        addendumExporter.generate(
            Optional.of(ContractIdentifier.of(contractIdentifier)),
            ContractExtensionIdentifier.of(extensionIdentifier),
            principal.requireTeamId(),
            language);
    return pdfResource("extension-addendum-" + language + ".pdf", pdf);
  }

  @Override
  public Resource getRentIncreaseLetter(
      String contractIdentifier, String extensionIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    String language = lang.orElse("en");
    byte[] pdf =
        rentIncreaseLetterExporter.generate(
            Optional.of(ContractIdentifier.of(contractIdentifier)),
            ContractExtensionIdentifier.of(extensionIdentifier),
            principal.requireTeamId(),
            language);
    return pdfResource("rent-increase-letter-" + language + ".pdf", pdf);
  }

  @Override
  public Resource getRentChangeDocument(
      ContractIdentifier contractIdentifier,
      ContractRentPeriodIdentifier periodIdentifier,
      Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    String language = lang.orElse("en");
    byte[] pdf =
        rentChangeDocumentExporter.generate(
            contractIdentifier, periodIdentifier, principal.requireTeamId(), language);
    return pdfResource("rent-change-" + periodIdentifier.value() + "-" + language + ".pdf", pdf);
  }

  @Override
  public List<DocumentResponse> generateExtensionDocuments(
      String contractIdentifier,
      String extensionIdentifier,
      GenerateExtensionDocumentsRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return extensionDocumentGenerationService.generateAndPersist(
        ContractIdentifier.of(contractIdentifier),
        ContractExtensionIdentifier.of(extensionIdentifier),
        request,
        principal);
  }

  @Override
  public List<DocumentResponse> generateRentChangeDocuments(
      String contractIdentifier,
      String periodIdentifier,
      GenerateRentChangeDocumentsRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentChangeDocumentGenerationService.generateAndPersist(
        ContractIdentifier.of(contractIdentifier),
        ContractRentPeriodIdentifier.of(periodIdentifier),
        request,
        principal);
  }
}
