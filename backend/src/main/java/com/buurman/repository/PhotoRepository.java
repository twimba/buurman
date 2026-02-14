package com.buurman.repository;

import com.buurman.domain.Photo;
import com.buurman.dto.request.PageRequest;
import com.buurman.mapper.PhotoRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import static org.jooq.impl.DSL.lower;
import org.springframework.stereotype.Repository;

import com.buurman.jooq.generated.tables.records.PhotosRecord;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PHOTOS;

@Repository
public class PhotoRepository {

    private final DSLContext dsl;
    private final PhotoRecordMapper mapper;
    private final Clock clock;

    public PhotoRepository(DSLContext dsl, PhotoRecordMapper mapper, Clock clock) {
        this.dsl = dsl;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Optional<Photo> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(PHOTOS)
                .where(PHOTOS.IDENTIFIER.eq(identifier)
                        .and(PHOTOS.TEAM_ID.eq(teamId))
                        .and(PHOTOS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Photo> findByIdentifiersAndTeamId(List<String> identifiers, UUID teamId) {
        if (identifiers == null || identifiers.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(PHOTOS)
                .where(PHOTOS.IDENTIFIER.in(identifiers)
                        .and(PHOTOS.TEAM_ID.eq(teamId))
                        .and(PHOTOS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
    }

    public Optional<Photo> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(PHOTOS)
                .where(PHOTOS.ID.eq(id)
                        .and(PHOTOS.TEAM_ID.eq(teamId))
                        .and(PHOTOS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Photo> findByEntityAndTeamId(String entityType, UUID entityId, UUID teamId) {
        return dsl.selectFrom(PHOTOS)
                .where(PHOTOS.ENTITY_TYPE.eq(entityType)
                        .and(PHOTOS.ENTITY_ID.eq(entityId))
                        .and(PHOTOS.TEAM_ID.eq(teamId))
                        .and(PHOTOS.DELETED_AT.isNull()))
                .orderBy(PHOTOS.IS_MAIN_PHOTO.desc(), PHOTOS.UPLOADED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Photo save(Photo photo) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (photo.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            String identifier = UlidGenerator.generate(EntityPrefix.PHO);
            LocalDateTime uploadedAt = photo.getUploadedAt() != null
                    ? LocalDateTime.ofInstant(photo.getUploadedAt(), ZoneOffset.UTC)
                    : now;

            dsl.insertInto(PHOTOS)
                    .set(PHOTOS.ID, newId)
                    .set(PHOTOS.IDENTIFIER, identifier)
                    .set(PHOTOS.TEAM_ID, photo.getTeamId())
                    .set(PHOTOS.ENTITY_TYPE, photo.getEntityType())
                    .set(PHOTOS.ENTITY_ID, photo.getEntityId())
                    .set(PHOTOS.FILE_KEY, photo.getFileKey())
                    .set(PHOTOS.FILE_NAME, photo.getFileName())
                    .set(PHOTOS.FILE_SIZE, photo.getFileSize())
                    .set(PHOTOS.MIME_TYPE, photo.getMimeType())
                    .set(PHOTOS.TITLE, photo.getTitle())
                    .set(PHOTOS.NOTES, photo.getNotes())
                    .set(PHOTOS.IS_MAIN_PHOTO, photo.getIsMainPhoto())
                    .set(PHOTOS.UPLOADED_BY, photo.getUploadedBy())
                    .set(PHOTOS.UPLOADED_AT, uploadedAt)
                    .execute();

            photo.setId(newId);
            photo.setIdentifier(identifier);
            photo.setUploadedAt(uploadedAt.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE (title, notes, and isMainPhoto are updatable)
            dsl.update(PHOTOS)
                    .set(PHOTOS.TITLE, photo.getTitle())
                    .set(PHOTOS.NOTES, photo.getNotes())
                    .set(PHOTOS.IS_MAIN_PHOTO, photo.getIsMainPhoto())
                    .where(PHOTOS.ID.eq(photo.getId())
                            .and(PHOTOS.TEAM_ID.eq(photo.getTeamId())))
                    .execute();
        }

        return photo;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(PHOTOS)
                .set(PHOTOS.DELETED_AT, now)
                .where(PHOTOS.ID.eq(id)
                        .and(PHOTOS.TEAM_ID.eq(teamId)))
                .execute();
    }

    public void unsetMainPhotoForEntity(String entityType, UUID entityId, UUID teamId) {
        dsl.update(PHOTOS)
                .set(PHOTOS.IS_MAIN_PHOTO, false)
                .where(PHOTOS.ENTITY_TYPE.eq(entityType)
                        .and(PHOTOS.ENTITY_ID.eq(entityId))
                        .and(PHOTOS.TEAM_ID.eq(teamId))
                        .and(PHOTOS.IS_MAIN_PHOTO.eq(true))
                        .and(PHOTOS.DELETED_AT.isNull()))
                .execute();
    }

    public PaginatedResult<Photo> findAllByTeamIdPaginated(UUID teamId, String search, String entityType, PageRequest pageRequest) {
        Condition condition = PHOTOS.TEAM_ID.eq(teamId).and(PHOTOS.DELETED_AT.isNull());
        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            condition = condition.and(
                lower(PHOTOS.TITLE).like(searchPattern)
                    .or(lower(PHOTOS.FILE_NAME).like(searchPattern))
                    .or(lower(PHOTOS.NOTES).like(searchPattern))
            );
        }
        if (entityType != null && !entityType.trim().isEmpty()) {
            condition = condition.and(PHOTOS.ENTITY_TYPE.eq(entityType));
        }
        Map<String, Field<?>> sortableFields = Map.of(
            "uploadedAt", PHOTOS.UPLOADED_AT,
            "title", PHOTOS.TITLE,
            "fileSize", PHOTOS.FILE_SIZE,
            "entityType", PHOTOS.ENTITY_TYPE
        );
        return PaginationHelper.paginate(dsl, PHOTOS, condition, sortableFields, PHOTOS.UPLOADED_AT, pageRequest, r -> mapper.toDomain((PhotosRecord) r));
    }

    public List<Photo> findByEntityTypeAndEntityIdsAndTeamId(String entityType, Collection<UUID> entityIds, UUID teamId) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(PHOTOS)
                .where(PHOTOS.ENTITY_TYPE.eq(entityType)
                        .and(PHOTOS.ENTITY_ID.in(entityIds))
                        .and(PHOTOS.TEAM_ID.eq(teamId))
                        .and(PHOTOS.DELETED_AT.isNull()))
                .orderBy(PHOTOS.UPLOADED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }
}
