package com.buurman.service.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.ContactType;
import com.buurman.domain.UnitType;

import lombok.Getter;

/** Mutable context passed between demo data generators to share generated IDs. */
public class DemoDataContext {

  /**
   * One planned unit of a curated multi-unit demo building (BUUR-106). {@link DemoUnitGenerator}
   * creates exactly these units — with differing area/energy label/bedrooms so per-unit WWS and
   * AREA-basis allocation are visibly different — instead of the single implicit unit every other
   * demo property gets. {@code occupied} tells {@link DemoContractGenerator} which units should get
   * a letting and which stay VACANT, so the portfolio shows a real, partial occupancy rate.
   */
  public record UnitPlan(
      String unitNumber,
      UnitType unitType,
      java.math.BigDecimal areaSqm,
      @Nullable String energyLabel,
      int bedrooms,
      int bathrooms,
      boolean furnished,
      long monthlyRentEuros,
      boolean occupied) {}

  // Team key (e.g., "demo-team") -> team UUID
  @Getter private final Map<String, UUID> teamIds = new LinkedHashMap<>();

  // Team key -> default currency code (e.g., "EUR")
  private final Map<String, String> teamCurrencies = new LinkedHashMap<>();

  // UUID -> identifier (Sid) for S3 key generation
  private final Map<UUID, String> identifiers = new LinkedHashMap<>();

  // Email -> user UUID
  @Getter private final Map<String, UUID> userIds = new LinkedHashMap<>();

  // Email -> Keycloak user ID
  @Getter private final Map<String, String> keycloakIds = new LinkedHashMap<>();

  // Team UUID -> list of property UUIDs
  @Getter private final Map<UUID, List<UUID>> propertyIdsByTeam = new LinkedHashMap<>();

  // Team UUID -> list of contact UUIDs
  @Getter private final Map<UUID, List<UUID>> contactIdsByTeam = new LinkedHashMap<>();

  // Team UUID -> list of contract UUIDs
  @Getter private final Map<UUID, List<UUID>> contractIdsByTeam = new LinkedHashMap<>();

  // Team UUID -> list of payment instruction UUIDs
  @Getter private final Map<UUID, List<UUID>> paymentInstructionIdsByTeam = new LinkedHashMap<>();

  // Contract UUID -> list of payment UUIDs
  @Getter private final Map<UUID, List<UUID>> paymentIdsByContract = new LinkedHashMap<>();

  // Team UUID -> list of financing UUIDs (for financing payment generation)
  @Getter private final Map<UUID, List<UUID>> financingIdsByTeam = new LinkedHashMap<>();

  // Property UUID -> property category (RESIDENTIAL, COMMERCIAL, INDUSTRIAL, AGRICULTURAL,
  // MIXED_USE)
  private final Map<UUID, String> propertyCategoriesByProperty = new LinkedHashMap<>();

  // Contact UUID -> whether the contact is a business entity
  private final Map<UUID, Boolean> businessContactFlags = new LinkedHashMap<>();

  // Contact UUID -> ContactType
  private final Map<UUID, ContactType> contactTypes = new LinkedHashMap<>();

  // Property UUID -> acquisition date (when the property was purchased)
  private final Map<UUID, LocalDate> propertyAcquisitionDates = new LinkedHashMap<>();

  // Property UUID -> country code (e.g., "NL", "DE", "GB")
  private final Map<UUID, String> propertyCountryCodes = new LinkedHashMap<>();

  // Property UUID -> current market rent baseline (for contract chain generation)
  private final Map<UUID, BigDecimal> propertyRentBaselines = new LinkedHashMap<>();

  // Property UUID -> property type (e.g., "APARTMENT", "HOUSE", "OFFICE")
  private final Map<UUID, String> propertyTypes = new LinkedHashMap<>();

  // Property UUID -> country rent multiplier (reflects cost-of-living for expense scaling)
  private final Map<UUID, Double> propertyCountryRentMultipliers = new LinkedHashMap<>();

  // Property UUID -> every unit id generated for it, in unit-number order (BUUR-106:
  // contracts.unit_id and friends are NOT NULL as of V068, so every demo property needs at least
  // one before contracts are generated). Single-unit properties get exactly one implicit unit;
  // curated multi-unit buildings get one entry per DemoUnitGenerator.UnitPlan below.
  private final Map<UUID, List<UUID>> unitIdsByProperty = new LinkedHashMap<>();

  // Property UUID -> the unit plan for curated multi-unit demo buildings. Absent (empty list) for
  // every other property, which signals DemoUnitGenerator to create a single implicit unit.
  private final Map<UUID, List<UnitPlan>> unitPlansByProperty = new LinkedHashMap<>();

  // Property UUID -> short tag identifying a specific curated multi-unit building, for generators
  // that need to single one out (e.g. DemoExpenseGenerator attaching a building-wide roof repair).
  private final Map<UUID, String> buildingTagByProperty = new LinkedHashMap<>();

  // Counters
  @Getter private int teamsCreated;
  @Getter private int usersCreated;
  @Getter private int propertiesCreated;
  @Getter private int contactsCreated;
  @Getter private int contractsCreated;
  @Getter private int paymentsCreated;
  @Getter private int expensesCreated;
  @Getter private int notificationsCreated;
  @Getter private int documentsCreated;

  public void putIdentifier(UUID id, String identifier) {
    identifiers.put(id, identifier);
  }

  public void putIdentifier(UUID id, com.buurman.domain.Sid identifier) {
    identifiers.put(id, identifier.value());
  }

  public String getIdentifierString(UUID id) {
    return identifiers.getOrDefault(id, id.toString());
  }

  public com.buurman.domain.Sid getIdentifier(UUID id) {
    return com.buurman.domain.Sid.of(identifiers.getOrDefault(id, id.toString()));
  }

  public void putTeamCurrency(String teamKey, String currency) {
    teamCurrencies.put(teamKey, currency);
  }

  public String getCurrencyForTeam(String teamKey) {
    return teamCurrencies.getOrDefault(teamKey, "EUR");
  }

  public void putPropertyCategory(UUID propertyId, String category) {
    propertyCategoriesByProperty.put(propertyId, category);
  }

  public String getPropertyCategory(UUID propertyId) {
    return propertyCategoriesByProperty.getOrDefault(propertyId, "RESIDENTIAL");
  }

  public void putBusinessContactFlag(UUID contactId, boolean isBusiness) {
    businessContactFlags.put(contactId, isBusiness);
  }

  public boolean isBusinessContact(UUID contactId) {
    return businessContactFlags.getOrDefault(contactId, false);
  }

  public void putContactType(UUID contactId, ContactType type) {
    contactTypes.put(contactId, type);
  }

  public ContactType getContactType(UUID contactId) {
    return contactTypes.getOrDefault(contactId, ContactType.INDIVIDUAL);
  }

  public void putPropertyAcquisitionDate(UUID propertyId, LocalDate date) {
    propertyAcquisitionDates.put(propertyId, date);
  }

  public LocalDate getPropertyAcquisitionDate(UUID propertyId) {
    return propertyAcquisitionDates.getOrDefault(propertyId, LocalDate.of(2020, 1, 1));
  }

  public void putPropertyCountryCode(UUID propertyId, String countryCode) {
    propertyCountryCodes.put(propertyId, countryCode);
  }

  public String getPropertyCountryCode(UUID propertyId) {
    return propertyCountryCodes.getOrDefault(propertyId, "NL");
  }

  public void putPropertyRentBaseline(UUID propertyId, BigDecimal rent) {
    propertyRentBaselines.put(propertyId, rent);
  }

  public BigDecimal getPropertyRentBaseline(UUID propertyId) {
    return propertyRentBaselines.getOrDefault(propertyId, BigDecimal.valueOf(1200));
  }

  public void putPropertyType(UUID propertyId, String type) {
    propertyTypes.put(propertyId, type);
  }

  public String getPropertyType(UUID propertyId) {
    return propertyTypes.getOrDefault(propertyId, "APARTMENT");
  }

  public void putPropertyCountryRentMultiplier(UUID propertyId, double multiplier) {
    propertyCountryRentMultipliers.put(propertyId, multiplier);
  }

  public double getPropertyCountryRentMultiplier(UUID propertyId) {
    return propertyCountryRentMultipliers.getOrDefault(propertyId, 1.0);
  }

  public void putUnitIds(UUID propertyId, List<UUID> unitIds) {
    unitIdsByProperty.put(propertyId, List.copyOf(unitIds));
  }

  public List<UUID> getUnitIds(UUID propertyId) {
    return unitIdsByProperty.getOrDefault(propertyId, List.of());
  }

  /**
   * The unit id for a property known to have exactly one unit. Fails loudly rather than silently
   * picking one of several units or inserting a null FK.
   */
  public UUID getSoleUnitId(UUID propertyId) {
    List<UUID> unitIds = getUnitIds(propertyId);
    if (unitIds.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly one unit for property " + propertyId + " but found " + unitIds.size());
    }
    return unitIds.get(0);
  }

  public void putUnitPlans(UUID propertyId, List<UnitPlan> plans) {
    unitPlansByProperty.put(propertyId, List.copyOf(plans));
  }

  /** Empty for every property except a curated multi-unit demo building. */
  public List<UnitPlan> getUnitPlans(UUID propertyId) {
    return unitPlansByProperty.getOrDefault(propertyId, List.of());
  }

  public void putBuildingTag(UUID propertyId, String tag) {
    buildingTagByProperty.put(propertyId, tag);
  }

  public Optional<String> getBuildingTag(UUID propertyId) {
    return Optional.ofNullable(buildingTagByProperty.get(propertyId));
  }

  public void incrementTeams() {
    teamsCreated++;
  }

  public void incrementUsers() {
    usersCreated++;
  }

  public void incrementProperties() {
    propertiesCreated++;
  }

  public void incrementContacts() {
    contactsCreated++;
  }

  public void incrementContracts() {
    contractsCreated++;
  }

  public void incrementPayments() {
    paymentsCreated++;
  }

  public void incrementExpenses() {
    expensesCreated++;
  }

  public void incrementNotifications(int count) {
    notificationsCreated += count;
  }

  public void incrementDocuments(int count) {
    documentsCreated += count;
  }

  /** Get the "admin" user UUID for a given team key (used as created_by/updated_by). */
  public Optional<UUID> getAdminUserForTeam(String teamKey) {
    return Optional.ofNullable(
        switch (teamKey) {
          case "demo-team" -> userIds.get("demo.user@demo.buurman.io");
          case "team-alpha" -> userIds.get("admin@demo.buurman.io");
          case "team-beta" -> userIds.get("admin.team2@demo.buurman.io");
          default -> userIds.values().stream().findFirst().orElse(null);
        });
  }
}
