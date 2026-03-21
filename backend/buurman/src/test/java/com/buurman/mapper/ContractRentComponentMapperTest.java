package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.Sid;
import com.buurman.dto.response.RentComponentResponse;
import com.buurman.util.MoneyAmount;

@DisplayName("ContractRentComponentMapper")
class ContractRentComponentMapperTest {

  private final ContractRentComponentMapper mapper = new ContractRentComponentMapper();

  private static final Sid COMPONENT_ID = Sid.of("RCM01HQJK4B2X5M3N7P8Q9R0S1T2");

  @Nested
  @DisplayName("toResponse")
  class ToResponseTest {

    @Test
    @DisplayName("maps complete component to response")
    void mapsCompleteComponent() {
      ContractRentComponent component = createComponent(
          RentComponentType.BASE_RENT, new BigDecimal("1000.00"), "EUR",
          Optional.of("Monthly base rent"), 1);

      RentComponentResponse response = mapper.toResponse(component);

      assertThat(response.identifier()).isEqualTo(COMPONENT_ID);
      assertThat(response.componentType()).isEqualTo(RentComponentType.BASE_RENT);
      assertThat(response.componentTypeDisplayName()).isEqualTo("Base Rent");
      assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("1000.00"));
      assertThat(response.currency()).isEqualTo("EUR");
      assertThat(response.description()).contains("Monthly base rent");
      assertThat(response.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("maps displayName for all component types")
    void mapsDisplayNameForAllTypes() {
      for (RentComponentType type : RentComponentType.values()) {
        ContractRentComponent component = createComponent(
            type, BigDecimal.ONE, "EUR", Optional.empty(), 0);

        RentComponentResponse response = mapper.toResponse(component);

        assertThat(response.componentType()).isEqualTo(type);
        assertThat(response.componentTypeDisplayName()).isEqualTo(type.getDisplayName());
      }
    }

    @Test
    @DisplayName("maps empty description")
    void mapsEmptyDescription() {
      ContractRentComponent component = createComponent(
          RentComponentType.PARKING, BigDecimal.TEN, "EUR", Optional.empty(), 5);

      RentComponentResponse response = mapper.toResponse(component);

      assertThat(response.description()).isEmpty();
    }

    @Test
    @DisplayName("throws when identifier is missing")
    void throwsWhenIdentifierMissing() {
      ContractRentComponent component = ContractRentComponent.builder()
          .componentType(RentComponentType.BASE_RENT)
          .amount(MoneyAmount.of(BigDecimal.ONE, "EUR"))
          .build();

      assertThatThrownBy(() -> mapper.toResponse(component))
          .isInstanceOf(NoSuchElementException.class);
    }
  }

  @Nested
  @DisplayName("toResponses")
  class ToResponsesTest {

    @Test
    @DisplayName("maps list of components")
    void mapsList() {
      List<ContractRentComponent> components = List.of(
          createComponent(RentComponentType.BASE_RENT, new BigDecimal("900.00"), "EUR",
              Optional.empty(), 1),
          createComponent(RentComponentType.SERVICE_COSTS, new BigDecimal("100.00"), "EUR",
              Optional.empty(), 2));

      List<RentComponentResponse> responses = mapper.toResponses(components);

      assertThat(responses).hasSize(2);
      assertThat(responses.get(0).componentType()).isEqualTo(RentComponentType.BASE_RENT);
      assertThat(responses.get(1).componentType()).isEqualTo(RentComponentType.SERVICE_COSTS);
    }

    @Test
    @DisplayName("returns empty list for empty input")
    void returnsEmptyForEmptyInput() {
      List<RentComponentResponse> responses = mapper.toResponses(List.of());

      assertThat(responses).isEmpty();
    }
  }

  private ContractRentComponent createComponent(
      RentComponentType type, BigDecimal amount, String currency,
      Optional<String> description, int sortOrder) {
    return ContractRentComponent.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(COMPONENT_ID))
        .teamId(UUID.randomUUID())
        .contractId(UUID.randomUUID())
        .componentType(type)
        .amount(MoneyAmount.of(amount, currency))
        .description(description)
        .sortOrder(sortOrder)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(UUID.randomUUID())
        .updatedBy(UUID.randomUUID())
        .build();
  }
}
