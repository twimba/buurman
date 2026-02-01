package com.buurman.mapper;

import com.buurman.domain.Document;
import com.buurman.dto.response.DocumentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "downloadUrl", ignore = true)
    DocumentResponse toResponse(Document document);
}
