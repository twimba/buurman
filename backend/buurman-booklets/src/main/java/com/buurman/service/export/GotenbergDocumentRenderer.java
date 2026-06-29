package com.buurman.service.export;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.buurman.exception.DocumentRenderException;

/**
 * Headless-Chromium implementation of {@link DocumentRenderer}, delegating to a <a
 * href="https://gotenberg.dev">Gotenberg</a> sidecar over HTTP. Chromium gives the full modern
 * CSS/typography ceiling (grid, flexbox, embedded WOFF2/variable fonts, flawless multilingual
 * shaping) and is the sole booklet/PDF renderer.
 *
 * <p>The sidecar URL is {@code booklet.gotenberg.url} (Docker/Dokploy: {@code
 * http://gotenberg:3000}; local host-run backend: {@code http://localhost:3000}). Fonts (Satoshi +
 * Noto) are baked into the custom Gotenberg image (see {@code docker/gotenberg}).
 */
@Component
class GotenbergDocumentRenderer implements DocumentRenderer {

  /** A4 in inches — Gotenberg/Chromium express paper size in inches. */
  private static final String A4_WIDTH_IN = "8.27";

  private static final String A4_HEIGHT_IN = "11.69";

  private final RestClient client;

  GotenbergDocumentRenderer(
      @Value("${booklet.gotenberg.url}") String baseUrl,
      @Value("${booklet.gotenberg.connect-timeout:5s}") Duration connectTimeout,
      @Value("${booklet.gotenberg.read-timeout:30s}") Duration readTimeout) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }

  @Override
  public byte[] render(String html, PageSpec page) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("files", indexHtml(html));
    parts.add("paperWidth", A4_WIDTH_IN);
    parts.add("paperHeight", A4_HEIGHT_IN);
    parts.add("landscape", Boolean.toString(page.isLandscape()));
    // Inner margins are owned by the template's CSS @page rules — keep Chromium's at zero.
    parts.add("marginTop", "0");
    parts.add("marginBottom", "0");
    parts.add("marginLeft", "0");
    parts.add("marginRight", "0");
    // Brand bands and tinted tiles are backgrounds; Chromium drops them unless asked.
    parts.add("printBackground", "true");

    try {
      byte[] pdf =
          client
              .post()
              .uri("/forms/chromium/convert/html")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(parts)
              .retrieve()
              .body(byte[].class);
      if (pdf == null || pdf.length == 0) {
        throw new DocumentRenderException("Gotenberg returned an empty PDF");
      }
      return pdf;
    } catch (DocumentRenderException e) {
      throw e;
    } catch (Exception e) {
      throw new DocumentRenderException("Failed to generate PDF via Gotenberg", e);
    }
  }

  private static ByteArrayResource indexHtml(String html) {
    return new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        // Gotenberg's Chromium HTML route requires the entrypoint file to be named index.html.
        return "index.html";
      }
    };
  }
}
