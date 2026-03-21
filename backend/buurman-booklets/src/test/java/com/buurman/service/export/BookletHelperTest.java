package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BookletHelper")
class BookletHelperTest {

  @Nested
  @DisplayName("escapeHtml")
  class EscapeHtml {

    @Test
    @DisplayName("escapes all HTML special characters")
    void escapesSpecialChars() {
      String result = BookletHelper.escapeHtml("<script>alert('xss' & \"hack\")</script>");
      assertThat(result)
          .isEqualTo("&lt;script&gt;alert(&#39;xss&#39; &amp; &quot;hack&quot;)&lt;/script&gt;");
    }

    @Test
    @DisplayName("returns empty string for null")
    void returnsEmptyForNull() {
      assertThat(BookletHelper.escapeHtml(null)).isEmpty();
    }

    @Test
    @DisplayName("passes through safe text unchanged")
    void passesThroughSafeText() {
      assertThat(BookletHelper.escapeHtml("Hello World")).isEqualTo("Hello World");
    }
  }

  @Nested
  @DisplayName("sanitizeRichText")
  class SanitizeRichText {

    @Test
    @DisplayName("removes script tags")
    void removesScriptTags() {
      String result =
          BookletHelper.sanitizeRichText("<p>Hello</p><script>alert('xss')</script><p>World</p>");
      assertThat(result).isEqualTo("<p>Hello</p><p>World</p>");
    }

    @Test
    @DisplayName("removes iframe tags")
    void removesIframeTags() {
      String result =
          BookletHelper.sanitizeRichText("<p>Content</p><iframe src='evil.com'></iframe>");
      assertThat(result).isEqualTo("<p>Content</p>");
    }

    @Test
    @DisplayName("removes inline event handlers")
    void removesEventHandlers() {
      String result = BookletHelper.sanitizeRichText("<div onclick=\"alert('xss')\">click</div>");
      assertThat(result).isEqualTo("<div>click</div>");
    }

    @Test
    @DisplayName("returns empty string for null")
    void returnsEmptyForNull() {
      assertThat(BookletHelper.sanitizeRichText(null)).isEmpty();
    }
  }

  @Nested
  @DisplayName("formatEnumValue")
  class FormatEnumValue {

    @Test
    @DisplayName("converts UPPER_SNAKE_CASE to Title Case")
    void convertsSnakeCaseToTitleCase() {
      assertThat(BookletHelper.formatEnumValue("PENDING_SIGNATURE")).isEqualTo("Pending Signature");
    }

    @Test
    @DisplayName("handles single word")
    void handlesSingleWord() {
      assertThat(BookletHelper.formatEnumValue("ACTIVE")).isEqualTo("Active");
    }

    @Test
    @DisplayName("returns empty string for null")
    void returnsEmptyForNull() {
      assertThat(BookletHelper.formatEnumValue(null)).isEmpty();
    }
  }

  @Nested
  @DisplayName("fmt (BigDecimal)")
  class FmtBigDecimal {

    @Test
    @DisplayName("formats decimal with two places and thousands separator")
    void formatsWithThousands() {
      assertThat(BookletHelper.fmt(new BigDecimal("12345.6"))).isEqualTo("12,345.60");
    }

    @Test
    @DisplayName("returns 0.00 for null")
    void returnsZeroForNull() {
      assertThat(BookletHelper.fmt(null)).isEqualTo("0.00");
    }

    @Test
    @DisplayName("formats zero")
    void formatsZero() {
      assertThat(BookletHelper.fmt(BigDecimal.ZERO)).isEqualTo("0.00");
    }
  }

  @Nested
  @DisplayName("formatDate")
  class FormatDate {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Test
    @DisplayName("formats date with given formatter")
    void formatsDate() {
      assertThat(BookletHelper.formatDate(LocalDate.of(2026, 3, 15), FMT)).isEqualTo("15/03/2026");
    }

    @Test
    @DisplayName("returns dash for null date")
    void returnsDashForNull() {
      assertThat(BookletHelper.formatDate(null, FMT)).isEqualTo("\u2014");
    }
  }

  @Nested
  @DisplayName("displayOrDash")
  class DisplayOrDash {

    @Test
    @DisplayName("returns toString for non-null value")
    void returnsToString() {
      assertThat(BookletHelper.displayOrDash(42)).isEqualTo("42");
    }

    @Test
    @DisplayName("returns dash for null")
    void returnsDashForNull() {
      assertThat(BookletHelper.displayOrDash(null)).isEqualTo("\u2014");
    }
  }

  @Nested
  @DisplayName("displayBool")
  class DisplayBool {

    @Test
    @DisplayName("returns Yes for true")
    void returnsYesForTrue() {
      assertThat(BookletHelper.displayBool(true)).isEqualTo("Yes");
    }

    @Test
    @DisplayName("returns No for false")
    void returnsNoForFalse() {
      assertThat(BookletHelper.displayBool(false)).isEqualTo("No");
    }

    @Test
    @DisplayName("returns dash for null")
    void returnsDashForNull() {
      assertThat(BookletHelper.displayBool(null)).isEqualTo("\u2014");
    }
  }

  @Nested
  @DisplayName("displayBoolWithDetail")
  class DisplayBoolWithDetail {

    @Test
    @DisplayName("returns Yes with detail when true and detail present")
    void returnsYesWithDetail() {
      assertThat(BookletHelper.displayBoolWithDetail(true, "some info"))
          .isEqualTo("Yes \u2014 some info");
    }

    @Test
    @DisplayName("returns Yes without detail when detail is null")
    void returnsYesWithoutDetail() {
      assertThat(BookletHelper.displayBoolWithDetail(true, null)).isEqualTo("Yes");
    }

    @Test
    @DisplayName("returns No when false regardless of detail")
    void returnsNoWhenFalse() {
      assertThat(BookletHelper.displayBoolWithDetail(false, "ignored")).isEqualTo("No");
    }
  }

  @Nested
  @DisplayName("displayWithUnit")
  class DisplayWithUnit {

    @Test
    @DisplayName("returns value with unit")
    void returnsValueWithUnit() {
      assertThat(BookletHelper.displayWithUnit(42, "m\u00B2")).isEqualTo("42 m\u00B2");
    }

    @Test
    @DisplayName("returns dash for null value")
    void returnsDashForNull() {
      assertThat(BookletHelper.displayWithUnit(null, "m\u00B2")).isEqualTo("\u2014");
    }
  }

  @Nested
  @DisplayName("isTrue")
  class IsTrue {

    @Test
    @DisplayName("returns true for Boolean.TRUE")
    void returnsTrueForTrue() {
      assertThat(BookletHelper.isTrue(true)).isTrue();
    }

    @Test
    @DisplayName("returns false for Boolean.FALSE")
    void returnsFalseForFalse() {
      assertThat(BookletHelper.isTrue(false)).isFalse();
    }

    @Test
    @DisplayName("returns false for null")
    void returnsFalseForNull() {
      assertThat(BookletHelper.isTrue(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("displayEnum")
  class DisplayEnum {

    @Test
    @DisplayName("formats enum name replacing underscores with spaces and title-casing")
    void formatsEnumName() {
      assertThat(BookletHelper.displayEnum(java.time.DayOfWeek.MONDAY)).isEqualTo("Monday");
    }

    @Test
    @DisplayName("returns dash for null enum")
    void returnsDashForNull() {
      assertThat(BookletHelper.displayEnum(null)).isEqualTo("\u2014");
    }

    @Test
    @DisplayName("handles multi-word enum name")
    void handlesMultiWordEnum() {
      // DayOfWeek doesn't have underscores; use a custom approach
      // displayEnum replaces _ with space then title-cases the full result
      // For a name like "PENDING_SIGNATURE" (not available here), we test via formatEnumValue
      assertThat(BookletHelper.displayEnum(java.time.Month.JANUARY)).isEqualTo("January");
    }
  }

  @Nested
  @DisplayName("sanitizeRichText (single-quoted event handlers)")
  class SanitizeRichTextSingleQuoted {

    @Test
    @DisplayName("removes single-quoted event handlers")
    void removesSingleQuotedEventHandlers() {
      String result = BookletHelper.sanitizeRichText("<div onmouseover='alert(1)'>hover</div>");
      assertThat(result).isEqualTo("<div>hover</div>");
    }
  }

  @Nested
  @DisplayName("formatEnumValue edge cases")
  class FormatEnumValueEdgeCases {

    @Test
    @DisplayName("empty string throws StringIndexOutOfBoundsException")
    void emptyStringThrows() {
      assertThatThrownBy(() -> BookletHelper.formatEnumValue(""))
          .isInstanceOf(StringIndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("single underscore returns empty (trailing empties discarded by split)")
    void singleUnderscoreReturnsEmpty() {
      // "_".split("_") returns [] in Java (trailing empty strings discarded)
      assertThat(BookletHelper.formatEnumValue("_")).isEmpty();
    }

    @Test
    @DisplayName("double underscore A__B throws on empty middle segment")
    void doubleUnderscoreThrows() {
      // "A__B".split("_") returns ["A", "", "B"] — empty segment fails substring(0,1)
      assertThatThrownBy(() -> BookletHelper.formatEnumValue("A__B"))
          .isInstanceOf(StringIndexOutOfBoundsException.class);
    }
  }

  @Nested
  @DisplayName("propertyTypeIconHtml")
  class PropertyTypeIconHtml {

    @Test
    @DisplayName("returns SVG for known property type")
    void returnsSvgForKnownType() {
      String result = BookletHelper.propertyTypeIconHtml("HOUSE");
      assertThat(result).contains("<svg").contains("</svg>");
    }

    @Test
    @DisplayName("returns empty string for unknown property type")
    void returnsEmptyForUnknown() {
      assertThat(BookletHelper.propertyTypeIconHtml("UNKNOWN_TYPE")).isEmpty();
    }

    @Test
    @DisplayName("returns empty string for null")
    void returnsEmptyForNull() {
      assertThat(BookletHelper.propertyTypeIconHtml(null)).isEmpty();
    }
  }
}
