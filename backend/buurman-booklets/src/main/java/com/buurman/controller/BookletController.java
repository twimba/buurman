package com.buurman.controller;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.generated.api.BookletsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;
import com.buurman.service.export.ContractExtensionAddendumExporter;
import com.buurman.service.export.RentIncreaseLetterExporter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookletController implements BookletsApi {

  private final ExportService exportService;
  private final ContractExtensionAddendumExporter addendumExporter;
  private final RentIncreaseLetterExporter rentIncreaseLetterExporter;
  private final HttpServletResponse httpServletResponse;

  @Override
  public byte[] exportPropertyBooklet(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=property-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generatePropertyBrochurePDF(propertyIdentifier, principal.requireTeamId());
  }

  @Override
  public byte[] exportTenantBooklet(ContactIdentifier tenantIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=contact-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generateContactReportPDF(tenantIdentifier, principal.requireTeamId());
  }

  @Override
  public byte[] exportContractBooklet(ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=contract-booklet.pdf");
    httpServletResponse.setContentType(APPLICATION_PDF_VALUE);
    return exportService.generateContractReportPDF(contractIdentifier, principal.requireTeamId());
  }

  @GetMapping("/contracts/{contractId}/extensions/{extensionId}/addendum")
  public ResponseEntity<byte[]> getExtensionAddendum(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf = addendumExporter.generate(extensionId, principal.requireTeamId());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extension-addendum.pdf")
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
        .body(pdf);
  }

  @GetMapping("/contracts/{contractId}/extensions/{extensionId}/rent-increase-letter")
  public ResponseEntity<byte[]> getRentIncreaseLetter(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] pdf = rentIncreaseLetterExporter.generate(extensionId, principal.requireTeamId());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rent-increase-letter.pdf")
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
        .body(pdf);
  }
}
