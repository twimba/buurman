package com.buurman.service.letters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.exception.BusinessRuleException;

/**
 * The {@code th:fragment} names a lease document defines, parsed once from the template resource.
 *
 * <p>Needed because the shell inserts {@code clause-<key>} fragments by name and a clause template
 * row without a matching fragment must be rejected before rendering, with a message naming the
 * clause and document, instead of surfacing as a Thymeleaf error from inside the PDF pipeline.
 */
final class LeaseDocumentFragments {

  private static final Pattern FRAGMENT = Pattern.compile("th:fragment=\"\\s*([A-Za-z0-9_-]+)");
  private static final Map<String, Set<String>> CACHE = new ConcurrentHashMap<>();

  private LeaseDocumentFragments() {}

  /**
   * @param templatePath relative to {@code templates/documents/}, without {@code .html}
   */
  static Set<String> names(String templatePath) {
    return CACHE.computeIfAbsent(templatePath, LeaseDocumentFragments::parse);
  }

  private static Set<String> parse(String templatePath) {
    ClassPathResource resource =
        new ClassPathResource(DocumentTemplateSupport.TEMPLATE_PREFIX + templatePath + ".html");
    try (InputStream in = resource.getInputStream()) {
      String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      Matcher m = FRAGMENT.matcher(html);
      Set<String> names = new HashSet<>();
      while (m.find()) {
        names.add(m.group(1));
      }
      return Set.copyOf(names);
    } catch (IOException e) {
      throw new BusinessRuleException("Lease document " + templatePath + " cannot be read");
    }
  }
}
