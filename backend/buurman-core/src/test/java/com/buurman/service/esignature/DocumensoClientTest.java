package com.buurman.service.esignature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.DocumensoProperties;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

@DisplayName("DocumensoClient")
class DocumensoClientTest {

  private HttpServer server;
  private volatile @Nullable String capturedCreateBody;
  private volatile int createStatus = 200;
  private volatile String createResponse = "{\"id\":\"envelope_abc123\"}";
  private volatile String distributeResponse =
      "{\"success\":true,\"id\":\"envelope_abc123\",\"recipients\":"
          + "[{\"id\":1,\"email\":\"tenant@example.com\"}]}";
  private volatile String envelopeResponse =
      "{\"status\":\"COMPLETED\",\"envelopeItems\":[{\"id\":\"envelope_item_1\"}]}";

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/v2/envelope/create",
        exchange -> {
          capturedCreateBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
          byte[] body = createResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(createStatus, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/distribute",
        exchange -> {
          byte[] body = distributeResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/envelope_abc123",
        exchange -> {
          byte[] bytes = envelopeResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/item/envelope_item_1/download",
        exchange -> {
          byte[] pdf = "%PDF-1.7\nsigned".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, pdf.length);
          exchange.getResponseBody().write(pdf);
          exchange.close();
        });
    server.createContext(
        "/api/v2/envelope/envelope_abc123/certificate/download",
        exchange -> {
          byte[] pdf = "%PDF-1.7\ncertificate".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, pdf.length);
          exchange.getResponseBody().write(pdf);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private DocumensoClient client(String webhookSecret) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(5));
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    RestClient restClient =
        RestClient.builder().baseUrl(baseUrl + "/api/v2").requestFactory(factory).build();
    return new DocumensoClient(
        restClient,
        new ObjectMapper(),
        new DocumensoProperties(baseUrl, "api_test", Optional.of(webhookSecret)));
  }

  @Test
  @DisplayName(
      "createSubmission posts multipart payload+files, then distributes, and returns provider"
          + " signers")
  void createSubmissionSendsAndDistributes() {
    SignatureSubmission submission =
        client("secret")
            .createSubmission(
                "%PDF-1.7\ndoc".getBytes(StandardCharsets.UTF_8),
                "addendum.pdf",
                List.of(
                    new SignerRequest(
                        "tenant@example.com", "Jane Tenant", SignatureSignerRole.TENANT)));

    assertThat(submission.providerSubmissionId()).isEqualTo("envelope_abc123");
    assertThat(submission.signers()).hasSize(1);
    assertThat(submission.signers().get(0).email()).isEqualTo("tenant@example.com");
    assertThat(capturedCreateBody).contains("addendum.pdf").contains("\"role\":\"SIGNER\"");
  }

  @Test
  @DisplayName("distribute failure (success:false) maps to ExternalServiceException")
  void distributeFailureMapsToExternalServiceException() {
    distributeResponse = "{\"success\":false}";
    assertThatThrownBy(
            () ->
                client("secret")
                    .createSubmission(
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "x.pdf",
                        List.of(
                            new SignerRequest("a@example.com", "A", SignatureSignerRole.TENANT))))
        .isInstanceOf(ExternalServiceException.class);
  }

  @Test
  @DisplayName("create HTTP error maps to ExternalServiceException")
  void createHttpErrorMapsToExternalServiceException() {
    createStatus = 503;
    createResponse = "service unavailable";
    assertThatThrownBy(
            () ->
                client("secret")
                    .createSubmission(
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "x.pdf",
                        List.of(
                            new SignerRequest("a@example.com", "A", SignatureSignerRole.TENANT))))
        .isInstanceOf(ExternalServiceException.class);
  }

  @Test
  @DisplayName("downloadCompleted fetches envelope, then signed PDF + certificate")
  void downloadCompletedFetchesBothPdfs() {
    SignedDocument document = client("secret").downloadCompleted("envelope_abc123");

    assertThat(new String(document.signedPdfBytes(), 0, 4, StandardCharsets.UTF_8))
        .isEqualTo("%PDF");
    assertThat(new String(document.certificatePdfBytes(), 0, 4, StandardCharsets.UTF_8))
        .isEqualTo("%PDF");
  }

  @Test
  @DisplayName("isValidWebhookSecret: correct secret passes, wrong/missing secret fails")
  void webhookSecretValidation() {
    DocumensoClient c = client("expected-secret");
    assertThat(c.isValidWebhookSecret("expected-secret")).isTrue();
    assertThat(c.isValidWebhookSecret("wrong-secret")).isFalse();
    assertThat(c.isValidWebhookSecret(null)).isFalse();
  }

  @Test
  @DisplayName("isValidWebhookSecret: no secret configured rejects every webhook (fail closed)")
  void noSecretConfiguredRejectsEverything() {
    DocumensoClient c = client("");
    assertThat(c.isValidWebhookSecret(null)).isFalse();
    assertThat(c.isValidWebhookSecret("anything")).isFalse();
    assertThat(c.isValidWebhookSecret("")).isFalse();
  }

  @Test
  @DisplayName(
      "downloadCompleted refuses to download when the real envelope status is not COMPLETED")
  void downloadCompletedRejectsNonCompletedEnvelope() {
    envelopeResponse = "{\"status\":\"PENDING\",\"envelopeItems\":[{\"id\":\"envelope_item_1\"}]}";

    assertThatThrownBy(() -> client("secret").downloadCompleted("envelope_abc123"))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageContaining("not COMPLETED");
  }
}
