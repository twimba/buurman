package com.buurman.service.notification;

import com.buurman.domain.NotificationChannel;

import java.util.Map;

public interface NotificationChannelSender {

    String send(NotificationSendRequest request) throws NotificationSendException;

    NotificationChannel getChannel();

    RenderedContent render(String templateName, Map<String, Object> variables);
}
