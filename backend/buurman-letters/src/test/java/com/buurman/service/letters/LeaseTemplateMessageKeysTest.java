package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/**
 * Every {@code #{key}} a lease template uses must exist in the base bundles: the letter message
 * source uses the code as default message, so a missing key silently prints the key itself.
 */
class LeaseTemplateMessageKeysTest {

  private static final Pattern MESSAGE_KEY = Pattern.compile("#\\{([a-zA-Z0-9_.-]+)");
  private static final List<String> BUNDLES =
      List.of("messages/document-letter-chrome", "messages/document-lease-agreement");

  private static Properties baseBundles() throws IOException {
    Properties properties = new Properties();
    for (String bundle : BUNDLES) {
      try (Reader reader =
          new InputStreamReader(
              new ClassPathResource(bundle + ".properties").getInputStream(),
              StandardCharsets.UTF_8)) {
        properties.load(reader);
      }
    }
    return properties;
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "templates/documents/lease-agreement/_shell.html",
        "templates/documents/lease-agreement/generic.html"
      })
  void everyMessageKeyExists(String template) throws IOException {
    String html;
    try (InputStream in = new ClassPathResource(template).getInputStream()) {
      html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    Set<String> used = new TreeSet<>();
    Matcher m = MESSAGE_KEY.matcher(html);
    while (m.find()) {
      used.add(m.group(1));
    }
    assertThat(used).as("%s uses message keys", template).isNotEmpty();
    assertThat(baseBundles().stringPropertyNames()).as(template).containsAll(used);
  }
}
