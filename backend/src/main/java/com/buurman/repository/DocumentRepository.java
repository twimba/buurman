package com.buurman.repository;

import com.buurman.domain.Document;
import com.buurman.dto.request.PageRequest;
import com.buurman.mapper.DocumentRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import static org.jooq.impl.DSL.lower;
import org.springframework.stereotype.Repository;

import com.buurman.jooq.generated.tables.records.DocumentsRecord;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    public Optional<Document> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.IDENTIFIER.eq(identifier)
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Document> findByIdentifiersAndTeamId(List<String> identifiers, UUID teamId) {
        if (identifiers == null || identifiers.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.IDENTIFIER.in(identifiers)
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
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
            String identifier = UlidGenerator.generate(EntityPrefix.DOC);
            dsl.insertInto(DOCUMENTS)
                    .set(DOCUMENTS.ID, newId)
                    .set(DOCUMENTS.IDENTIFIER, identifier)
                    .set(DOCUMENTS.TEAM_ID, document.getTeamId())
                    .set(DOCUMENTS.ENTITY_TYPE, document.getEntityType())
                    .set(DOCUMENTS.ENTITY_ID, document.getEntityId())
                    .set(DOCUMENTS.FILE_KEY, document.getFileKey())
                    .set(DOCUMENTS.FILE_NAME, document.getFileName())
                    .set(DOCUMENTS.FILE_SIZE, document.getFileSize())
                    .set(DOCUMENTS.MIME_TYPE, document.getMimeType())
                    .set(DOCUMENTS.TITLE, document.getTitle())
                    .set(DOCUMENTS.NOTES, document.getNotes())
                    .set(DOCUMENTS.UPLOADED_BY, document.getUploadedBy())
                    .set(DOCUMENTS.UPLOADED_AT, now)
                    .execute();

            document.setId(newId);
            document.setIdentifier(identifier);
            document.setUploadedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE (title and notes are updatable)
            dsl.update(DOCUMENTS)
                    .set(DOCUMENTS.TITLE, document.getTitle())
                    .set(DOCUMENTS.NOTES, document.getNotes())
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

    public List<Document> searchDocuments(String searchTerm, String entityType, UUID teamId) {
        var query = dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.TEAM_ID.eq(teamId)
                        .and(DOCUMENTS.DELETED_AT.isNull()));

        // Add search filter if provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            query = query.and(
                    lower(DOCUMENTS.TITLE).like(searchPattern)
                            .or(lower(DOCUMENTS.FILE_NAME).like(searchPattern))
                            .or(lower(DOCUMENTS.NOTES).like(searchPattern))
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

    public List<Document> findByEntityTypeAndEntityIdsAndTeamId(String entityType, Collection<UUID> entityIds, UUID teamId) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(DOCUMENTS)
                .where(DOCUMENTS.ENTITY_TYPE.eq(entityType)
                        .and(DOCUMENTS.ENTITY_ID.in(entityIds))
                        .and(DOCUMENTS.TEAM_ID.eq(teamId))
                        .and(DOCUMENTS.DELETED_AT.isNull()))
                .orderBy(DOCUMENTS.UPLOADED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public PaginatedResult<Document> findAllByTeamIdPaginated(UUID teamId, String search, String entityType, PageRequest pageRequest) {
        Condition condition = DOCUMENTS.TEAM_ID.eq(teamId).and(DOCUMENTS.DELETED_AT.isNull());
        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            condition = condition.and(
                lower(DOCUMENTS.TITLE).like(searchPattern)
                    .or(lower(DOCUMENTS.FILE_NAME).like(searchPattern))
                    .or(lower(DOCUMENTS.NOTES).like(searchPattern))
            );
        }
        if (entityType != null && !entityType.trim().isEmpty()) {
            condition = condition.and(DOCUMENTS.ENTITY_TYPE.eq(entityType));
        }
        Map<String, Field<?>> sortableFields = Map.of(
            "uploadedAt", DOCUMENTS.UPLOADED_AT,
            "title", DOCUMENTS.TITLE,
            "fileSize", DOCUMENTS.FILE_SIZE,
            "entityType", DOCUMENTS.ENTITY_TYPE
        );
        return PaginationHelper.paginate(dsl, DOCUMENTS, condition, sortableFields, DOCUMENTS.UPLOADED_AT, pageRequest, r -> mapper.toDomain((DocumentsRecord) r));
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
