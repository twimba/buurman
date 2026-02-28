package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyStatus;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties")
@Tag(name = "Properties", description = "Property management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyController {

  private final PropertyService propertyService;

  @Operation(summary = "Create property", description = "Create a new property (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyResponse createProperty(
      @Valid @RequestBody CreatePropertyRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.createProperty(request, principal);
  }

  @Operation(
      summary = "List properties",
      description =
          "Get all properties with optional status, category, and search filters with pagination")
  @GetMapping
  public PageResponse<PropertyResponse> getProperties(
      @Parameter(description = "Filter by status") @RequestParam Optional<PropertyStatus> status,
      @Parameter(description = "Filter by category") @RequestParam
          Optional<PropertyCategory> category,
      @Parameter(description = "Search query") @RequestParam Optional<String> query,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size", example = "25") @RequestParam(defaultValue = "25")
          Integer size,
      @Parameter(description = "Sort field name", example = "createdAt") @RequestParam
          Optional<String> sort,
      @Parameter(description = "Sort direction", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {
    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return propertyService.getPropertiesPaginated(
        principal,
        status.map(PropertyStatus::name).orElse(null),
        category.map(PropertyCategory::name).orElse(null),
        query.orElse(null),
        pageRequest);
  }

  @Operation(summary = "Get property", description = "Get property details by identifier")
  @GetMapping("/{identifier}")
  public PropertyResponse getProperty(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.getProperty(identifier, principal);
  }

  @Operation(summary = "Update property", description = "Update property details (Admin/Editor)")
  @PutMapping("/{identifier}")
  public PropertyResponse updateProperty(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Valid @RequestBody UpdatePropertyRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.updateProperty(identifier, request, principal);
  }

  @Operation(summary = "Delete property", description = "Soft delete a property (Admin only)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteProperty(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    propertyService.deleteProperty(identifier, principal);
  }

  @Operation(
      summary = "Upload document",
      description = "Upload a document for a property (Admin/Editor)")
  @PostMapping("/{identifier}/documents")
  @ResponseStatus(CREATED)
  public DocumentResponse uploadDocument(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
      @Parameter(description = "Document title") @RequestParam Optional<String> title,
      @Parameter(description = "Additional notes") @RequestParam Optional<String> notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.uploadDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Operation(summary = "List documents", description = "Get all documents for a property")
  @GetMapping("/{identifier}/documents")
  public List<DocumentResponse> getDocuments(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.getDocuments(identifier, principal);
  }

  @Operation(
      summary = "Get download URL",
      description = "Get presigned download URL for a document")
  @GetMapping("/documents/{documentIdentifier}/download")
  public Map<String, String> getDownloadUrl(
      @Parameter(description = "Document ULID identifier") @PathVariable String documentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.getDownloadUrl(documentIdentifier, principal);
  }

  @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
  @DeleteMapping("/documents/{documentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDocument(
      @Parameter(description = "Document ULID identifier") @PathVariable String documentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    propertyService.deleteDocument(documentIdentifier, principal);
  }

  @Operation(summary = "Get audit log", description = "Get audit history for a property")
  @GetMapping("/{identifier}/audit-log")
  public List<RecentActivityResponse> getPropertyAuditLog(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.getAuditLog(identifier, principal);
  }

  @Operation(summary = "List photos", description = "Get all photos for a property")
  @GetMapping("/{identifier}/photos")
  public List<PhotoResponse> getPhotos(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.getPhotos(identifier, principal);
  }

  @Operation(summary = "Upload photo", description = "Upload a photo for a property (Admin/Editor)")
  @PostMapping("/{identifier}/photos")
  @ResponseStatus(CREATED)
  public PhotoResponse uploadPhoto(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
      @Parameter(description = "Document title") @RequestParam Optional<String> title,
      @Parameter(description = "Additional notes") @RequestParam Optional<String> notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.uploadPhoto(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Operation(
      summary = "Set main photo",
      description = "Set a photo as the main photo for a property (Admin/Editor)")
  @PutMapping("/{identifier}/photos/{photoIdentifier}/set-main")
  public PhotoResponse setMainPhoto(
      @Parameter(description = "Property ULID identifier") @PathVariable String identifier,
      @Parameter(description = "Photo ULID identifier") @PathVariable String photoIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return propertyService.setMainPhoto(identifier, photoIdentifier, principal);
  }
}
