package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract.ContractStatus;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.SidGenerator;

/**
 * Uses reflection to test {@code validateStatusTransition} because ContractService has 13+
 * constructor dependencies — mocking them all provides no value for this pure business-rule method.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService — status transitions")
class ContractServiceTest {

  private Method validateStatusTransition;
  private Object serviceInstance;

  @BeforeEach
  void setUp() throws Exception {
    Class<?> clazz = ContractService.class;
    validateStatusTransition =
        clazz.getDeclaredMethod(
            "validateStatusTransition", ContractStatus.class, ContractStatus.class);
    validateStatusTransition.setAccessible(true);

    // CALLS_REAL_METHODS so the private method body runs; it uses no fields, so this is safe.
    serviceInstance =
        org.mockito.Mockito.mock(
            ContractService.class,
            org.mockito.Mockito.withSettings()
                .defaultAnswer(org.mockito.Mockito.CALLS_REAL_METHODS));
  }

  private void invokeValidation(ContractStatus from, ContractStatus to) throws Exception {
    try {
      validateStatusTransition.invoke(serviceInstance, from, to);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }

  @Nested
  @DisplayName("valid transitions")
  class ValidTransitions {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "DRAFT, PENDING_SIGNATURE",
      "DRAFT, ACTIVE",
      "PENDING_SIGNATURE, DRAFT",
      "PENDING_SIGNATURE, ACTIVE",
      "ACTIVE, TERMINATED",
      "ACTIVE, EXPIRED"
    })
    @DisplayName("accepts valid transition")
    void acceptsValidTransition(ContractStatus from, ContractStatus to) {
      assertThatCode(() -> invokeValidation(from, to)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("invalid transitions")
  class InvalidTransitions {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "DRAFT, TERMINATED",
      "DRAFT, EXPIRED",
      "DRAFT, DRAFT",
      "PENDING_SIGNATURE, TERMINATED",
      "PENDING_SIGNATURE, EXPIRED",
      "PENDING_SIGNATURE, PENDING_SIGNATURE",
      "ACTIVE, DRAFT",
      "ACTIVE, PENDING_SIGNATURE",
      "ACTIVE, ACTIVE",
      "EXPIRED, DRAFT",
      "EXPIRED, ACTIVE",
      "EXPIRED, PENDING_SIGNATURE",
      "EXPIRED, TERMINATED",
      "EXPIRED, EXPIRED",
      "TERMINATED, DRAFT",
      "TERMINATED, ACTIVE",
      "TERMINATED, PENDING_SIGNATURE",
      "TERMINATED, EXPIRED",
      "TERMINATED, TERMINATED"
    })
    @DisplayName("rejects invalid transition")
    void rejectsInvalidTransition(ContractStatus from, ContractStatus to) {
      assertThatThrownBy(() -> invokeValidation(from, to))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid status transition");
    }
  }

  @Nested
  @DisplayName("terminal states")
  class TerminalStates {

    @Test
    @DisplayName("EXPIRED is a terminal state — no transitions out")
    void expiredIsTerminal() {
      for (ContractStatus to : ContractStatus.values()) {
        assertThatThrownBy(() -> invokeValidation(ContractStatus.EXPIRED, to))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Test
    @DisplayName("TERMINATED is a terminal state — no transitions out")
    void terminatedIsTerminal() {
      for (ContractStatus to : ContractStatus.values()) {
        assertThatThrownBy(() -> invokeValidation(ContractStatus.TERMINATED, to))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  /**
   * Same reflection technique as above, applied to {@code resolveUnitId}: it only reaches {@code
   * unitRepository}, so that one field is injected into an otherwise-unconstructed mock rather than
   * satisfying all 13+ constructor dependencies.
   */
  @Nested
  @DisplayName("resolveUnitId")
  class ResolveUnitId {

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID OTHER_PROPERTY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private UnitRepository unitRepository;
    private Method resolveUnitId;
    private Object contractServiceInstance;

    @BeforeEach
    void setUp() throws Exception {
      unitRepository = Mockito.mock(UnitRepository.class);

      contractServiceInstance =
          Mockito.mock(
              ContractService.class,
              Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

      Field unitRepositoryField = ContractService.class.getDeclaredField("unitRepository");
      unitRepositoryField.setAccessible(true);
      unitRepositoryField.set(contractServiceInstance, unitRepository);

      resolveUnitId =
          ContractService.class.getDeclaredMethod(
              "resolveUnitId", Property.class, String.class, UserPrincipal.class);
      resolveUnitId.setAccessible(true);
    }

    private UUID invoke(Property property, @Nullable String unitIdentifier, UserPrincipal principal)
        throws Throwable {
      try {
        return (UUID)
            resolveUnitId.invoke(contractServiceInstance, property, unitIdentifier, principal);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }

    private Property property(UUID id, UUID teamId) {
      return Property.builder().id(id).teamId(teamId).street("Keizersgracht 1").build();
    }

    private UserPrincipal principal(UUID teamId) {
      return new UserPrincipal(
          USER_ID,
          "usr_test",
          "kc-123",
          "test@example.com",
          "Test User",
          teamId,
          "team_test",
          com.buurman.domain.TeamRole.TEAM_ADMIN);
    }

    private Unit unit(UUID id, UUID propertyId, UUID teamId, String number) {
      return Unit.builder()
          .id(id)
          .identifier(Optional.of(SidGenerator.newUnitId()))
          .teamId(teamId)
          .propertyId(propertyId)
          .unitNumber(number)
          .unitType(UnitType.APARTMENT)
          .status(UnitStatus.VACANT)
          .build();
    }

    @Test
    @DisplayName("omitted identifier resolves the property's single unit")
    void omittedIdentifierResolvesSingleUnit() throws Throwable {
      Property property = property(PROPERTY_ID, TEAM_ID);
      Unit onlyUnit = unit(UUID.randomUUID(), PROPERTY_ID, TEAM_ID, "1");
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(List.of(onlyUnit));

      UUID resolved = invoke(property, null, principal(TEAM_ID));

      assertThat(resolved).isEqualTo(onlyUnit.getId());
    }

    @Test
    @DisplayName(
        "omitted identifier with several units names the property and asks the client to choose")
    void omittedIdentifierWithMultipleUnitsThrows() {
      Property property = property(PROPERTY_ID, TEAM_ID);
      when(unitRepository.findAllByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID))
          .thenReturn(
              List.of(
                  unit(UUID.randomUUID(), PROPERTY_ID, TEAM_ID, "1"),
                  unit(UUID.randomUUID(), PROPERTY_ID, TEAM_ID, "2"),
                  unit(UUID.randomUUID(), PROPERTY_ID, TEAM_ID, "3")));

      assertThatThrownBy(() -> invoke(property, null, principal(TEAM_ID)))
          .isInstanceOf(BadRequestException.class)
          .hasMessage(
              "Property Keizersgracht 1 has 3 units. Specify which unit the contract is for.");
    }

    @Test
    @DisplayName("supplied identifier belonging to this property is used")
    void suppliedIdentifierForThisPropertyIsUsed() throws Throwable {
      Property property = property(PROPERTY_ID, TEAM_ID);
      Unit unit = unit(UUID.randomUUID(), PROPERTY_ID, TEAM_ID, "2");
      String identifierValue = unit.getIdentifier().orElseThrow().value();
      when(unitRepository.getByIdentifierAndTeamId(Sid.of(identifierValue), TEAM_ID))
          .thenReturn(unit);

      UUID resolved = invoke(property, identifierValue, principal(TEAM_ID));

      assertThat(resolved).isEqualTo(unit.getId());
    }

    @Test
    @DisplayName(
        "supplied identifier belonging to a different property is rejected — never a silent"
            + " cross-property contract")
    void suppliedIdentifierForDifferentPropertyThrows() {
      Property property = property(PROPERTY_ID, TEAM_ID);
      Unit otherPropertyUnit = unit(UUID.randomUUID(), OTHER_PROPERTY_ID, TEAM_ID, "1");
      String identifierValue = otherPropertyUnit.getIdentifier().orElseThrow().value();
      when(unitRepository.getByIdentifierAndTeamId(Sid.of(identifierValue), TEAM_ID))
          .thenReturn(otherPropertyUnit);

      assertThatThrownBy(() -> invoke(property, identifierValue, principal(TEAM_ID)))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("The chosen unit does not belong to this property.");
    }

    @Test
    @DisplayName("supplied identifier belonging to another team 404s instead of leaking existence")
    void suppliedIdentifierForAnotherTeamThrowsNotFound() {
      Property property = property(PROPERTY_ID, TEAM_ID);
      String identifierValue = SidGenerator.newUnitId().value();
      // Simulates UnitRepository.getByIdentifierAndTeamId's real behaviour for a wrong-team unit:
      // NotFoundException, never a BadRequestException, so existence is never leaked as a 400.
      when(unitRepository.getByIdentifierAndTeamId(Sid.of(identifierValue), TEAM_ID))
          .thenThrow(new NotFoundException("Unit not found"));

      assertThatThrownBy(() -> invoke(property, identifierValue, principal(TEAM_ID)))
          .isInstanceOf(NotFoundException.class)
          .hasMessage("Unit not found");
    }
  }
}
