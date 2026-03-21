package com.buurman.repository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Team;
import com.buurman.domain.Tenant;
import com.buurman.domain.TenantAddress;
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
    p.setStatus(Property.PropertyStatus.OCCUPIED);
    p.setAreaUnit(Optional.of("sqm"));
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
        .set(DSL.field("status", String.class), "OCCUPIED")
        .set(DSL.field("area_unit", String.class), "sqm")
        .set(DSL.field("created_at", LocalDateTime.class), NOW)
        .set(DSL.field("updated_at", LocalDateTime.class), NOW)
        .set(DSL.field("created_by", UUID.class), createdBy)
        .set(DSL.field("updated_by", UUID.class), createdBy)
        .execute();
    return id;
  }

  static Tenant buildTenant(UUID teamId, UUID createdBy) {
    Tenant t = new Tenant();
    t.setIdentifier(Optional.of(SidGenerator.newTenantId()));
    t.setTeamId(teamId);
    t.setFirstName("Jan");
    t.setLastName(Optional.of("de Vries"));
    t.setEmail(Optional.of("jan@test.io"));
    t.setPhone(Optional.of("+31612345678"));
    t.setTaxNumber(Optional.empty());
    t.setIdNumber(Optional.empty());
    t.setAdditionalInfo(Optional.empty());
    t.setCurrentPropertyId(Optional.empty());
    t.setCreatedBy(createdBy);
    t.setUpdatedBy(createdBy);
    t.setDeletedAt(Optional.empty());
    return t;
  }

  static UUID insertTenant(DSLContext dsl, UUID teamId, UUID createdBy) {
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("tenants"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newTenantId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
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
    UUID id = UUID.randomUUID();
    dsl.insertInto(DSL.table("contracts"))
        .set(DSL.field("id", UUID.class), id)
        .set(DSL.field("identifier", String.class), SidGenerator.newContractId().value())
        .set(DSL.field("team_id", UUID.class), teamId)
        .set(DSL.field("property_id", UUID.class), propertyId)
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

  static TenantAddress buildTenantAddress(UUID tenantId, UUID teamId, UUID createdBy) {
    return TenantAddress.builder()
        .tenantId(tenantId)
        .teamId(teamId)
        .street("Herengracht 100")
        .city("Amsterdam")
        .postalCode("1015BN")
        .countryCode("NL")
        .addressType(TenantAddress.AddressType.CURRENT)
        .status(TenantAddress.AddressStatus.ACTIVE)
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
