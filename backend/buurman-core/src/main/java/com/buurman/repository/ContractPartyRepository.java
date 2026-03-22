package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractPartyRepository {

  private final DSLContext dsl;
  private final Clock clock;

  private static final Table<?> CONTRACT_PARTIES = table("contract_parties");
  private static final Field<UUID> ID = field("id", UUID.class);
  private static final Field<String> IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> TEAM_ID = field("team_id", UUID.class);
  private static final Field<UUID> CONTRACT_ID = field("contract_id", UUID.class);
  private static final Field<UUID> CONTACT_ID = field("contact_id", UUID.class);
  private static final Field<String> ROLE = field("role", String.class);
  private static final Field<Timestamp> CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
  private static final Field<UUID> UPDATED_BY = field("updated_by", UUID.class);
  private static final Field<Timestamp> DELETED_AT = field("deleted_at", Timestamp.class);

  public ContractParty save(ContractParty party) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();

    Timestamp createdAt = Timestamp.from(party.getCreatedAt());
    Timestamp updatedAt = Timestamp.from(party.getUpdatedAt());

    dsl.insertInto(CONTRACT_PARTIES)
        .set(ID, id)
        .set(IDENTIFIER, party.getIdentifier().orElseThrow().value())
        .set(TEAM_ID, party.getTeamId())
        .set(CONTRACT_ID, party.getContractId())
        .set(CONTACT_ID, party.getContactId().orElse(null))
        .set(ROLE, party.getRole().name())
        .set(CREATED_AT, createdAt)
        .set(UPDATED_AT, updatedAt)
        .set(CREATED_BY, party.getCreatedBy())
        .set(UPDATED_BY, party.getUpdatedBy())
        .execute();

    party.setId(id);
    party.setCreatedAt(createdAt.toInstant());
    party.setUpdatedAt(updatedAt.toInstant());
    return party;
  }

  public List<ContractParty> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return List.copyOf(
        dsl.select()
            .from(CONTRACT_PARTIES)
            .where(CONTRACT_ID.eq(contractId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(ROLE.asc(), CREATED_AT.asc())
            .fetch(this::toDomain));
  }

  public List<ContractParty> findByContractIdsAndTeamId(Collection<UUID> contractIds, UUID teamId) {
    if (contractIds == null || contractIds.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.select()
            .from(CONTRACT_PARTIES)
            .where(CONTRACT_ID.in(contractIds).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(CONTRACT_ID.asc(), ROLE.asc(), CREATED_AT.asc())
            .fetch(this::toDomain));
  }

  public ContractParty getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contract party not found"));
  }

  public ContractParty getPrimaryContactByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return findPrimaryContactByContractIdAndTeamId(contractId, teamId)
        .orElseThrow(() -> new NotFoundException("Contract party not found"));
  }

  public Optional<ContractParty> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.select()
        .from(CONTRACT_PARTIES)
        .where(IDENTIFIER.eq(identifier.value()).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetchOptional(this::toDomain);
  }

  public Optional<ContractParty> findPrimaryContactByContractIdAndTeamId(
      UUID contractId, UUID teamId) {
    return dsl.select()
        .from(CONTRACT_PARTIES)
        .where(
            CONTRACT_ID
                .eq(contractId)
                .and(TEAM_ID.eq(teamId))
                .and(ROLE.eq(ContractPartyRole.PRIMARY_TENANT.name()))
                .and(DELETED_AT.isNull()))
        .fetchOptional(this::toDomain);
  }

  public Optional<ContractParty> findByContactIdAndContractIdAndTeamId(
      UUID contactId, UUID contractId, UUID teamId) {
    return dsl.select()
        .from(CONTRACT_PARTIES)
        .where(
            CONTACT_ID
                .eq(contactId)
                .and(CONTRACT_ID.eq(contractId))
                .and(TEAM_ID.eq(teamId))
                .and(DELETED_AT.isNull()))
        .fetchOptional(this::toDomain);
  }

  public boolean existsByContractIdAndContactIdAndTeamId(
      UUID contractId, UUID contactId, UUID teamId) {
    return dsl.fetchExists(
        dsl.selectOne()
            .from(CONTRACT_PARTIES)
            .where(
                CONTRACT_ID
                    .eq(contractId)
                    .and(CONTACT_ID.eq(contactId))
                    .and(TEAM_ID.eq(teamId))
                    .and(DELETED_AT.isNull())));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(CONTRACT_PARTIES)
        .set(DELETED_AT, now)
        .where(ID.eq(id).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  public void softDeleteByContractIdAndTeamId(UUID contractId, UUID teamId) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(CONTRACT_PARTIES)
        .set(DELETED_AT, now)
        .where(CONTRACT_ID.eq(contractId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  private ContractParty toDomain(Record record) {
    ContractParty party = new ContractParty();
    party.setId(record.get(ID));
    party.setIdentifier(java.util.Optional.of(com.buurman.domain.Sid.of(record.get(IDENTIFIER))));
    party.setTeamId(record.get(TEAM_ID));
    party.setContractId(record.get(CONTRACT_ID));
    party.setContactId(Optional.ofNullable(record.get(CONTACT_ID)));
    party.setRole(ContractPartyRole.valueOf(record.get(ROLE)));

    Timestamp createdAtVal = record.get(CREATED_AT);
    if (createdAtVal != null) {
      party.setCreatedAt(createdAtVal.toInstant());
    }

    Timestamp updatedAtVal = record.get(UPDATED_AT);
    if (updatedAtVal != null) {
      party.setUpdatedAt(updatedAtVal.toInstant());
    }

    party.setCreatedBy(record.get(CREATED_BY));
    party.setUpdatedBy(record.get(UPDATED_BY));

    Timestamp deletedAtVal = record.get(DELETED_AT);
    party.setDeletedAt(Optional.ofNullable(deletedAtVal).map(Timestamp::toInstant));

    return party;
  }
}
