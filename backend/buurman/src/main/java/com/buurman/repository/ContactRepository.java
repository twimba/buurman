package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.lower;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactType;
import com.buurman.domain.DataRetentionStatus;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContactRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactRepository {

  private final DSLContext dsl;
  private final ContactRecordMapper mapper;
  private final Clock clock;

  public Optional<Contact> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTACTS)
        .where(
            CONTACTS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTACTS.TEAM_ID.eq(teamId))
                .and(CONTACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Optional<Contact> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(CONTACTS)
        .where(CONTACTS.ID.eq(id).and(CONTACTS.TEAM_ID.eq(teamId)).and(CONTACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Contact getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contact not found"));
  }

  public Contact getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Contact not found"));
  }

  public List<Contact> findAllByTeamId(UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(CONTACTS)
            .where(CONTACTS.TEAM_ID.eq(teamId).and(CONTACTS.DELETED_AT.isNull()))
            .orderBy(CONTACTS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<Contact> searchByTeamId(UUID teamId, String searchTerm) {
    String searchPattern = "%" + searchTerm.toLowerCase(Locale.ROOT) + "%";
    return List.copyOf(
        dsl.selectFrom(CONTACTS)
            .where(
                CONTACTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(CONTACTS.DELETED_AT.isNull())
                    .and(
                        lower(CONTACTS.DISPLAY_NAME)
                            .like(searchPattern)
                            .or(lower(CONTACTS.FIRST_NAME).like(searchPattern))
                            .or(lower(CONTACTS.LAST_NAME).like(searchPattern))
                            .or(lower(CONTACTS.COMPANY_NAME).like(searchPattern))
                            .or(lower(CONTACTS.EMAIL).like(searchPattern))
                            .or(CONTACTS.PHONE.like(searchPattern))))
            .orderBy(CONTACTS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<Contact> findByEmailAndTeamId(String email, UUID teamId) {
    return dsl.selectFrom(CONTACTS)
        .where(
            CONTACTS
                .EMAIL
                .eq(email)
                .and(CONTACTS.TEAM_ID.eq(teamId))
                .and(CONTACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Contact save(Contact contact) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (contact.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          contact.getCreatedAt() != null ? LocalDateTime.ofInstant(contact.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          contact.getUpdatedAt() != null ? LocalDateTime.ofInstant(contact.getUpdatedAt(), UTC) : now;

      dsl.insertInto(CONTACTS)
          .set(CONTACTS.ID, newId)
          .set(CONTACTS.IDENTIFIER, contact.getIdentifier().orElseThrow())
          .set(CONTACTS.TEAM_ID, contact.getTeamId())
          .set(CONTACTS.CONTACT_TYPE, contact.getContactType().name())
          .set(CONTACTS.DISPLAY_NAME, contact.getDisplayName())
          .set(CONTACTS.FIRST_NAME, contact.getFirstName().orElse(null))
          .set(CONTACTS.LAST_NAME, contact.getLastName().orElse(null))
          .set(CONTACTS.COMPANY_NAME, contact.getCompanyName().orElse(null))
          .set(CONTACTS.TRADE_NAME, contact.getTradeName().orElse(null))
          .set(CONTACTS.INDUSTRY, contact.getIndustry().orElse(null))
          .set(CONTACTS.EMAIL, contact.getEmail().orElse(null))
          .set(CONTACTS.INVOICE_EMAIL, contact.getInvoiceEmail().orElse(null))
          .set(CONTACTS.PHONE, contact.getPhone().orElse(null))
          .set(CONTACTS.WEBSITE, contact.getWebsite().orElse(null))
          .set(CONTACTS.TAX_NUMBER, contact.getTaxNumber().orElse(null))
          .set(CONTACTS.ID_NUMBER, contact.getIdNumber().orElse(null))
          .set(CONTACTS.DATE_OF_BIRTH, contact.getDateOfBirth().orElse(null))
          .set(CONTACTS.ID_EXPIRY_DATE, contact.getIdExpiryDate().orElse(null))
          .set(CONTACTS.NOTES, contact.getNotes().orElse(null))
          .set(CONTACTS.DATA_RETENTION_STATUS, contact.getDataRetentionStatus().name())
          .set(CONTACTS.CREATED_AT, createdAt)
          .set(CONTACTS.UPDATED_AT, updatedAt)
          .set(CONTACTS.CREATED_BY, contact.getCreatedBy())
          .set(CONTACTS.UPDATED_BY, contact.getUpdatedBy())
          .execute();

      contact.setId(newId);
      contact.setCreatedAt(createdAt.toInstant(UTC));
      contact.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // UPDATE
      LocalDateTime updatedAt =
          contact.getUpdatedAt() != null ? LocalDateTime.ofInstant(contact.getUpdatedAt(), UTC) : now;

      dsl.update(CONTACTS)
          .set(CONTACTS.CONTACT_TYPE, contact.getContactType().name())
          .set(CONTACTS.DISPLAY_NAME, contact.getDisplayName())
          .set(CONTACTS.FIRST_NAME, contact.getFirstName().orElse(null))
          .set(CONTACTS.LAST_NAME, contact.getLastName().orElse(null))
          .set(CONTACTS.COMPANY_NAME, contact.getCompanyName().orElse(null))
          .set(CONTACTS.TRADE_NAME, contact.getTradeName().orElse(null))
          .set(CONTACTS.INDUSTRY, contact.getIndustry().orElse(null))
          .set(CONTACTS.EMAIL, contact.getEmail().orElse(null))
          .set(CONTACTS.INVOICE_EMAIL, contact.getInvoiceEmail().orElse(null))
          .set(CONTACTS.PHONE, contact.getPhone().orElse(null))
          .set(CONTACTS.WEBSITE, contact.getWebsite().orElse(null))
          .set(CONTACTS.TAX_NUMBER, contact.getTaxNumber().orElse(null))
          .set(CONTACTS.ID_NUMBER, contact.getIdNumber().orElse(null))
          .set(CONTACTS.DATE_OF_BIRTH, contact.getDateOfBirth().orElse(null))
          .set(CONTACTS.ID_EXPIRY_DATE, contact.getIdExpiryDate().orElse(null))
          .set(CONTACTS.NOTES, contact.getNotes().orElse(null))
          .set(CONTACTS.DATA_RETENTION_STATUS, contact.getDataRetentionStatus().name())
          .set(CONTACTS.UPDATED_AT, updatedAt)
          .set(CONTACTS.UPDATED_BY, contact.getUpdatedBy())
          .where(CONTACTS.ID.eq(contact.getId()).and(CONTACTS.TEAM_ID.eq(contact.getTeamId())))
          .execute();

      contact.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return contact;
  }

  public PaginatedResult<Contact> findAllByTeamIdPaginated(
      UUID teamId, @Nullable String search, PageRequest pageRequest) {
    Condition condition = CONTACTS.TEAM_ID.eq(teamId).and(CONTACTS.DELETED_AT.isNull());
    if (search != null && !search.isBlank()) {
      String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
      condition =
          condition.and(
              lower(CONTACTS.DISPLAY_NAME)
                  .like(pattern)
                  .or(lower(CONTACTS.FIRST_NAME).like(pattern))
                  .or(lower(CONTACTS.LAST_NAME).like(pattern))
                  .or(lower(CONTACTS.COMPANY_NAME).like(pattern))
                  .or(lower(CONTACTS.EMAIL).like(pattern))
                  .or(CONTACTS.PHONE.like(pattern)));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", CONTACTS.CREATED_AT,
            "displayName", CONTACTS.DISPLAY_NAME,
            "firstName", CONTACTS.FIRST_NAME,
            "lastName", CONTACTS.LAST_NAME,
            "companyName", CONTACTS.COMPANY_NAME,
            "email", CONTACTS.EMAIL);
    return PaginationHelper.paginate(
        dsl, CONTACTS, condition, sortableFields, CONTACTS.CREATED_AT, pageRequest, mapper::toDomain);
  }

  public List<Contact> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(CONTACTS)
            .where(
                CONTACTS.ID.in(ids).and(CONTACTS.TEAM_ID.eq(teamId)).and(CONTACTS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACTS)
        .set(CONTACTS.DELETED_AT, now)
        .where(CONTACTS.ID.eq(id).and(CONTACTS.TEAM_ID.eq(teamId)))
        .execute();
  }

  public void anonymize(UUID contactId, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACTS)
        .set(CONTACTS.DISPLAY_NAME, "[erased]")
        .set(CONTACTS.FIRST_NAME, (String) null)
        .set(CONTACTS.LAST_NAME, (String) null)
        .set(CONTACTS.COMPANY_NAME, (String) null)
        .set(CONTACTS.TRADE_NAME, (String) null)
        .set(CONTACTS.INDUSTRY, (String) null)
        .set(CONTACTS.EMAIL, (String) null)
        .set(CONTACTS.INVOICE_EMAIL, (String) null)
        .set(CONTACTS.PHONE, (String) null)
        .set(CONTACTS.WEBSITE, (String) null)
        .set(CONTACTS.TAX_NUMBER, (String) null)
        .set(CONTACTS.ID_NUMBER, (String) null)
        .set(CONTACTS.DATE_OF_BIRTH, (java.time.LocalDate) null)
        .set(CONTACTS.ID_EXPIRY_DATE, (java.time.LocalDate) null)
        .set(CONTACTS.NOTES, (String) null)
        .set(CONTACTS.DATA_RETENTION_STATUS, DataRetentionStatus.ANONYMIZED.name())
        .set(CONTACTS.UPDATED_AT, now)
        .where(CONTACTS.ID.eq(contactId).and(CONTACTS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
