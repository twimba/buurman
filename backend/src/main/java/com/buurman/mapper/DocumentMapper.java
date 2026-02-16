package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Document;
import com.buurman.dto.response.DocumentResponse;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

  @Mapping(target = "downloadUrl", ignore = true)
  @Mapping(target = "entityIdentifier", ignore = true)
  DocumentResponse toResponse(Document document);
}
