package com.buurman.dto.request;

import com.buurman.domain.ContactTag;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;

@Generated
public record AddContactTagRequest(@NotNull(message = "Tag is required") ContactTag tag) {}
