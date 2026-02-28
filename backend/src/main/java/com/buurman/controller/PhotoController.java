package com.buurman.controller;

import java.net.URL;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.BulkDownloadRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePhotoRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PhotoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/photos")
@Tag(name = "Photos", description = "Photo management endpoints")
@RequiredArgsConstructor
public class PhotoController {

  private final PhotoService photoService;

  @GetMapping
  @Operation(
      summary = "Search and list all photos",
      description = "Search across all photos with optional filters and pagination")
  public ResponseEntity<PageResponse<PhotoResponse>> getAllPhotos(
      @Parameter(description = "Search term") @RequestParam Optional<String> search,
      @Parameter(description = "Filter by entity type") @RequestParam Optional<String> entityType,
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
    PageResponse<PhotoResponse> photos =
        photoService.searchPhotosPaginated(
            search.orElse(null), entityType.orElse(null), principal, pageRequest);
    return ResponseEntity.ok(photos);
  }

  @GetMapping("/{identifier}")
  @Operation(
      summary = "Get photo metadata",
      description = "Get detailed metadata for a specific photo")
  public ResponseEntity<PhotoResponse> getPhoto(
      @Parameter(description = "Photo ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {

    PhotoResponse photo = photoService.getPhoto(identifier, principal);
    return ResponseEntity.ok(photo);
  }

  @GetMapping("/{identifier}/download")
  @Operation(
      summary = "Get download URL",
      description = "Get presigned URL for downloading a photo")
  public ResponseEntity<String> getDownloadUrl(
      @Parameter(description = "Photo ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {

    URL downloadUrl = photoService.getDownloadUrl(identifier, principal);
    return ResponseEntity.ok(downloadUrl.toString());
  }

  @GetMapping("/{identifier}/preview")
  @Operation(summary = "Get preview URL", description = "Get presigned URL for previewing a photo")
  public ResponseEntity<String> getPreviewUrl(
      @Parameter(description = "Photo ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {

    // For now, preview URL is same as download URL
    // In the future, we could generate thumbnails or lower-res previews
    URL previewUrl = photoService.getDownloadUrl(identifier, principal);
    return ResponseEntity.ok(previewUrl.toString());
  }

  @PutMapping("/{identifier}")
  public ResponseEntity<PhotoResponse> updatePhoto(
      @Parameter(description = "Photo ULID identifier") @PathVariable String identifier,
      @RequestBody UpdatePhotoRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    PhotoResponse response = photoService.updatePhoto(identifier, request, principal);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{identifier}")
  @Operation(summary = "Delete photo", description = "Soft delete a photo")
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ResponseEntity<Void> deletePhoto(
      @Parameter(description = "Photo ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {

    photoService.deletePhoto(identifier, principal);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/bulk-download")
  @Operation(
      summary = "Bulk download photos",
      description = "Download multiple photos as a zip archive (max 50)")
  public ResponseEntity<ByteArrayResource> bulkDownload(
      @Valid @RequestBody BulkDownloadRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {

    byte[] zipData = photoService.bulkDownload(request.documentIdentifiers(), principal);

    ByteArrayResource resource = new ByteArrayResource(zipData);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=photos.zip")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(zipData.length)
        .body(resource);
  }
}
