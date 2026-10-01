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
import com.buurman.domain.SortDirection;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.dto.request.PageRequest;
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
  @DisplayName(
      "a NOTICE_GIVEN contract still occupies its unit: findActiveByUnitId finds it and"
          + " countActiveByUnitId counts it, while a TERMINATED one is neither found nor counted")
  void noticeGivenContractStillOccupiesItsUnit() {
    Contract underNotice =
        repository.save(contractBuilder().status(Contract.ContractStatus.NOTICE_GIVEN).build());

    assertThat(repository.findActiveByUnitId(unitId, TEAM_A_ID))
        .isPresent()
        .get()
        .extracting(Contract::getId)
        .isEqualTo(underNotice.getId());
    assertThat(repository.countActiveByUnitId(unitId, TEAM_A_ID)).isEqualTo(1);
    assertThat(repository.findInForceByTeamId(TEAM_A_ID))
        .extracting(Contract::getId)
        .containsExactly(underNotice.getId());
    // Team scoping still holds for the widened queries.
    assertThat(repository.countActiveByUnitId(unitId, TEAM_B_ID)).isZero();
    assertThat(repository.findInForceByTeamId(TEAM_B_ID)).isEmpty();

    underNotice.setStatus(Contract.ContractStatus.TERMINATED);
    repository.save(underNotice);

    assertThat(repository.findActiveByUnitId(unitId, TEAM_A_ID)).isEmpty();
    assertThat(repository.countActiveByUnitId(unitId, TEAM_A_ID)).isZero();
    assertThat(repository.findInForceByTeamId(TEAM_A_ID)).isEmpty();
  }

  @Test
  @DisplayName(
      "dashboard monthly income counts a NOTICE_GIVEN contract's rent (still in force), not a"
          + " TERMINATED one's, and stays team-scoped")
  void inForceContractIncomeIncludesNoticeGiven() {
    repository.save(
        contractBuilder()
            .status(Contract.ContractStatus.NOTICE_GIVEN)
            .rentAmount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
            .build());
    UUID unit2Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit2Id, propertyId, TEAM_A_ID, "2", "VACANT");
    repository.save(
        contractBuilder().unitId(unit2Id).status(Contract.ContractStatus.TERMINATED).build());

    assertThat(repository.findInForceContractIncomeByTeamId(TEAM_A_ID))
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.rentAmount()).isEqualByComparingTo("1200.00");
              assertThat(e.rentAmountCurrency()).isEqualTo("EUR");
            });
    assertThat(repository.findInForceContractIncomeByTeamId(TEAM_B_ID)).isEmpty();
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
          + " (uq_contracts_one_in_force_per_unit, V072/V085) -- a 409, not the 500"
          + " TooManyRowsException used to throw once two concurrent activations raced past the"
          + " application-level guard")
  void rejectsASecondActiveContractOnTheSameUnit() {
    repository.save(contractBuilder().build());

    assertThatThrownBy(() -> repository.save(contractBuilder().build()))
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("uq_contracts_one_in_force_per_unit");
  }

  @Test
  @DisplayName(
      "the database itself rejects a NOTICE_GIVEN contract on a unit that already has an ACTIVE"
          + " one, and vice versa, and two NOTICE_GIVEN contracts on the same unit"
          + " (uq_contracts_one_in_force_per_unit, V085) -- proving the DB backstop itself, not"
          + " just ContractService#assertUnitHasNoActiveContract")
  void rejectsAnyTwoInForceContractsOnTheSameUnit() {
    repository.save(contractBuilder().status(Contract.ContractStatus.ACTIVE).build());

    // ACTIVE + NOTICE_GIVEN on the same unit must collide at the DB level, even though
    // ContractService's own guard (findActiveByUnitId) would normally reject this first --
    // inserting directly through the repository bypasses that application-level check.
    assertThatThrownBy(
            () ->
                repository.save(
                    contractBuilder().status(Contract.ContractStatus.NOTICE_GIVEN).build()))
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("uq_contracts_one_in_force_per_unit");

    // A fresh unit: two NOTICE_GIVEN contracts (no ACTIVE one involved at all) must also collide.
    UUID otherUnitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, otherUnitId, propertyId, TEAM_A_ID, "2", "VACANT");
    repository.save(
        contractBuilder().unitId(otherUnitId).status(Contract.ContractStatus.NOTICE_GIVEN).build());

    assertThatThrownBy(
            () ->
                repository.save(
                    contractBuilder()
                        .unitId(otherUnitId)
                        .status(Contract.ContractStatus.NOTICE_GIVEN)
                        .build()))
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("uq_contracts_one_in_force_per_unit");
  }

  @Test
  @DisplayName(
      "search matches contact display name, property street/city, and contract identifier; excludes"
          + " non-matches; never crosses teams")
  void searchAcrossFieldsAndTeams() {
    UUID teamAPropertyId =
        TestDataHelper.insertProperty(
            dsl, TEAM_A_ID, USER_ID); // street "Main Street 1", city "Amsterdam"
    UUID teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    UUID teamAContactId =
        TestDataHelper.insertContact(dsl, TEAM_A_ID, USER_ID); // display_name "Jan de Vries"
    insertContractParty(teamAContractId, teamAContactId, TEAM_A_ID, USER_ID, "PRIMARY_TENANT");

    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);

    var byContactName =
        repository.findAllByTeamIdPaginated(
            TEAM_A_ID,
            null,
            null,
            null,
            "de vries",
            null,
            PageRequest.of(0, 25, null, (SortDirection) null));
    assertThat(byContactName.items()).extracting(Contract::getId).containsExactly(teamAContractId);

    var byPropertyCity =
        repository.findAllByTeamIdPaginated(
            TEAM_A_ID,
            null,
            null,
            null,
            "amsterdam",
            null,
            PageRequest.of(0, 25, null, (SortDirection) null));
    assertThat(byPropertyCity.items()).extracting(Contract::getId).contains(teamAContractId);

    // Every TEAM_B fixture has identical street/city/display_name values (the helper hardcodes
    // them), so this proves the search join is team-scoped: TEAM_A's query for TEAM_A-only data
    // (its own contract identifier) never returns TEAM_B's otherwise-identical-looking contract.
    var byContractIdentifier =
        repository.findAllByTeamIdPaginated(
            TEAM_A_ID,
            null,
            null,
            null,
            repository
                .getByIdAndTeamId(teamAContractId, TEAM_A_ID)
                .getIdentifier()
                .orElseThrow()
                .value(),
            null,
            PageRequest.of(0, 25, null, (SortDirection) null));
    assertThat(byContractIdentifier.items())
        .extracting(Contract::getId)
        .containsExactly(teamAContractId);
  }

  @Test
  @DisplayName(
      "search does not match a contract whose property has been soft-deleted, even though its"
          + " street/city would otherwise match")
  void searchExcludesSoftDeletedProperty() {
    UUID propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID); // "Main Street 1"
    UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, propertyId, USER_ID);

    // Sanity check: before the soft-delete, the search term does match.
    var beforeDelete =
        repository.findAllByTeamIdPaginated(
            TEAM_A_ID,
            null,
            null,
            null,
            "main street",
            null,
            PageRequest.of(0, 25, null, (SortDirection) null));
    assertThat(beforeDelete.items()).extracting(Contract::getId).contains(contractId);

    dsl.update(DSL.table("properties"))
        .set(DSL.field("deleted_at", LocalDateTime.class), LocalDateTime.of(2026, 3, 1, 12, 0))
        .where(DSL.field("id", UUID.class).eq(propertyId))
        .execute();

    var afterDelete =
        repository.findAllByTeamIdPaginated(
            TEAM_A_ID,
            null,
            null,
            null,
            "main street",
            null,
            PageRequest.of(0, 25, null, (SortDirection) null));
    assertThat(afterDelete.items()).extracting(Contract::getId).doesNotContain(contractId);
  }

  @Test
  @DisplayName(
      "endingWithinDays includes a contract ending inside the window and excludes one outside it,"
          + " using the unaliased WHERE-safe expression")
  void endingWithinDaysWindow() {
    // TestDataHelper.insertContract always creates its implicit unit with unit_number "1"; two
    // contracts on the SAME property would collide on uq_units_property_number (V072), so each
    // contract here gets its own property.
    UUID soonPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID farPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID soonContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, soonPropertyId, USER_ID);
    UUID farContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, farPropertyId, USER_ID);
    LocalDate today = LocalDate.now(CLOCK);
    dsl.update(DSL.table("contracts"))
        .set(DSL.field("end_date", LocalDate.class), today.plusDays(30))
        .where(DSL.field("id", UUID.class).eq(soonContractId))
        .execute();
    dsl.update(DSL.table("contracts"))
        .set(DSL.field("end_date", LocalDate.class), today.plusDays(200))
        .where(DSL.field("id", UUID.class).eq(farContractId))
        .execute();

    var result =
        repository.findAllByTeamIdPaginated(
            TEAM_A_ID,
            null,
            null,
            null,
            null,
            90,
            PageRequest.of(0, 25, null, (SortDirection) null));

    assertThat(result.items()).extracting(Contract::getId).contains(soonContractId);
    assertThat(result.items()).extracting(Contract::getId).doesNotContain(farContractId);
  }

  /**
   * Inserts a {@code contract_parties} row linking a contact to a contract, matching the exact
   * column set {@link ContractPartyRepository#save} uses (there is no existing {@code
   * TestDataHelper} fixture for this join table).
   */
  private void insertContractParty(
      UUID contractId, UUID contactId, UUID teamId, UUID createdBy, String role) {
    LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0, 0);
    dsl.insertInto(DSL.table("contract_parties"))
        .set(DSL.field("id", UUID.class), UUID.randomUUID())
        .set(DSL.field("identifier", String.class), SidGenerator.newContractPartyId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("contract_id", UUID.class), contractId)
        .set(DSL.field("contact_id", UUID.class), contactId)
        .set(DSL.field("role", String.class), role)
        .set(DSL.field("created_at", LocalDateTime.class), now)
        .set(DSL.field("updated_at", LocalDateTime.class), now)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .set(DSL.field("updated_by", UUID.class), createdBy)
        .execute();
  }
}
