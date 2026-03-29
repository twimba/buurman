package com.buurman.dto.request;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ImportExecuteRequest(
    @NotBlank String fileName,
    @NotEmpty Map<String, String> mappings,
    @NotNull ContactType contactType,
    Optional<List<ContactTag>> tags,
    boolean skipErrors) {}
