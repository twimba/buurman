package com.buurman.controller;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_PDF;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/booklets")
@Tag(name = "Booklets", description = "Entity booklet PDF exports")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BookletController {

  private final ExportService exportService;

  @Operation(
      summary = "Export property booklet to PDF",
      description = "Download detailed property booklet as PDF")
  @GetMapping("/property/{propertyIdentifier}")
  public ResponseEntity<byte[]> exportPropertyBooklet(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {

    byte[] pdf =
        exportService.generatePropertyBrochurePDF(propertyIdentifier, principal.getTeamId());

    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment; filename=property-booklet.pdf")
        .contentType(APPLICATION_PDF)
        .body(pdf);
  }

  @Operation(
      summary = "Export tenant booklet to PDF",
      description = "Download detailed tenant booklet as PDF")
  @GetMapping("/tenant/{tenantIdentifier}")
  public ResponseEntity<byte[]> exportTenantBooklet(
      @PathVariable String tenantIdentifier, @AuthenticationPrincipal UserPrincipal principal) {

    byte[] pdf = exportService.generateTenantReportPDF(tenantIdentifier, principal.getTeamId());

    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment; filename=tenant-booklet.pdf")
        .contentType(APPLICATION_PDF)
        .body(pdf);
  }

  @Operation(
      summary = "Export contract booklet to PDF",
      description = "Download detailed contract booklet as PDF")
  @GetMapping("/contract/{contractIdentifier}")
  public ResponseEntity<byte[]> exportContractBooklet(
      @PathVariable String contractIdentifier, @AuthenticationPrincipal UserPrincipal principal) {

    byte[] pdf = exportService.generateContractReportPDF(contractIdentifier, principal.getTeamId());

    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment; filename=contract-booklet.pdf")
        .contentType(APPLICATION_PDF)
        .body(pdf);
  }
}
