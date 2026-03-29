package com.buurman.dto.request;

import com.buurman.domain.ContactTag;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record AddContactTagRequest(@NotNull(message = "Tag is required") ContactTag tag) {}
