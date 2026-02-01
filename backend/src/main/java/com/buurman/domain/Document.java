package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class Document {

    private UUID id;
    private UUID teamId;
    private String entityType;
    private UUID entityId;
    private String fileKey;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String title;
    private String notes;
    private String category;
    private Boolean isMainPhoto;
    private UUID uploadedBy;
    private Instant uploadedAt;
    private Instant deletedAt;

    public Document() {
    }

    public Document(UUID id, UUID teamId, String entityType, UUID entityId, String fileKey,
                    String fileName, Long fileSize, String mimeType, String title, String notes,
                    UUID uploadedBy, Instant uploadedAt, Instant deletedAt) {
        this.id = id;
        this.teamId = teamId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.fileKey = fileKey;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.title = title;
        this.notes = notes;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsMainPhoto() {
        return isMainPhoto;
    }

    public void setIsMainPhoto(Boolean isMainPhoto) {
        this.isMainPhoto = isMainPhoto;
    }

    public enum EntityType {
        PROPERTY,
        TENANT,
        CONTRACT,
        PAYMENT,
        EXPENSE
    }

    public enum Category {
        DOCUMENT,
        PHOTO
    }
}
