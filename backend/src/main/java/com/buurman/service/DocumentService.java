package com.buurman.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Ulid;
import com.buurman.domain.Document;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateDocumentRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.exception.ExternalServiceException;
import com.buurman.mapper.DocumentMapper;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final DocumentMapper documentMapper;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final long maxFileSize;
  private final List<String> allowedMimeTypes;

  public DocumentService(
      DocumentRepository documentRepository,
      S3StorageService s3StorageService,
      DocumentMapper documentMapper,
      AuditService auditService,
      MetricsService metricsService,
      AppProperties appProperties) {
    this.documentRepository = documentRepository;
    this.s3StorageService = s3StorageService;
    this.documentMapper = documentMapper;
    this.auditService = auditService;
    this.metricsService = metricsService;
    this.maxFileSize = appProperties.documents().maxFileSize();
    this.allowedMimeTypes = appProperties.documents().allowedMimeTypes();
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DocumentResponse uploadDocument(
      MultipartFile file,
      String entityType,
      UUID entityId,
      Ulid entityIdentifier,
      @Nullable String title,
      @Nullable String notes,
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
    String fileKey =
        s3StorageService.uploadFile(
            file, Ulid.of(principal.requireTeamIdentifier()), entityType, entityIdentifier);

    // Save document metadata
    Document document = new Document();
    document.setTeamId(principal.requireTeamId());
    document.setEntityType(entityType);
    document.setEntityId(entityId);
    document.setFileKey(fileKey);
    document.setFileName(
        java.util.Objects.requireNonNullElse(file.getOriginalFilename(), "unknown"));
    document.setFileSize(file.getSize());
    document.setMimeType(mimeType);
    document.setTitle(Optional.ofNullable(title));
    document.setNotes(Optional.ofNullable(notes));
    document.setUploadedBy(principal.getUserId());

    Document savedDocument = documentRepository.save(document);

    metricsService.incrementCounter("document.upload.total", "entity_type", entityType);
    metricsService.incrementCounter("document.upload.bytes.total", "entity_type", entityType);
    metricsService.recordHistogram(
        "document.upload.bytes", (double) file.getSize(), "entity_type", entityType);

    log.info(
        "Document uploaded: {} for entity {}/{}",
        savedDocument.getIdentifier().orElseThrow(),
        entityType,
        entityId);

    // Log to audit trail for the parent entity
    java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
    changedFields.put("documentAdded", savedDocument.getFileName());
    savedDocument
        .getTitle()
        .filter(t -> !t.isEmpty())
        .ifPresent(t -> changedFields.put("title", t));
    auditService.logUpdate(
        principal.requireTeamId(),
        entityType.toUpperCase(Locale.ROOT),
        entityId,
        principal.getUserId(),
        java.util.Map.of("documentCount", "unchanged"),
        java.util.Map.of("documentCount", "increased"),
        changedFields);

    return toResponseWithDownloadUrl(savedDocument);
  }

  public List<DocumentResponse> getDocuments(
      String entityType, UUID entityId, UserPrincipal principal) {
    List<Document> documents =
        documentRepository.findByEntityAndTeamId(entityType, entityId, principal.requireTeamId());
    return documents.stream().map(this::toResponseWithDownloadUrl).toList();
  }

  public URL getDownloadUrl(Ulid identifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    metricsService.incrementCounter(
        "document.download.total", "entity_type", document.getEntityType());

    return s3StorageService.generatePresignedUrl(document.getFileKey());
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteDocument(Ulid identifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    // Soft delete in database
    documentRepository.softDeleteByIdAndTeamId(document.getId(), principal.requireTeamId());

    // Delete from S3
    s3StorageService.deleteFile(document.getFileKey());

    metricsService.incrementCounter(
        "document.delete.total", "entity_type", document.getEntityType());

    log.info("Document deleted: {}", document.getIdentifier().orElseThrow());

    // Log to audit trail for the parent entity
    java.util.Map<String, Object> changedFields = new java.util.HashMap<>();
    changedFields.put("documentRemoved", document.getFileName());
    document.getTitle().filter(t -> !t.isEmpty()).ifPresent(t -> changedFields.put("title", t));
    auditService.logUpdate(
        principal.requireTeamId(),
        document.getEntityType().toUpperCase(Locale.ROOT),
        document.getEntityId(),
        principal.getUserId(),
        java.util.Map.of("documentCount", "unchanged"),
        java.util.Map.of("documentCount", "decreased"),
        changedFields);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DocumentResponse updateDocument(
      Ulid identifier, UpdateDocumentRequest request, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    String oldTitle = document.getTitle().orElse(null);
    String oldNotes = document.getNotes().orElse(null);

    document.setTitle(request.title());
    document.setNotes(request.notes());

    documentRepository.save(document);

    // Build clean audit data comparing only title and notes
    Map<String, Object> changedFields = new java.util.HashMap<>();
    Map<String, Object> oldValues = new java.util.HashMap<>();
    Map<String, Object> newValues = new java.util.HashMap<>();

    if (!java.util.Objects.equals(oldTitle, request.title().orElse(null))) {
      changedFields.put("title", request.title().orElse(null));
      oldValues.put("title", oldTitle);
      newValues.put("title", request.title().orElse(null));
    }
    if (!java.util.Objects.equals(oldNotes, request.notes().orElse(null))) {
      changedFields.put("notes", request.notes().orElse(null));
      oldValues.put("notes", oldNotes);
      newValues.put("notes", request.notes().orElse(null));
    }

    if (!changedFields.isEmpty()) {
      // Add marker so the audit description identifies this as a document edit
      changedFields.put("documentEdited", document.getFileName());
      oldValues.put("fileName", document.getFileName());
      newValues.put("fileName", document.getFileName());

      auditService.logUpdate(
          principal.requireTeamId(),
          document.getEntityType().toUpperCase(Locale.ROOT),
          document.getEntityId(),
          principal.getUserId(),
          oldValues,
          newValues,
          changedFields);
    }

    return toResponseWithDownloadUrl(document);
  }

  private DocumentResponse toResponseWithDownloadUrl(Document document) {
    DocumentResponse response = documentMapper.toResponse(document);
    String downloadUrl = s3StorageService.generatePresignedUrl(document.getFileKey()).toString();

    return new DocumentResponse(
        response.identifier(),
        response.entityType(),
        response.entityIdentifier(),
        response.fileKey(),
        response.fileName(),
        response.fileSize(),
        response.mimeType(),
        response.title(),
        response.notes(),
        response.uploadedAt(),
        Optional.of(downloadUrl));
  }

  public PageResponse<DocumentResponse> searchDocumentsPaginated(
      @Nullable String search,
      @Nullable String entityType,
      UserPrincipal principal,
      PageRequest pageRequest) {
    PaginatedResult<Document> result =
        documentRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(), search, entityType, pageRequest);
    List<DocumentResponse> responses =
        result.items().stream().map(this::toResponseWithDownloadUrl).toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  public List<DocumentResponse> searchDocuments(
      @Nullable String searchTerm, @Nullable String entityType, UserPrincipal principal) {
    List<Document> documents =
        documentRepository.searchDocuments(searchTerm, entityType, principal.requireTeamId());
    return documents.stream().map(this::toResponseWithDownloadUrl).toList();
  }

  public DocumentResponse getDocument(Ulid identifier, UserPrincipal principal) {
    Document document =
        documentRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return toResponseWithDownloadUrl(document);
  }

  public byte[] bulkDownload(List<Ulid> documentIdentifiers, UserPrincipal principal) {
    if (documentIdentifiers == null || documentIdentifiers.isEmpty()) {
      throw new IllegalArgumentException("No documents selected for download");
    }

    // Fetch all documents by identifiers
    List<Document> documents =
        documentRepository.findByIdentifiersAndTeamId(
            documentIdentifiers, principal.requireTeamId());

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
          log.error(
              "Failed to add document {} to zip: {}", document.getIdentifier().orElseThrow(), e.getMessage());
          // Continue with other documents even if one fails
        }
      }

      zos.finish();
      metricsService.incrementCounter("document.bulk.download.total");
      log.info(
          "Created zip archive with {} documents for team {}",
          documents.size(),
          principal.requireTeamId());
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
