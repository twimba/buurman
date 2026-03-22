package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.Sid;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractRentComponentRepository {

  private final DSLContext dsl;
  private final Clock clock;

  private static final Table<?> TABLE = table("contract_rent_components");
  private static final Field<UUID> ID = field("id", UUID.class);
  private static final Field<String> IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> TEAM_ID = field("team_id", UUID.class);
  private static final Field<UUID> CONTRACT_ID = field("contract_id", UUID.class);
  private static final Field<UUID> RENT_PERIOD_ID = field("rent_period_id", UUID.class);
  private static final Field<String> COMPONENT_TYPE = field("component_type", String.class);
  private static final Field<Long> AMOUNT = field("amount", Long.class);
  private static final Field<String> CURRENCY = field("currency", String.class);
  private static final Field<String> DESCRIPTION = field("description", String.class);
  private static final Field<Integer> SORT_ORDER = field("sort_order", Integer.class);
  private static final Field<Timestamp> CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
  private static final Field<UUID> UPDATED_BY = field("updated_by", UUID.class);
  private static final Field<Timestamp> DELETED_AT = field("deleted_at", Timestamp.class);

  public List<ContractRentComponent> findByRentPeriodIdAndTeamId(UUID rentPeriodId, UUID teamId) {
    return List.copyOf(
        dsl.select()
            .from(TABLE)
            .where(RENT_PERIOD_ID.eq(rentPeriodId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(SORT_ORDER.asc())
            .fetch(this::toDomain));
  }

  public Map<UUID, List<ContractRentComponent>> findByRentPeriodIdsAndTeamId(
      Collection<UUID> rentPeriodIds, UUID teamId) {
    if (rentPeriodIds.isEmpty()) {
      return Map.of();
    }
    List<ContractRentComponent> all =
        dsl.select()
            .from(TABLE)
            .where(
                RENT_PERIOD_ID.in(rentPeriodIds).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(SORT_ORDER.asc())
            .fetch(this::toDomain);
    return all.stream().collect(Collectors.groupingBy(ContractRentComponent::getRentPeriodId));
  }

  public List<ContractRentComponent> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return List.copyOf(
        dsl.select()
            .from(TABLE)
            .where(CONTRACT_ID.eq(contractId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(SORT_ORDER.asc())
            .fetch(this::toDomain));
  }

  public Map<UUID, List<ContractRentComponent>> findByContractIdsAndTeamId(
      Collection<UUID> contractIds, UUID teamId) {
    if (contractIds.isEmpty()) {
      return Map.of();
    }
    List<ContractRentComponent> all =
        dsl.select()
            .from(TABLE)
            .where(CONTRACT_ID.in(contractIds).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(SORT_ORDER.asc())
            .fetch(this::toDomain);
    return all.stream().collect(Collectors.groupingBy(ContractRentComponent::getContractId));
  }

  public void replaceForRentPeriod(
      UUID rentPeriodId, UUID contractId, UUID teamId, List<ContractRentComponent> components) {
    // Soft-delete all existing active components for this rent period
    softDeleteByRentPeriodIdAndTeamId(rentPeriodId, teamId);

    // Insert new components
    for (ContractRentComponent component : components) {
      UUID id = UUID.randomUUID();
      Timestamp createdAt = Timestamp.from(component.getCreatedAt());
      Timestamp updatedAt = Timestamp.from(component.getUpdatedAt());

      dsl.insertInto(TABLE)
          .set(ID, id)
          .set(IDENTIFIER, component.getIdentifier().orElseThrow().value())
          .set(TEAM_ID, component.getTeamId())
          .set(CONTRACT_ID, component.getContractId())
          .set(RENT_PERIOD_ID, component.getRentPeriodId())
          .set(COMPONENT_TYPE, component.getComponentType().name())
          .set(AMOUNT, component.getAmount().toMinorUnits())
          .set(CURRENCY, component.getAmount().currency())
          .set(DESCRIPTION, component.getDescription().orElse(null))
          .set(SORT_ORDER, component.getSortOrder())
          .set(CREATED_AT, createdAt)
          .set(UPDATED_AT, updatedAt)
          .set(CREATED_BY, component.getCreatedBy())
          .set(UPDATED_BY, component.getUpdatedBy())
          .execute();

      component.setId(id);
    }
  }

  public void softDeleteByRentPeriodIdAndTeamId(UUID rentPeriodId, UUID teamId) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(TABLE)
        .set(DELETED_AT, now)
        .where(RENT_PERIOD_ID.eq(rentPeriodId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  public void softDeleteByContractIdAndTeamId(UUID contractId, UUID teamId) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(TABLE)
        .set(DELETED_AT, now)
        .where(CONTRACT_ID.eq(contractId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  private ContractRentComponent toDomain(Record record) {
    String currency = record.get(CURRENCY);
    Long minorUnits = record.get(AMOUNT);
    int digits = CurrencyUtils.getFractionalDigits(currency);
    BigDecimal majorUnits =
        minorUnits != null ? BigDecimal.valueOf(minorUnits, digits) : BigDecimal.ZERO;

    ContractRentComponent component = new ContractRentComponent();
    component.setId(record.get(ID));
    component.setIdentifier(Optional.of(Sid.of(record.get(IDENTIFIER))));
    component.setTeamId(record.get(TEAM_ID));
    component.setContractId(record.get(CONTRACT_ID));
    component.setRentPeriodId(record.get(RENT_PERIOD_ID));
    component.setComponentType(RentComponentType.valueOf(record.get(COMPONENT_TYPE)));
    component.setAmount(MoneyAmount.of(majorUnits, currency));
    component.setDescription(Optional.ofNullable(record.get(DESCRIPTION)));
    component.setSortOrder(record.get(SORT_ORDER));

    Timestamp createdAtVal = record.get(CREATED_AT);
    if (createdAtVal != null) {
      component.setCreatedAt(createdAtVal.toInstant());
    }

    Timestamp updatedAtVal = record.get(UPDATED_AT);
    if (updatedAtVal != null) {
      component.setUpdatedAt(updatedAtVal.toInstant());
    }

    component.setCreatedBy(record.get(CREATED_BY));
    component.setUpdatedBy(record.get(UPDATED_BY));

    Timestamp deletedAtVal = record.get(DELETED_AT);
    component.setDeletedAt(Optional.ofNullable(deletedAtVal).map(Timestamp::toInstant));

    return component;
  }
}
