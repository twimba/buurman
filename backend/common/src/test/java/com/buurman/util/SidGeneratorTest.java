package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;

class SidGeneratorTest {

  @Nested
  @DisplayName("newToken")
  class NewToken {

    @Test
    @DisplayName("returns non-null Sid with 26 characters")
    void generatesToken() {
      Sid token = SidGenerator.newToken();

      assertThat(token).isNotNull();
      assertThat(token.value()).hasSize(26);
    }
  }

  @Nested
  @DisplayName("newPropertyId")
  class NewPropertyId {

    @Test
    @DisplayName("returns PropertyIdentifier starting with PRO, 29 chars")
    void generatesPropertyId() {
      PropertyIdentifier id = SidGenerator.newPropertyId();

      assertThat(id).isInstanceOf(PropertyIdentifier.class);
      assertThat(id.value()).startsWith("PRO");
      assertThat(id.value()).hasSize(29);
    }
  }

  @Nested
  @DisplayName("newContractId")
  class NewContractId {

    @Test
    @DisplayName("returns ContractIdentifier starting with CON, 29 chars")
    void generatesContractId() {
      ContractIdentifier id = SidGenerator.newContractId();

      assertThat(id).isInstanceOf(ContractIdentifier.class);
      assertThat(id.value()).startsWith("CON");
      assertThat(id.value()).hasSize(29);
    }
  }

  @Nested
  @DisplayName("newTenantId")
  class NewTenantId {

    @Test
    @DisplayName("returns TenantIdentifier starting with TEN, 29 chars")
    void generatesTenantId() {
      TenantIdentifier id = SidGenerator.newTenantId();

      assertThat(id).isInstanceOf(TenantIdentifier.class);
      assertThat(id.value()).startsWith("TEN");
      assertThat(id.value()).hasSize(29);
    }
  }

  @Nested
  @DisplayName("uniqueness")
  class Uniqueness {

    @Test
    @DisplayName("generates 100 unique IDs")
    void allDistinct() {
      List<String> ids =
          IntStream.range(0, 100).mapToObj(i -> SidGenerator.newPropertyId().value()).toList();

      assertThat(ids).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("sortability")
  class Sortability {

    @Test
    @DisplayName("timestamp prefix of IDs is non-decreasing")
    void timestampNonDecreasing() {
      // ULID format: first 10 chars encode the timestamp (after the 3-char entity prefix)
      // Within the same millisecond, the random suffix is NOT guaranteed sorted
      List<String> timestamps =
          IntStream.range(0, 10)
              .mapToObj(i -> SidGenerator.newPropertyId().value().substring(3, 13))
              .toList();

      for (int i = 1; i < timestamps.size(); i++) {
        assertThat(timestamps.get(i))
            .as("timestamp at index %d should be >= timestamp at index %d", i, i - 1)
            .isGreaterThanOrEqualTo(timestamps.get(i - 1));
      }
    }
  }
}
