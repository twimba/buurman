package com.buurman.controller;

import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.util.Map;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.config.models.SendGridProperties;
import com.buurman.config.models.TwilioProperties;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.WebhooksApi;
import com.buurman.service.notification.WebhookService;
import com.sendgrid.helpers.eventwebhook.EventWebhook;
import com.twilio.security.RequestValidator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class WebhookController implements WebhooksApi {

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  private final WebhookService webhookService;
  private final TwilioProperties twilioProperties;
  private final SendGridProperties sendGridProperties;
  private final HttpServletRequest httpServletRequest;

  @Override
  public void handleSendGridEvents(
      String body,
      String xTwilioEmailEventWebhookSignature,
      String xTwilioEmailEventWebhookTimestamp) {
    try {
      if (!verifySendGridSignature(
          body, xTwilioEmailEventWebhookSignature, xTwilioEmailEventWebhookTimestamp)) {
        log.warn("SendGrid webhook signature verification failed");
        throw new ForbiddenException("SendGrid webhook signature verification failed");
      }
      webhookService.processSendGridEvents(body);
    } catch (ForbiddenException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing SendGrid webhook: {}", e.getMessage(), e);
    }
  }

  @Override
  public String handleTwilioStatus(Map<String, String> params, String xTwilioSignature) {
    try {
      if (!verifyTwilioSignature(httpServletRequest, params, xTwilioSignature)) {
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
