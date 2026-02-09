package com.buurman.controller;

import com.buurman.dto.request.BulkDownloadRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateDocumentRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @Operation(summary = "Search and list all documents", description = "Search across all documents with optional filters and pagination")
    public ResponseEntity<PageResponse<DocumentResponse>> getAllDocuments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        PageResponse<DocumentResponse> documents = documentService.searchDocumentsPaginated(search, entityType, principal, pageRequest);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get document metadata", description = "Get detailed metadata for a specific document")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        DocumentResponse document = documentService.getDocument(identifier, principal);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{identifier}/download")
    @Operation(summary = "Get download URL", description = "Get presigned URL for downloading a document")
    public ResponseEntity<String> getDownloadUrl(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        URL downloadUrl = documentService.getDownloadUrl(identifier, principal);
        return ResponseEntity.ok(downloadUrl.toString());
    }

    @GetMapping("/{identifier}/preview")
    @Operation(summary = "Get preview URL", description = "Get presigned URL for previewing a document (PDFs and images)")
    public ResponseEntity<String> getPreviewUrl(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        // For now, preview URL is same as download URL
        // In the future, we could generate thumbnails or lower-res previews
        URL previewUrl = documentService.getDownloadUrl(identifier, principal);
        return ResponseEntity.ok(previewUrl.toString());
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable String identifier,
            @RequestBody UpdateDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse response = documentService.updateDocument(identifier, request, principal);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Delete document", description = "Soft delete a document")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        documentService.deleteDocument(identifier, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-download")
    @Operation(summary = "Bulk download documents", description = "Download multiple documents as a zip archive (max 50)")
    public ResponseEntity<ByteArrayResource> bulkDownload(
            @Valid @RequestBody BulkDownloadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] zipData = documentService.bulkDownload(request.documentIdentifiers(), principal);

        ByteArrayResource resource = new ByteArrayResource(zipData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documents.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipData.length)
                .body(resource);
    }
}
