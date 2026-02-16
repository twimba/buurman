package com.buurman.util;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CountryGroups {

  private CountryGroups() {}

  public record Country(String code, String name) {}

  public record Group(String id, String name, List<Country> countries) {}

  public static final List<String> ALL_NUMBER_TYPES =
      List.of(
          "MOBILE",
          "FIXED_LINE",
          "FIXED_LINE_OR_MOBILE",
          "VOIP",
          "TOLL_FREE",
          "PREMIUM_RATE",
          "SHARED_COST",
          "PERSONAL_NUMBER",
          "PAGER",
          "UAN");

  public static final List<Group> GROUPS =
      List.of(
          new Group(
              "EUROPEAN_UNION",
              "European Union",
              List.of(
                  c("AT", "Austria"),
                  c("BE", "Belgium"),
                  c("BG", "Bulgaria"),
                  c("HR", "Croatia"),
                  c("CY", "Cyprus"),
                  c("CZ", "Czech Republic"),
                  c("DK", "Denmark"),
                  c("EE", "Estonia"),
                  c("FI", "Finland"),
                  c("FR", "France"),
                  c("DE", "Germany"),
                  c("GR", "Greece"),
                  c("HU", "Hungary"),
                  c("IE", "Ireland"),
                  c("IT", "Italy"),
                  c("LV", "Latvia"),
                  c("LT", "Lithuania"),
                  c("LU", "Luxembourg"),
                  c("MT", "Malta"),
                  c("NL", "Netherlands"),
                  c("PL", "Poland"),
                  c("PT", "Portugal"),
                  c("RO", "Romania"),
                  c("SK", "Slovakia"),
                  c("SI", "Slovenia"),
                  c("ES", "Spain"),
                  c("SE", "Sweden"))),
          new Group(
              "OTHER_EUROPE",
              "Other Europe",
              List.of(
                  c("AL", "Albania"),
                  c("AD", "Andorra"),
                  c("BY", "Belarus"),
                  c("BA", "Bosnia and Herzegovina"),
                  c("GE", "Georgia"),
                  c("IS", "Iceland"),
                  c("LI", "Liechtenstein"),
                  c("MC", "Monaco"),
                  c("ME", "Montenegro"),
                  c("MK", "North Macedonia"),
                  c("MD", "Moldova"),
                  c("NO", "Norway"),
                  c("RS", "Serbia"),
                  c("SM", "San Marino"),
                  c("CH", "Switzerland"),
                  c("UA", "Ukraine"),
                  c("GB", "United Kingdom"),
                  c("VA", "Vatican City"))),
          new Group(
              "NORTH_AMERICA",
              "North America",
              List.of(c("US", "United States"), c("CA", "Canada"), c("MX", "Mexico"))),
          new Group(
              "CENTRAL_AMERICA",
              "Central America",
              List.of(
                  c("BZ", "Belize"),
                  c("CR", "Costa Rica"),
                  c("SV", "El Salvador"),
                  c("GT", "Guatemala"),
                  c("HN", "Honduras"),
                  c("NI", "Nicaragua"),
                  c("PA", "Panama"))),
          new Group(
              "SOUTH_AMERICA",
              "South America",
              List.of(
                  c("AR", "Argentina"),
                  c("BO", "Bolivia"),
                  c("BR", "Brazil"),
                  c("CL", "Chile"),
                  c("CO", "Colombia"),
                  c("EC", "Ecuador"),
                  c("GY", "Guyana"),
                  c("PY", "Paraguay"),
                  c("PE", "Peru"),
                  c("SR", "Suriname"),
                  c("UY", "Uruguay"),
                  c("VE", "Venezuela"))),
          new Group(
              "CARIBBEAN",
              "Caribbean",
              List.of(
                  c("AG", "Antigua and Barbuda"),
                  c("BS", "Bahamas"),
                  c("BB", "Barbados"),
                  c("CU", "Cuba"),
                  c("DM", "Dominica"),
                  c("DO", "Dominican Republic"),
                  c("GD", "Grenada"),
                  c("HT", "Haiti"),
                  c("JM", "Jamaica"),
                  c("KN", "Saint Kitts and Nevis"),
                  c("LC", "Saint Lucia"),
                  c("VC", "Saint Vincent and the Grenadines"),
                  c("TT", "Trinidad and Tobago"))),
          new Group(
              "MIDDLE_EAST",
              "Middle East",
              List.of(
                  c("BH", "Bahrain"),
                  c("IR", "Iran"),
                  c("IQ", "Iraq"),
                  c("IL", "Israel"),
                  c("JO", "Jordan"),
                  c("KW", "Kuwait"),
                  c("LB", "Lebanon"),
                  c("OM", "Oman"),
                  c("PS", "Palestine"),
                  c("QA", "Qatar"),
                  c("SA", "Saudi Arabia"),
                  c("SY", "Syria"),
                  c("AE", "United Arab Emirates"),
                  c("YE", "Yemen"))),
          new Group(
              "EAST_ASIA",
              "East Asia",
              List.of(
                  c("CN", "China"),
                  c("JP", "Japan"),
                  c("KP", "North Korea"),
                  c("KR", "South Korea"),
                  c("MN", "Mongolia"),
                  c("TW", "Taiwan"))),
          new Group(
              "SOUTH_ASIA",
              "South Asia",
              List.of(
                  c("AF", "Afghanistan"),
                  c("BD", "Bangladesh"),
                  c("BT", "Bhutan"),
                  c("IN", "India"),
                  c("MV", "Maldives"),
                  c("NP", "Nepal"),
                  c("PK", "Pakistan"),
                  c("LK", "Sri Lanka"))),
          new Group(
              "SOUTHEAST_ASIA",
              "Southeast Asia",
              List.of(
                  c("BN", "Brunei"),
                  c("KH", "Cambodia"),
                  c("ID", "Indonesia"),
                  c("LA", "Laos"),
                  c("MY", "Malaysia"),
                  c("MM", "Myanmar"),
                  c("PH", "Philippines"),
                  c("SG", "Singapore"),
                  c("TH", "Thailand"),
                  c("TL", "Timor-Leste"),
                  c("VN", "Vietnam"))),
          new Group(
              "CENTRAL_ASIA",
              "Central Asia",
              List.of(
                  c("KZ", "Kazakhstan"),
                  c("KG", "Kyrgyzstan"),
                  c("TJ", "Tajikistan"),
                  c("TM", "Turkmenistan"),
                  c("UZ", "Uzbekistan"))),
          new Group(
              "OCEANIA",
              "Oceania",
              List.of(
                  c("AU", "Australia"),
                  c("FJ", "Fiji"),
                  c("NZ", "New Zealand"),
                  c("PG", "Papua New Guinea"),
                  c("WS", "Samoa"),
                  c("SB", "Solomon Islands"),
                  c("TO", "Tonga"),
                  c("VU", "Vanuatu"))),
          new Group(
              "AFRICA",
              "Africa",
              List.of(
                  c("DZ", "Algeria"),
                  c("AO", "Angola"),
                  c("BJ", "Benin"),
                  c("BW", "Botswana"),
                  c("BF", "Burkina Faso"),
                  c("BI", "Burundi"),
                  c("CV", "Cape Verde"),
                  c("CM", "Cameroon"),
                  c("CF", "Central African Republic"),
                  c("TD", "Chad"),
                  c("KM", "Comoros"),
                  c("CG", "Congo"),
                  c("CD", "DR Congo"),
                  c("CI", "Ivory Coast"),
                  c("DJ", "Djibouti"),
                  c("EG", "Egypt"),
                  c("GQ", "Equatorial Guinea"),
                  c("ER", "Eritrea"),
                  c("SZ", "Eswatini"),
                  c("ET", "Ethiopia"),
                  c("GA", "Gabon"),
                  c("GM", "Gambia"),
                  c("GH", "Ghana"),
                  c("GN", "Guinea"),
                  c("GW", "Guinea-Bissau"),
                  c("KE", "Kenya"),
                  c("LS", "Lesotho"),
                  c("LR", "Liberia"),
                  c("LY", "Libya"),
                  c("MG", "Madagascar"),
                  c("MW", "Malawi"),
                  c("ML", "Mali"),
                  c("MR", "Mauritania"),
                  c("MU", "Mauritius"),
                  c("MA", "Morocco"),
                  c("MZ", "Mozambique"),
                  c("NA", "Namibia"),
                  c("NE", "Niger"),
                  c("NG", "Nigeria"),
                  c("RW", "Rwanda"),
                  c("ST", "Sao Tome and Principe"),
                  c("SN", "Senegal"),
                  c("SC", "Seychelles"),
                  c("SL", "Sierra Leone"),
                  c("SO", "Somalia"),
                  c("ZA", "South Africa"),
                  c("SS", "South Sudan"),
                  c("SD", "Sudan"),
                  c("TZ", "Tanzania"),
                  c("TG", "Togo"),
                  c("TN", "Tunisia"),
                  c("UG", "Uganda"),
                  c("ZM", "Zambia"),
                  c("ZW", "Zimbabwe"))));

  private static final Set<String> ALL_COUNTRY_CODES;
  private static final Set<String> NUMBER_TYPE_SET;

  static {
    Set<String> codes = new HashSet<>();
    for (Group group : GROUPS) {
      for (Country country : group.countries()) {
        codes.add(country.code());
      }
    }
    ALL_COUNTRY_CODES = unmodifiableSet(codes);
    NUMBER_TYPE_SET = new HashSet<>(ALL_NUMBER_TYPES);
  }

  public static Set<String> allCountryCodes() {
    return ALL_COUNTRY_CODES;
  }

  public static void validateMatrix(Map<String, List<String>> matrix) {
    if (matrix == null) {
      return;
    }
    for (var entry : matrix.entrySet()) {
      if (!ALL_COUNTRY_CODES.contains(entry.getKey())) {
        throw new IllegalArgumentException("Unknown country code: " + entry.getKey());
      }
      if (entry.getValue() != null) {
        for (String type : entry.getValue()) {
          if (!NUMBER_TYPE_SET.contains(type)) {
            throw new IllegalArgumentException("Unknown number type: " + type);
          }
        }
      }
    }
  }

  private static Country c(String code, String name) {
    return new Country(code, name);
  }
}
