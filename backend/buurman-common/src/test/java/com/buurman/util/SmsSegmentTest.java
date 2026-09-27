package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SmsSegment")
class SmsSegmentTest {

  @Nested
  @DisplayName("fold")
  class Fold {

    @Test
    @DisplayName("leaves characters GSM-7 already covers untouched")
    void keepsNativeGsm7Characters() {
      assertThat(SmsSegment.fold("Café Müller à Ørsted ñ É ß"))
          .isEqualTo("Café Müller à Ørsted ñ É ß");
    }

    @Test
    @DisplayName("strips diacritics GSM-7 does not cover")
    void stripsUncoveredDiacritics() {
      assertThat(SmsSegment.fold("Pagamento está atrasado")).isEqualTo("Pagamento esta atrasado");
      // é is in the GSM-7 basic alphabet, so it survives; í is not, so it folds.
      assertThat(SmsSegment.fold("José Antonio García")).isEqualTo("José Antonio Garcia");
    }

    @Test
    @DisplayName("folds letters that carry no decomposable diacritic")
    void foldsNonDecomposableLetters() {
      assertThat(SmsSegment.fold("Łukasz Wałęsa")).isEqualTo("Lukasz Walesa");
      assertThat(SmsSegment.fold("œuvre")).isEqualTo("oeuvre");
    }

    @Test
    @DisplayName("leaves Greek alone — there is no acceptable Latin fold for it")
    void leavesGreekAlone() {
      assertThat(SmsSegment.fold("Καθυστερημένη πληρωμή")).isEqualTo("Καθυστερημένη πληρωμή");
    }
  }

  @Nested
  @DisplayName("budget")
  class Budget {

    @Test
    @DisplayName("GSM-7 text gets 160 septets")
    void gsm7Budget() {
      String body = "a".repeat(160);
      assertThat(SmsSegment.encodingOf(body)).isEqualTo(SmsSegment.Encoding.GSM_7);
      assertThat(SmsSegment.fitsOneSegment(body)).isTrue();
      assertThat(SmsSegment.fitsOneSegment(body + "a")).isFalse();
    }

    @Test
    @DisplayName("extension-table characters cost two septets each")
    void extensionCharactersCostTwo() {
      assertThat(SmsSegment.unitsOf("€")).isEqualTo(2);
      assertThat(SmsSegment.unitsOf("[]")).isEqualTo(4);
      assertThat(SmsSegment.fitsOneSegment("€".repeat(80))).isTrue();
      assertThat(SmsSegment.fitsOneSegment("€".repeat(81))).isFalse();
    }

    @Test
    @DisplayName("any non-GSM-7 character drops the whole message to 70 units")
    void ucs2Budget() {
      String body = "π".repeat(70);
      assertThat(SmsSegment.encodingOf(body)).isEqualTo(SmsSegment.Encoding.UCS_2);
      assertThat(SmsSegment.fitsOneSegment(body)).isTrue();
      assertThat(SmsSegment.fitsOneSegment(body + "π")).isFalse();
    }
  }

  @Nested
  @DisplayName("fitToOneSegment")
  class FitToOneSegment {

    @Test
    @DisplayName("folds a Latin-script body so it stays GSM-7 and fits")
    void foldsLatinBody() {
      String body = "Buurman: Pagamento de 1.250,00 EUR para Keizersgracht está em atraso.";

      String result = SmsSegment.fitToOneSegment(body, "Keizersgracht");

      assertThat(result)
          .isEqualTo("Buurman: Pagamento de 1.250,00 EUR para Keizersgracht esta em atraso.");
      assertThat(SmsSegment.encodingOf(result)).isEqualTo(SmsSegment.Encoding.GSM_7);
      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
    }

    @Test
    @DisplayName("truncates the value, not the sentence, when the body overflows")
    void truncatesTheValueNotTheSentence() {
      String property = "P".repeat(200);
      String body = "Buurman: Payment for " + property + " is overdue.";

      String result = SmsSegment.fitToOneSegment(body, property);

      assertThat(result).startsWith("Buurman: Payment for P");
      assertThat(result).endsWith(" is overdue.");
      assertThat(result).contains("...");
      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
    }

    @Test
    @DisplayName("a Greek value in an English body forces UCS-2 and is budgeted at 70")
    void nonLatinValueFlipsTheEncoding() {
      String property = "Παλαιό Φάληρο Λεωφόρος Ποσειδώνος 42";
      String body =
          "Buurman: Payment of EUR 1.250,00 for " + property + " is overdue (due 15/10/2026).";

      // The hazard: the raw interpolation is UCS-2, so its budget is 70, not 160.
      assertThat(SmsSegment.encodingOf(body)).isEqualTo(SmsSegment.Encoding.UCS_2);
      assertThat(SmsSegment.singleSegmentBudget(body)).isEqualTo(70);
      assertThat(SmsSegment.fitsOneSegment(body)).isFalse();

      String result = SmsSegment.fitToOneSegment(body, property);

      // Budgeting on the template instead of the interpolated string would return this
      // 102-unit UCS-2 body untouched and ship two segments.
      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
      assertThat(result).startsWith("Buurman: Payment of EUR 1.250,00 for ");
      assertThat(result).endsWith(" is overdue (due 15/10/2026).");
    }

    @Test
    @DisplayName("never splits a surrogate pair")
    void neverSplitsASurrogatePair() {
      String property = "🏠".repeat(60);
      String body = "Buurman: Payment for " + property + " is overdue.";

      String result = SmsSegment.fitToOneSegment(body, property);

      assertThat(SmsSegment.fitsOneSegment(result)).isTrue();
      for (int i = 0; i < result.length(); i++) {
        char c = result.charAt(i);
        if (Character.isHighSurrogate(c)) {
          assertThat(i + 1).isLessThan(result.length());
          assertThat(Character.isLowSurrogate(result.charAt(i + 1))).isTrue();
        }
        if (Character.isLowSurrogate(c)) {
          assertThat(i).isGreaterThan(0);
          assertThat(Character.isHighSurrogate(result.charAt(i - 1))).isTrue();
        }
      }
    }

    @Test
    @DisplayName("uses an ASCII marker so truncation cannot itself flip the encoding")
    void truncationMarkerIsAscii() {
      String property = "P".repeat(200);

      String result = SmsSegment.fitToOneSegment("Buurman: " + property + " overdue.", property);

      assertThat(result).doesNotContain("…");
      assertThat(SmsSegment.encodingOf(result)).isEqualTo(SmsSegment.Encoding.GSM_7);
    }

    @Test
    @DisplayName("a body that already fits is returned folded and otherwise untouched")
    void shortBodyUntouched() {
      assertThat(SmsSegment.fitToOneSegment("Buurman: all good.", ""))
          .isEqualTo("Buurman: all good.");
    }
  }
}
