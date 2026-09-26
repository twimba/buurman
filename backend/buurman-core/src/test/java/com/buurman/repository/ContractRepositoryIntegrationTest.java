package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * V068, but until this test existed, no integration test created a contract through {@link
 * ContractRepository#save} — every existing test either mocked the repository or inserted contracts
 * with raw SQL that included {@code unit_id} by hand (see {@link TestDataHelper#insertContract}).
 * That gap let a real regression (the repository silently dropping {@code unit_id} on insert) pass
 * the full suite; see the temporary revert performed while writing this test, recorded in the Task
 * 16 report.
 */
@DisplayName("ContractRepository — unit_id write path (V068)")
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
}
