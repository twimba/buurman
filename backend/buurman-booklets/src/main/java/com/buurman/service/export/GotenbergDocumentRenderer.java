package com.buurman.service.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.buurman.exception.ExternalServiceException;

/**
 * Headless-Chromium implementation of {@link DocumentRenderer}, delegating to a <a
 * href="https://gotenberg.dev">Gotenberg</a> sidecar over HTTP. Chromium gives the full modern
 * CSS/typography ceiling (grid, flexbox, embedded WOFF2/variable fonts, flawless multilingual
 * shaping) and is the sole booklet/PDF renderer.
 *
 * <p>The sidecar URL is {@code booklet.gotenberg.url} (Docker/Dokploy: {@code
 * http://gotenberg:3000}; local host-run backend: {@code http://localhost:3000}).
 *
 * <p>Fonts: the <b>brand</b> face (Satoshi) is shipped with each request — its WOFF2 is attached as
 * a sibling asset and an {@code @font-face} is injected into the HTML head, so Chromium embeds it
 * without relying on the image. <b>Fallback</b> faces (Noto Sans, covering Latin-extended + Greek +
 * Cyrillic so non-Latin locales render without tofu) are baked into the custom Gotenberg image (see
 * {@code docker/gotenberg}). If the brand WOFF2 is absent from the classpath, rendering still works
 * (Noto fallback) — no hard failure.
 */
@Component
class GotenbergDocumentRenderer implements DocumentRenderer {

  /** A4 in inches — Gotenberg/Chromium express paper size in inches. */
  private static final String A4_WIDTH_IN = "8.27";

  private static final String A4_HEIGHT_IN = "11.69";

  private static final String BRAND_FONT_FILE = "Satoshi-Variable.woff2";

  private final RestClient client;
  private final byte @Nullable [] brandFont;

  GotenbergDocumentRenderer(
      @Value("${booklet.gotenberg.url}") String baseUrl,
      @Value("${booklet.gotenberg.connect-timeout:5s}") Duration connectTimeout,
      @Value("${booklet.gotenberg.read-timeout:30s}") Duration readTimeout,
      @Value("${booklet.gotenberg.brand-font:classpath:fonts/" + BRAND_FONT_FILE + "}")
          Resource brandFontResource) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    this.brandFont = readBrandFont(brandFontResource);
  }

  private static byte @Nullable [] readBrandFont(Resource resource) {
    if (!resource.exists()) {
      return null;
    }
    try (var in = resource.getInputStream()) {
      return in.readAllBytes();
    } catch (IOException e) {
      return null; // brand font is best-effort; fall back to Noto rather than fail rendering
    }
  }

  @Override
  public byte[] render(String html, PageSpec page) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("files", asset(withBrandFont(html).getBytes(StandardCharsets.UTF_8), "index.html"));
    if (brandFont != null) {
      // Ship the brand WOFF2 as a sibling asset so the injected @font-face resolves.
      parts.add("files", asset(brandFont, BRAND_FONT_FILE));
    }
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

    byte[] pdf;
    try {
      pdf =
          client
              .post()
              .uri("/forms/chromium/convert/html")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(parts)
              .retrieve()
              .body(byte[].class);
    } catch (Exception e) {
      // Sidecar unreachable / timed out / non-2xx → upstream dependency failure (HTTP 502).
      throw new ExternalServiceException("Gotenberg PDF rendering failed", e);
    }
    if (pdf == null || pdf.length == 0) {
      throw new ExternalServiceException("Gotenberg returned an empty PDF");
    }
    return pdf;
  }

  /**
   * Injects the brand {@code @font-face} into the HTML head (no-op when the WOFF2 is unavailable).
   */
  private String withBrandFont(String html) {
    if (brandFont == null) {
      return html;
    }
    String fontFace =
        "<style>@font-face{font-family:'Satoshi';"
            + "src:url('"
            + BRAND_FONT_FILE
            + "') format('woff2');font-weight:300"
            + " 900;font-style:normal;font-display:swap;}</style>";
    int head = html.indexOf("</head>");
    return head < 0 ? fontFace + html : html.substring(0, head) + fontFace + html.substring(head);
  }

  private static ByteArrayResource asset(byte[] bytes, String filename) {
    // Gotenberg's Chromium HTML route requires the entrypoint to be named index.html; sibling
    // assets (fonts) are addressable by their filename from the HTML.
    return new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return filename;
      }
    };
  }
}
