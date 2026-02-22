package com.buurman.service.notification;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.NotificationChannel;

public record RenderedContent(@Nullable String subject, String body, NotificationChannel channel) {}
