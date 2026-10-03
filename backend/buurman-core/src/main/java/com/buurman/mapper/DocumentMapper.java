package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Document;
import com.buurman.dto.response.DocumentResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface DocumentMapper {

  @Mapping(target = "downloadUrl", ignore = true)
  @Mapping(target = "entityIdentifier", ignore = true)
  // sourceDocumentId is an internal UUID; resolving it to the source document's Sid requires a
  // lookup the caller already has the context (and sibling documents) to do cheaply.
  @Mapping(target = "sourceDocumentIdentifier", ignore = true)
  DocumentResponse toResponse(Document document);
}
