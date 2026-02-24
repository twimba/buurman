package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.SMS;

import java.time.Instant;
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

  public LocalSmsSender(MetricsService metricsService) {
    this.metricsService = metricsService;
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
  public RenderedContent render(String templateName, Map<String, Object> variables) {
    String body = renderSmsTemplate(templateName, variables);
    return new RenderedContent(Optional.empty(), body, SMS);
  }

  private String renderSmsTemplate(String templateName, Map<String, Object> variables) {
    String template = getSmsTemplate(templateName);
    if (variables != null) {
      for (Map.Entry<String, Object> entry : variables.entrySet()) {
        template = template.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
      }
    }
    return template;
  }

  private String getSmsTemplate(String templateName) {
    return switch (templateName) {
      case "welcome" -> "Buurman: Welcome, {userName}! Your account is ready at {baseUrl}";
      case "verification-code" -> "Buurman: Your code is {verificationCode}. Expires in 15 min.";
      case "phone-verification-code" ->
          "Buurman: Your phone verification code is {verificationCode}. Expires in {expiresMinutes}"
              + " min.";
      case "team-invitation" ->
          "Buurman: {inviterName} invited you to {teamName}. Check your email.";
      case "invitation-accepted" -> "Buurman: {memberName} joined your team {teamName}.";
      case "password-changed" ->
          "Buurman: Your password was changed. Contact support if unexpected.";
      case "payment-reminder" ->
          "Buurman: Payment of {amount} for {propertyName} is overdue (due {dueDate}).";
      case "contract-expiry" ->
          "Buurman: Contract for {propertyName} expires in {daysUntilExpiry} days ({expiryDate}).";
      case "property-created" -> "Buurman: Property {propertyName} has been created.";
      case "contract-created" ->
          "Buurman: New contract created for {propertyName} with {tenantName}.";
      case "contract-status-changed" ->
          "Buurman: Contract for {propertyName} changed from {oldStatus} to {newStatus}.";
      case "contract-reopened" ->
          "Buurman: Contract for {propertyName} ({tenantName}) has been reopened for editing.";
      case "payment-paid" ->
          "Buurman: Payment of {amount} for {propertyName} has been marked as paid.";
      case "payment-receival" ->
          "Buurman: Receival of {receivalAmount} registered for {propertyName} payment.";
      case "expense-created" ->
          "Buurman: Expense of {amount} ({category}) created for {propertyName}.";
      default -> "Buurman: You have a new notification.";
    };
  }
}
