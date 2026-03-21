package com.buurman.dto.request;

import com.buurman.domain.ContactTag;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record AddContactTagRequest(
    @NotNull(message = "Tag is required") ContactTag tag) {}
