package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contract;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.mapper.ContractRecordMapper;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Proves {@code ContractRepository.save()} actually persists {@code unit_id} against a real
 * PostgreSQL NOT NULL constraint (BUUR-106 Task 16). {@code contracts.unit_id} became NOT NULL in
 * V070, but until this test existed, no integration test created a contract through {@link
 * ContractRepository#save} — every existing test either mocked the repository or inserted contracts
 * with raw SQL that included {@code unit_id} by hand (see {@link TestDataHelper#insertContract}).
 * That gap let a real regression (the repository silently dropping {@code unit_id} on insert) pass
 * the full suite; see the temporary revert performed while writing this test, recorded in the Task
 * 16 report.
 */
@DisplayName("ContractRepository — unit_id write path (V070)")
class ContractRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContractRepository repository;
  private UUID propertyId;
  private UUID unitId;

  @BeforeEach
  void setUpRepository() {
    CountryMetadataSerializer countryMetadataSerializer =
        new CountryMetadataSerializer(new ObjectMapper());
    repository =
        new ContractRepository(
            dsl,
            new ContractRecordMapper(countryMetadataSerializer),
            countryMetadataSerializer,
            CLOCK);

    propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    unitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unitId, propertyId, TEAM_A_ID, "1", "OCCUPIED");
  }

  private Contract.ContractBuilder contractBuilder() {
    return Contract.builder()
        .identifier(Optional.of(SidGenerator.newContractId()))
        .teamId(TEAM_A_ID)
        .propertyId(propertyId)
        .unitId(unitId)
        .contractType(Contract.ContractType.FIXED_TERM)
        .startDate(LocalDate.of(2026, 1, 1))
        .rentAmount(MoneyAmount.of(new BigDecimal("1500.00"), "EUR"))
        .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
        .status(Contract.ContractStatus.ACTIVE)
        .createdBy(USER_ID)
        .updatedBy(USER_ID);
  }

  @Test
  @DisplayName("persists unit_id on insert, pointing at a unit of the contract's own property")
  void savesAndReadsBackUnitId() {
    Contract saved = repository.save(contractBuilder().build());

    Contract found =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

    assertThat(found.getUnitId()).isEqualTo(unitId);
    assertThat(found.getPropertyId()).isEqualTo(propertyId);

    UUID unitsOwnPropertyId =
        dsl.select(DSL.field("property_id", UUID.class))
            .from(DSL.table("units"))
            .where(DSL.field("id", UUID.class).eq(found.getUnitId()))
            .fetchOne(0, UUID.class);
    assertThat(unitsOwnPropertyId).isEqualTo(propertyId);
  }

  @Test
  @DisplayName("duplicateContract's save() also persists unit_id, copied from the source contract")
  void duplicatePersistsUnitId() {
    Contract source = repository.save(contractBuilder().build());

    // Mirrors ContractService#duplicateContract: a fresh identifier/DRAFT status, everything else
    // — including unitId — copied from the source. That save() call is the one this test guards;
    // it goes through the exact same repository method as the original insert.
    Contract duplicate =
        repository.save(
            Contract.builder()
                .identifier(Optional.of(SidGenerator.newContractId()))
                .teamId(TEAM_A_ID)
                .propertyId(source.getPropertyId())
                .unitId(source.getUnitId())
                .contractType(source.getContractType())
                .startDate(source.getStartDate())
                .rentAmount(source.getRentAmount())
                .paymentFrequency(source.getPaymentFrequency())
                .status(Contract.ContractStatus.DRAFT)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build());

    Contract foundDuplicate =
        repository.getByIdentifierAndTeamId(duplicate.getIdentifier().orElseThrow(), TEAM_A_ID);

    assertThat(foundDuplicate.getUnitId()).isEqualTo(unitId);
    assertThat(foundDuplicate.getId()).isNotEqualTo(source.getId());
    assertThat(repository.findByPropertyId(propertyId, TEAM_A_ID)).hasSize(2);
  }

  @Test
  @DisplayName(
      "findActiveByUnitId is scoped to the unit, not the property (BUUR-106 Critical 1): a second"
          + " unit of an already-let building has no active contract of its own, while the first"
          + " unit's is still found")
  void findActiveByUnitIdIsScopedPerUnitNotProperty() {
    // unitId (from setUp) already has an ACTIVE contract via contractBuilder()'s default status.
    Contract activeOnUnit1 = repository.save(contractBuilder().build());

    UUID unit2Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit2Id, propertyId, TEAM_A_ID, "2", "VACANT");

    // The building's first unit is let: ContractService's create/activate guard, which calls
    // exactly this method keyed on the resolved unit, must still find it and refuse a second
    // ACTIVE contract on the SAME unit.
    assertThat(repository.findActiveByUnitId(unitId, TEAM_A_ID))
        .isPresent()
        .get()
        .extracting(Contract::getId)
        .isEqualTo(activeOnUnit1.getId());

    // A second, different unit of the same property has no active contract of its own — so the
    // guard must let a contract be created for it. Before this fix, the guard queried by
    // property_id and would incorrectly reject this as "property already has an active contract".
    assertThat(repository.findActiveByUnitId(unit2Id, TEAM_A_ID)).isEmpty();
  }

  @Test
  @DisplayName("a team-B-scoped read cannot see team A's contract")
  void isolatesByTeam() {
    Contract saved = repository.save(contractBuilder().build());

    assertThat(repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
        .isEmpty();
    assertThat(repository.findByIdAndTeamId(saved.getId(), TEAM_B_ID)).isEmpty();
    assertThat(repository.findAllByTeamId(TEAM_B_ID)).isEmpty();
  }

  @Test
  @DisplayName(
      "the database itself rejects a contract whose unit_id belongs to a different property"
          + " (fk_contracts_unit_property, V072)")
  void rejectsUnitFromADifferentProperty() {
    UUID otherPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0);

    // unitId (from setUp) belongs to propertyId, not otherPropertyId -- a plain application-level
    // bug (or a sixth write site that forgets the check) would otherwise attach this contract to
    // a neighbour's flat.
    assertThatThrownBy(
            () ->
                dsl.insertInto(DSL.table("contracts"))
                    .set(DSL.field("id", UUID.class), UUID.randomUUID())
                    .set(
                        DSL.field("identifier", String.class), SidGenerator.newContractId().value())
                    .set(DSL.field("team_id", UUID.class), TEAM_A_ID)
                    .set(DSL.field("property_id", UUID.class), otherPropertyId)
                    .set(DSL.field("unit_id", UUID.class), unitId)
                    .set(DSL.field("contract_type", String.class), "FIXED_TERM")
                    .set(DSL.field("start_date", LocalDate.class), LocalDate.of(2026, 1, 1))
                    .set(DSL.field("rent_amount", Long.class), 100000L)
                    .set(DSL.field("rent_amount_currency", String.class), "EUR")
                    .set(DSL.field("payment_frequency", String.class), "MONTHLY")
                    .set(DSL.field("status", String.class), "ACTIVE")
                    .set(DSL.field("created_at", LocalDateTime.class), now)
                    .set(DSL.field("updated_at", LocalDateTime.class), now)
                    .set(DSL.field("created_by", UUID.class), USER_ID)
                    .set(DSL.field("updated_by", UUID.class), USER_ID)
                    .execute())
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("fk_contracts_unit_property");
  }

  @Test
  @DisplayName(
      "the database itself rejects a second ACTIVE contract on the same unit"
          + " (uq_contracts_one_active_per_unit, V072) -- a 409, not the 500 TooManyRowsException"
          + " used to throw once two concurrent activations raced past the application-level guard")
  void rejectsASecondActiveContractOnTheSameUnit() {
    repository.save(contractBuilder().build());

    assertThatThrownBy(() -> repository.save(contractBuilder().build()))
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("uq_contracts_one_active_per_unit");
  }
}
