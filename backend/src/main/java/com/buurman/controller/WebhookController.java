package com.buurman.controller;

import com.buurman.service.notification.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Provider status callbacks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(summary = "SendGrid event webhook", description = "Receives delivery status events from SendGrid")
    @PostMapping("/sendgrid/events")
    public ResponseEntity<Void> handleSendGridEvents(@RequestBody String rawPayload) {
        try {
            webhookService.processSendGridEvents(rawPayload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing SendGrid webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }

    @Operation(summary = "Twilio status callback", description = "Receives SMS delivery status from Twilio")
    @PostMapping("/twilio/status")
    public ResponseEntity<String> handleTwilioStatus(@RequestParam Map<String, String> params) {
        try {
            webhookService.processTwilioStatus(params);
        } catch (Exception e) {
            log.error("Error processing Twilio webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("<Response></Response>");
    }
}
