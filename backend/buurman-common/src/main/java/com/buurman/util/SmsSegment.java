package com.buurman.util;

import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * GSM-7 folding and single-segment budgeting for SMS bodies.
 *
 * <p>An SMS costs one segment while it fits 160 septets in the GSM-7 alphabet, or 70 UTF-16 code
 * units once any character falls outside it and the message drops to UCS-2. Latin-script text is
 * therefore folded to GSM-7 first ({@code á} to {@code a}), which keeps the full 160 for twelve of
 * the thirteen languages. Greek has no acceptable Latin fold, so it keeps its own script and is
 * budgeted at 70.
 *
 * <p>The budget is always computed on the final interpolated string, never on the template: a
 * single Greek or Polish property name is enough to halve the budget of an otherwise English
 * message.
 */
public final class SmsSegment {

  /** Which alphabet a body encodes to, and therefore which budget applies. */
  public enum Encoding {
    GSM_7,
    UCS_2
  }

  private static final int GSM_7_SINGLE_SEGMENT = 160;
  private static final int UCS_2_SINGLE_SEGMENT = 70;

  /** ASCII on purpose: U+2026 is outside GSM-7 and would flip the message to UCS-2. */
  private static final String TRUNCATION_MARKER = "...";

  private static final String GSM_7_BASIC =
      "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
          + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§"
          + "¿abcdefghijklmnopqrstuvwxyzäöñüà";

  /** Present in GSM-7 only via the extension table, costing two septets each. */
  private static final String GSM_7_EXTENDED = "^{}\\[~]|€";

  /** Letters carrying no decomposable diacritic, so NFD normalisation cannot fold them. */
  private static final Map<String, String> EXPLICIT_FOLD =
      Map.ofEntries(
          Map.entry("ł", "l"),
          Map.entry("Ł", "L"),
          Map.entry("đ", "d"),
          Map.entry("Đ", "D"),
          Map.entry("ð", "d"),
          Map.entry("Ð", "D"),
          Map.entry("þ", "th"),
          Map.entry("Þ", "Th"),
          Map.entry("œ", "oe"),
          Map.entry("Œ", "OE"),
          Map.entry("ı", "i"),
          Map.entry("ŋ", "n"));

  private SmsSegment() {}

  /** Replaces characters outside GSM-7 with a GSM-7 equivalent where one exists. */
  public static String fold(String text) {
    StringBuilder folded = new StringBuilder(text.length());
    text.codePoints().forEach(codePoint -> folded.append(foldCodePoint(codePoint)));
    return folded.toString();
  }

  public static boolean isGsm7(String text) {
    return text.codePoints().allMatch(SmsSegment::isGsm7CodePoint);
  }

  public static Encoding encodingOf(String text) {
    return isGsm7(text) ? Encoding.GSM_7 : Encoding.UCS_2;
  }

  /** Septets for GSM-7 text, UTF-16 code units for UCS-2 text. */
  public static int unitsOf(String text) {
    if (encodingOf(text) == Encoding.UCS_2) {
      return text.length();
    }
    return text.codePoints().map(SmsSegment::septetsOf).sum();
  }

  public static int singleSegmentBudget(String text) {
    return encodingOf(text) == Encoding.GSM_7 ? GSM_7_SINGLE_SEGMENT : UCS_2_SINGLE_SEGMENT;
  }

  public static boolean fitsOneSegment(String text) {
    return unitsOf(text) <= singleSegmentBudget(text);
  }

  /**
   * Folds the body and, if it still overflows one segment, shortens {@code truncatableValue} — the
   * longest interpolated value — rather than the sentence around it, so the message stays
   * grammatical.
   *
   * <p>Hard-truncating the whole body is a last resort that only triggers when even a one-character
   * value overflows, which {@code SmsSegmentBudgetTest} rules out at build time for every shipped
   * body.
   */
  public static String fitToOneSegment(String body, String truncatableValue) {
    String folded = fold(body);
    if (fitsOneSegment(folded)) {
      return folded;
    }
    String foldedValue = fold(truncatableValue);
    if (!foldedValue.isBlank() && folded.contains(foldedValue)) {
      String shortened = shrinkValue(folded, foldedValue);
      if (fitsOneSegment(shortened)) {
        return shortened;
      }
      folded = shortened;
    }
    return hardTruncate(folded);
  }

  private static String foldCodePoint(int codePoint) {
    String original = new String(Character.toChars(codePoint));
    if (isGsm7(original)) {
      return original;
    }
    String explicit = EXPLICIT_FOLD.get(original);
    if (explicit != null) {
      return explicit;
    }
    String stripped = Normalizer.normalize(original, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    if (!stripped.isEmpty() && isGsm7(stripped)) {
      return stripped;
    }
    return original;
  }

  private static boolean isGsm7CodePoint(int codePoint) {
    if (codePoint > 0xFFFF) {
      return false;
    }
    char character = (char) codePoint;
    return GSM_7_BASIC.indexOf(character) >= 0 || GSM_7_EXTENDED.indexOf(character) >= 0;
  }

  private static int septetsOf(int codePoint) {
    if (codePoint <= 0xFFFF && GSM_7_EXTENDED.indexOf((char) codePoint) >= 0) {
      return 2;
    }
    return 1;
  }

  private static String shrinkValue(String body, String value) {
    int overflow = unitsOf(body) - singleSegmentBudget(body);
    int keep = value.length() - overflow - TRUNCATION_MARKER.length();
    if (keep < 1) {
      keep = 1;
    }
    return body.replace(value, cutAtGrapheme(value, keep) + TRUNCATION_MARKER);
  }

  private static String hardTruncate(String body) {
    int budget = singleSegmentBudget(body) - TRUNCATION_MARKER.length();
    if (budget < 1) {
      budget = 1;
    }
    return cutAtGrapheme(body, budget) + TRUNCATION_MARKER;
  }

  /** Cuts to at most {@code maxChars} UTF-16 units without splitting a grapheme cluster. */
  private static String cutAtGrapheme(String text, int maxChars) {
    if (text.length() <= maxChars) {
      return text;
    }
    BreakIterator boundaries = BreakIterator.getCharacterInstance(Locale.ROOT);
    boundaries.setText(text);
    int end = boundaries.preceding(maxChars + 1);
    if (end == BreakIterator.DONE || end == 0) {
      end = Math.min(maxChars, text.length());
    }
    return text.substring(0, end);
  }
}
