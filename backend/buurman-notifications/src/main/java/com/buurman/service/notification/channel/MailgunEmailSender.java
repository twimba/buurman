package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.EMAIL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.config.models.MailgunProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
import com.buurman.service.S3StorageService;
import com.buurman.service.notification.EmailAttachment;
import com.buurman.service.notification.EmailSubjectResolver;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!local")
@Slf4j
public class MailgunEmailSender implements NotificationChannelSender {

  private final MailgunMessagesApi mailgunApi;
  private final TemplateEngine templateEngine;
  private final EmailSubjectResolver emailSubjectResolver;
  private final MetricsService metricsService;
  private final S3StorageService s3StorageService;
  private final String domain;
  private final String fromEmail;
  private final String fromName;

  public MailgunEmailSender(
      MailgunMessagesApi mailgunApi,
      @Qualifier("emailTemplateEngine") TemplateEngine templateEngine,
      EmailSubjectResolver emailSubjectResolver,
      MailgunProperties mailgunProperties,
      MetricsService metricsService,
      S3StorageService s3StorageService) {
    this.s3StorageService = s3StorageService;
    this.mailgunApi = mailgunApi;
    this.templateEngine = templateEngine;
    this.emailSubjectResolver = emailSubjectResolver;
    this.metricsService = metricsService;
    this.domain = mailgunProperties.domain();
    this.fromEmail = mailgunProperties.fromEmail();
    this.fromName = mailgunProperties.fromName();
  }

  @Override
  public String send(NotificationSendRequest request) throws NotificationSendException {
    Instant start = Instant.now();
    try {
      String recipient =
          request
              .recipientEmail()
              .orElseThrow(() -> new NotificationSendException("recipientEmail is required"));
      String subject =
          request.subject().orElseThrow(() -> new NotificationSendException("subject is required"));

      List<File> tempFiles = new ArrayList<>();
      Message.MessageBuilder builder =
          Message.builder()
              .from(fromName + " <" + fromEmail + ">")
              .to(recipient)
              .subject(subject)
              .html(request.body());
      for (EmailAttachment attachment : request.attachments().orElse(List.of())) {
        tempFiles.add(materialize(attachment));
      }
      if (!tempFiles.isEmpty()) {
        builder.attachment(tempFiles);
      }
      Message message = builder.build();

      MessageResponse response;
      try {
        response = mailgunApi.sendMessage(domain, message);
      } finally {
        tempFiles.forEach(f -> f.delete());
      }
      String messageId = response.getId().replaceAll("^<|>$", "");

      log.info(
          "Mailgun email sent to {}, message ID: {}",
          com.buurman.util.LogMasking.email(recipient),
          messageId);
      metricsService.recordNotificationSend(start, "email", "mailgun", "success");
      return messageId;
    } catch (NotificationSendException e) {
      metricsService.recordNotificationSend(start, "email", "mailgun", "failure");
      throw e;
    } catch (Exception e) {
      metricsService.recordNotificationSend(start, "email", "mailgun", "failure");
      throw new NotificationSendException("Failed to send email via Mailgun", e);
    }
  }

  /** Mailgun's SDK attaches java.io.File only, so stored attachments are spooled to a temp dir. */
  private File materialize(EmailAttachment attachment) throws IOException {
    Path dir = Files.createTempDirectory("buurman-mail-");
    Path file = dir.resolve(Path.of(attachment.fileName()).getFileName());
    try (InputStream in = s3StorageService.downloadFile(attachment.fileKey())) {
      Files.copy(in, file);
    }
    file.toFile().deleteOnExit();
    dir.toFile().deleteOnExit();
    return file.toFile();
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
