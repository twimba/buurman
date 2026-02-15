package com.buurman.mapper;

import com.buurman.domain.Document;
import com.buurman.jooq.generated.tables.records.DocumentsRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.ZoneOffset.UTC;

@Mapper(componentModel = "spring")
public interface DocumentRecordMapper {

    @Mapping(target = "uploadedAt", expression = "java(toInstant(record.getUploadedAt()))")
    @Mapping(target = "deletedAt", expression = "java(toInstant(record.getDeletedAt()))")
    Document toDomain(DocumentsRecord record);

    @Mapping(target = "uploadedAt", expression = "java(toLocalDateTime(document.getUploadedAt()))")
    @Mapping(target = "deletedAt", expression = "java(toLocalDateTime(document.getDeletedAt()))")
    DocumentsRecord toRecord(Document document);

    List<Document> toDomainList(List<DocumentsRecord> records);

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(UTC);
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
    }
}
