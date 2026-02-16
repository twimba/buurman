package com.buurman.dto.request;

public record NotificationFilterRequest(
    String type,
    String channel,
    String status,
    String recipientEmail,
    String dateFrom,
    String dateTo) {}
