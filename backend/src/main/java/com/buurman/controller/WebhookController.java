package com.buurman.controller;

import com.buurman.config.models.SendGridProperties;
import com.buurman.config.models.TwilioProperties;
import com.buurman.service.notification.WebhookService;
import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.twilio.security.RequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Provider status callbacks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final WebhookService webhookService;
    private final TwilioProperties twilioProperties;
    private final SendGridProperties sendGridProperties;

    public WebhookController(WebhookService webhookService,
                             TwilioProperties twilioProperties,
                             SendGridProperties sendGridProperties) {
        this.webhookService = webhookService;
        this.twilioProperties = twilioProperties;
        this.sendGridProperties = sendGridProperties;
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
                return ResponseEntity.status(FORBIDDEN).build();
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
                return ResponseEntity.status(FORBIDDEN).body("<Response></Response>");
            }
            webhookService.processTwilioStatus(params);
        } catch (Exception e) {
            log.error("Error processing Twilio webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("<Response></Response>");
    }

    private boolean verifySendGridSignature(String payload, String signature, String timestamp) {
        String verificationKey = sendGridProperties.webhookVerificationKey();
        if (verificationKey == null || verificationKey.isBlank()) {
            return true; // Skip verification in dev
        }
        if (signature == null || timestamp == null) {
            return false;
        }
        try {
            EventWebhook eventWebhook = new EventWebhook();
            ECPublicKey publicKey = eventWebhook.ConvertPublicKeyToECDSA(verificationKey);
            return eventWebhook.VerifySignature(publicKey, payload, signature, timestamp);
        } catch (Exception e) {
            log.error("SendGrid signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyTwilioSignature(HttpServletRequest request, Map<String, String> params, String signature) {
        String authToken = twilioProperties.authToken();
        if (authToken == null || authToken.isBlank()) {
            return true; // Skip verification in dev
        }
        if (signature == null) {
            return false;
        }
        try {
            RequestValidator validator = new RequestValidator(authToken);
            String url = request.getRequestURL().toString();
            return validator.validate(url, params, signature);
        } catch (Exception e) {
            log.error("Twilio signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
