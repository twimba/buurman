package com.buurman.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Ulid;
import com.buurman.generated.api.BookletsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ExportService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookletController implements BookletsApi {

  private final ExportService exportService;
  private final HttpServletResponse httpServletResponse;

  @Override
  public byte[] exportPropertyBooklet(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=property-booklet.pdf");
    httpServletResponse.setContentType(MediaType.APPLICATION_PDF_VALUE);
    return exportService.generatePropertyBrochurePDF(Ulid.of(propertyIdentifier), principal.requireTeamId());
  }

  @Override
  public byte[] exportTenantBooklet(String tenantIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=tenant-booklet.pdf");
    httpServletResponse.setContentType(MediaType.APPLICATION_PDF_VALUE);
    return exportService.generateTenantReportPDF(Ulid.of(tenantIdentifier), principal.requireTeamId());
  }

  @Override
  public byte[] exportContractBooklet(String contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    httpServletResponse.setHeader(
        "Content-Disposition", "attachment; filename=contract-booklet.pdf");
    httpServletResponse.setContentType(MediaType.APPLICATION_PDF_VALUE);
    return exportService.generateContractReportPDF(Ulid.of(contractIdentifier), principal.requireTeamId());
  }
}
