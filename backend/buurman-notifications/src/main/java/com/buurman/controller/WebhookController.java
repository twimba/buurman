package com.buurman.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.config.models.MailgunProperties;
import com.buurman.config.models.TwilioProperties;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.WebhooksApi;
import com.buurman.service.notification.SignatureWebhookService;
import com.buurman.service.notification.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.security.RequestValidator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class WebhookController implements WebhooksApi {

  private final WebhookService webhookService;
  private final SignatureWebhookService signatureWebhookService;
  private final TwilioProperties twilioProperties;
  private final MailgunProperties mailgunProperties;
  private final ObjectMapper objectMapper;
  private final HttpServletRequest httpServletRequest;

  @Override
  public void handleMailgunEvents(String body) {
    try {
      if (!verifyMailgunSignature(body)) {
        log.warn("Mailgun webhook signature verification failed");
        throw new ForbiddenException("Mailgun webhook signature verification failed");
      }
      webhookService.processMailgunEvents(body);
    } catch (ForbiddenException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing Mailgun webhook: {}", e.getMessage(), e);
    }
  }

  @Override
  public String handleTwilioStatus(Map<String, String> params, Optional<String> xTwilioSignature) {
    try {
      if (!verifyTwilioSignature(httpServletRequest, params, xTwilioSignature.orElse(null))) {
        log.warn("Twilio webhook signature verification failed");
        throw new ForbiddenException("Twilio webhook signature verification failed");
      }
      webhookService.processTwilioStatus(params);
    } catch (ForbiddenException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing Twilio webhook: {}", e.getMessage(), e);
    }
    return "<Response></Response>";
  }

  @Override
  public void handleDocumensoEvents(String body, Optional<String> xDocumensoSecret) {
    // No catch here on purpose — see processDocumensoEvent's javadoc. A processing failure must
    // reach the caller as a 5xx so Documenso retries the delivery instead of the event being
    // silently lost.
    signatureWebhookService.processDocumensoEvent(body, xDocumensoSecret.orElse(null));
  }

  private boolean verifyMailgunSignature(String body) {
    Optional<String> signingKey = mailgunProperties.webhookSigningKey().filter(k -> !k.isBlank());
    if (signingKey.isEmpty()) {
      return true;
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode sig = root.path("signature");
      String timestamp = sig.path("timestamp").asText();
      String token = sig.path("token").asText();
      String signature = sig.path("signature").asText();

      if (timestamp.isEmpty() || token.isEmpty() || signature.isEmpty()) {
        return false;
      }

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(
          new SecretKeySpec(
              signingKey.orElseThrow().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal((timestamp + token).getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash).equals(signature);
    } catch (Exception e) {
      log.error("Mailgun signature verification error: {}", e.getMessage());
      return false;
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private boolean verifyTwilioSignature(
      HttpServletRequest request, Map<String, String> params, @Nullable String signature) {
    String authToken = twilioProperties.authToken();
    if (authToken == null || authToken.isBlank()) {
      return true;
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
