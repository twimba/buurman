package com.buurman.service.notification.channel;

import com.buurman.domain.NotificationChannel;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("!local")
public class TwilioSmsSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final String fromNumber;

    public TwilioSmsSender(@Value("${twilio.from-number}") String fromNumber) {
        this.fromNumber = fromNumber;
    }

    @Override
    public String send(NotificationSendRequest request) throws NotificationSendException {
        try {
            Message message = Message.creator(
                    new PhoneNumber(request.recipientPhone()),
                    new PhoneNumber(fromNumber),
                    request.body()
            ).create();

            log.info("Twilio SMS sent to {}, SID: {}", request.recipientPhone(), message.getSid());
            return message.getSid();
        } catch (Exception e) {
            throw new NotificationSendException("Failed to send SMS via Twilio: " + e.getMessage(), e);
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public RenderedContent render(String templateName, Map<String, Object> variables) {
        String body = renderSmsTemplate(templateName, variables);
        return new RenderedContent(null, body, NotificationChannel.SMS);
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
            case "team-invitation" -> "Buurman: {inviterName} invited you to {teamName}. Check your email.";
            case "invitation-accepted" -> "Buurman: {memberName} joined your team {teamName}.";
            case "password-changed" -> "Buurman: Your password was changed. Contact support if unexpected.";
            case "payment-reminder" -> "Buurman: Payment of {amount} for {propertyName} is overdue (due {dueDate}).";
            case "contract-expiry" -> "Buurman: Contract for {propertyName} expires in {daysUntilExpiry} days ({expiryDate}).";
            case "property-created" -> "Buurman: Property {propertyName} has been created.";
            case "contract-created" -> "Buurman: New contract created for {propertyName} with {tenantName}.";
            case "contract-status-changed" -> "Buurman: Contract for {propertyName} changed from {oldStatus} to {newStatus}.";
            case "contract-reopened" -> "Buurman: Contract for {propertyName} ({tenantName}) has been reopened for editing.";
            case "payment-paid" -> "Buurman: Payment of {amount} for {propertyName} has been marked as paid.";
            case "payment-receival" -> "Buurman: Receival of {receivalAmount} registered for {propertyName} payment.";
            case "expense-created" -> "Buurman: Expense of {amount} ({category}) created for {propertyName}.";
            default -> "Buurman: You have a new notification.";
        };
    }
}
