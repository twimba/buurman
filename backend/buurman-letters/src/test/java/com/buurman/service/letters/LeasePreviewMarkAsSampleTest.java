package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeasePreviewService.markAsSample")
class LeasePreviewMarkAsSampleTest {

  private static final String CSP_START = "<meta http-equiv=\"Content-Security-Policy\"";

  @Test
  @DisplayName("the CSP meta follows <head> immediately and the banner follows <body>")
  void normalHtml() {
    String html =
        "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>a{}</style></head>"
            + "<body><p>hi</p></body></html>";

    String out = LeasePreviewService.markAsSample(html);

    assertThat(out).contains("<head>" + CSP_START);
    assertThat(out.indexOf(CSP_START)).isLessThan(out.indexOf("<style>a{}"));
    assertThat(out).contains("<body><div class=\"sample-banner\">");
    assertThat(out).startsWith("<!DOCTYPE html>");
  }

  @Test
  @DisplayName("attributes on head and body are tolerated")
  void attributeBearingTags() {
    String html =
        "<html><head lang=\"nl\"><style>a{}</style></head><body class=\"x\">b</body></html>";

    String out = LeasePreviewService.markAsSample(html);

    assertThat(out).contains("<head lang=\"nl\">" + CSP_START);
    assertThat(out).contains("<body class=\"x\"><div class=\"sample-banner\">");
  }

  @Test
  @DisplayName("<header> is not <head>: without a real head it fails closed")
  void headerIsNotHead() {
    String html = "<html><header>h</header><body>b</body></html>";

    assertThatThrownBy(() -> LeasePreviewService.markAsSample(html))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("a missing body fails closed instead of prepending before the doctype")
  void missingBody() {
    assertThatThrownBy(
            () ->
                LeasePreviewService.markAsSample(
                    "<!DOCTYPE html><html><head></head><p>x</p></html>"))
        .isInstanceOf(IllegalStateException.class);
  }
}
