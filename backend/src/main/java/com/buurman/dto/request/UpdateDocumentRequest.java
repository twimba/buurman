package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

public record UpdateDocumentRequest(@Nullable String title, @Nullable String notes) {}
