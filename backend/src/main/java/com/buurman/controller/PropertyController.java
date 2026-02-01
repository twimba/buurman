package com.buurman.controller;

import com.buurman.domain.Property;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.DocumentService;
import com.buurman.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@Tag(name = "Properties", description = "Property management")
@SecurityRequirement(name = "bearer-jwt")
public class PropertyController {

    private final PropertyService propertyService;
    private final DocumentService documentService;

    public PropertyController(PropertyService propertyService, DocumentService documentService) {
        this.propertyService = propertyService;
        this.documentService = documentService;
    }

    @Operation(summary = "Create property", description = "Create a new property (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyResponse createProperty(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return propertyService.createProperty(request, principal);
    }

    @Operation(summary = "List properties", description = "Get all properties with optional status filter")
    @GetMapping
    public List<PropertyResponse> getProperties(
            @RequestParam(required = false) Property.PropertyStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return propertyService.getProperties(principal, status);
    }

    @Operation(summary = "Get property", description = "Get property details by ID")
    @GetMapping("/{id}")
    public PropertyResponse getProperty(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return propertyService.getProperty(id, principal);
    }

    @Operation(summary = "Update property", description = "Update property details (Admin/Editor)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyResponse updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return propertyService.updateProperty(id, request, principal);
    }

    @Operation(summary = "Delete property", description = "Soft delete a property (Admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteProperty(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        propertyService.deleteProperty(id, principal);
    }

    @Operation(summary = "Upload document", description = "Upload a document for a property (Admin/Editor)")
    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public DocumentResponse uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.uploadDocument(file, "PROPERTY", id, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for a property")
    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.getDocuments("PROPERTY", id, principal);
    }

    @Operation(summary = "Get download URL", description = "Get presigned download URL for a document")
    @GetMapping("/documents/{documentId}/download")
    public Map<String, String> getDownloadUrl(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        URL url = documentService.getDownloadUrl(documentId, principal);
        return Map.of("url", url.toString());
    }

    @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.deleteDocument(documentId, principal);
    }
}
