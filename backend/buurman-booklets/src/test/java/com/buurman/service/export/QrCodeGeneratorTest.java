package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QrCodeGenerator")
class QrCodeGeneratorTest {

  private final QrCodeGenerator generator = new QrCodeGenerator();

  @Test
  @DisplayName("emits a base64 SVG data URI with QR module paths")
  void emitsSvgDataUri() {
    String uri = generator.toSvgDataUri("https://app.buurman.io/p/P-001");

    assertThat(uri).startsWith("data:image/svg+xml;base64,");
    String svg =
        new String(
            Base64.getDecoder().decode(uri.substring("data:image/svg+xml;base64,".length())),
            StandardCharsets.UTF_8);
    assertThat(svg)
        .contains("<svg")
        .contains("viewBox=\"0 0")
        .contains("<path")
        .contains("h1v1h-1z");
  }

  @Test
  @DisplayName("produces a square viewBox and different output for different content")
  void distinctOutput() {
    String a = generator.toSvg("C-2024-001");
    String b = generator.toSvg("C-2024-002");

    assertThat(a).matches("(?s).*viewBox=\"0 0 (\\d+) \\1\".*"); // square
    assertThat(a).isNotEqualTo(b);
  }
}
