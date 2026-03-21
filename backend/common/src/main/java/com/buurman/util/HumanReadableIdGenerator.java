package com.buurman.util;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Function;

import net.datafaker.Faker;

public final class HumanReadableIdGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Faker FAKER = new Faker(Locale.ENGLISH, RANDOM);

  private HumanReadableIdGenerator() {}

  public static String generate() {
    String adj1 = FAKER.word().adjective().toLowerCase(Locale.ENGLISH);
    String animal1 = FAKER.animal().name().toLowerCase(Locale.ENGLISH);
    String adj2 = FAKER.word().adjective().toLowerCase(Locale.ENGLISH);
    String animal2 = FAKER.animal().name().toLowerCase(Locale.ENGLISH);
    return sanitize(adj1)
        + "-"
        + sanitize(animal1)
        + "-"
        + sanitize(adj2)
        + "-"
        + sanitize(animal2);
  }

  /**
   * Generates a unique HRID by checking against an existence predicate. Retries up to 10 times,
   * then appends 4 random digits as fallback.
   */
  public static String generateUnique(Function<String, Boolean> existsChecker) {
    for (int i = 0; i < 10; i++) {
      String code = generate();
      if (!existsChecker.apply(code)) {
        return code;
      }
    }
    return generate() + "-" + String.format("%04d", RANDOM.nextInt(10000));
  }

  private static String sanitize(String word) {
    return word.replaceAll("[^a-z]", "");
  }
}
