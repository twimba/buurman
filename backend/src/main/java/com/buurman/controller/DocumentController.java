package com.buurman.controller;

import com.buurman.dto.response.DocumentResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @Operation(summary = "Search and list all documents", description = "Search across all documents with optional filters")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String entityType,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<DocumentResponse> documents = documentService.searchDocuments(search, entityType, principal);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata", description = "Get detailed metadata for a specific document")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        DocumentResponse document = documentService.getDocument(id, principal);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Get download URL", description = "Get presigned URL for downloading a document")
    public ResponseEntity<String> getDownloadUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        URL downloadUrl = documentService.getDownloadUrl(id, principal);
        return ResponseEntity.ok(downloadUrl.toString());
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Get preview URL", description = "Get presigned URL for previewing a document (PDFs and images)")
    public ResponseEntity<String> getPreviewUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        // For now, preview URL is same as download URL
        // In the future, we could generate thumbnails or lower-res previews
        URL previewUrl = documentService.getDownloadUrl(id, principal);
        return ResponseEntity.ok(previewUrl.toString());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Soft delete a document")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        documentService.deleteDocument(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-download")
    @Operation(summary = "Bulk download documents", description = "Download multiple documents as a zip archive")
    public ResponseEntity<ByteArrayResource> bulkDownload(
            @RequestBody List<UUID> documentIds,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] zipData = documentService.bulkDownload(documentIds, principal);

        ByteArrayResource resource = new ByteArrayResource(zipData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documents.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipData.length)
                .body(resource);
    }
}
