package com.buurman.controller;

import com.buurman.service.notification.WebhookService;
import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.twilio.security.RequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Provider status callbacks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;
    private final String twilioAuthToken;
    private final String sendGridVerificationKey;

    public WebhookController(WebhookService webhookService,
                             @Value("${twilio.auth-token:}") String twilioAuthToken,
                             @Value("${sendgrid.webhook-verification-key:}") String sendGridVerificationKey) {
        this.webhookService = webhookService;
        this.twilioAuthToken = twilioAuthToken;
        this.sendGridVerificationKey = sendGridVerificationKey;
    }

    @Operation(summary = "SendGrid event webhook", description = "Receives delivery status events from SendGrid")
    @PostMapping("/sendgrid/events")
    public ResponseEntity<Void> handleSendGridEvents(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Timestamp", required = false) String timestamp) {
        try {
            if (!verifySendGridSignature(rawPayload, signature, timestamp)) {
                log.warn("SendGrid webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            webhookService.processSendGridEvents(rawPayload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing SendGrid webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }

    @Operation(summary = "Twilio status callback", description = "Receives SMS delivery status from Twilio")
    @PostMapping("/twilio/status")
    public ResponseEntity<String> handleTwilioStatus(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String twilioSignature,
            HttpServletRequest request) {
        try {
            if (!verifyTwilioSignature(request, params, twilioSignature)) {
                log.warn("Twilio webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("<Response></Response>");
            }
            webhookService.processTwilioStatus(params);
        } catch (Exception e) {
            log.error("Error processing Twilio webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("<Response></Response>");
    }

    private boolean verifySendGridSignature(String payload, String signature, String timestamp) {
        if (sendGridVerificationKey == null || sendGridVerificationKey.isBlank()) {
            return true; // Skip verification in dev
        }
        if (signature == null || timestamp == null) {
            return false;
        }
        try {
            EventWebhook eventWebhook = new EventWebhook();
            ECPublicKey publicKey = eventWebhook.ConvertPublicKeyToECDSA(sendGridVerificationKey);
            return eventWebhook.VerifySignature(publicKey, payload, signature, timestamp);
        } catch (Exception e) {
            log.error("SendGrid signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyTwilioSignature(HttpServletRequest request, Map<String, String> params, String signature) {
        if (twilioAuthToken == null || twilioAuthToken.isBlank()) {
            return true; // Skip verification in dev
        }
        if (signature == null) {
            return false;
        }
        try {
            RequestValidator validator = new RequestValidator(twilioAuthToken);
            String url = request.getRequestURL().toString();
            return validator.validate(url, params, signature);
        } catch (Exception e) {
            log.error("Twilio signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
