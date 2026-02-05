package com.buurman.mapper;

import com.buurman.domain.Photo;
import com.buurman.dto.response.PhotoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PhotoMapper {

    @Mapping(target = "downloadUrl", ignore = true)
    @Mapping(target = "entityIdentifier", ignore = true)
    PhotoResponse toResponse(Photo photo);
}
