package com.buurman.repository;

import com.buurman.domain.Document;
import com.buurman.mapper.DocumentRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.DOCUMENTS;

@Repository
public class DocumentRepository {

    private final DSLContext dsl;
    private final DocumentRecordMapper mapper;

    public DocumentRepository(DSLContext dsl, DocumentRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<Document> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.ID.eq(id)
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Document> findByEntityAndTeamId(String entityType, UUID entityId, UUID teamId) {
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.ENTITY_TYPE.eq(entityType)
                        .and(DOCUMENTS.ENTITY_ID.eq(entityId))
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .orderBy(DOCUMENTS.UPLOADED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Document save(Document document) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (document.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(DOCUMENTS)
                    .set(DOCUMENTS.ID, newId)
                    .set(DOCUMENTS.TEAM_ID, document.getTeamId())
                    .set(DOCUMENTS.ENTITY_TYPE, document.getEntityType())
                    .set(DOCUMENTS.ENTITY_ID, document.getEntityId())
                    .set(DOCUMENTS.FILE_KEY, document.getFileKey())
                    .set(DOCUMENTS.FILE_NAME, document.getFileName())
                    .set(DOCUMENTS.FILE_SIZE, document.getFileSize())
                    .set(DOCUMENTS.MIME_TYPE, document.getMimeType())
                    .set(DOCUMENTS.TITLE, document.getTitle())
                    .set(DOCUMENTS.NOTES, document.getNotes())
                    .set(DOCUMENTS.CATEGORY, document.getCategory())
                    .set(DOCUMENTS.IS_MAIN_PHOTO, document.getIsMainPhoto())
                    .set(DOCUMENTS.UPLOADED_BY, document.getUploadedBy())
                    .set(DOCUMENTS.UPLOADED_AT, now)
                    .execute();

            document.setId(newId);
            document.setUploadedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE (title, notes, and isMainPhoto are updatable)
            dsl.update(DOCUMENTS)
                    .set(DOCUMENTS.TITLE, document.getTitle())
                    .set(DOCUMENTS.NOTES, document.getNotes())
                    .set(DOCUMENTS.IS_MAIN_PHOTO, document.getIsMainPhoto())
                    .where(DOCUMENTS.ID.eq(document.getId())
                            .and(DOCUMENTS.TEAM_ID.eq(document.getTeamId())))
                    .execute();
        }

        return document;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(DOCUMENTS)
                .set(DOCUMENTS.DELETED_AT, now)
                .where(DOCUMENTS.ID.eq(id)
                        .and(DOCUMENTS.TEAM_ID.eq(teamId)))
                .execute();
    }

    public List<Document> findByEntityAndTeamIdAndCategory(String entityType, UUID entityId, UUID teamId, String category) {
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.ENTITY_TYPE.eq(entityType)
                        .and(DOCUMENTS.ENTITY_ID.eq(entityId))
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.CATEGORY.eq(category))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .orderBy(DOCUMENTS.IS_MAIN_PHOTO.desc(), DOCUMENTS.UPLOADED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public void unsetMainPhotoForEntity(String entityType, UUID entityId, UUID teamId) {
        dsl.update(DOCUMENTS)
                .set(DOCUMENTS.IS_MAIN_PHOTO, false)
                .where(DOCUMENTS.ENTITY_TYPE.eq(entityType)
                        .and(DOCUMENTS.ENTITY_ID.eq(entityId))
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.IS_MAIN_PHOTO.eq(true))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .execute();
    }

    public List<Document> searchDocuments(String searchTerm, String entityType, UUID teamId) {
        var query = dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.TEAM_ID.eq(teamId)
                        .and(DOCUMENTS.DELETED_AT.isNull()));

        // Add search filter if provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            query = query.and(
                    DOCUMENTS.TITLE.lower().like(searchPattern)
                            .or(DOCUMENTS.FILE_NAME.lower().like(searchPattern))
                            .or(DOCUMENTS.NOTES.lower().like(searchPattern))
            );
        }

        // Add entity type filter if provided
        if (entityType != null && !entityType.trim().isEmpty()) {
            query = query.and(DOCUMENTS.ENTITY_TYPE.eq(entityType));
        }

        return query.orderBy(DOCUMENTS.UPLOADED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Document> findByIdsAndTeamId(List<UUID> ids, UUID teamId) {
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.ID.in(ids)
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
    }
}
