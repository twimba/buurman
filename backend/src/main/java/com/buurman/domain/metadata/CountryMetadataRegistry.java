package com.buurman.domain.metadata;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

public final class CountryMetadataRegistry {

  private static final Map<String, Class<? extends ContractCountryMetadata>> SCHEMAS =
      Map.ofEntries(
          // Tier 1 — Western Europe + UK + US
          Map.entry("NL", NlContractMetadata.class),
          Map.entry("DE", DeContractMetadata.class),
          Map.entry("FR", FrContractMetadata.class),
          Map.entry("BE", BeContractMetadata.class),
          Map.entry("PT", PtContractMetadata.class),
          Map.entry("ES", EsContractMetadata.class),
          Map.entry("IT", ItContractMetadata.class),
          Map.entry("GB", UkContractMetadata.class),
          Map.entry("US", UsContractMetadata.class),
          // DACH + Nordics + Ireland
          Map.entry("AT", AtContractMetadata.class),
          Map.entry("CH", ChContractMetadata.class),
          Map.entry("DK", DkContractMetadata.class),
          Map.entry("SE", SeContractMetadata.class),
          Map.entry("FI", FiContractMetadata.class),
          Map.entry("NO", NoContractMetadata.class),
          Map.entry("IE", IeContractMetadata.class),
          // Central & Eastern Europe
          Map.entry("PL", PlContractMetadata.class),
          Map.entry("CZ", CzContractMetadata.class),
          Map.entry("HU", HuContractMetadata.class),
          Map.entry("RO", RoContractMetadata.class),
          Map.entry("BG", BgContractMetadata.class),
          Map.entry("SK", SkContractMetadata.class),
          Map.entry("SI", SiContractMetadata.class),
          Map.entry("HR", HrContractMetadata.class),
          Map.entry("LT", LtContractMetadata.class),
          Map.entry("LV", LvContractMetadata.class),
          Map.entry("EE", EeContractMetadata.class),
          // Mediterranean & Benelux
          Map.entry("GR", GrContractMetadata.class),
          Map.entry("MT", MtContractMetadata.class),
          Map.entry("CY", CyContractMetadata.class),
          Map.entry("LU", LuContractMetadata.class),
          // Balkans
          Map.entry("RS", RsContractMetadata.class),
          Map.entry("BA", BaContractMetadata.class),
          Map.entry("AL", AlContractMetadata.class),
          Map.entry("ME", MeContractMetadata.class),
          Map.entry("MK", MkContractMetadata.class),
          Map.entry("XK", XkContractMetadata.class),
          // Americas
          Map.entry("CA", CaContractMetadata.class),
          Map.entry("MX", MxContractMetadata.class),
          Map.entry("BR", BrContractMetadata.class),
          Map.entry("AR", ArContractMetadata.class),
          Map.entry("CL", ClContractMetadata.class),
          Map.entry("CO", CoContractMetadata.class),
          Map.entry("PE", PeContractMetadata.class),
          Map.entry("UY", UyContractMetadata.class));

  /** All supported country codes. */
  private static final Set<String> SUPPORTED_COUNTRIES =
      Set.of(
          // Tier 1 — Western Europe + UK + US
          "NL",
          "DE",
          "FR",
          "BE",
          "PT",
          "ES",
          "IT",
          "GB",
          "US",
          // DACH + Nordics + Ireland
          "AT",
          "CH",
          "DK",
          "SE",
          "FI",
          "NO",
          "IE",
          // Central & Eastern Europe
          "PL",
          "CZ",
          "HU",
          "RO",
          "BG",
          "SK",
          "SI",
          "HR",
          "LT",
          "LV",
          "EE",
          // Mediterranean & Benelux
          "GR",
          "MT",
          "CY",
          "LU",
          // Balkans
          "RS",
          "BA",
          "AL",
          "ME",
          "MK",
          "XK",
          // Americas
          "CA",
          "MX",
          "BR",
          "AR",
          "CL",
          "CO",
          "PE",
          "UY");

  private static final Map<String, String> COUNTRY_NAMES =
      Map.ofEntries(
          Map.entry("NL", "Netherlands"),
          Map.entry("DE", "Germany"),
          Map.entry("FR", "France"),
          Map.entry("BE", "Belgium"),
          Map.entry("PT", "Portugal"),
          Map.entry("ES", "Spain"),
          Map.entry("IT", "Italy"),
          Map.entry("GB", "United Kingdom"),
          Map.entry("US", "United States"),
          Map.entry("AT", "Austria"),
          Map.entry("CH", "Switzerland"),
          Map.entry("DK", "Denmark"),
          Map.entry("SE", "Sweden"),
          Map.entry("FI", "Finland"),
          Map.entry("NO", "Norway"),
          Map.entry("IE", "Ireland"),
          Map.entry("PL", "Poland"),
          Map.entry("CZ", "Czech Republic"),
          Map.entry("HU", "Hungary"),
          Map.entry("RO", "Romania"),
          Map.entry("BG", "Bulgaria"),
          Map.entry("SK", "Slovakia"),
          Map.entry("SI", "Slovenia"),
          Map.entry("HR", "Croatia"),
          Map.entry("LT", "Lithuania"),
          Map.entry("LV", "Latvia"),
          Map.entry("EE", "Estonia"),
          Map.entry("GR", "Greece"),
          Map.entry("MT", "Malta"),
          Map.entry("CY", "Cyprus"),
          Map.entry("LU", "Luxembourg"),
          Map.entry("RS", "Serbia"),
          Map.entry("BA", "Bosnia and Herzegovina"),
          Map.entry("AL", "Albania"),
          Map.entry("ME", "Montenegro"),
          Map.entry("MK", "North Macedonia"),
          Map.entry("XK", "Kosovo"),
          Map.entry("CA", "Canada"),
          Map.entry("MX", "Mexico"),
          Map.entry("BR", "Brazil"),
          Map.entry("AR", "Argentina"),
          Map.entry("CL", "Chile"),
          Map.entry("CO", "Colombia"),
          Map.entry("PE", "Peru"),
          Map.entry("UY", "Uruguay"));

  /** Maps ISO 3166-1 alpha-2 country codes to their default ISO 4217 currency code. */
  private static final Map<String, String> COUNTRY_CURRENCIES =
      Map.ofEntries(
          // Eurozone
          Map.entry("NL", "EUR"),
          Map.entry("DE", "EUR"),
          Map.entry("FR", "EUR"),
          Map.entry("BE", "EUR"),
          Map.entry("PT", "EUR"),
          Map.entry("ES", "EUR"),
          Map.entry("IT", "EUR"),
          Map.entry("AT", "EUR"),
          Map.entry("IE", "EUR"),
          Map.entry("FI", "EUR"),
          Map.entry("GR", "EUR"),
          Map.entry("MT", "EUR"),
          Map.entry("CY", "EUR"),
          Map.entry("LU", "EUR"),
          Map.entry("SK", "EUR"),
          Map.entry("SI", "EUR"),
          Map.entry("LT", "EUR"),
          Map.entry("LV", "EUR"),
          Map.entry("EE", "EUR"),
          Map.entry("HR", "EUR"),
          Map.entry("XK", "EUR"),
          Map.entry("ME", "EUR"),
          // Non-euro European
          Map.entry("GB", "GBP"),
          Map.entry("CH", "CHF"),
          Map.entry("DK", "DKK"),
          Map.entry("SE", "SEK"),
          Map.entry("NO", "NOK"),
          Map.entry("PL", "PLN"),
          Map.entry("CZ", "CZK"),
          Map.entry("HU", "HUF"),
          Map.entry("RO", "RON"),
          Map.entry("BG", "BGN"),
          Map.entry("RS", "RSD"),
          Map.entry("BA", "BAM"),
          Map.entry("AL", "ALL"),
          Map.entry("MK", "MKD"),
          // Americas
          Map.entry("US", "USD"),
          Map.entry("CA", "CAD"),
          Map.entry("MX", "MXN"),
          Map.entry("BR", "BRL"),
          Map.entry("AR", "ARS"),
          Map.entry("CL", "CLP"),
          Map.entry("CO", "COP"),
          Map.entry("PE", "PEN"),
          Map.entry("UY", "UYU"));

  /** Maps common country name strings to ISO 3166-1 alpha-2 codes. */
  private static final Map<String, String> NAME_TO_CODE =
      Map.ofEntries(
          // Tier 1
          Map.entry("netherlands", "NL"),
          Map.entry("the netherlands", "NL"),
          Map.entry("nederland", "NL"),
          Map.entry("germany", "DE"),
          Map.entry("deutschland", "DE"),
          Map.entry("france", "FR"),
          Map.entry("belgium", "BE"),
          Map.entry("belgique", "BE"),
          Map.entry("belgie", "BE"),
          Map.entry("portugal", "PT"),
          Map.entry("spain", "ES"),
          Map.entry("espa\u00f1a", "ES"),
          Map.entry("italy", "IT"),
          Map.entry("italia", "IT"),
          Map.entry("united kingdom", "GB"),
          Map.entry("uk", "GB"),
          Map.entry("great britain", "GB"),
          Map.entry("england", "GB"),
          Map.entry("united states", "US"),
          Map.entry("usa", "US"),
          Map.entry("united states of america", "US"),
          // DACH + Nordics + Ireland
          Map.entry("austria", "AT"),
          Map.entry("\u00f6sterreich", "AT"),
          Map.entry("switzerland", "CH"),
          Map.entry("schweiz", "CH"),
          Map.entry("suisse", "CH"),
          Map.entry("svizzera", "CH"),
          Map.entry("denmark", "DK"),
          Map.entry("danmark", "DK"),
          Map.entry("sweden", "SE"),
          Map.entry("sverige", "SE"),
          Map.entry("finland", "FI"),
          Map.entry("suomi", "FI"),
          Map.entry("norway", "NO"),
          Map.entry("norge", "NO"),
          Map.entry("ireland", "IE"),
          // Central & Eastern Europe
          Map.entry("poland", "PL"),
          Map.entry("polska", "PL"),
          Map.entry("czech republic", "CZ"),
          Map.entry("czechia", "CZ"),
          Map.entry("hungary", "HU"),
          Map.entry("magyarorsz\u00e1g", "HU"),
          Map.entry("romania", "RO"),
          Map.entry("rom\u00e2nia", "RO"),
          Map.entry("bulgaria", "BG"),
          Map.entry("slovakia", "SK"),
          Map.entry("slovensko", "SK"),
          Map.entry("slovenia", "SI"),
          Map.entry("slovenija", "SI"),
          Map.entry("croatia", "HR"),
          Map.entry("hrvatska", "HR"),
          Map.entry("lithuania", "LT"),
          Map.entry("lietuva", "LT"),
          Map.entry("latvia", "LV"),
          Map.entry("latvija", "LV"),
          Map.entry("estonia", "EE"),
          Map.entry("eesti", "EE"),
          // Mediterranean & Benelux
          Map.entry("greece", "GR"),
          Map.entry("malta", "MT"),
          Map.entry("cyprus", "CY"),
          Map.entry("luxembourg", "LU"),
          Map.entry("luxemburg", "LU"),
          // Balkans
          Map.entry("serbia", "RS"),
          Map.entry("srbija", "RS"),
          Map.entry("bosnia and herzegovina", "BA"),
          Map.entry("bosnia", "BA"),
          Map.entry("albania", "AL"),
          Map.entry("shqip\u00ebria", "AL"),
          Map.entry("montenegro", "ME"),
          Map.entry("crna gora", "ME"),
          Map.entry("north macedonia", "MK"),
          Map.entry("macedonia", "MK"),
          Map.entry("kosovo", "XK"),
          // Americas
          Map.entry("canada", "CA"),
          Map.entry("mexico", "MX"),
          Map.entry("m\u00e9xico", "MX"),
          Map.entry("brazil", "BR"),
          Map.entry("brasil", "BR"),
          Map.entry("argentina", "AR"),
          Map.entry("chile", "CL"),
          Map.entry("colombia", "CO"),
          Map.entry("peru", "PE"),
          Map.entry("per\u00fa", "PE"),
          Map.entry("uruguay", "UY"));

  private CountryMetadataRegistry() {}

  public static Class<? extends ContractCountryMetadata> getSchemaClass(String countryCode) {
    return SCHEMAS.getOrDefault(
        countryCode.toUpperCase(Locale.ROOT), GenericContractMetadata.class);
  }

  public static Map<String, Class<? extends ContractCountryMetadata>> getSchemaClasses() {
    return SCHEMAS;
  }

  public static Set<String> getSupportedCountries() {
    return SUPPORTED_COUNTRIES;
  }

  public static boolean hasDedicatedSchema(String countryCode) {
    return SCHEMAS.containsKey(countryCode.toUpperCase(Locale.ROOT));
  }

  public static String getCountryName(String countryCode) {
    return COUNTRY_NAMES.getOrDefault(countryCode.toUpperCase(Locale.ROOT), countryCode);
  }

  /**
   * Normalizes a free-text country string to an ISO 3166-1 alpha-2 code. Handles both ISO codes and
   * common country names (English + native).
   *
   * @return the ISO code, or null if unrecognized
   */
  public static @Nullable String normalizeCountryCode(@Nullable String country) {
    if (country == null || country.isBlank()) {
      return null;
    }
    String trimmed = country.trim();
    // Already an ISO code?
    String upper = trimmed.toUpperCase(Locale.ROOT);
    if (SUPPORTED_COUNTRIES.contains(upper)) {
      return upper;
    }
    // Try name lookup
    return NAME_TO_CODE.get(trimmed.toLowerCase(Locale.ROOT));
  }

  /**
   * Returns the default currency code for a given country code.
   *
   * @return the ISO 4217 currency code, or "EUR" if the country is unknown
   */
  public static String getDefaultCurrency(String countryCode) {
    if (countryCode == null || countryCode.isBlank()) {
      return "EUR";
    }
    return COUNTRY_CURRENCIES.getOrDefault(countryCode.toUpperCase(Locale.ROOT), "EUR");
  }

  /** Returns the full country-to-currency mapping for all supported countries. */
  public static Map<String, String> getCountryCurrencies() {
    return COUNTRY_CURRENCIES;
  }
}
