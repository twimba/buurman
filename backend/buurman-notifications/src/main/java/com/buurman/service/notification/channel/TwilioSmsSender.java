package com.buurman.service.notification.channel;

import static com.buurman.domain.NotificationChannel.SMS;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.buurman.config.models.TwilioProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.service.MetricsService;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!local")
@Slf4j
public class TwilioSmsSender implements NotificationChannelSender {

  private final MetricsService metricsService;
  private final String fromNumber;
  private final Optional<String> messagingServiceSid;
  private final Optional<String> statusCallbackUrl;

  public TwilioSmsSender(TwilioProperties twilioProperties, MetricsService metricsService) {
    this.metricsService = metricsService;
    this.fromNumber = twilioProperties.fromNumber();
    this.messagingServiceSid = twilioProperties.messagingServiceSid();
    this.statusCallbackUrl = twilioProperties.statusCallbackUrl();
  }

  @Override
  public String send(NotificationSendRequest request) throws NotificationSendException {
    Instant start = Instant.now();
    try {
      String recipientPhone =
          request
              .recipientPhone()
              .orElseThrow(() -> new NotificationSendException("recipientPhone is required"));

      MessageCreator creator;
      Optional<String> sid = messagingServiceSid.filter(s -> !s.isBlank());
      if (sid.isPresent()) {
        creator =
            Message.creator(
                new PhoneNumber(recipientPhone),
                sid.orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Twilio messaging service SID verified present but missing")),
                request.body());
      } else {
        creator =
            Message.creator(
                new PhoneNumber(recipientPhone), new PhoneNumber(fromNumber), request.body());
      }

      statusCallbackUrl
          .filter(s -> !s.isBlank())
          .ifPresent(url -> creator.setStatusCallback(URI.create(url)));

      Message message = creator.create();

      log.info(
          "Twilio SMS sent to {}, SID: {}",
          com.buurman.util.LogMasking.phone(recipientPhone),
          message.getSid());
      metricsService.recordNotificationSend(start, "sms", "twilio", "success");
      return message.getSid();
    } catch (NotificationSendException e) {
      metricsService.recordNotificationSend(start, "sms", "twilio", "failure");
      throw e;
    } catch (Exception e) {
      metricsService.recordNotificationSend(start, "sms", "twilio", "failure");
      throw new NotificationSendException("Failed to send SMS via Twilio: " + e.getMessage(), e);
    }
  }

  @Override
  public NotificationChannel getChannel() {
    return SMS;
  }

  @Override
  public RenderedContent render(String templateName, Map<String, Object> variables, Locale locale) {
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
          "Buurman: New contract created for {propertyName} with {contactName}.";
      case "contract-status-changed" ->
          "Buurman: Contract for {propertyName} changed from {oldStatus} to {newStatus}.";
      case "contract-reopened" ->
          "Buurman: Contract for {propertyName} ({contactName}) has been reopened for editing.";
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
