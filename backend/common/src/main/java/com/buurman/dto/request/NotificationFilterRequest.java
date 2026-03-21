package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record NotificationFilterRequest(
    Optional<String> type,
    Optional<String> channel,
    Optional<String> status,
    Optional<String> recipientEmail,
    Optional<String> dateFrom,
    Optional<String> dateTo) {}
