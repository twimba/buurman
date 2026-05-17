package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTACT_NOTES;
import static com.buurman.jooq.generated.Tables.CONTACT_TAGS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.USERS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.castNull;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.concat;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.falseCondition;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.selectCount;

import java.time.Clock;
import java.time.Instant;
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
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.impl.SQLDataType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.domain.DataRetentionStatus;
import com.buurman.domain.InteractionType;
import com.buurman.domain.Sid;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.ContactActivityItem;
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
        .where(
            CONTACTS.ID.eq(id).and(CONTACTS.TEAM_ID.eq(teamId)).and(CONTACTS.DELETED_AT.isNull()))
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

  // TODO: Consider pg_trgm GIN indexes for fuzzy/partial-match search performance
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

  public Map<String, Contact> findByEmailsAndTeamId(Collection<String> emails, UUID teamId) {
    if (emails.isEmpty()) {
      return Map.of();
    }
    return dsl
        .selectFrom(CONTACTS)
        .where(
            CONTACTS
                .EMAIL
                .in(emails)
                .and(CONTACTS.TEAM_ID.eq(teamId))
                .and(CONTACTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .filter(c -> c.getEmail().isPresent())
        .collect(
            java.util.stream.Collectors.toMap(
                c -> c.getEmail().orElseThrow(), c -> c, (a, b) -> a));
  }

  public Contact save(Contact contact) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (contact.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          contact.getCreatedAt() != null
              ? LocalDateTime.ofInstant(contact.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          contact.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(contact.getUpdatedAt(), UTC)
              : now;

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
          contact.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(contact.getUpdatedAt(), UTC)
              : now;

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
        dsl,
        CONTACTS,
        condition,
        sortableFields,
        CONTACTS.CREATED_AT,
        pageRequest,
        mapper::toDomain);
  }

  /**
   * Returns contacts with their active contract count, computed via a correlated subquery that
   * joins contract_parties and contracts. Each result is a {@link ContactWithCount} containing the
   * domain Contact and an int count.
   */
  public PaginatedResult<ContactWithCount> findAllByTeamIdPaginatedWithCounts(
      UUID teamId,
      @Nullable String search,
      @Nullable ContactType contactType,
      @Nullable List<ContactTag> tags,
      PageRequest pageRequest) {

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
    if (contactType != null) {
      condition = condition.and(CONTACTS.CONTACT_TYPE.eq(contactType.name()));
    }
    if (tags != null && !tags.isEmpty()) {
      // Contacts must have ALL specified tags (AND semantics)
      for (ContactTag tag : tags) {
        condition =
            condition.and(
                CONTACTS.ID.in(
                    dsl.select(CONTACT_TAGS.CONTACT_ID)
                        .from(CONTACT_TAGS)
                        .where(
                            CONTACT_TAGS.TAG.eq(tag.name()).and(CONTACT_TAGS.TEAM_ID.eq(teamId)))));
      }
    }

    Field<Integer> activeContractCountField =
        selectCount()
            .from(CONTRACT_PARTIES)
            .join(CONTRACTS)
            .on(
                CONTRACT_PARTIES
                    .CONTRACT_ID
                    .eq(CONTRACTS.ID)
                    .and(CONTRACTS.STATUS.eq("ACTIVE"))
                    .and(CONTRACTS.DELETED_AT.isNull()))
            .where(
                CONTRACT_PARTIES
                    .CONTACT_ID
                    .eq(CONTACTS.ID)
                    .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                    .and(CONTRACT_PARTIES.DELETED_AT.isNull()))
            .asField("active_contract_count");

    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", CONTACTS.CREATED_AT,
            "displayName", CONTACTS.DISPLAY_NAME,
            "firstName", CONTACTS.FIRST_NAME,
            "lastName", CONTACTS.LAST_NAME,
            "companyName", CONTACTS.COMPANY_NAME,
            "email", CONTACTS.EMAIL,
            "activeContractCount", activeContractCountField);

    Field<?> sortField =
        pageRequest
            .sort()
            .filter(sortableFields::containsKey)
            .map(sortableFields::get)
            .orElse(CONTACTS.CREATED_AT);

    SortField<?> orderBy =
        pageRequest.direction().orElse(SortDirection.DESC) == SortDirection.ASC
            ? sortField.asc()
            : sortField.desc();

    long totalElements = dsl.fetchCount(CONTACTS, condition);

    List<ContactWithCount> items =
        dsl.select(CONTACTS.asterisk(), activeContractCountField)
            .from(CONTACTS)
            .where(condition)
            .orderBy(orderBy)
            .limit(pageRequest.size())
            .offset(pageRequest.offset())
            .fetch(
                record -> {
                  Contact contact = mapper.toDomain(record.into(CONTACTS));
                  int activeCount = record.get("active_contract_count", Integer.class);
                  return new ContactWithCount(contact, activeCount);
                });

    return new PaginatedResult<>(items, totalElements);
  }

  public record ContactWithCount(Contact contact, int activeContractCount) {}

  public List<Contact> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(CONTACTS)
            .where(
                CONTACTS
                    .ID
                    .in(ids)
                    .and(CONTACTS.TEAM_ID.eq(teamId))
                    .and(CONTACTS.DELETED_AT.isNull()))
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

  public List<Contact> findDuplicatesByEmail(String email, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(CONTACTS)
            .where(
                lower(CONTACTS.EMAIL)
                    .eq(email.toLowerCase(Locale.ROOT))
                    .and(CONTACTS.TEAM_ID.eq(teamId))
                    .and(CONTACTS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public List<Contact> findDuplicatesByName(
      @Nullable String firstName,
      @Nullable String lastName,
      @Nullable String companyName,
      UUID teamId) {
    Condition nameCondition = falseCondition();
    boolean anyClause = false;
    if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
      nameCondition =
          nameCondition.or(
              lower(CONTACTS.FIRST_NAME)
                  .eq(firstName.toLowerCase(Locale.ROOT))
                  .and(lower(CONTACTS.LAST_NAME).eq(lastName.toLowerCase(Locale.ROOT))));
      anyClause = true;
    }
    if (companyName != null && !companyName.isBlank()) {
      nameCondition =
          nameCondition.or(lower(CONTACTS.COMPANY_NAME).eq(companyName.toLowerCase(Locale.ROOT)));
      anyClause = true;
    }
    if (!anyClause) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(CONTACTS)
            .where(nameCondition.and(CONTACTS.TEAM_ID.eq(teamId)).and(CONTACTS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  /**
   * Returns contact activity items from 6 sources via UNION ALL, paginated at the database level.
   * Sources: contact_notes, audit_log, contract_parties, payments, documents, notifications.
   *
   * <p>TODO: The UNION ALL count + data queries execute the same sub-selects twice. Consider
   * wrapping in a CTE or caching the count to avoid double execution.
   */
  public PaginatedResult<ContactActivityItem> findActivityByContactIdPaginated(
      UUID contactId, UUID teamId, PageRequest pageRequest) {

    // Common null expressions
    Field<String> nullVarchar = castNull(SQLDataType.VARCHAR);
    Field<Boolean> nullBoolean = castNull(SQLDataType.BOOLEAN);

    // Define output fields for the UNION
    Field<String> fEventType = org.jooq.impl.DSL.field("event_type", String.class);
    Field<LocalDateTime> fOccurredAt = org.jooq.impl.DSL.field("occurred_at", LocalDateTime.class);
    Field<String> fDescription = org.jooq.impl.DSL.field("description", String.class);
    Field<String> fRelatedIdent = org.jooq.impl.DSL.field("related_ident", String.class);
    Field<String> fRelatedType = org.jooq.impl.DSL.field("related_type", String.class);
    Field<String> fNoteIdent = org.jooq.impl.DSL.field("note_ident", String.class);
    Field<String> fInteractionType = org.jooq.impl.DSL.field("interaction_type", String.class);
    Field<String> fNoteBody = org.jooq.impl.DSL.field("note_body", String.class);
    Field<String> fNoteSubject = org.jooq.impl.DSL.field("note_subject", String.class);
    Field<Boolean> fPinned = org.jooq.impl.DSL.field("pinned", Boolean.class);
    Field<String> fCreatedByName = org.jooq.impl.DSL.field("created_by_name", String.class);

    // Build a raw SQL UNION ALL as a table expression via individual SELECTs
    var u1 = USERS.as("u1");
    var u2 = USERS.as("u2");
    var u3 = USERS.as("u3");
    var u4 = USERS.as("u4");
    var u5 = USERS.as("u5");
    var u6 = USERS.as("u6");

    // 1. Notes
    var noteQuery =
        dsl.select(
                inline("NOTE").as(fEventType),
                CONTACT_NOTES.OCCURRED_AT.as(fOccurredAt),
                coalesce(
                        CONTACT_NOTES.SUBJECT,
                        concat(CONTACT_NOTES.INTERACTION_TYPE, inline(" note")))
                    .as(fDescription),
                nullVarchar.as(fRelatedIdent),
                nullVarchar.as(fRelatedType),
                CONTACT_NOTES.IDENTIFIER.cast(SQLDataType.VARCHAR(29)).as(fNoteIdent),
                CONTACT_NOTES.INTERACTION_TYPE.cast(SQLDataType.VARCHAR).as(fInteractionType),
                CONTACT_NOTES.BODY.as(fNoteBody),
                CONTACT_NOTES.SUBJECT.as(fNoteSubject),
                CONTACT_NOTES.PINNED.as(fPinned),
                concat(u1.FIRST_NAME, inline(" "), u1.LAST_NAME).as(fCreatedByName))
            .from(CONTACT_NOTES)
            .leftJoin(u1)
            .on(CONTACT_NOTES.CREATED_BY.eq(u1.ID))
            .where(
                CONTACT_NOTES
                    .CONTACT_ID
                    .eq(contactId)
                    .and(CONTACT_NOTES.TEAM_ID.eq(teamId))
                    .and(CONTACT_NOTES.DELETED_AT.isNull()));

    // 2. Audit log
    var auditQuery =
        dsl.select(
                inline("AUDIT").as(fEventType),
                AUDIT_LOG.TIMESTAMP.as(fOccurredAt),
                AUDIT_LOG.ACTION.as(fDescription),
                nullVarchar.as(fRelatedIdent),
                AUDIT_LOG.ENTITY_TYPE.as(fRelatedType),
                nullVarchar.as(fNoteIdent),
                nullVarchar.as(fInteractionType),
                nullVarchar.as(fNoteBody),
                nullVarchar.as(fNoteSubject),
                nullBoolean.as(fPinned),
                concat(u2.FIRST_NAME, inline(" "), u2.LAST_NAME).as(fCreatedByName))
            .from(AUDIT_LOG)
            .leftJoin(u2)
            .on(AUDIT_LOG.USER_ID.eq(u2.ID))
            .where(
                AUDIT_LOG
                    .ENTITY_TYPE
                    .eq("CONTACT")
                    .and(AUDIT_LOG.ENTITY_ID.eq(contactId))
                    .and(AUDIT_LOG.TEAM_ID.eq(teamId)));

    // 3. Contract parties (contact added to contract)
    var contractQuery =
        dsl.select(
                inline("CONTRACT").as(fEventType),
                CONTRACT_PARTIES.CREATED_AT.as(fOccurredAt),
                concat(inline("Added as "), CONTRACT_PARTIES.ROLE).as(fDescription),
                CONTRACTS.IDENTIFIER.cast(SQLDataType.VARCHAR(29)).as(fRelatedIdent),
                inline("CONTRACT").as(fRelatedType),
                nullVarchar.as(fNoteIdent),
                nullVarchar.as(fInteractionType),
                nullVarchar.as(fNoteBody),
                nullVarchar.as(fNoteSubject),
                nullBoolean.as(fPinned),
                concat(u3.FIRST_NAME, inline(" "), u3.LAST_NAME).as(fCreatedByName))
            .from(CONTRACT_PARTIES)
            .join(CONTRACTS)
            .on(CONTRACT_PARTIES.CONTRACT_ID.eq(CONTRACTS.ID))
            .leftJoin(u3)
            .on(CONTRACT_PARTIES.CREATED_BY.eq(u3.ID))
            .where(
                CONTRACT_PARTIES
                    .CONTACT_ID
                    .eq(contactId)
                    .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                    .and(CONTRACT_PARTIES.DELETED_AT.isNull()));

    // 4. Payments (via contract_parties)
    var paymentQuery =
        dsl.select(
                inline("PAYMENT").as(fEventType),
                PAYMENTS.CREATED_AT.as(fOccurredAt),
                concat(inline("Payment "), PAYMENTS.STATUS).as(fDescription),
                PAYMENTS.IDENTIFIER.cast(SQLDataType.VARCHAR(29)).as(fRelatedIdent),
                inline("PAYMENT").as(fRelatedType),
                nullVarchar.as(fNoteIdent),
                nullVarchar.as(fInteractionType),
                nullVarchar.as(fNoteBody),
                nullVarchar.as(fNoteSubject),
                nullBoolean.as(fPinned),
                concat(u4.FIRST_NAME, inline(" "), u4.LAST_NAME).as(fCreatedByName))
            .from(PAYMENTS)
            .join(CONTRACT_PARTIES)
            .on(
                PAYMENTS
                    .CONTRACT_ID
                    .eq(CONTRACT_PARTIES.CONTRACT_ID)
                    .and(CONTRACT_PARTIES.CONTACT_ID.eq(contactId))
                    .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                    .and(CONTRACT_PARTIES.DELETED_AT.isNull()))
            .leftJoin(u4)
            .on(PAYMENTS.CREATED_BY.eq(u4.ID))
            .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()));

    // 5. Documents
    var documentQuery =
        dsl.select(
                inline("DOCUMENT").as(fEventType),
                DOCUMENTS.UPLOADED_AT.as(fOccurredAt),
                concat(inline("Document: "), coalesce(DOCUMENTS.TITLE, DOCUMENTS.FILE_NAME))
                    .as(fDescription),
                DOCUMENTS.IDENTIFIER.cast(SQLDataType.VARCHAR(29)).as(fRelatedIdent),
                inline("DOCUMENT").as(fRelatedType),
                nullVarchar.as(fNoteIdent),
                nullVarchar.as(fInteractionType),
                nullVarchar.as(fNoteBody),
                nullVarchar.as(fNoteSubject),
                nullBoolean.as(fPinned),
                concat(u5.FIRST_NAME, inline(" "), u5.LAST_NAME).as(fCreatedByName))
            .from(DOCUMENTS)
            .leftJoin(u5)
            .on(DOCUMENTS.UPLOADED_BY.eq(u5.ID))
            .where(
                DOCUMENTS
                    .ENTITY_TYPE
                    .eq("CONTACT")
                    .and(DOCUMENTS.ENTITY_ID.eq(contactId))
                    .and(DOCUMENTS.TEAM_ID.eq(teamId))
                    .and(DOCUMENTS.DELETED_AT.isNull()));

    // 6. Notifications
    var notificationQuery =
        dsl.select(
                inline("NOTIFICATION").as(fEventType),
                NOTIFICATIONS.CREATED_AT.as(fOccurredAt),
                concat(inline("Notification: "), NOTIFICATIONS.NOTIFICATION_TYPE).as(fDescription),
                NOTIFICATIONS.IDENTIFIER.cast(SQLDataType.VARCHAR(29)).as(fRelatedIdent),
                inline("NOTIFICATION").as(fRelatedType),
                nullVarchar.as(fNoteIdent),
                nullVarchar.as(fInteractionType),
                nullVarchar.as(fNoteBody),
                nullVarchar.as(fNoteSubject),
                nullBoolean.as(fPinned),
                concat(u6.FIRST_NAME, inline(" "), u6.LAST_NAME).as(fCreatedByName))
            .from(NOTIFICATIONS)
            .leftJoin(u6)
            .on(NOTIFICATIONS.CREATED_BY.eq(u6.ID))
            .where(
                NOTIFICATIONS
                    .RECIPIENT_CONTACT_ID
                    .eq(contactId)
                    .and(NOTIFICATIONS.TEAM_ID.eq(teamId)));

    // Combine all 6 queries with UNION ALL
    var unionQuery =
        noteQuery
            .unionAll(auditQuery)
            .unionAll(contractQuery)
            .unionAll(paymentQuery)
            .unionAll(documentQuery)
            .unionAll(notificationQuery);

    // Count total across all sources
    long totalElements = dsl.fetchCount(unionQuery);

    // Fetch paginated results
    Field<Object> occurredAtField = org.jooq.impl.DSL.field("occurred_at");
    List<ContactActivityItem> items =
        dsl.select()
            .from(unionQuery.asTable("activity"))
            .orderBy(occurredAtField.desc())
            .limit(pageRequest.size())
            .offset(pageRequest.offset())
            .fetch(record -> mapToActivityItem(record));

    return new PaginatedResult<>(items, totalElements);
  }

  private ContactActivityItem mapToActivityItem(Record record) {
    String eventType = record.get("event_type", String.class);
    LocalDateTime occurredAt = record.get("occurred_at", LocalDateTime.class);
    String description = record.get("description", String.class);
    String relatedIdent = record.get("related_ident", String.class);
    String relatedType = record.get("related_type", String.class);
    String noteIdent = record.get("note_ident", String.class);
    String interactionTypeStr = record.get("interaction_type", String.class);
    String noteBody = record.get("note_body", String.class);
    String noteSubject = record.get("note_subject", String.class);
    Boolean pinned = record.get("pinned", Boolean.class);
    String createdByName = record.get("created_by_name", String.class);

    return new ContactActivityItem(
        eventType,
        occurredAt != null ? occurredAt.toInstant(UTC) : Instant.now(clock),
        description != null ? description : eventType,
        Optional.ofNullable(relatedIdent).map(Sid::of),
        Optional.ofNullable(relatedType),
        Optional.ofNullable(noteIdent).map(Sid::of),
        Optional.ofNullable(interactionTypeStr)
            .flatMap(
                s -> {
                  try {
                    return Optional.of(InteractionType.valueOf(s));
                  } catch (IllegalArgumentException e) {
                    return Optional.empty();
                  }
                }),
        Optional.ofNullable(noteBody),
        Optional.ofNullable(noteSubject),
        Optional.ofNullable(pinned),
        Optional.ofNullable(createdByName));
  }

  public Map<UUID, Integer> countActiveContractsByContactIds(
      Collection<UUID> contactIds, UUID teamId) {
    if (contactIds == null || contactIds.isEmpty()) {
      return Map.of();
    }
    return dsl.select(CONTRACT_PARTIES.CONTACT_ID, count())
        .from(CONTRACT_PARTIES)
        .join(CONTRACTS)
        .on(
            CONTRACT_PARTIES
                .CONTRACT_ID
                .eq(CONTRACTS.ID)
                .and(CONTRACTS.STATUS.eq("ACTIVE"))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .where(
            CONTRACT_PARTIES
                .CONTACT_ID
                .in(contactIds)
                .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                .and(CONTRACT_PARTIES.DELETED_AT.isNull()))
        .groupBy(CONTRACT_PARTIES.CONTACT_ID)
        .fetchMap(CONTRACT_PARTIES.CONTACT_ID, count());
  }

  public void anonymizeContact(UUID contactId, UUID teamId, UUID updatedBy, Instant now) {
    LocalDateTime updatedAt = LocalDateTime.ofInstant(now, UTC);
    dsl.update(CONTACTS)
        .set(CONTACTS.DISPLAY_NAME, "[ERASED]")
        .set(CONTACTS.FIRST_NAME, "[ERASED]")
        .set(CONTACTS.LAST_NAME, "[ERASED]")
        .set(CONTACTS.COMPANY_NAME, "[ERASED]")
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
        .set(CONTACTS.UPDATED_AT, updatedAt)
        .set(CONTACTS.UPDATED_BY, updatedBy)
        .where(CONTACTS.ID.eq(contactId).and(CONTACTS.TEAM_ID.eq(teamId)))
        .execute();
  }

  public int countByTeamId(UUID teamId) {
    return dsl.fetchCount(
        dsl.selectFrom(CONTACTS)
            .where(CONTACTS.TEAM_ID.eq(teamId).and(CONTACTS.DELETED_AT.isNull())));
  }
}
