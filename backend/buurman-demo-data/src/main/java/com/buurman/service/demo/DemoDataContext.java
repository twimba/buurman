package com.buurman.service.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

  // Team UUID -> list of tenant UUIDs
  @Getter private final Map<UUID, List<UUID>> tenantIdsByTeam = new LinkedHashMap<>();

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

  // Tenant UUID -> whether the tenant is a business entity
  private final Map<UUID, Boolean> businessTenantFlags = new LinkedHashMap<>();

  // Counters
  @Getter private int teamsCreated;
  @Getter private int usersCreated;
  @Getter private int propertiesCreated;
  @Getter private int tenantsCreated;
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

  public void putBusinessTenantFlag(UUID tenantId, boolean isBusiness) {
    businessTenantFlags.put(tenantId, isBusiness);
  }

  public boolean isBusinessTenant(UUID tenantId) {
    return businessTenantFlags.getOrDefault(tenantId, false);
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

  public void incrementTenants() {
    tenantsCreated++;
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
          default -> userIds.values().iterator().next();
        });
  }
}
