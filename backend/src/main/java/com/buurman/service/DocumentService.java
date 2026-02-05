package com.buurman.service;

import com.buurman.domain.Document;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final DocumentMapper documentMapper;
    private final AuditService auditService;
    private final long maxFileSize;
    private final List<String> allowedMimeTypes;

    public DocumentService(
            DocumentRepository documentRepository,
            S3StorageService s3StorageService,
            DocumentMapper documentMapper,
            AuditService auditService,
            @Value("${app.documents.max-file-size}") long maxFileSize,
            @Value("${app.documents.allowed-mime-types}") String allowedMimeTypesStr) {
        this.documentRepository = documentRepository;
        this.s3StorageService = s3StorageService;
        this.documentMapper = documentMapper;
        this.auditService = auditService;
        this.maxFileSize = maxFileSize;
        this.allowedMimeTypes = Arrays.asList(allowedMimeTypesStr.split(","));
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public DocumentResponse uploadDocument(
            MultipartFile file,
            String entityType,
            UUID entityId,
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
        String fileKey = s3StorageService.uploadFile(file, principal.getTeamId(), entityType, entityId);

        // Save document metadata
        Document document = new Document();
        document.setTeamId(principal.getTeamId());
        document.setEntityType(entityType);
        document.setEntityId(entityId);
        document.setFileKey(fileKey);
        document.setFileName(file.getOriginalFilename());
        document.setFileSize(file.getSize());
        document.setMimeType(mimeType);
        document.setTitle(title);
        document.setNotes(notes);
        document.setUploadedBy(principal.getUserId());

        // Automatically categorize as PHOTO if it's an image
        if (mimeType != null && mimeType.startsWith("image/")) {
            document.setCategory(Document.Category.PHOTO.name());
            document.setIsMainPhoto(false);
        } else {
            document.setCategory(Document.Category.DOCUMENT.name());
            document.setIsMainPhoto(false);
        }

        Document savedDocument = documentRepository.save(document);
        log.info("Document uploaded: {} (category: {}) for entity {}/{}",
                savedDocument.getId(), savedDocument.getCategory(), entityType, entityId);

        // Log to audit trail for the parent entity
        // Use a custom audit entry to indicate document upload
        java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
        changedFields.put("documentAdded", savedDocument.getFileName());
        if (savedDocument.getTitle() != null && !savedDocument.getTitle().isEmpty()) {
            changedFields.put("title", savedDocument.getTitle());
        }
        changedFields.put("category", savedDocument.getCategory());

        auditService.logUpdate(
                principal.getTeamId(),
                entityType.toUpperCase(),
                entityId,
                principal.getUserId(),
                java.util.Map.of("documentCount", "unchanged"),
                java.util.Map.of("documentCount", "increased"),
                changedFields
        );

        return toResponseWithDownloadUrl(savedDocument);
    }

    public List<DocumentResponse> getDocuments(String entityType, UUID entityId, UserPrincipal principal) {
        List<Document> documents = documentRepository.findByEntityAndTeamId(
                entityType, entityId, principal.getTeamId());
        return documents.stream()
                .map(this::toResponseWithDownloadUrl)
                .toList();
    }

    public URL getDownloadUrl(UUID documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdAndTeamId(documentId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        return s3StorageService.generatePresignedUrl(document.getFileKey());
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void deleteDocument(UUID documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdAndTeamId(documentId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        // Soft delete in database
        documentRepository.softDeleteByIdAndTeamId(documentId, principal.getTeamId());

        // Delete from S3
        s3StorageService.deleteFile(document.getFileKey());

        log.info("Document deleted: {}", documentId);

        // Log to audit trail for the parent entity
        // Use a custom audit entry to indicate document removal
        java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
        changedFields.put("documentRemoved", document.getFileName());
        if (document.getTitle() != null && !document.getTitle().isEmpty()) {
            changedFields.put("title", document.getTitle());
        }
        changedFields.put("category", document.getCategory());

        auditService.logUpdate(
                principal.getTeamId(),
                document.getEntityType().toUpperCase(),
                document.getEntityId(),
                principal.getUserId(),
                java.util.Map.of("documentCount", "unchanged"),
                java.util.Map.of("documentCount", "decreased"),
                changedFields
        );
    }

    public List<DocumentResponse> getPhotos(String entityType, UUID entityId, UserPrincipal principal) {
        List<Document> photos = documentRepository.findByEntityAndTeamIdAndCategory(
                entityType, entityId, principal.getTeamId(), Document.Category.PHOTO.name());
        return photos.stream()
                .map(this::toResponseWithDownloadUrl)
                .toList();
    }

    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public DocumentResponse setMainPhoto(UUID photoId, String entityType, UUID entityId, UserPrincipal principal) {
        // Verify the photo exists and belongs to the team
        Document photo = documentRepository.findByIdAndTeamId(photoId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        // Verify it's actually a photo
        if (!Document.Category.PHOTO.name().equals(photo.getCategory())) {
            throw new IllegalArgumentException("Document is not a photo");
        }

        // Verify it belongs to the correct entity
        if (!entityType.equals(photo.getEntityType()) || !entityId.equals(photo.getEntityId())) {
            throw new IllegalArgumentException("Photo does not belong to this entity");
        }

        // Unset any existing main photo for this entity
        documentRepository.unsetMainPhotoForEntity(entityType, entityId, principal.getTeamId());

        // Set this photo as main
        photo.setIsMainPhoto(true);
        Document savedPhoto = documentRepository.save(photo);

        log.info("Main photo set: {} for entity {}/{}", photoId, entityType, entityId);

        return toResponseWithDownloadUrl(savedPhoto);
    }

    private DocumentResponse toResponseWithDownloadUrl(Document document) {
        DocumentResponse response = documentMapper.toResponse(document);
        String downloadUrl = s3StorageService.generatePresignedUrl(document.getFileKey()).toString();

        return new DocumentResponse(
                response.id(),
                response.teamId(),
                response.entityType(),
                response.entityId(),
                response.fileKey(),
                response.fileName(),
                response.fileSize(),
                response.mimeType(),
                response.title(),
                response.notes(),
                response.category(),
                response.isMainPhoto(),
                response.uploadedBy(),
                response.uploadedAt(),
                downloadUrl
        );
    }

    public List<DocumentResponse> searchDocuments(String searchTerm, String entityType, UserPrincipal principal) {
        List<Document> documents = documentRepository.searchDocuments(searchTerm, entityType, principal.getTeamId());
        return documents.stream()
                .map(this::toResponseWithDownloadUrl)
                .toList();
    }

    public DocumentResponse getDocument(UUID documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdAndTeamId(documentId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        return toResponseWithDownloadUrl(document);
    }

    public byte[] bulkDownload(List<UUID> documentIds, UserPrincipal principal) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException("No documents selected for download");
        }

        // Fetch all documents
        List<Document> documents = documentRepository.findByIdsAndTeamId(documentIds, principal.getTeamId());

        if (documents.isEmpty()) {
            throw new IllegalArgumentException("No documents found");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Document document : documents) {
                try {
                    // Download file from S3
                    InputStream fileStream = s3StorageService.downloadFile(document.getFileKey());

                    // Create zip entry with unique filename
                    String fileName = sanitizeFilename(document.getFileName());
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

                    log.debug("Added document to zip: {}", fileName);
                } catch (Exception e) {
                    log.error("Failed to add document {} to zip: {}", document.getId(), e.getMessage());
                    // Continue with other documents even if one fails
                }
            }

            zos.finish();
            log.info("Created zip archive with {} documents for team {}", documents.size(), principal.getTeamId());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to create zip archive: {}", e.getMessage());
            throw new RuntimeException("Failed to create zip archive", e);
        }
    }

    private String sanitizeFilename(String filename) {
        // Remove any path separators and invalid characters
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_");
    }
}
