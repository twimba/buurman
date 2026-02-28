package com.buurman.controller;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.config.models.SendGridProperties;
import com.buurman.config.models.TwilioProperties;
import com.buurman.service.notification.WebhookService;
import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.twilio.security.RequestValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Provider status callbacks")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  private final WebhookService webhookService;
  private final TwilioProperties twilioProperties;
  private final SendGridProperties sendGridProperties;

  @Operation(
      summary = "SendGrid event webhook",
      description = "Receives delivery status events from SendGrid")
  @PostMapping("/sendgrid/events")
  public ResponseEntity<Void> handleSendGridEvents(
      @Parameter(description = "Raw JSON event payload") @RequestBody String rawPayload,
      @Parameter(description = "SendGrid ECDSA signature")
          @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Signature", required = false)
          @Nullable String signature,
      @Parameter(description = "SendGrid event timestamp")
          @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Timestamp", required = false)
          @Nullable String timestamp) {
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

  @Operation(
      summary = "Twilio status callback",
      description = "Receives SMS delivery status from Twilio")
  @PostMapping("/twilio/status")
  public ResponseEntity<String> handleTwilioStatus(
      @Parameter(description = "Twilio status callback form parameters") @RequestParam
          Map<String, String> params,
      @Parameter(description = "Twilio request signature")
          @RequestHeader(value = "X-Twilio-Signature", required = false)
          @Nullable String twilioSignature,
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

  private boolean verifySendGridSignature(
      String payload, @Nullable String signature, @Nullable String timestamp) {
    Optional<String> verificationKey =
        sendGridProperties.webhookVerificationKey().filter(k -> !k.isBlank());
    if (verificationKey.isEmpty()) {
      return true; // Skip verification in dev
    }
    if (signature == null || timestamp == null) {
      return false;
    }
    try {
      EventWebhook eventWebhook = new EventWebhook();
      ECPublicKey publicKey =
          eventWebhook.ConvertPublicKeyToECDSA(
              verificationKey.orElseThrow(
                  () ->
                      new IllegalStateException("Verification key verified present but missing")));
      return eventWebhook.VerifySignature(publicKey, payload, signature, timestamp);
    } catch (Exception e) {
      log.error("SendGrid signature verification error: {}", e.getMessage());
      return false;
    }
  }

  private boolean verifyTwilioSignature(
      HttpServletRequest request, Map<String, String> params, @Nullable String signature) {
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
