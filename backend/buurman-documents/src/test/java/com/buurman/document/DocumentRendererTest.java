package com.buurman.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.buurman.exception.ExternalServiceException;
import com.sun.net.httpserver.HttpServer;

/**
 * Drives {@link DocumentRenderer} against a throwaway local HTTP server (no real Gotenberg) to
 * verify the multipart request shape, brand-font attachment + {@code @font-face} injection,
 * landscape flag, and that sidecar failures surface as a 502-mapped {@link
 * ExternalServiceException}.
 */
@DisplayName("DocumentRenderer")
class DocumentRendererTest {

  private HttpServer server;
  private volatile @Nullable String capturedBody;
  private volatile int responseStatus = 200;
  private volatile byte[] responseBody = "%PDF-1.7\nrendered".getBytes(StandardCharsets.UTF_8);

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/forms/chromium/convert/html",
        exchange -> {
          capturedBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
          exchange.sendResponseHeaders(responseStatus, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  private DocumentRenderer renderer(Resource brandFont) {
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    return new DocumentRenderer(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(5), brandFont);
  }

  @Test
  @DisplayName("posts index.html + brand font, injects @font-face, sets landscape, returns the PDF")
  void rendersWithBrandFont() {
    byte[] pdf =
        renderer(new ClassPathResource("fonts/Satoshi-Variable.woff2"))
            .render(
                "<html><head><title>t</title></head><body>hi</body></html>", PageSpec.A4_LANDSCAPE);

    assertThat(new String(pdf, 0, 4, StandardCharsets.UTF_8)).isEqualTo("%PDF");
    assertThat(capturedBody)
        .contains("filename=\"index.html\"")
        .contains("filename=\"Satoshi-Variable.woff2\"")
        .contains("@font-face")
        .contains("Satoshi-Variable.woff2') format('woff2')")
        .contains("name=\"landscape\"")
        .contains("name=\"printBackground\"");
  }

  @Test
  @DisplayName("works without a brand font (no @font-face, no font part) — Noto fallback")
  void rendersWithoutBrandFont() {
    byte[] pdf =
        renderer(
                new ByteArrayResource(new byte[0]) {
                  @Override
                  public boolean exists() {
                    return false;
                  }
                })
            .render("<html><head></head><body>hi</body></html>", PageSpec.A4_PORTRAIT);

    assertThat(new String(pdf, 0, 4, StandardCharsets.UTF_8)).isEqualTo("%PDF");
    assertThat(capturedBody).doesNotContain("@font-face").doesNotContain("Satoshi-Variable.woff2");
  }

  @Test
  @DisplayName("maps an empty Gotenberg response to ExternalServiceException (502)")
  void emptyResponseFails() {
    responseBody = new byte[0];
    assertThatThrownBy(
            () ->
                renderer(new ClassPathResource("fonts/Satoshi-Variable.woff2"))
                    .render("<html><head></head><body/></html>", PageSpec.A4_PORTRAIT))
        .isInstanceOf(ExternalServiceException.class);
  }

  @Test
  @DisplayName("maps a sidecar HTTP error to ExternalServiceException (502)")
  void httpErrorFails() {
    responseStatus = 503;
    responseBody = "service unavailable".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(
            () ->
                renderer(new ClassPathResource("fonts/Satoshi-Variable.woff2"))
                    .render("<html><head></head><body/></html>", PageSpec.A4_PORTRAIT))
        .isInstanceOf(ExternalServiceException.class);
  }
}
