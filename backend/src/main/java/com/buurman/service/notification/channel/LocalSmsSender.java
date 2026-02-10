package com.buurman.service.notification.channel;

import com.buurman.domain.NotificationChannel;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Profile("local")
public class LocalSmsSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(LocalSmsSender.class);

    @Override
    public String send(NotificationSendRequest request) throws NotificationSendException {
        String fakeSid = "SM" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        log.info("========== LOCAL SMS ==========");
        log.info("To: {}", request.recipientPhone());
        log.info("Body: {}", request.body());
        log.info("Fake SID: {}", fakeSid);
        log.info("===============================");

        return fakeSid;
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
            default -> "Buurman: You have a new notification.";
        };
    }
}
