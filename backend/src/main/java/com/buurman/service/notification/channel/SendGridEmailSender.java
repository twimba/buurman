package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.EMAIL;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.config.models.SendGridProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
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
  private final MetricsService metricsService;
  private final String fromEmail;
  private final String fromName;

  public SendGridEmailSender(
      SendGrid sendGrid,
      TemplateEngine templateEngine,
      SendGridProperties sendGridProperties,
      MetricsService metricsService) {
    this.sendGrid = sendGrid;
    this.templateEngine = templateEngine;
    this.metricsService = metricsService;
    this.fromEmail = sendGridProperties.fromEmail();
    this.fromName = sendGridProperties.fromName();
  }

  @Override
  public String send(NotificationSendRequest request) throws NotificationSendException {
    Instant start = Instant.now();
    try {
      Email from = new Email(fromEmail, fromName);
      Email to = new Email(request.recipientEmail());
      Content content = new Content("text/html", request.body());
      Mail mail = new Mail(from, request.subject(), to, content);

      Request sgRequest = new Request();
      sgRequest.setMethod(Method.POST);
      sgRequest.setEndpoint("mail/send");
      sgRequest.setBody(mail.build());

      Response response = sendGrid.api(sgRequest);

      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        String messageId = response.getHeaders().get("X-Message-Id");
        log.info("SendGrid email sent to {}, message ID: {}", request.recipientEmail(), messageId);
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
  public RenderedContent render(String templateName, Map<String, Object> variables) {
    Context context = new Context();
    if (variables != null) {
      variables.forEach(context::setVariable);
    }

    String subject = deriveSubject(templateName, variables);
    String body = templateEngine.process("email/" + templateName, context);

    return new RenderedContent(subject, body, EMAIL);
  }

  private String deriveSubject(String templateName, Map<String, Object> variables) {
    return switch (templateName) {
      case "welcome" -> "Welcome to Buurman!";
      case "verification-code" -> "Verify your email - Buurman";
      case "team-invitation" ->
          "You've been invited to join " + getVar(variables, "teamName", "a team");
      case "invitation-accepted" ->
          getVar(variables, "memberName", "Someone")
              + " joined "
              + getVar(variables, "teamName", "your team");
      case "password-changed" -> "Your password has been changed";
      case "payment-reminder" ->
          "Payment reminder for " + getVar(variables, "propertyName", "your property");
      case "contract-expiry" ->
          "Contract expiring soon for " + getVar(variables, "propertyName", "your property");
      case "property-created" ->
          "Property created: " + getVar(variables, "propertyName", "New property");
      case "contract-created" ->
          "New contract for " + getVar(variables, "propertyName", "your property");
      case "contract-status-changed" ->
          "Contract status changed to " + getVar(variables, "newStatus", "updated");
      case "contract-reopened" ->
          "Contract reopened: " + getVar(variables, "propertyName", "your property");
      case "payment-paid" ->
          "Payment marked as paid for " + getVar(variables, "propertyName", "your property");
      case "payment-receival" ->
          "Payment receival registered for " + getVar(variables, "propertyName", "your property");
      case "expense-created" ->
          "New expense recorded for " + getVar(variables, "propertyName", "your property");
      case "registration-invitation" -> "You're invited to join Buurman!";
      default -> "Notification from Buurman";
    };
  }

  private String getVar(Map<String, Object> variables, String key, String defaultValue) {
    if (variables == null) {
      return defaultValue;
    }
    Object val = variables.get(key);
    return val != null ? val.toString() : defaultValue;
  }
}
