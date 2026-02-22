package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

public record UpdatePhotoRequest(@Nullable String title, @Nullable String notes) {}
