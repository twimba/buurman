package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("EntityPrefix")
class EntityPrefixTest {

  @Nested
  @DisplayName("enum values")
  class EnumValues {

    @Test
    @DisplayName("has at least one value")
    void hasValues() {
      assertThat(EntityPrefix.values()).isNotEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EntityPrefix.class)
    @DisplayName("code is exactly 3 uppercase characters")
    void codeIsThreeUppercase(EntityPrefix prefix) {
      assertThat(prefix.getCode()).matches("[A-Z]{3}");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EntityPrefix.class)
    @DisplayName("entityName is non-blank")
    void entityNameNonBlank(EntityPrefix prefix) {
      assertThat(prefix.getEntityName()).isNotBlank();
    }

    @Test
    @DisplayName("all codes are unique")
    void allCodesUnique() {
      List<String> codes = Arrays.stream(EntityPrefix.values()).map(EntityPrefix::getCode).toList();
      assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("all entity names are unique")
    void allEntityNamesUnique() {
      List<String> names =
          Arrays.stream(EntityPrefix.values()).map(EntityPrefix::getEntityName).toList();
      assertThat(names).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("specific prefixes")
  class SpecificPrefixes {

    @Test
    @DisplayName("AMN maps to Amenities")
    void amn() {
      assertThat(EntityPrefix.AMN.getCode()).isEqualTo("AMN");
      assertThat(EntityPrefix.AMN.getEntityName()).isEqualTo("Amenities");
    }

    @Test
    @DisplayName("CON maps to Contracts")
    void con() {
      assertThat(EntityPrefix.CON.getCode()).isEqualTo("CON");
      assertThat(EntityPrefix.CON.getEntityName()).isEqualTo("Contracts");
    }

    @Test
    @DisplayName("PRO maps to Properties")
    void pro() {
      assertThat(EntityPrefix.PRO.getCode()).isEqualTo("PRO");
      assertThat(EntityPrefix.PRO.getEntityName()).isEqualTo("Properties");
    }

    @Test
    @DisplayName("CTC maps to Contacts")
    void ctc() {
      assertThat(EntityPrefix.CTC.getCode()).isEqualTo("CTC");
      assertThat(EntityPrefix.CTC.getEntityName()).isEqualTo("Contacts");
    }

    @Test
    @DisplayName("enum name equals its own code")
    void enumNameEqualsCode() {
      // Each enum constant name IS the 3-char code
      for (EntityPrefix prefix : EntityPrefix.values()) {
        assertThat(prefix.name()).isEqualTo(prefix.getCode());
      }
    }
  }
}
