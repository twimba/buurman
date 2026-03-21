package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Sid;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.TenantSummary;

@DisplayName("ContractPartyMapper")
class ContractPartyMapperTest {

  private final ContractPartyMapper mapper = new ContractPartyMapper();

  private static final Sid PARTY_ID = Sid.of("CPT01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final Sid TENANT_ID = Sid.of("TEN01HQJK4B2X5M3N7P8Q9R0S1T2");

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    @DisplayName("maps party with tenant summary")
    void mapsWithTenantSummary() {
      ContractParty party = createParty(ContractPartyRole.PRIMARY_TENANT);
      TenantSummary tenant =
          new TenantSummary(
              TENANT_ID, "John", "Doe", Optional.of("john@example.com"), Optional.empty());

      ContractPartyResponse response = mapper.toResponse(party, tenant);

      assertThat(response.identifier()).isEqualTo(PARTY_ID);
      assertThat(response.role()).isEqualTo(ContractPartyRole.PRIMARY_TENANT);
      assertThat(response.tenant()).isPresent();
      assertThat(response.tenant().get().firstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("maps party with null tenant summary to empty Optional")
    void mapsWithNullTenantSummary() {
      ContractParty party = createParty(ContractPartyRole.GUARANTOR);

      ContractPartyResponse response = mapper.toResponse(party, null);

      assertThat(response.identifier()).isEqualTo(PARTY_ID);
      assertThat(response.role()).isEqualTo(ContractPartyRole.GUARANTOR);
      assertThat(response.tenant()).isEmpty();
    }

    @Test
    @DisplayName("maps all ContractPartyRole values")
    void mapsAllRoles() {
      for (ContractPartyRole role : ContractPartyRole.values()) {
        ContractParty party = createParty(role);

        ContractPartyResponse response = mapper.toResponse(party, null);

        assertThat(response.role()).isEqualTo(role);
      }
    }

    @Test
    @DisplayName("throws when party has no identifier")
    void throwsWhenIdentifierMissing() {
      ContractParty party =
          ContractParty.builder()
              .id(UUID.randomUUID())
              .teamId(UUID.randomUUID())
              .contractId(UUID.randomUUID())
              .role(ContractPartyRole.PRIMARY_TENANT)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .createdBy(UUID.randomUUID())
              .updatedBy(UUID.randomUUID())
              .build();

      assertThatThrownBy(() -> mapper.toResponse(party, null))
          .isInstanceOf(NoSuchElementException.class);
    }
  }

  private ContractParty createParty(ContractPartyRole role) {
    return ContractParty.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(PARTY_ID))
        .teamId(UUID.randomUUID())
        .contractId(UUID.randomUUID())
        .role(role)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(UUID.randomUUID())
        .updatedBy(UUID.randomUUID())
        .build();
  }
}
