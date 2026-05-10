package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.EMAIL;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
import com.buurman.service.notification.EmailSubjectResolver;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("local")
@Slf4j
public class LocalEmailSender implements NotificationChannelSender {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final EmailSubjectResolver emailSubjectResolver;
  private final MetricsService metricsService;
  private final String fromEmail;
  private final String fromName;

  public LocalEmailSender(
      JavaMailSender mailSender,
      @Qualifier("emailTemplateEngine") TemplateEngine templateEngine,
      EmailSubjectResolver emailSubjectResolver,
      AppProperties appProperties,
      MetricsService metricsService) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.emailSubjectResolver = emailSubjectResolver;
    this.metricsService = metricsService;
    this.fromEmail = appProperties.email().from();
    this.fromName = appProperties.email().fromName();
  }

  @Override
  public String send(NotificationSendRequest request) throws NotificationSendException {
    Instant start = Instant.now();
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail, fromName);
      helper.setTo(
          request
              .recipientEmail()
              .orElseThrow(() -> new NotificationSendException("recipientEmail is required")));
      helper.setSubject(
          request
              .subject()
              .orElseThrow(() -> new NotificationSendException("subject is required")));
      helper.setText(request.body(), true);

      mailSender.send(message);

      String fakeMessageId = UUID.randomUUID().toString();
      log.info(
          "[LOCAL] Email sent to {} via Mailpit, subject: {}, fakeId: {}",
          request.recipientEmail().orElse(""),
          request.subject().orElse(""),
          fakeMessageId);
      metricsService.recordNotificationSend(start, "email", "mailpit", "success");
      return fakeMessageId;
    } catch (MessagingException | java.io.UnsupportedEncodingException e) {
      metricsService.recordNotificationSend(start, "email", "mailpit", "failure");
      throw new NotificationSendException("Failed to send email via Mailpit: " + e.getMessage(), e);
    }
  }

  @Override
  public NotificationChannel getChannel() {
    return EMAIL;
  }

  @Override
  public RenderedContent render(String templateName, Map<String, Object> variables, Locale locale) {
    Context context = new Context(locale);
    if (variables != null) {
      variables.forEach(context::setVariable);
    }

    String subject = emailSubjectResolver.resolve(templateName, variables, locale);
    String body = templateEngine.process(templateName, context);

    return new RenderedContent(Optional.of(subject), body, EMAIL);
  }
}
