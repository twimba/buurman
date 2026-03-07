package com.buurman.service.notification;

import java.util.Map;

import com.buurman.domain.NotificationChannel;

public interface NotificationChannelSender {

  String send(NotificationSendRequest request) throws NotificationSendException;

  NotificationChannel getChannel();

  RenderedContent render(String templateName, Map<String, Object> variables);
}
