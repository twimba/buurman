package com.buurman.service.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.buurman.domain.ContactType;

import lombok.Getter;

/** Mutable context passed between demo data generators to share generated IDs. */
public class DemoDataContext {

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
