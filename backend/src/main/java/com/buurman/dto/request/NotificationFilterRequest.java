package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

public record NotificationFilterRequest(
    @Nullable String type,
    @Nullable String channel,
    @Nullable String status,
    @Nullable String recipientEmail,
    @Nullable String dateFrom,
    @Nullable String dateTo) {}
