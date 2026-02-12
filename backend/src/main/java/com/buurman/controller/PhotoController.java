package com.buurman.controller;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.BulkDownloadRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePhotoRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PhotoService;
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
@RequestMapping("/photos")
@Tag(name = "Photos", description = "Photo management endpoints")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    @Operation(summary = "Search and list all photos", description = "Search across all photos with optional filters and pagination")
    public ResponseEntity<PageResponse<PhotoResponse>> getAllPhotos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        PageResponse<PhotoResponse> photos = photoService.searchPhotosPaginated(search, entityType, principal, pageRequest);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{identifier}")
    @Operation(summary = "Get photo metadata", description = "Get detailed metadata for a specific photo")
    public ResponseEntity<PhotoResponse> getPhoto(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        PhotoResponse photo = photoService.getPhoto(identifier, principal);
        return ResponseEntity.ok(photo);
    }

    @GetMapping("/{identifier}/download")
    @Operation(summary = "Get download URL", description = "Get presigned URL for downloading a photo")
    public ResponseEntity<String> getDownloadUrl(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        URL downloadUrl = photoService.getDownloadUrl(identifier, principal);
        return ResponseEntity.ok(downloadUrl.toString());
    }

    @GetMapping("/{identifier}/preview")
    @Operation(summary = "Get preview URL", description = "Get presigned URL for previewing a photo")
    public ResponseEntity<String> getPreviewUrl(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        // For now, preview URL is same as download URL
        // In the future, we could generate thumbnails or lower-res previews
        URL previewUrl = photoService.getDownloadUrl(identifier, principal);
        return ResponseEntity.ok(previewUrl.toString());
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<PhotoResponse> updatePhoto(
            @PathVariable String identifier,
            @RequestBody UpdatePhotoRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PhotoResponse response = photoService.updatePhoto(identifier, request, principal);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{identifier}")
    @Operation(summary = "Delete photo", description = "Soft delete a photo")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {

        photoService.deletePhoto(identifier, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-download")
    @Operation(summary = "Bulk download photos", description = "Download multiple photos as a zip archive (max 50)")
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
