package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.buurman.document.DocumentFormatting;

/**
 * Shared formatting + escaping utilities for booklet/report exporters. All methods are static and
 * stateless — this is a pure utility class. (The legacy StringBuilder page-builders were retired
 * when the booklets moved to Thymeleaf templates; only the value-formatting helpers remain.)
 */
final class BookletHelper {

  private BookletHelper() {}

  // ── Formatting ──────────────────────────────────────────────────

  static String escapeHtml(@Nullable String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  static String sanitizeRichText(@Nullable String html) {
    if (html == null) {
      return "";
    }
    return html.replaceAll("(?i)<script[^>]*>.*?</script>", "")
        .replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "")
        .replaceAll("(?i)<object[^>]*>.*?</object>", "")
        .replaceAll("(?i)<embed[^>]*>", "")
        .replaceAll("(?i)<link[^>]*>", "")
        .replaceAll("(?i)\\s+on\\w+\\s*=\\s*\"[^\"]*\"", "")
        .replaceAll("(?i)\\s+on\\w+\\s*=\\s*'[^']*'", "");
  }

  static String formatEnumValue(@Nullable String value) {
    return DocumentFormatting.formatEnumValue(value);
  }

  static String fmt(@Nullable BigDecimal value) {
    return value != null ? String.format(Locale.US, "%,.2f", value) : "0.00";
  }

  static String formatDate(@Nullable LocalDate date, DateTimeFormatter fmt) {
    return DocumentFormatting.formatDate(date, fmt);
  }

  static boolean isTrue(@Nullable Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  // ── Null-safe display utilities ───────────────────────────────────

  static String displayOrDash(@Nullable Object value) {
    return value != null ? value.toString() : "—";
  }

  static String displayBool(@Nullable Boolean value) {
    if (value == null) {
      return "—";
    }
    return value ? "Yes" : "No";
  }

  static String displayBoolWithDetail(@Nullable Boolean value, @Nullable String detail) {
    if (value == null) {
      return "—";
    }
    if (!value) {
      return "No";
    }
    return detail != null ? "Yes — " + detail : "Yes";
  }

  static String displayWithUnit(@Nullable Number value, String unit) {
    if (value == null) {
      return "—";
    }
    return value + " " + unit;
  }

  static String displayEnum(@Nullable Enum<?> value) {
    if (value == null) {
      return "—";
    }
    return value.name().replace('_', ' ').substring(0, 1).toUpperCase(Locale.ROOT)
        + value.name().replace('_', ' ').substring(1).toLowerCase(Locale.ROOT);
  }

  // ── Property type icons (inline SVG, Lucide-compatible paths) ───

  /**
   * Returns an inline SVG icon for the given property type name. The SVG is 16×16, uses the
   * booklet's brand blue (#0284c7) for stroke, and is safe to embed directly in cover-cell HTML.
   */
  static String propertyTypeIconHtml(@Nullable String typeName) {
    if (typeName == null) {
      return "";
    }
    String paths =
        switch (typeName) {
          case "HOUSE" ->
              "<path d='M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8'/><path d='M3 10a2 2 0 0 1"
                  + " .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0"
                  + " 0 1-2-2z'/>";
          case "APARTMENT", "BUILDING2" ->
              "<path d='M10 12h4'/><path d='M10 8h4'/><path d='M14 21v-3a2 2 0 0 0-4 0v3'/><path"
                  + " d='M6 10H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0"
                  + " 0-2-2h-2'/><path d='M6 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16'/>";
          case "TOWNHOUSE" ->
              "<path d='M12 10h.01'/><path d='M12 14h.01'/><path d='M12 6h.01'/><path d='M16"
                  + " 10h.01'/><path d='M16 14h.01'/><path d='M16 6h.01'/><path d='M8"
                  + " 10h.01'/><path d='M8 14h.01'/><path d='M8 6h.01'/><path d='M9 22v-3a1 1 0 0 1"
                  + " 1-1h4a1 1 0 0 1 1 1v3'/><rect x='4' y='2' width='16' height='20' rx='2'/>";
          case "VILLA" ->
              "<path d='M10 5V3'/><path d='M14 5V3'/><path d='M15 21v-3a3 3 0 0 0-6 0v3'/><path"
                  + " d='M18 3v8'/><path d='M18 5H6'/><path d='M22 11H2'/><path d='M22 9v10a2 2 0 0"
                  + " 1-2 2H4a2 2 0 0 1-2-2V9'/><path d='M6 3v8'/>";
          case "STUDIO" ->
              "<rect width='7' height='9' x='3' y='3' rx='1'/><rect width='7' height='5' x='14'"
                  + " y='3' rx='1'/><rect width='7' height='9' x='14' y='12' rx='1'/><rect"
                  + " width='7' height='5' x='3' y='16' rx='1'/>";
          case "ROOM" ->
              "<path d='M11 20H2'/><path d='M11 4.562v16.157a1 1 0 0 0 1.242.97L19 20V5.562a2 2 0 0"
                  + " 0-1.515-1.94l-4-1A2 2 0 0 0 11 4.561z'/><path d='M11 4H8a2 2 0 0 0-2"
                  + " 2v14'/><path d='M14 12h.01'/><path d='M22 20h-3'/>";
          case "OFFICE" ->
              "<path d='M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16'/><rect width='20' height='14'"
                  + " x='2' y='6' rx='2'/>";
          case "RETAIL" ->
              "<path d='M16 10a4 4 0 0 1-8 0'/><path d='M3.103 6.034h17.794'/><path d='M3.4 5.467a2"
                  + " 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0"
                  + " 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z'/>";
          case "RESTAURANT" ->
              "<path d='m16 2-2.3 2.3a3 3 0 0 0 0 4.2l1.8 1.8a3 3 0 0 0 4.2 0L22 8'/><path d='M15"
                  + " 15 3.3 3.3a4.2 4.2 0 0 0 0 6l7.3 7.3c.7.7 2 .7 2.8 0L15 15Zm0 0 7 7'/><path"
                  + " d='m2.1 21.8 6.4-6.3'/><path d='m19 5-7 7'/>";
          case "HOTEL" ->
              "<path d='M2 20v-8a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v8'/><path d='M4 10V6a2 2 0 0 1"
                  + " 2-2h12a2 2 0 0 1 2 2v4'/><path d='M12 4v6'/><path d='M2 18h20'/>";
          case "SHOWROOM" ->
              "<rect width='20' height='14' x='2' y='3' rx='2'/><line x1='8' x2='16' y1='21'"
                  + " y2='21'/><line x1='12' x2='12' y1='17' y2='21'/>";
          case "AUTO_DEALERSHIP" ->
              "<path d='M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16"
                  + " 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7"
                  + " 0 0 0 2 12v4c0 .6.4 1 1 1h2'/><circle cx='7' cy='17' r='2'/><path d='M9"
                  + " 17h6'/><circle cx='17' cy='17' r='2'/>";
          case "SNACKBAR" ->
              "<path d='M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2'/><path d='M7 2v20'/><path d='M21"
                  + " 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7'/>";
          case "CAFE" ->
              "<path d='M10 2v2'/><path d='M14 2v2'/><path d='M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4"
                  + " 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1'/><path d='M6 2v2'/>";
          case "MOTEL" ->
              "<path d='M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0"
                  + " 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0"
                  + " 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z'/><circle cx='16.5' cy='7.5' r='.5'"
                  + " fill='currentColor'/>";
          case "BAR" ->
              "<path d='M8 22h8'/><path d='M7 10h10'/><path d='M12 15v7'/><path d='M12 15a5 5 0 0 0"
                  + " 5-5c0-2-.5-4-2-8H9c-1.5 4-2 6-2 8a5 5 0 0 0 5 5Z'/>";
          case "BED_AND_BREAKFAST" ->
              "<path d='M12 2v8'/><path d='m4.93 10.93 1.41 1.41'/><path d='M2 18h2'/><path d='M20"
                  + " 18h2'/><path d='m19.07 10.93-1.41 1.41'/><path d='M22 22H2'/><path d='m8 6"
                  + " 4-4 4 4'/><path d='M16 18a4 4 0 0 0-8 0'/>";
          case "WAREHOUSE" ->
              "<path d='M18 21V10a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1v11'/><path d='M22 19a2 2 0 0 1-2"
                  + " 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 1.132-1.803l7.95-3.974a2 2 0 0 1 1.837 0l7.948"
                  + " 3.974A2 2 0 0 1 22 8z'/><path d='M6 13h12'/><path d='M6 17h12'/>";
          case "WORKSHOP" ->
              "<path d='M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4"
                  + " 0l3.106-3.105c.32-.322.863-.22.983.218a6 6 0 0 1-8.259 7.057l-7.91 7.91a1 1 0"
                  + " 0 1-2.999-3l7.91-7.91a6 6 0 0 1 7.057-8.259c.438.12.54.662.219.984z'/>";
          case "FACTORY" ->
              "<path d='M12 16h.01'/><path d='M16 16h.01'/><path d='M3 19a2 2 0 0 0 2 2h14a2 2 0 0"
                  + " 0 2-2V8.5a.5.5 0 0 0-.769-.422l-4.462 2.844A.5.5 0 0 1 15 10.5v-2a.5.5 0 0"
                  + " 0-.769-.422L9.77 10.922A.5.5 0 0 1 9 10.5V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2"
                  + " 2z'/><path d='M8 16h.01'/>";
          case "DATA_CENTER" ->
              "<rect width='20' height='8' x='2' y='2' rx='2' ry='2'/><rect width='20' height='8'"
                  + " x='2' y='14' rx='2' ry='2'/><line x1='6' x2='6.01' y1='6' y2='6'/><line"
                  + " x1='6' x2='6.01' y1='18' y2='18'/>";
          case "COLD_STORAGE" ->
              "<path d='m10 20-1.25-2.5L6 18'/><path d='M10 4 8.75 6.5 6 6'/><path d='m14 20"
                  + " 1.25-2.5L18 18'/><path d='m14 4 1.25 2.5L18 6'/><path d='m17"
                  + " 21-3-6h-4'/><path d='m17 3-3 6 1.5 3'/><path d='M2 12h6.5L10 9'/><path d='m20"
                  + " 10-1.5 2 1.5 2'/><path d='M22 12h-6.5L14 15'/><path d='m4 10 1.5 2L4"
                  + " 14'/><path d='m7 21 3-6-1.5-3'/><path d='m7 3 3 6h4'/>";
          case "GARAGE" ->
              "<rect width='18' height='18' x='3' y='3' rx='2'/><path d='M9 17V7h4a3 3 0 0 1 0"
                  + " 6H9'/>";
          case "FARMLAND" ->
              "<path d='M2 22 16 8'/><path d='M3.47 12.53 5 11l1.53 1.53a3.5 3.5 0 0 1 0 4.94L5"
                  + " 19l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z'/><path d='M7.47 8.53 9 7l1.53 1.53a3.5"
                  + " 3.5 0 0 1 0 4.94L9 15l-1.53-1.53a3.5 3.5 0 0 1 0-4.94Z'/><path d='M11.47 4.53"
                  + " 13 3l1.53 1.53a3.5 3.5 0 0 1 0 4.94L13 11l-1.53-1.53a3.5 3.5 0 0 1"
                  + " 0-4.94Z'/><path d='M20 2h2v2a4 4 0 0 1-4 4h-2V6a4 4 0 0 1 4-4Z'/><path"
                  + " d='M11.47 17.47 13 19l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L5 19l1.53-1.53a3.5 3.5"
                  + " 0 0 1 4.94 0Z'/><path d='M15.47 13.47 17 15l-1.53 1.53a3.5 3.5 0 0 1-4.94 0L9"
                  + " 15l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z'/><path d='M19.47 9.47 21 11l-1.53"
                  + " 1.53a3.5 3.5 0 0 1-4.94 0L13 11l1.53-1.53a3.5 3.5 0 0 1 4.94 0Z'/>";
          case "RANCH" ->
              "<path d='m17 14 3 3.3a1 1 0 0 1-.7 1.7H4.7a1 1 0 0 1-.7-1.7L7 14h-.3a1 1 0 0"
                  + " 1-.7-1.7L9 9h-.2A1 1 0 0 1 8 7.3L12 3l4 4.3a1 1 0 0 1-.8 1.7H15l3 3.3a1 1 0 0"
                  + " 1-.7 1.7H17Z'/><path d='M12 22v-3'/>";
          case "GREENHOUSE" ->
              "<path d='M14 9.536V7a4 4 0 0 1 4-4h1.5a.5.5 0 0 1 .5.5V5a4 4 0 0 1-4 4 4 4 0 0 0-4"
                  + " 4c0 2 1 3 1 5a5 5 0 0 1-1 3'/><path d='M4 9a5 5 0 0 1 8 4 5 5 0 0"
                  + " 1-8-4'/><path d='M5 21h14'/>";
          case "ORCHARD" ->
              "<path d='M2 17a5 5 0 0 0 10 0c0-2.76-2.5-5-5-3-2.5-2-5 .24-5 3Z'/><path d='M12 17a5"
                  + " 5 0 0 0 10 0c0-2.76-2.5-5-5-3-2.5-2-5 .24-5 3Z'/><path d='M7 14c3.22-2.91"
                  + " 4.29-8.75 5-12 1.66 2.38 4.94 9 5 12'/><path d='M22 9c-4.29 0-7.14-2.33-10-7"
                  + " 5.71 0 10 4.67 10 7Z'/>";
          case "VINEYARD" ->
              "<path d='M22 5V2l-5.89 5.89'/><circle cx='16.6' cy='15.89' r='3'/><circle cx='8.11'"
                  + " cy='7.4' r='3'/><circle cx='12.35' cy='11.65' r='3'/><circle cx='13.91'"
                  + " cy='5.85' r='3'/><circle cx='18.15' cy='10.09' r='3'/><circle cx='6.56'"
                  + " cy='13.2' r='3'/><circle cx='10.8' cy='17.44' r='3'/><circle cx='5' cy='19'"
                  + " r='3'/>";
          case "MIXED_USE", "COMMERCIAL" ->
              "<path d='M15 21v-5a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v5'/><path d='M17.774 10.31a1.12"
                  + " 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.451 0 1.12 1.12 0 0 0-1.548 0 2.5 2.5 0 0"
                  + " 1-3.452 0 1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.77-3.248l2.889-4.184A2 2 0"
                  + " 0 1 7 2h10a2 2 0 0 1 1.653.873l2.895 4.192a2.5 2.5 0 0 1-3.774 3.244'/><path"
                  + " d='M4 10.95V19a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8.05'/>";
          default -> "";
        };
    if (paths.isEmpty()) {
      return "";
    }
    return "<span style='display:inline-block;vertical-align:middle;margin-right:6px;'>"
        + "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' "
        + "stroke='#0284c7' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' "
        + "width='16' height='16' style='vertical-align:middle;'>"
        + paths
        + "</svg></span>";
  }
}
