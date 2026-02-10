package com.buurman.service.notification;

import com.buurman.domain.NotificationChannel;

public record RenderedContent(
        String subject,
        String body,
        NotificationChannel channel
) {}
