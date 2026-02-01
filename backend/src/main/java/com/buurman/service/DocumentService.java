package com.buurman.service;

import com.buurman.domain.Document;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final DocumentMapper documentMapper;
    private final long maxFileSize;
    private final List<String> allowedMimeTypes;

    public DocumentService(
            DocumentRepository documentRepository,
            S3StorageService s3StorageService,
            DocumentMapper documentMapper,
            @Value("${app.documents.max-file-size}") long maxFileSize,
            @Value("${app.documents.allowed-mime-types}") String allowedMimeTypesStr) {
        this.documentRepository = documentRepository;
        this.s3StorageService = s3StorageService;
        this.documentMapper = documentMapper;
        this.maxFileSize = maxFileSize;
        this.allowedMimeTypes = Arrays.asList(allowedMimeTypesStr.split(","));
    }

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

        // Validate MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !allowedMimeTypes.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed types: " + String.join(", ", allowedMimeTypes));
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

        Document savedDocument = documentRepository.save(document);
        log.info("Document uploaded: {} for entity {}/{}", savedDocument.getId(), entityType, entityId);

        return documentMapper.toResponse(savedDocument);
    }

    public List<DocumentResponse> getDocuments(String entityType, UUID entityId, UserPrincipal principal) {
        List<Document> documents = documentRepository.findByEntityAndTeamId(
                entityType, entityId, principal.getTeamId());
        return documents.stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    public URL getDownloadUrl(UUID documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdAndTeamId(documentId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        return s3StorageService.generatePresignedUrl(document.getFileKey());
    }

    public void deleteDocument(UUID documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdAndTeamId(documentId, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        // Soft delete in database
        documentRepository.softDeleteByIdAndTeamId(documentId, principal.getTeamId());

        // Delete from S3
        s3StorageService.deleteFile(document.getFileKey());

        log.info("Document deleted: {}", documentId);
    }
}
