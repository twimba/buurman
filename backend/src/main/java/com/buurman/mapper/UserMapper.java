package com.buurman.mapper;

import com.buurman.domain.Team;
import com.buurman.domain.User;
import com.buurman.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "teamId", source = "teamId")
    @Mapping(target = "role", source = "role")
    UserResponse toResponse(User user, UUID teamId, String role);
}
