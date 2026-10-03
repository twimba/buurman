package com.buurman.service.esignature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
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
      createSignatureFields(envelopeId, signers);
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

  /**
   * Documenso refuses to distribute an envelope whose recipients have no fields to sign — see
   * {@code create-envelope.js}'s MISSING_SIGNATURE_FIELD check. A field is placed per signer by
   * matching that signer's {@code placeholder} text (e.g. {@code "signature-landlord"}) against the
   * PDF's own text content, so the document template must render that exact literal text at the
   * spot the signature belongs (see {@code SignerRequest}).
   *
   * <p>Not every PDF sent for signature was rendered by one of our own templates — a demo-data
   * fixture or a landlord's own uploaded scan has no placeholder text at all. When Documenso
   * reports the placeholder wasn't found, this falls back to fixed coordinates near the bottom of
   * the first page (stacked per signer) instead of failing the whole request. Documenso rejects the
   * entire batch if even one placeholder in it is missing, so the fallback re-submits every
   * signer's field with coordinates, not just the one that was missing.
   *
   * <p>{@code envelope/create}'s response carries no recipient ids — a fresh {@code GET} is the
   * only way to resolve each signer's email to the recipient id {@code envelope/field/create-many}
   * requires.
   */
  private void createSignatureFields(String envelopeId, List<SignerRequest> signers)
      throws JsonProcessingException {
    String envelopeBody =
        client.get().uri("/envelope/{envelopeId}", envelopeId).retrieve().body(String.class);
    JsonNode envelope = readTree(envelopeBody);
    if (envelope == null) {
      throw new ExternalServiceException("Documenso envelope not found: " + envelopeId);
    }

    Map<String, Integer> recipientIdByEmail = new HashMap<>();
    for (JsonNode recipient : envelope.path("recipients")) {
      String email = recipient.path("email").asText(null);
      if (email != null) {
        recipientIdByEmail.put(email.toLowerCase(Locale.ROOT), recipient.path("id").asInt());
      }
    }

    List<Integer> recipientIds = new ArrayList<>();
    List<Map<String, Object>> placeholderFields = new ArrayList<>();
    for (SignerRequest signer : signers) {
      Integer recipientId = recipientIdByEmail.get(signer.email().toLowerCase(Locale.ROOT));
      if (recipientId == null) {
        throw new ExternalServiceException(
            "Documenso envelope "
                + envelopeId
                + " has no recipient matching signer "
                + signer.email());
      }
      recipientIds.add(recipientId);
      placeholderFields.add(
          Map.of(
              "recipientId",
              recipientId,
              "type",
              "SIGNATURE",
              "placeholder",
              signer.placeholder()));
    }

    try {
      createFields(envelopeId, placeholderFields);
    } catch (HttpClientErrorException e) {
      if (!isPlaceholderNotFoundError(e)) {
        throw e;
      }
      createFields(envelopeId, coordinateFields(recipientIds));
    }
  }

  private void createFields(String envelopeId, List<Map<String, Object>> fields) {
    client
        .post()
        .uri("/envelope/field/create-many")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("envelopeId", envelopeId, "data", fields))
        .retrieve()
        .body(String.class);
  }

  private static boolean isPlaceholderNotFoundError(HttpClientErrorException e) {
    String body = e.getResponseBodyAsString();
    return body != null && body.contains("not found in PDF");
  }

  /**
   * One signature field per recipient, stacked upward from the bottom-left of the first page.
   * Percentage-based coordinates (Documenso's {@code positionY} is measured from the top), so this
   * works regardless of the page's actual size. Page 1 is a deliberate, universal choice — we have
   * no reliable way to know the document's real page count here, and every PDF has a first page.
   */
  private static List<Map<String, Object>> coordinateFields(List<Integer> recipientIds) {
    List<Map<String, Object>> fields = new ArrayList<>();
    for (int i = 0; i < recipientIds.size(); i++) {
      fields.add(
          Map.of(
              "recipientId", recipientIds.get(i),
              "type", "SIGNATURE",
              "page", 1,
              "positionX", 10,
              "positionY", Math.max(5, 90 - i * 8),
              "width", 25,
              "height", 5));
    }
    return fields;
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

      // The webhook payload's claimed event ("DOCUMENT_COMPLETED") is untrusted input — a forged
      // webhook could claim completion for an envelope that was never actually signed. Before
      // downloading and persisting a PDF as "signed", re-verify the envelope's real status
      // directly from Documenso.
      String envelopeStatus = envelope.path("status").asText(null);
      if (!"COMPLETED".equals(envelopeStatus)) {
        throw new ExternalServiceException(
            "Documenso envelope "
                + providerSubmissionId
                + " is not COMPLETED (status="
                + envelopeStatus
                + "); refusing to download as signed");
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
      // Fail closed. Unlike Mailgun/Twilio (delivery-status-only webhooks), a forged Documenso
      // webhook can mark a signature request DECLINED/CANCELLED/COMPLETED and, on COMPLETED,
      // cause a PDF to be stored as "signed" — so an unconfigured secret must reject every
      // webhook rather than accept them unauthenticated. Configure DOCUMENSO_WEBHOOK_SECRET
      // (on both this app and the Documenso sidecar) to enable the webhook.
      return false;
    }
    if (providedSecret == null) {
      return false;
    }
    return MessageDigest.isEqual(
        providedSecret.getBytes(StandardCharsets.UTF_8),
        webhookSecret.getBytes(StandardCharsets.UTF_8));
  }
}
