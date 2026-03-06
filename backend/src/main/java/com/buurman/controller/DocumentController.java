package com.buurman.controller;

import java.net.URL;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.dto.request.BulkDownloadRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateDocumentRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.generated.api.DocumentsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentsApi {

  private final DocumentService documentService;
  private final HttpServletResponse httpServletResponse;

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getAllDocuments(
      Optional<String> search,
      Optional<String> entityType,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return documentService.searchDocumentsPaginated(
        search.orElse(null), entityType.orElse(null), principal, pageRequest);
  }

  @Override
  public DocumentResponse getDocument(DocumentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return documentService.getDocument(identifier, principal);
  }

  @Override
  public String getDocumentDownloadUrl(DocumentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL downloadUrl = documentService.getDownloadUrl(identifier, principal);
    return downloadUrl.toString();
  }

  @Override
  public String getDocumentPreviewUrl(DocumentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // For now, preview URL is same as download URL
    URL previewUrl = documentService.getDownloadUrl(identifier, principal);
    return previewUrl.toString();
  }

  @Override
  public DocumentResponse updateDocument(
      DocumentIdentifier identifier, UpdateDocumentRequest updateDocumentRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return documentService.updateDocument(identifier, updateDocumentRequest, principal);
  }

  @Override
  public void deleteDocument(DocumentIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    documentService.deleteDocument(identifier, principal);
  }

  @Override
  public Resource bulkDownloadDocuments(BulkDownloadRequest bulkDownloadRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    byte[] zipData =
        documentService.bulkDownload(bulkDownloadRequest.documentIdentifiers(), principal);
    httpServletResponse.setHeader("Content-Disposition", "attachment; filename=documents.zip");
    httpServletResponse.setContentType("application/octet-stream");
    return new ByteArrayResource(zipData);
  }
}
