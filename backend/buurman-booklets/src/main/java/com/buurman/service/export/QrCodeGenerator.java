package com.buurman.service.export;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.buurman.exception.DocumentRenderException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Generates QR codes for the summary cards as a resolution-independent inline SVG {@code data:}
 * URI, so the booklet HTML stays self-contained (no external image fetch, crisp at any print size).
 * Renders the ZXing {@link BitMatrix} straight to SVG rects — no AWT/{@code javase} dependency.
 */
@Component
public class QrCodeGenerator {

  private static final int QUIET_ZONE_MODULES = 2;

  /**
   * Encodes {@code content} (e.g. a deep link {@code https://app.buurman.io/p/P-001}) as a QR code
   * and returns it as {@code data:image/svg+xml;base64,...} suitable for an {@code <img src>}.
   */
  public String toSvgDataUri(String content) {
    String svg = toSvg(content);
    String base64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    return "data:image/svg+xml;base64," + base64;
  }

  /** Encodes {@code content} as a standalone, viewBox-scaled SVG string (1 module = 1 unit). */
  public String toSvg(String content) {
    BitMatrix matrix = encode(content);
    int size = matrix.getWidth(); // square; quiet zone already baked in via the margin hint
    StringBuilder path = new StringBuilder();
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        if (matrix.get(x, y)) {
          path.append("M").append(x).append(" ").append(y).append("h1v1h-1z");
        }
      }
    }
    return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 "
        + size
        + " "
        + size
        + "\" shape-rendering=\"crispEdges\">"
        + "<path fill=\"#0c4a6e\" d=\""
        + path
        + "\"/></svg>";
  }

  private static BitMatrix encode(String content) {
    Map<EncodeHintType, Object> hints =
        Map.of(
            EncodeHintType.ERROR_CORRECTION,
            ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN,
            QUIET_ZONE_MODULES,
            EncodeHintType.CHARACTER_SET,
            StandardCharsets.UTF_8.name());
    try {
      // width/height 0 lets ZXing pick the natural module grid; we scale via the SVG viewBox.
      return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
    } catch (WriterException e) {
      throw new DocumentRenderException("Failed to generate QR code", e);
    }
  }
}
