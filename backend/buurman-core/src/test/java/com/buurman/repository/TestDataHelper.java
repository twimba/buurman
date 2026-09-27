package com.buurman.repository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactType;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Team;
import com.buurman.mapper.OptionalMappingConfig;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

/** Factory methods for inserting test data directly via JOOQ DSL. */
final class TestDataHelper {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  private static final OptionalMappingConfig OPTIONAL_MAPPING_CONFIG = new OptionalMappingConfig();

  private TestDataHelper() {}

  /**
   * Injects {@link OptionalMappingConfig} into a MapStruct-generated mapper that uses field
   * injection via {@code @Autowired}. Required because we instantiate mappers outside Spring.
   */
  @SuppressWarnings("NullAway")
  static <T> T wireMapper(T mapper) {
    try {
      Field field = mapper.getClass().getDeclaredField("optionalMappingConfig");
      field.setAccessible(true);
      field.set(mapper, OPTIONAL_MAPPING_CONFIG);
    } catch (NoSuchFieldException e) {
      // Mapper doesn't use OptionalMappingConfig — nothing to wire
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to wire OptionalMappingConfig into mapper", e);
    }
    return mapper;
  }

  static void insertUser(DSLContext dsl, UUID userId) {
    dsl.insertInto(DSL.table("users"))
        .set(DSL.field("id", UUID.class), userId)
        .set(DSL.field("identifier", String.class), SidGenerator.newUserId().value())
        .set(DSL.field("keycloak_id", String.class), "kc-" + userId)
        .set(
            DSL.field("email", String.class),
            "user-" + userId.toString().substring(0, 8) + "@test.io")
        .set(DSL.field("first_name", String.class), "Test")
        .set(DSL.field("last_name", String.class), "User")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .execute();
  }

  static void insertTeam(DSLContext dsl, UUID teamId, String name, UUID createdBy) {
    dsl.insertInto(DSL.table("teams"))
        .set(DSL.field("id", UUID.class), teamId)
        .set(DSL.field("identifier", String.class), SidGenerator.newTeamId().value())
        .set(DSL.field("name", String.class), name)
        .set(DSL.field("demo", Boolean.class), false)
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .execute();
  }

  static Team buildTeam(UUID teamId, UUID createdBy) {
    Team team = new Team();
    team.setIdentifier(Optional.of(SidGenerator.newTeamId()));
    team.setName("New Team");
    team.setDemo(false);
    team.setCreatedBy(createdBy);
    team.setUpdatedBy(createdBy);
    return team;
  }

  static Property buildProperty(UUID teamId, UUID createdBy) {
    Property p = new Property();
    p.setIdentifier(Optional.of(SidGenerator.newPropertyId()));
    p.setTeamId(teamId);
    p.setStreet("Main Street 1");
    p.setCity("Amsterdam");
    p.setPostalCode("1012AB");
    p.setCountryCode("NL");
    p.setPropertyCategory(Property.PropertyCategory.RESIDENTIAL);
    p.setPropertyType(Property.PropertyType.APARTMENT);
    p.setCreatedBy(createdBy);
    p.setUpdatedBy(createdBy);
    return p;
  }

  static UUID insertProperty(DSLContext dsl, UUID teamId, UUID createdBy) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("properties"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newPropertyId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("street", String.class), "Main Street 1")
        .set(DSL.field("city", String.class), "Amsterdam")
        .set(DSL.field("postal_code", String.class), "1012AB")
        .set(DSL.field("country_code", String.class), "NL")
        .set(DSL.field("property_category", String.class), "RESIDENTIAL")
        .set(DSL.field("property_type", String.class), "APARTMENT")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .set(DSL.field("updated_by", UUID.class), createdBy)
        .execute();
    return id;
  }

  static void insertUnit(
      DSLContext dsl, UUID unitId, UUID propertyId, UUID teamId, String unitNumber, String status) {
    dsl.insertInto(DSL.table("units"))
        .set(DSL.field("id", UUID.class), unitId)
        .set(DSL.field("identifier", String.class), SidGenerator.newUnitId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("unit_number", String.class), unitNumber)
        .set(DSL.field("unit_type", String.class), "APARTMENT")
        .set(DSL.field("status", String.class), status)
        .set(DSL.field("is_implicit", Boolean.class), false)
        .set(DSL.field("area_unit", String.class), "sqm")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .execute();
  }

  static UUID insertExpense(
      DSLContext dsl, UUID propertyId, UUID teamId, UUID createdBy, BigDecimal amountMinorUnits) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("expenses"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newExpenseId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("category", String.class), "MAINTENANCE")
        .set(DSL.field("amount", Long.class), amountMinorUnits.longValueExact())
        .set(DSL.field("currency", String.class), "EUR")
        .set(DSL.field("expense_date", LocalDate.class), LocalDate.of(2026, 3, 1))
        .set(DSL.field("description", String.class), "Roof repair")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .set(DSL.field("updated_by", UUID.class), createdBy)
        .execute();
    return id;
  }

  static Contact buildContact(UUID teamId, UUID createdBy) {
    Contact c = new Contact();
    c.setIdentifier(Optional.of(SidGenerator.newContactId()));
    c.setTeamId(teamId);
    c.setContactType(ContactType.INDIVIDUAL);
    c.setDisplayName("Jan de Vries");
    c.setFirstName(Optional.of("Jan"));
    c.setLastName(Optional.of("de Vries"));
    c.setEmail(Optional.of("jan@test.io"));
    c.setPhone(Optional.of("+31612345678"));
    c.setTaxNumber(Optional.empty());
    c.setIdNumber(Optional.empty());
    c.setNotes(Optional.empty());
    c.setCreatedBy(createdBy);
    c.setUpdatedBy(createdBy);
    c.setDeletedAt(Optional.empty());
    return c;
  }

  static UUID insertContact(DSLContext dsl, UUID teamId, UUID createdBy) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("contacts"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newContactId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("contact_type", String.class), "INDIVIDUAL")
        .set(DSL.field("display_name", String.class), "Jan de Vries")
        .set(DSL.field("first_name", String.class), "Jan")
        .set(DSL.field("last_name", String.class), "de Vries")
        .set(DSL.field("email", String.class), "jan@test.io")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .set(DSL.field("updated_by", UUID.class), createdBy)
        .execute();
    return id;
  }

  static UUID insertContract(DSLContext dsl, UUID teamId, UUID propertyId, UUID createdBy) {
    // contracts.unit_id is NOT NULL as of V070; create the implicit unit for this property so
    // the contract has something valid to reference.
    UUID unitId = UUID.randomUUID();
    insertUnit(dsl, unitId, propertyId, teamId, "1", "OCCUPIED");

    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("contracts"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newContractId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
        .set(DSL.field("unit_id", UUID.class), unitId)
        .set(DSL.field("contract_type", String.class), "FIXED_TERM")
        .set(DSL.field("start_date", LocalDate.class), LocalDate.of(2026, 1, 1))
        .set(DSL.field("rent_amount", Long.class), 100000L)
        .set(DSL.field("rent_amount_currency", String.class), "EUR")
        .set(DSL.field("payment_frequency", String.class), "MONTHLY")
        .set(DSL.field("status", String.class), "ACTIVE")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .set(DSL.field("updated_by", UUID.class), createdBy)
        .execute();
    return id;
  }

  static ContactAddress buildContactAddress(UUID contactId, UUID teamId, UUID createdBy) {
    return ContactAddress.builder()
        .contactId(contactId)
        .teamId(teamId)
        .street("Herengracht 100")
        .city("Amsterdam")
        .postalCode("1015BN")
        .countryCode("NL")
        .addressType(ContactAddress.AddressType.CURRENT)
        .status(ContactAddress.AddressStatus.ACTIVE)
        .latitude(Optional.of(52.3676))
        .longitude(Optional.of(4.9041))
        .geocodeAccuracy(Optional.of("ROOFTOP"))
        .createdBy(createdBy)
        .updatedBy(createdBy)
        .deletedAt(Optional.empty())
        .build();
  }

  static Payment buildPayment(
      UUID teamId, UUID contractId, UUID createdBy, BigDecimal amount, LocalDate dueDate) {
    Payment p = new Payment();
    p.setIdentifier(Optional.of(SidGenerator.newPaymentId()));
    p.setTeamId(teamId);
    p.setContractId(contractId);
    p.setAmount(MoneyAmount.of(amount, "EUR"));
    p.setDueDate(dueDate);
    p.setStatus(Payment.PaymentStatus.PENDING);
    p.setPaymentDate(Optional.empty());
    p.setNotes(Optional.empty());
    p.setAutoGenerated(false);
    p.setCreatedBy(createdBy);
    p.setUpdatedBy(createdBy);
    p.setDeletedAt(Optional.empty());
    return p;
  }
}
