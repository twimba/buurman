package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Photo;
import com.buurman.dto.response.PhotoResponse;

@Mapper(componentModel = "spring")
public interface PhotoMapper {

  @Mapping(target = "downloadUrl", ignore = true)
  @Mapping(target = "thumbnailUrl", ignore = true)
  @Mapping(target = "entityIdentifier", ignore = true)
  PhotoResponse toResponse(Photo photo);
}
