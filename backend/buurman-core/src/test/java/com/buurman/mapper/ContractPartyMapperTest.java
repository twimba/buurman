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

import com.buurman.domain.ContactType;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.DataRetentionStatus;
import com.buurman.domain.Sid;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.ContractPartyResponse;

@DisplayName("ContractPartyMapper")
class ContractPartyMapperTest {

  private final ContractPartyMapper mapper = new ContractPartyMapper();

  private static final Sid PARTY_ID = Sid.of("CPT01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final Sid CONTACT_ID = Sid.of("CON01HQJK4B2X5M3N7P8Q9R0S1T2");

  @Nested
  @DisplayName("toResponse")
  class ToResponse {

    @Test
    @DisplayName("maps party with contact summary")
    void mapsWithContactSummary() {
      ContractParty party = createParty(ContractPartyRole.PRIMARY_TENANT);
      ContactSummary contact =
          new ContactSummary(
              CONTACT_ID,
              ContactType.INDIVIDUAL,
              "John Doe",
              Optional.of("John"),
              Optional.of("Doe"),
              Optional.of("john@example.com"),
              Optional.empty(),
              DataRetentionStatus.ACTIVE);

      ContractPartyResponse response = mapper.toResponse(party, contact);

      assertThat(response.identifier()).isEqualTo(PARTY_ID);
      assertThat(response.role()).isEqualTo(ContractPartyRole.PRIMARY_TENANT);
      assertThat(response.contact()).isPresent();
      assertThat(response.contact().get().displayName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("maps party with null contact summary to empty Optional")
    void mapsWithNullContactSummary() {
      ContractParty party = createParty(ContractPartyRole.GUARANTOR);

      ContractPartyResponse response = mapper.toResponse(party, null);

      assertThat(response.identifier()).isEqualTo(PARTY_ID);
      assertThat(response.role()).isEqualTo(ContractPartyRole.GUARANTOR);
      assertThat(response.contact()).isEmpty();
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
