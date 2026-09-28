package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.SMS;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("local")
@Slf4j
public class LocalSmsSender implements NotificationChannelSender {

  private final MetricsService metricsService;
  private final SmsBodyRenderer smsBodyRenderer;

  public LocalSmsSender(MetricsService metricsService, SmsBodyRenderer smsBodyRenderer) {
    this.metricsService = metricsService;
    this.smsBodyRenderer = smsBodyRenderer;
  }

  @Override
  public String send(NotificationSendRequest request) throws NotificationSendException {
    Instant start = Instant.now();
    String fakeSid = "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);

    log.info("========== LOCAL SMS ==========");
    log.info("To: {}", request.recipientPhone().orElse(""));
    log.info("Body: {}", request.body());
    log.info("Fake SID: {}", fakeSid);
    log.info("===============================");

    metricsService.recordNotificationSend(start, "sms", "local", "success");
    return fakeSid;
  }

  @Override
  public NotificationChannel getChannel() {
    return SMS;
  }

  @Override
  public RenderedContent render(String templateName, Map<String, Object> variables, Locale locale) {
    return new RenderedContent(
        Optional.empty(), smsBodyRenderer.render(templateName, variables, locale), SMS);
  }
}
