package com.buurman.util;

import java.security.SecureRandom;
import java.util.List;
import java.util.function.Function;

public final class HumanReadableIdGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final List<String> ADJECTIVES =
      List.of(
          "swift",
          "bright",
          "gentle",
          "bold",
          "calm",
          "warm",
          "quick",
          "cool",
          "happy",
          "brave",
          "wild",
          "proud",
          "sleek",
          "vivid",
          "fresh",
          "keen",
          "snowy",
          "breezy",
          "sunny",
          "frosty",
          "misty",
          "dusty",
          "rusty",
          "mighty",
          "tiny",
          "silky",
          "fuzzy",
          "shiny",
          "lucky",
          "peppy",
          "zesty",
          "witty",
          "jolly",
          "merry",
          "cozy",
          "rosy",
          "crisp",
          "nimble",
          "plucky",
          "dapper",
          "clever",
          "golden",
          "silver",
          "copper",
          "amber",
          "coral",
          "ivory",
          "mossy",
          "sandy",
          "stormy",
          "cloudy",
          "starry",
          "lunar",
          "solar",
          "ocean",
          "arctic",
          "tropic",
          "alpine",
          "cosmic",
          "velvet",
          "marble",
          "crystal",
          "scarlet",
          "cobalt",
          "bronze",
          "steel",
          "indigo",
          "violet",
          "crimson",
          "tawny",
          "rustic",
          "Nordic",
          "Celtic",
          "Alpine",
          "primal",
          "noble",
          "regal",
          "epic",
          "super",
          "turbo",
          "ultra",
          "mega",
          "grand",
          "prime",
          "elite",
          "rapid",
          "steady",
          "silent",
          "humble",
          "loyal",
          "mellow",
          "tender",
          "fierce",
          "spry",
          "agile",
          "hearty",
          "lively",
          "chipper",
          "perky",
          "snappy",
          "gleaming",
          "radiant",
          "dazzling",
          "glowing",
          "blazing",
          "frozen",
          "molten",
          "gentle",
          "serene",
          "placid",
          "feisty",
          "gutsy",
          "plush",
          "satin",
          "linen",
          "cedar",
          "maple",
          "birch",
          "aspen",
          "olive");

  private static final List<String> NOUNS =
      List.of(
          "cat", "bear", "wolf", "hawk", "fox", "deer", "lion", "owl", "seal", "dove", "swan",
          "crow", "pike", "bass", "colt", "mare", "bull", "ram", "elk", "lynx", "mole", "puma",
          "ibis", "wren", "lark", "hare", "toad", "moth", "newt", "kiwi", "orca", "yak", "boar",
          "vole", "crab", "wasp", "frog", "slug", "clam", "mink", "finch", "robin", "crane",
          "egret", "heron", "raven", "eagle", "falcon", "osprey", "parrot", "otter", "badger",
          "ferret", "rabbit", "donkey", "panda", "koala", "lemur", "sloth", "bison", "moose",
          "tiger", "jaguar", "panther", "cobra", "viper", "gecko", "iguana", "turtle", "whale",
          "shark", "squid", "coral", "pearl", "cedar", "maple", "aspen", "birch", "willow",
          "poplar", "daisy", "tulip", "lotus", "orchid", "iris", "peony", "aster", "sage", "thyme",
          "basil", "ember", "spark", "flame", "frost", "storm", "creek", "brook", "ridge", "cliff",
          "dune", "pebble", "flint", "quartz", "opal", "jade", "ruby", "onyx", "agate", "topaz",
          "pearl", "comet", "nova", "pulse", "echo", "drift", "gleam", "bloom", "crest", "vale",
          "harbor");

  private HumanReadableIdGenerator() {}

  public static String generate() {
    String adj1 = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
    String noun1 = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
    String adj2 = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
    String noun2 = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
    return adj1 + "-" + noun1 + "-" + adj2 + "-" + noun2;
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
    // Fallback: append random digits
    return generate() + "-" + String.format("%04d", RANDOM.nextInt(10000));
  }
}
