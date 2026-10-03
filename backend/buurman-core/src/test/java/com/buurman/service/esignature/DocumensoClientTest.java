package com.buurman.service.esignature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
      "{\"status\":\"COMPLETED\",\"envelopeItems\":[{\"id\":\"envelope_item_1\"}],"
          + "\"recipients\":[{\"id\":1,\"email\":\"tenant@example.com\"},"
          + "{\"id\":2,\"email\":\"a@example.com\"}]}";
  private volatile @Nullable String capturedFieldsBody;
  private volatile @Nullable String capturedCancelBody;
  private volatile int cancelStatus = 200;
  private volatile String cancelResponse = "{\"success\":true}";
  private final List<String> capturedFieldsBodies = new ArrayList<>();
  private final AtomicInteger fieldsCallCount = new AtomicInteger();

  /** When > 0, the first N calls to field/create-many return {@link #PLACEHOLDER_NOT_FOUND}. */
  private volatile int failFieldsCallsWithPlaceholderNotFound = 0;

  /** When true, every call to field/create-many returns {@link #UNRELATED_FIELD_ERROR} (400). */
  private volatile boolean failFieldsWithUnrelatedError = false;

  private static final String PLACEHOLDER_NOT_FOUND =
      "{\"message\":\"Placeholder \\\"signature-landlord\\\" not found in PDF\","
          + "\"code\":\"INTERNAL_SERVER_ERROR\"}";
  private static final String UNRELATED_FIELD_ERROR =
      "{\"message\":\"Recipient 999 not found\",\"code\":\"INTERNAL_SERVER_ERROR\"}";

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
        "/api/v2/envelope/field/create-many",
        exchange -> {
          capturedFieldsBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
          capturedFieldsBodies.add(capturedFieldsBody);
          int call = fieldsCallCount.incrementAndGet();
          byte[] body;
          int status;
          if (failFieldsWithUnrelatedError) {
            body = UNRELATED_FIELD_ERROR.getBytes(StandardCharsets.UTF_8);
            status = 400;
          } else if (call <= failFieldsCallsWithPlaceholderNotFound) {
            body = PLACEHOLDER_NOT_FOUND.getBytes(StandardCharsets.UTF_8);
            status = 400;
          } else {
            body = "{\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            status = 200;
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, body.length);
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
        "/api/v2/envelope/cancel",
        exchange -> {
          capturedCancelBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
          byte[] body = cancelResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(cancelStatus, body.length);
          exchange.getResponseBody().write(body);
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
        new DocumensoProperties(baseUrl, "api_test", Optional.of(webhookSecret), Optional.empty()));
  }

  private DocumensoClient clientWithPublicUrl(@Nullable String publicUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(5));
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    RestClient restClient =
        RestClient.builder().baseUrl(baseUrl + "/api/v2").requestFactory(factory).build();
    return new DocumensoClient(
        restClient,
        new ObjectMapper(),
        new DocumensoProperties(
            baseUrl, "api_test", Optional.of("secret"), Optional.ofNullable(publicUrl)));
  }

  private static final String RECIPIENTS_ENVELOPE =
      "{\"status\":\"PENDING\",\"recipients\":["
          + "{\"id\":7,\"name\":\"Lena Landlord\",\"email\":\"l@example.com\","
          + "\"token\":\"tok_landlord\",\"signingStatus\":\"NOT_SIGNED\"},"
          + "{\"id\":8,\"name\":\"t@example.com\",\"email\":\"t@example.com\","
          + "\"token\":\"tok_tenant\",\"signingStatus\":\"SIGNED\"},"
          + "{\"id\":10,\"name\":\"\",\"email\":\"blank@example.com\",\"token\":\"t\"},"
          + "{\"id\":9,\"name\":\"No Token\",\"email\":\"n@example.com\"}]}";

  @Test
  @DisplayName(
      "fetchSigningLinks maps every recipient, building the link from the token on the PUBLIC url")
  void fetchSigningLinksBuildsPublicLinks() {
    envelopeResponse = RECIPIENTS_ENVELOPE;

    List<ProviderSigningLink> links =
        clientWithPublicUrl("https://sign.example.com/").fetchSigningLinks("envelope_abc123");

    assertThat(links).hasSize(4);
    assertThat(links.get(0).providerSignerId()).isEqualTo("7");
    assertThat(links.get(0).name()).isEqualTo("Lena Landlord");
    assertThat(links.get(0).email()).isEqualTo("l@example.com");
    assertThat(links.get(0).signingUrl()).contains("https://sign.example.com/sign/tok_landlord");
    assertThat(links.get(1).signingUrl()).contains("https://sign.example.com/sign/tok_tenant");
    assertThat(links.get(2).name()).isEqualTo("blank@example.com");
    assertThat(links.get(3).signingUrl()).isEmpty();
  }

  @Test
  @DisplayName("fetchSigningLinks falls back to the base url when no public url is configured")
  void fetchSigningLinksFallsBackToBaseUrl() {
    envelopeResponse = RECIPIENTS_ENVELOPE;

    List<ProviderSigningLink> links =
        clientWithPublicUrl(null).fetchSigningLinks("envelope_abc123");

    assertThat(links.get(0).signingUrl().orElseThrow())
        .startsWith("http://127.0.0.1:" + server.getAddress().getPort() + "/sign/tok_landlord");
  }

  @Test
  @DisplayName("fetchSigningLinks failure maps to ExternalServiceException without echoing tokens")
  void fetchSigningLinksFailureDoesNotLeakTokens() {
    envelopeResponse = "not json tok_secret_value";

    assertThatThrownBy(
            () -> clientWithPublicUrl("https://x.example").fetchSigningLinks("envelope_abc123"))
        .isInstanceOf(ExternalServiceException.class)
        .satisfies(
            e -> {
              assertThat(e.getMessage()).doesNotContain("tok_secret_value");
              assertThat(e.getCause()).isNull();
            });
  }

  @Test
  @DisplayName("fetchSigningLinks on an unknown envelope maps to ExternalServiceException")
  void fetchSigningLinksUnknownEnvelope() {
    assertThatThrownBy(() -> clientWithPublicUrl("https://x.example").fetchSigningLinks("nope"))
        .isInstanceOf(ExternalServiceException.class);
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
                        "tenant@example.com",
                        "Jane Tenant",
                        SignatureSignerRole.TENANT,
                        "signature-tenant-1")));

    assertThat(submission.providerSubmissionId()).isEqualTo("envelope_abc123");
    assertThat(submission.signers()).hasSize(1);
    assertThat(submission.signers().get(0).email()).isEqualTo("tenant@example.com");
    assertThat(capturedCreateBody).contains("addendum.pdf").contains("\"role\":\"SIGNER\"");
    assertThat(capturedFieldsBody)
        .contains("\"recipientId\":1")
        .contains("\"type\":\"SIGNATURE\"")
        .contains("\"placeholder\":\"signature-tenant-1\"");
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
                            new SignerRequest(
                                "a@example.com",
                                "A",
                                SignatureSignerRole.TENANT,
                                "signature-tenant-1"))))
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
                            new SignerRequest(
                                "a@example.com",
                                "A",
                                SignatureSignerRole.TENANT,
                                "signature-tenant-1"))))
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
  @DisplayName(
      "createSubmission fails loudly when Documenso's envelope has no recipient matching a"
          + " signer's email, instead of silently skipping that signer's field")
  void createSubmissionFailsWhenNoRecipientMatchesSignerEmail() {
    assertThatThrownBy(
            () ->
                client("secret")
                    .createSubmission(
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "x.pdf",
                        List.of(
                            new SignerRequest(
                                "unmatched@example.com",
                                "Nobody",
                                SignatureSignerRole.TENANT,
                                "signature-tenant-1"))))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageContaining("no recipient matching signer unmatched@example.com");
  }

  @Test
  @DisplayName(
      "createSubmission falls back to coordinate-based fields when Documenso reports the"
          + " placeholder wasn't found (e.g. a demo-data PDF or a landlord's own upload, which"
          + " was never rendered with placeholder text)")
  void createSubmissionFallsBackToCoordinatesWhenPlaceholderMissing() {
    failFieldsCallsWithPlaceholderNotFound = 1;

    SignatureSubmission submission =
        client("secret")
            .createSubmission(
                "%PDF-1.7\ndoc".getBytes(StandardCharsets.UTF_8),
                "uploaded-scan.pdf",
                List.of(
                    new SignerRequest(
                        "tenant@example.com",
                        "Jane Tenant",
                        SignatureSignerRole.TENANT,
                        "signature-tenant-1")));

    assertThat(submission.providerSubmissionId()).isEqualTo("envelope_abc123");
    assertThat(capturedFieldsBodies).hasSize(2);
    assertThat(capturedFieldsBodies.get(0)).contains("\"placeholder\":\"signature-tenant-1\"");
    assertThat(capturedFieldsBodies.get(1))
        .contains("\"recipientId\":1")
        .contains("\"type\":\"SIGNATURE\"")
        .contains("\"page\":1")
        .doesNotContain("placeholder");
  }

  @Test
  @DisplayName(
      "createSubmission rethrows a field-creation error unrelated to a missing placeholder")
  void createSubmissionRethrowsUnrelatedFieldCreationError() {
    // A different 400 (e.g. a malformed request) must NOT trigger the coordinate-fallback
    // retry — only "not found in PDF" should.
    failFieldsWithUnrelatedError = true;
    assertThatThrownBy(
            () ->
                client("secret")
                    .createSubmission(
                        "%PDF".getBytes(StandardCharsets.UTF_8),
                        "x.pdf",
                        List.of(
                            new SignerRequest(
                                "tenant@example.com",
                                "Jane Tenant",
                                SignatureSignerRole.TENANT,
                                "signature-tenant-1"))))
        .isInstanceOf(ExternalServiceException.class);
    // Only the one (failed, placeholder-based) field-creation call happened — no retry.
    assertThat(capturedFieldsBodies).hasSize(1);
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
  @DisplayName("cancelSubmission posts envelopeId and reason, succeeds on success:true")
  void cancelSubmissionPostsReason() {
    client("secret").cancelSubmission("envelope_abc123", "Tenant moved out before signing");

    assertThat(capturedCancelBody)
        .contains("\"envelopeId\":\"envelope_abc123\"")
        .contains("\"reason\":\"Tenant moved out before signing\"");
  }

  @Test
  @DisplayName("cancelSubmission omits reason from the payload when none is given")
  void cancelSubmissionOmitsBlankReason() {
    client("secret").cancelSubmission("envelope_abc123", null);

    assertThat(capturedCancelBody)
        .contains("\"envelopeId\":\"envelope_abc123\"")
        .doesNotContain("reason");
  }

  @Test
  @DisplayName("cancelSubmission failure (success:false) maps to ExternalServiceException")
  void cancelSubmissionFailureMapsToExternalServiceException() {
    cancelResponse = "{\"success\":false}";
    assertThatThrownBy(() -> client("secret").cancelSubmission("envelope_abc123", null))
        .isInstanceOf(ExternalServiceException.class);
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
