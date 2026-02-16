package com.buurman.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.User;
import com.buurman.dto.response.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "identifier", source = "user.identifier")
  @Mapping(target = "email", source = "user.email")
  @Mapping(target = "firstName", source = "user.firstName")
  @Mapping(target = "lastName", source = "user.lastName")
  @Mapping(target = "createdAt", source = "user.createdAt")
  @Mapping(target = "teamIdentifier", source = "teamIdentifier")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "emailVerified", expression = "java(user.getEmailVerifiedAt() != null)")
  UserResponse toResponse(User user, String teamIdentifier, String role);
}
