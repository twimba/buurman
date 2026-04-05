package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.EMAIL;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.config.models.SendGridProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
import com.buurman.service.notification.EmailSubjectResolver;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!local")
@Slf4j
public class SendGridEmailSender implements NotificationChannelSender {

  private final SendGrid sendGrid;
  private final TemplateEngine templateEngine;
  private final EmailSubjectResolver emailSubjectResolver;
  private final MetricsService metricsService;
  private final String fromEmail;
  private final String fromName;

  public SendGridEmailSender(
      SendGrid sendGrid,
      @Qualifier("emailTemplateEngine") TemplateEngine templateEngine,
      EmailSubjectResolver emailSubjectResolver,
      SendGridProperties sendGridProperties,
      MetricsService metricsService) {
    this.sendGrid = sendGrid;
    this.templateEngine = templateEngine;
    this.emailSubjectResolver = emailSubjectResolver;
    this.metricsService = metricsService;
    this.fromEmail = sendGridProperties.fromEmail();
    this.fromName = sendGridProperties.fromName();
  }

  @Override
  public String send(NotificationSendRequest request) throws NotificationSendException {
    Instant start = Instant.now();
    try {
      Email from = new Email(fromEmail, fromName);
      Email to =
          new Email(
              request
                  .recipientEmail()
                  .orElseThrow(() -> new NotificationSendException("recipientEmail is required")));
      Content content = new Content("text/html", request.body());
      Mail mail =
          new Mail(
              from,
              request
                  .subject()
                  .orElseThrow(() -> new NotificationSendException("subject is required")),
              to,
              content);

      Request sgRequest = new Request();
      sgRequest.setMethod(Method.POST);
      sgRequest.setEndpoint("mail/send");
      sgRequest.setBody(mail.build());

      Response response = sendGrid.api(sgRequest);

      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        String messageId = response.getHeaders().get("X-Message-Id");
        if (messageId == null) {
          messageId = "";
        }
        log.info(
            "SendGrid email sent to {}, message ID: {}",
            request.recipientEmail().orElse(""),
            messageId);
        metricsService.recordNotificationSend(start, "email", "sendgrid", "success");
        return messageId;
      } else {
        metricsService.recordNotificationSend(start, "email", "sendgrid", "failure");
        throw new NotificationSendException(
            "SendGrid returned status " + response.getStatusCode() + ": " + response.getBody());
      }
    } catch (IOException e) {
      metricsService.recordNotificationSend(start, "email", "sendgrid", "failure");
      throw new NotificationSendException("Failed to send email via SendGrid", e);
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
