package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.EMAIL;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
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
  private final MetricsService metricsService;
  private final String fromEmail;
  private final String fromName;

  public LocalEmailSender(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      AppProperties appProperties,
      MetricsService metricsService) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
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
      helper.setTo(request.recipientEmail());
      helper.setSubject(request.subject());
      helper.setText(request.body(), true);

      mailSender.send(message);

      String fakeMessageId = UUID.randomUUID().toString();
      log.info(
          "[LOCAL] Email sent to {} via Mailpit, subject: {}, fakeId: {}",
          request.recipientEmail(),
          request.subject(),
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
