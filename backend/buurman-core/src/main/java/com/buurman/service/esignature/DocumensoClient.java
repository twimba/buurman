package com.buurman.service.esignature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.DocumensoProperties;
import com.buurman.exception.ExternalServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DocumensoClient implements SignatureProviderClient {

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final @Nullable String webhookSecret;

  public DocumensoClient(
      RestClient documensoRestClient, ObjectMapper objectMapper, DocumensoProperties properties) {
    this.client = documensoRestClient;
    this.objectMapper = objectMapper;
    this.webhookSecret = properties.webhookSecret().orElse(null);
  }

  // Parse with the app's Jackson 2 ObjectMapper — Boot 4's RestClient converter is Jackson 3 and
  // can't bind to a Jackson 2 JsonNode directly (see PrometheusClient for the same pattern).
  private JsonNode readTree(@Nullable String body) throws JsonProcessingException {
    if (body == null || body.isBlank()) {
      return null;
    }
    return objectMapper.readTree(body);
  }

  @Override
  public SignatureSubmission createSubmission(
      byte[] pdfBytes, String fileName, List<SignerRequest> signers) {
    try {
      String envelopeId = createEnvelope(pdfBytes, fileName, signers);
      return distributeEnvelope(envelopeId);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException("Documenso submission failed", e);
    }
  }

  private String createEnvelope(byte[] pdfBytes, String fileName, List<SignerRequest> signers)
      throws Exception {
    List<Map<String, Object>> recipients =
        signers.stream()
            .map(
                s -> Map.<String, Object>of("email", s.email(), "name", s.name(), "role", "SIGNER"))
            .toList();
    Map<String, Object> payload =
        Map.of("type", "DOCUMENT", "title", fileName, "recipients", recipients);

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("payload", objectMapper.writeValueAsString(payload));
    parts.add(
        "files",
        new ByteArrayResource(pdfBytes) {
          @Override
          public String getFilename() {
            return fileName;
          }
        });

    String responseBody =
        client
            .post()
            .uri("/envelope/create")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(parts)
            .retrieve()
            .body(String.class);
    JsonNode response = readTree(responseBody);

    String envelopeId = response == null ? null : response.path("id").asText(null);
    if (envelopeId == null || envelopeId.isEmpty()) {
      throw new ExternalServiceException("Documenso did not return an envelope id");
    }
    return envelopeId;
  }

  private SignatureSubmission distributeEnvelope(String envelopeId) throws JsonProcessingException {
    String responseBody =
        client
            .post()
            .uri("/envelope/distribute")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("envelopeId", envelopeId))
            .retrieve()
            .body(String.class);
    JsonNode response = readTree(responseBody);

    if (response == null || !response.path("success").asBoolean(false)) {
      throw new ExternalServiceException("Documenso failed to distribute envelope " + envelopeId);
    }

    List<ProviderSigner> providerSigners = new ArrayList<>();
    for (JsonNode recipient : response.path("recipients")) {
      providerSigners.add(
          new ProviderSigner(recipient.path("id").asText(), recipient.path("email").asText()));
    }
    return new SignatureSubmission(envelopeId, providerSigners);
  }

  @Override
  public SignedDocument downloadCompleted(String providerSubmissionId) {
    try {
      String envelopeBody =
          client
              .get()
              .uri("/envelope/{envelopeId}", providerSubmissionId)
              .retrieve()
              .body(String.class);
      JsonNode envelope = readTree(envelopeBody);
      if (envelope == null) {
        throw new ExternalServiceException("Documenso envelope not found: " + providerSubmissionId);
      }

      String envelopeItemId = envelope.path("envelopeItems").path(0).path("id").asText(null);
      if (envelopeItemId == null) {
        throw new ExternalServiceException(
            "Documenso envelope has no items: " + providerSubmissionId);
      }

      byte[] signedPdf =
          client
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/envelope/item/{envelopeItemId}/download")
                          .queryParam("version", "signed")
                          .build(envelopeItemId))
              .retrieve()
              .body(byte[].class);
      byte[] certificate =
          client
              .get()
              .uri("/envelope/{envelopeId}/certificate/download", providerSubmissionId)
              .retrieve()
              .body(byte[].class);

      if (signedPdf == null || signedPdf.length == 0) {
        throw new ExternalServiceException("Documenso returned an empty signed document");
      }
      return new SignedDocument(signedPdf, certificate == null ? new byte[0] : certificate);
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException(
          "Documenso download failed for " + providerSubmissionId, e);
    }
  }

  @Override
  public boolean isValidWebhookSecret(@Nullable String providedSecret) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      return true; // no secret configured (e.g. local dev) — matches Mailgun/Twilio's unconfigured
      // behavior
    }
    if (providedSecret == null) {
      return false;
    }
    return MessageDigest.isEqual(
        providedSecret.getBytes(StandardCharsets.UTF_8),
        webhookSecret.getBytes(StandardCharsets.UTF_8));
  }
}
