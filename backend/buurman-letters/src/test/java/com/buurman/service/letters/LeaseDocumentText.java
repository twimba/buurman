package com.buurman.service.letters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text helpers shared by the lease document gates: the legal header, the clause fragments, the
 * prose of a fragment, and the single header policy.
 *
 * <p>Header policy (strict for now): every document carries {@code legal-basis:}, {@code
 * reviewed-by:} and {@code translation:}; the authoritative document says {@code translation:
 * authoritative}; every other document says {@code translation: machine-drafted}; all say {@code
 * reviewed-by: none} (all text is an unvetted draft). Loosen it here, once, when a human review
 * process exists.
 */
final class LeaseDocumentText {

  private LeaseDocumentText() {}

  static final List<String> HEADER_MARKERS =
      List.of("legal-basis:", "reviewed-by:", "translation:");

  private static final Pattern FRAGMENT_START = Pattern.compile("th:fragment=\"([^\"]+)\"");
  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern TAG =
      Pattern.compile("<(/?)([a-zA-Z][\\w:-]*)(?:\"[^\"]*\"|'[^']*'|[^>\"'])*>");
  private static final Pattern ENTITY = Pattern.compile("&[#a-zA-Z0-9]+;");
  private static final Pattern TRANSLATION = Pattern.compile("translation:\\s*(\\S+)");
  private static final Pattern REVIEWED_BY = Pattern.compile("reviewed-by:\\s*(\\S+)");

  /** The leading comment of a document, or empty when it does not start with one. */
  static String header(String html) {
    String trimmed = html.stripLeading();
    if (!trimmed.startsWith("<!--")) {
      return "";
    }
    return trimmed.substring(0, Math.max(trimmed.indexOf("-->"), 0));
  }

  /** Fragment name to the markup from its opening tag up to the next fragment's opening tag. */
  static Map<String, String> fragments(String html) {
    String body = COMMENT.matcher(html).replaceAll("");
    Map<String, String> result = new LinkedHashMap<>();
    Matcher m = FRAGMENT_START.matcher(body);
    List<Integer> starts = new ArrayList<>();
    List<String> names = new ArrayList<>();
    while (m.find()) {
      names.add(m.group(1));
      starts.add(body.lastIndexOf('<', m.start()));
    }
    for (int i = 0; i < names.size(); i++) {
      int end = i + 1 < names.size() ? starts.get(i + 1) : body.length();
      result.put(names.get(i), body.substring(starts.get(i), end));
    }
    return result;
  }

  /** The text of a fragment: tags and entities replaced by spaces. */
  static String prose(String fragment) {
    return ENTITY.matcher(TAG.matcher(fragment).replaceAll(" ")).replaceAll(" ");
  }

  static Optional<String> value(Pattern pattern, String header) {
    Matcher m = pattern.matcher(header);
    return m.find() ? Optional.of(m.group(1)) : Optional.empty();
  }

  /** All violations of the header policy of one document ({@code authoritative}: its role). */
  static List<String> headerPolicyViolations(String header, boolean authoritative) {
    List<String> problems = new ArrayList<>();
    for (String marker : HEADER_MARKERS) {
      if (!header.contains(marker)) {
        problems.add("header marker missing: " + marker);
      }
    }
    Optional<String> translation = value(TRANSLATION, header);
    Optional<String> reviewedBy = value(REVIEWED_BY, header);
    if (translation.isPresent() && reviewedBy.isPresent()) {
      boolean machine = translation.get().equals("machine-drafted");
      if (authoritative) {
        if (!translation.get().equals("authoritative")) {
          problems.add(
              "header rule: the authoritative document must say translation: authoritative");
        }
      } else {
        if (machine != reviewedBy.get().equals("none")) {
          problems.add("header rule: translation machine-drafted <=> reviewed-by none");
        }
        if (translation.get().equals("authoritative")) {
          problems.add("header rule: a translation must not claim translation: authoritative");
        } else if (!machine) {
          problems.add("header rule: a translation must be machine-drafted");
        }
      }
      if (!reviewedBy.get().equals("none")) {
        problems.add("header rule: reviewed-by must be none");
      }
    }
    return problems;
  }
}
