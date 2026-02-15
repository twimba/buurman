package com.buurman.service;

import com.buurman.domain.Photo;
import com.buurman.exception.ExternalServiceException;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PhotoResponse;
import com.buurman.mapper.PhotoMapper;
import com.buurman.config.models.AppProperties;
import com.buurman.repository.PhotoRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.dto.request.UpdatePhotoRequest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PhotoService {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);

    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    private final PhotoMapper photoMapper;
    private final AuditService auditService;
    private final MetricsService metricsService;
    private final long maxFileSize;
    private final List<String> allowedMimeTypes;

    public PhotoService(
            PhotoRepository photoRepository,
            S3StorageService s3StorageService,
            PhotoMapper photoMapper,
            AuditService auditService,
            MetricsService metricsService,
            AppProperties appProperties) {
        this.photoRepository = photoRepository;
        this.s3StorageService = s3StorageService;
        this.photoMapper = photoMapper;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.maxFileSize = appProperties.documents().maxFileSize();
        this.allowedMimeTypes = appProperties.documents().allowedMimeTypes();
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PhotoResponse uploadPhoto(
            MultipartFile file,
            String entityType,
            UUID entityId,
            String entityIdentifier,
            String title,
            String notes,
            UserPrincipal principal) {

        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + " MB");
        }

        // Validate MIME type from Content-Type header
        String mimeType = file.getContentType();
        if (mimeType == null || !allowedMimeTypes.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed types: " + String.join(", ", allowedMimeTypes));
        }

        // Validate that MIME type is an image
        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Only image files are allowed for photos. Received: " + mimeType);
        }

        // Validate MIME type from file content (magic bytes) to prevent spoofed Content-Type
        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            String detectedType = URLConnection.guessContentTypeFromStream(bis);
            if (detectedType != null && !allowedMimeTypes.contains(detectedType)) {
                throw new IllegalArgumentException(
                        "File content does not match an allowed type. Detected: " + detectedType);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file content for validation");
        }

        // Upload to S3
        String fileKey = s3StorageService.uploadFile(file, principal.getTeamIdentifier(), entityType, entityIdentifier);

        // Save photo metadata
        Photo photo = new Photo();
        photo.setTeamId(principal.getTeamId());
        photo.setEntityType(entityType);
        photo.setEntityId(entityId);
        photo.setFileKey(fileKey);
        photo.setFileName(file.getOriginalFilename());
        photo.setFileSize(file.getSize());
        photo.setMimeType(mimeType);
        photo.setTitle(title);
        photo.setNotes(notes);
        photo.setIsMainPhoto(false);
        photo.setUploadedBy(principal.getUserId());

        Photo savedPhoto = photoRepository.save(photo);

        metricsService.incrementCounter("photo.upload.total",
                "entity_type", entityType);
        metricsService.recordHistogram("photo.upload.bytes", file.getSize(),
                "entity_type", entityType);

        log.info("Photo uploaded: {} for entity {}/{}",
                savedPhoto.getIdentifier(), entityType, entityId);

        // Log to audit trail for the parent entity
        java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
        changedFields.put("photoAdded", savedPhoto.getFileName());
        if (savedPhoto.getTitle() != null && !savedPhoto.getTitle().isEmpty()) {
            changedFields.put("title", savedPhoto.getTitle());
        }

        auditService.logUpdate(
                principal.getTeamId(),
                entityType.toUpperCase(),
                entityId,
                principal.getUserId(),
                java.util.Map.of("photoCount", "unchanged"),
                java.util.Map.of("photoCount", "increased"),
                changedFields
        );

        return toResponseWithDownloadUrl(savedPhoto);
    }

    public List<PhotoResponse> getPhotos(String entityType, UUID entityId, UserPrincipal principal) {
        List<Photo> photos = photoRepository.findByEntityAndTeamId(
                entityType, entityId, principal.getTeamId());
        return photos.stream()
                .map(this::toResponseWithDownloadUrl)
                .toList();
    }

    public PhotoResponse getPhoto(String identifier, UserPrincipal principal) {
        Photo photo = photoRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        return toResponseWithDownloadUrl(photo);
    }

    public URL getDownloadUrl(String identifier, UserPrincipal principal) {
        Photo photo = photoRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        metricsService.incrementCounter("photo.download.total",
                "entity_type", photo.getEntityType());

        return s3StorageService.generatePresignedUrl(photo.getFileKey());
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void deletePhoto(String identifier, UserPrincipal principal) {
        Photo photo = photoRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        // Soft delete in database
        photoRepository.softDeleteByIdAndTeamId(photo.getId(), principal.getTeamId());

        // Delete from S3
        s3StorageService.deleteFile(photo.getFileKey());

        metricsService.incrementCounter("photo.delete.total",
                "entity_type", photo.getEntityType());

        log.info("Photo deleted: {}", photo.getIdentifier());

        // Log to audit trail for the parent entity
        java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
        changedFields.put("photoRemoved", photo.getFileName());
        if (photo.getTitle() != null && !photo.getTitle().isEmpty()) {
            changedFields.put("title", photo.getTitle());
        }

        auditService.logUpdate(
                principal.getTeamId(),
                photo.getEntityType().toUpperCase(),
                photo.getEntityId(),
                principal.getUserId(),
                java.util.Map.of("photoCount", "unchanged"),
                java.util.Map.of("photoCount", "decreased"),
                changedFields
        );
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PhotoResponse updatePhoto(String identifier, UpdatePhotoRequest request, UserPrincipal principal) {
        Photo photo = photoRepository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        String oldTitle = photo.getTitle();
        String oldNotes = photo.getNotes();

        photo.setTitle(request.title());
        photo.setNotes(request.notes());

        photoRepository.save(photo);

        // Build clean audit data comparing only title and notes
        Map<String, Object> changedFields = new java.util.HashMap<>();
        Map<String, Object> oldValues = new java.util.HashMap<>();
        Map<String, Object> newValues = new java.util.HashMap<>();

        if (!java.util.Objects.equals(oldTitle, request.title())) {
            changedFields.put("title", request.title());
            oldValues.put("title", oldTitle);
            newValues.put("title", request.title());
        }
        if (!java.util.Objects.equals(oldNotes, request.notes())) {
            changedFields.put("notes", request.notes());
            oldValues.put("notes", oldNotes);
            newValues.put("notes", request.notes());
        }

        if (!changedFields.isEmpty()) {
            // Add marker so the audit description identifies this as a photo edit
            changedFields.put("photoEdited", photo.getFileName());
            oldValues.put("fileName", photo.getFileName());
            newValues.put("fileName", photo.getFileName());

            auditService.logUpdate(
                    principal.getTeamId(),
                    photo.getEntityType().toUpperCase(),
                    photo.getEntityId(),
                    principal.getUserId(),
                    oldValues,
                    newValues,
                    changedFields
            );
        }

        return toResponseWithDownloadUrl(photo);
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PhotoResponse setMainPhoto(UUID photoId, String entityType, UUID entityId, UserPrincipal principal) {
        // Verify the photo exists and belongs to the team
        Photo photo = photoRepository.findByIdAndTeamId(photoId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        // Verify it belongs to the correct entity
        if (!entityType.equals(photo.getEntityType()) || !entityId.equals(photo.getEntityId())) {
            throw new IllegalArgumentException("Photo does not belong to this entity");
        }

        // Unset any existing main photo for this entity
        photoRepository.unsetMainPhotoForEntity(entityType, entityId, principal.getTeamId());

        // Set this photo as main
        photo.setIsMainPhoto(true);
        Photo savedPhoto = photoRepository.save(photo);

        log.info("Main photo set: {} for entity {}/{}", photoId, entityType, entityId);

        return toResponseWithDownloadUrl(savedPhoto);
    }

    public PageResponse<PhotoResponse> searchPhotosPaginated(String search, String entityType, UserPrincipal principal, PageRequest pageRequest) {
        PaginatedResult<Photo> result = photoRepository.findAllByTeamIdPaginated(
                principal.getTeamId(), search, entityType, pageRequest);
        List<PhotoResponse> responses = result.items().stream()
                .map(this::toResponseWithDownloadUrl)
                .toList();
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    private PhotoResponse toResponseWithDownloadUrl(Photo photo) {
        PhotoResponse response = photoMapper.toResponse(photo);
        String downloadUrl = s3StorageService.generatePresignedUrl(photo.getFileKey()).toString();

        return new PhotoResponse(
                response.identifier(),
                response.entityType(),
                response.entityIdentifier(),
                response.fileKey(),
                response.fileName(),
                response.fileSize(),
                response.mimeType(),
                response.title(),
                response.notes(),
                response.isMainPhoto(),
                response.uploadedAt(),
                downloadUrl
        );
    }

    public byte[] bulkDownload(List<String> photoIdentifiers, UserPrincipal principal) {
        if (photoIdentifiers == null || photoIdentifiers.isEmpty()) {
            throw new IllegalArgumentException("No photos selected for download");
        }

        // Fetch all photos by identifiers
        List<Photo> photos = photoRepository.findByIdentifiersAndTeamId(photoIdentifiers, principal.getTeamId());

        if (photos.isEmpty()) {
            throw new IllegalArgumentException("No photos found");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Photo photo : photos) {
                try {
                    // Download file from S3
                    InputStream fileStream = s3StorageService.downloadFile(photo.getFileKey());

                    // Create zip entry with unique filename
                    String fileName = sanitizeFilename(photo.getFileName());
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zos.putNextEntry(zipEntry);

                    // Copy file to zip
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fileStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                    fileStream.close();

                    log.debug("Added photo to zip: {}", fileName);
                } catch (Exception e) {
                    log.error("Failed to add photo {} to zip: {}", photo.getIdentifier(), e.getMessage());
                    // Continue with other photos even if one fails
                }
            }

            zos.finish();
            metricsService.incrementCounter("photo.bulk.download.total");
            log.info("Created zip archive with {} photos for team {}", photos.size(), principal.getTeamId());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to create zip archive: {}", e.getMessage());
            throw new ExternalServiceException("Failed to create zip archive", e);
        }
    }

    private String sanitizeFilename(String filename) {
        // Remove any path separators and invalid characters
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_");
    }
}
