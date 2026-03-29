package com.buurman.controller;

import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.identifier.DataImportIdentifier;
import com.buurman.dto.request.ImportExecuteRequest;
import com.buurman.dto.request.ImportPreviewRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.DataImportDetailResponse;
import com.buurman.dto.response.ImportExecuteResponse;
import com.buurman.dto.response.ImportPreviewResponse;
import com.buurman.dto.response.ImportRevertResponse;
import com.buurman.dto.response.ImportUploadResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.generated.api.DataImportsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DataImportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DataImportController implements DataImportsApi {

  private final DataImportService dataImportService;

  @Override
  public ImportUploadResponse uploadImportFile(MultipartFile file, Optional<Boolean> headerRow) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dataImportService.uploadFile(file, headerRow.orElse(true), principal);
  }

  @Override
  public ImportPreviewResponse previewImport(ImportPreviewRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dataImportService.previewImport(request, principal);
  }

  @Override
  public ImportExecuteResponse executeImport(ImportExecuteRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dataImportService.executeImport(request, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse listImports(Optional<Integer> page, Optional<Integer> size) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest = PageRequest.of(page.orElse(0), size.orElse(25), null, (String) null);
    return dataImportService.listImports(principal, pageRequest);
  }

  @Override
  public DataImportDetailResponse getImport(DataImportIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dataImportService.getImportDetail(identifier, principal);
  }

  @Override
  public ImportRevertResponse revertImport(DataImportIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dataImportService.revertImport(identifier, principal);
  }

  @Override
  public Resource downloadImportErrorReport(DataImportIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return dataImportService.downloadErrorReport(identifier, principal);
  }
}
