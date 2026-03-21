package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CountryGroups")
class CountryGroupsTest {

  @Nested
  @DisplayName("GROUPS")
  class Groups {

    @Test
    @DisplayName("has 13 geographic regions")
    void has13Regions() {
      assertThat(CountryGroups.GROUPS).hasSize(13);
    }

    @Test
    @DisplayName("every group has non-empty id, name, and countries")
    void allGroupsHaveRequiredFields() {
      for (CountryGroups.Group group : CountryGroups.GROUPS) {
        assertThat(group.id()).as("group id").isNotBlank();
        assertThat(group.name()).as("group name").isNotBlank();
        assertThat(group.countries()).as("countries in %s", group.id()).isNotEmpty();
      }
    }

    @Test
    @DisplayName("no duplicate country codes across all groups")
    void noDuplicateCountryCodes() {
      Set<String> seen = new HashSet<>();
      for (CountryGroups.Group group : CountryGroups.GROUPS) {
        for (CountryGroups.Country country : group.countries()) {
          assertThat(seen.add(country.code()))
              .as("duplicate country code %s in group %s", country.code(), group.id())
              .isTrue();
        }
      }
    }

    @Test
    @DisplayName("all country codes are 2-char uppercase ISO 3166-1 alpha-2")
    void allCodesAreIso3166() {
      for (CountryGroups.Group group : CountryGroups.GROUPS) {
        for (CountryGroups.Country country : group.countries()) {
          assertThat(country.code())
              .as("country code in %s", group.id())
              .matches("[A-Z]{2}");
        }
      }
    }

    @Test
    @DisplayName("all country names are non-blank")
    void allNamesNonBlank() {
      for (CountryGroups.Group group : CountryGroups.GROUPS) {
        for (CountryGroups.Country country : group.countries()) {
          assertThat(country.name())
              .as("country name for %s", country.code())
              .isNotBlank();
        }
      }
    }

    @Test
    @DisplayName("no duplicate group ids")
    void noDuplicateGroupIds() {
      List<String> ids = CountryGroups.GROUPS.stream().map(CountryGroups.Group::id).toList();
      assertThat(ids).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("ALL_NUMBER_TYPES")
  class AllNumberTypes {

    @Test
    @DisplayName("has exactly 10 types")
    void hasTenTypes() {
      assertThat(CountryGroups.ALL_NUMBER_TYPES).hasSize(10);
    }

    @Test
    @DisplayName("contains expected phone number types")
    void containsExpectedTypes() {
      assertThat(CountryGroups.ALL_NUMBER_TYPES)
          .contains("MOBILE", "FIXED_LINE", "VOIP", "TOLL_FREE", "PAGER", "UAN");
    }

    @Test
    @DisplayName("no duplicates")
    void noDuplicates() {
      assertThat(CountryGroups.ALL_NUMBER_TYPES).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("allCountryCodes")
  class AllCountryCodes {

    @Test
    @DisplayName("returns non-empty set")
    void nonEmpty() {
      assertThat(CountryGroups.allCountryCodes()).isNotEmpty();
    }

    @Test
    @DisplayName("contains known codes NL, US, JP")
    void containsKnownCodes() {
      assertThat(CountryGroups.allCountryCodes()).contains("NL", "US", "JP");
    }

    @Test
    @DisplayName("does not contain invalid codes")
    void doesNotContainInvalid() {
      assertThat(CountryGroups.allCountryCodes()).doesNotContain("XX", "ZZ", "");
    }

    @Test
    @DisplayName("set is unmodifiable")
    void unmodifiable() {
      Set<String> codes = CountryGroups.allCountryCodes();
      assertThatThrownBy(() -> codes.add("XX"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("count matches total countries across all groups")
    void countMatchesGroups() {
      long totalCountries =
          CountryGroups.GROUPS.stream()
              .mapToLong(g -> g.countries().size())
              .sum();
      assertThat(CountryGroups.allCountryCodes()).hasSize((int) totalCountries);
    }
  }

  @Nested
  @DisplayName("validateMatrix")
  class ValidateMatrix {

    @Test
    @DisplayName("empty map is valid")
    void emptyMapValid() {
      assertThatCode(() -> CountryGroups.validateMatrix(Map.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("valid country with valid number types passes")
    void validMatrixPasses() {
      Map<String, List<String>> matrix = Map.of("NL", List.of("MOBILE", "FIXED_LINE"));
      assertThatCode(() -> CountryGroups.validateMatrix(matrix)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("unknown country code throws IllegalArgumentException")
    void unknownCountryThrows() {
      Map<String, List<String>> matrix = Map.of("XX", List.of("MOBILE"));
      assertThatThrownBy(() -> CountryGroups.validateMatrix(matrix))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown country code");
    }

    @Test
    @DisplayName("unknown number type throws IllegalArgumentException")
    void unknownNumberTypeThrows() {
      Map<String, List<String>> matrix = Map.of("NL", List.of("INVALID_TYPE"));
      assertThatThrownBy(() -> CountryGroups.validateMatrix(matrix))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown number type");
    }

    @Test
    @DisplayName("null value list for a valid country is accepted")
    void nullValueListAccepted() {
      java.util.HashMap<String, List<String>> matrix = new java.util.HashMap<>();
      matrix.put("NL", null);
      assertThatCode(() -> CountryGroups.validateMatrix(matrix)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("multiple valid entries pass")
    void multipleValidEntries() {
      Map<String, List<String>> matrix =
          Map.of(
              "NL", List.of("MOBILE", "VOIP"),
              "US", List.of("FIXED_LINE"),
              "JP", List.of("PAGER", "UAN"));
      assertThatCode(() -> CountryGroups.validateMatrix(matrix)).doesNotThrowAnyException();
    }
  }
}
