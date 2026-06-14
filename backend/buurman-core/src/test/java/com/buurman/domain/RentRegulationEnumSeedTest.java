package com.buurman.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression guard: every {@code max_increase_type} and {@code frequency} value seeded by {@code
 * V053__rent_regulation_overhaul.sql} must be a member of the corresponding Java enum. Adding new
 * values to V053 without updating the enum produces a runtime {@code IllegalArgumentException} from
 * {@code Enum.valueOf} when the rule is read from the database — easy to miss until the page that
 * consumes the rule blows up in production.
 *
 * <p>This test parses the migration once and asserts membership for both columns.
 */
class RentRegulationEnumSeedTest {

  private static final Path MIGRATION =
      Paths.get(
          "..",
          "buurman-jooq",
          "src",
          "main",
          "resources",
          "db",
          "migration",
          "V053__rent_regulation_overhaul.sql");

  @Test
  @DisplayName("every max_increase_type value in V053 is a valid MaxIncreaseType enum constant")
  void maxIncreaseType_valuesInMigration_areKnownEnumConstants() throws IOException {
    Set<String> seeded = extractSeededValues("max_increase_type");
    Set<String> declared = enumNames(MaxIncreaseType.values());
    assertThat(seeded)
        .as(
            "V053 seeds these max_increase_type values; every one must exist as"
                + " an enum constant or the runtime read will throw")
        .isSubsetOf(declared);
  }

  @Test
  @DisplayName("every frequency value in V053 is a valid RentFrequency enum constant")
  void frequency_valuesInMigration_areKnownEnumConstants() throws IOException {
    Set<String> seeded = extractSeededValues("frequency");
    Set<String> declared = enumNames(RentFrequency.values());
    assertThat(seeded)
        .as(
            "V053 seeds these frequency values; every one must exist as"
                + " a RentFrequency constant or the runtime read will throw")
        .isSubsetOf(declared);
  }

  /**
   * Parses both INSERT (column-positional) and UPDATE (column = 'VALUE') forms for a single column
   * out of the V053 migration file.
   */
  private static Set<String> extractSeededValues(String column) throws IOException {
    String sql = Files.readString(MIGRATION);
    Set<String> values = new TreeSet<>();

    // UPDATE column = 'VALUE'
    Pattern updatePattern = Pattern.compile(column + "\\s*=\\s*'([A-Z_0-9]+)'");
    Matcher m = updatePattern.matcher(sql);
    while (m.find()) {
      values.add(m.group(1));
    }

    // INSERT INTO rent_regulation_rules (cols) VALUES (...) — paren-aware
    Pattern insertHead =
        Pattern.compile("INSERT\\s+INTO\\s+rent_regulation_rules\\s*\\(", Pattern.CASE_INSENSITIVE);
    Matcher head = insertHead.matcher(sql);
    while (head.find()) {
      int colsStart = head.end() - 1;
      int colsEnd = matchingClose(sql, colsStart);
      if (colsEnd < 0) continue;
      String colsRaw = sql.substring(colsStart + 1, colsEnd);
      String[] cols = Stream.of(colsRaw.split(",")).map(String::trim).toArray(String[]::new);
      int idx = -1;
      for (int i = 0; i < cols.length; i++) {
        if (cols[i].equals(column)) {
          idx = i;
          break;
        }
      }
      if (idx < 0) continue;
      // Find VALUES keyword after colsEnd
      Matcher valuesKw = Pattern.compile("VALUES\\s*", Pattern.CASE_INSENSITIVE).matcher(sql);
      valuesKw.region(colsEnd, sql.length());
      if (!valuesKw.find()) continue;
      int k = valuesKw.end();
      while (k < sql.length()) {
        // skip whitespace and commas
        while (k < sql.length()
            && (sql.charAt(k) == ' '
                || sql.charAt(k) == '\t'
                || sql.charAt(k) == '\n'
                || sql.charAt(k) == ',')) {
          k++;
        }
        if (k >= sql.length() || sql.charAt(k) == ';') break;
        if (sql.charAt(k) != '(') {
          k++;
          continue;
        }
        int rowStart = k;
        int rowEnd = matchingClose(sql, rowStart);
        if (rowEnd < 0) break;
        String row = sql.substring(rowStart + 1, rowEnd);
        // Split by commas at depth 0
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (int p = 0; p < row.length(); p++) {
          char ch = row.charAt(p);
          if (ch == ',' && depth == 0) {
            tokens.add(cur.toString().trim());
            cur.setLength(0);
          } else {
            if (ch == '(') depth++;
            else if (ch == ')') depth--;
            cur.append(ch);
          }
        }
        if (cur.length() > 0) tokens.add(cur.toString().trim());
        if (idx < tokens.size()) {
          String tok = tokens.get(idx);
          Matcher mm = Pattern.compile("^'([A-Z_0-9]+)'$").matcher(tok);
          if (mm.matches()) {
            values.add(mm.group(1));
          }
        }
        k = rowEnd + 1;
      }
    }
    return values;
  }

  private static int matchingClose(String s, int openIdx) {
    int depth = 0;
    for (int i = openIdx; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '(') depth++;
      else if (c == ')') {
        depth--;
        if (depth == 0) return i;
      }
    }
    return -1;
  }

  private static <E extends Enum<E>> Set<String> enumNames(E[] values) {
    Set<String> names = new TreeSet<>();
    for (E v : values) {
      names.add(v.name());
    }
    return names;
  }
}
