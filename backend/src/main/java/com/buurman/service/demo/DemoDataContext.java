package com.buurman.service.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Mutable context passed between demo data generators to share generated IDs. */
public class DemoDataContext {

  // Team key (e.g., "demo-team") -> team UUID
  private final Map<String, UUID> teamIds = new LinkedHashMap<>();

  // UUID -> identifier (ULID) for S3 key generation
  private final Map<UUID, String> identifiers = new LinkedHashMap<>();

  // Email -> user UUID
  private final Map<String, UUID> userIds = new LinkedHashMap<>();

  // Email -> Keycloak user ID
  private final Map<String, String> keycloakIds = new LinkedHashMap<>();

  // Team UUID -> list of property UUIDs
  private final Map<UUID, List<UUID>> propertyIdsByTeam = new LinkedHashMap<>();

  // Team UUID -> list of tenant UUIDs
  private final Map<UUID, List<UUID>> tenantIdsByTeam = new LinkedHashMap<>();

  // Team UUID -> list of contract UUIDs
  private final Map<UUID, List<UUID>> contractIdsByTeam = new LinkedHashMap<>();

  // Team UUID -> list of payment instruction UUIDs
  private final Map<UUID, List<UUID>> paymentInstructionIdsByTeam = new LinkedHashMap<>();

  // Contract UUID -> list of payment UUIDs
  private final Map<UUID, List<UUID>> paymentIdsByContract = new LinkedHashMap<>();

  // Counters
  private int teamsCreated;
  private int usersCreated;
  private int propertiesCreated;
  private int tenantsCreated;
  private int contractsCreated;
  private int paymentsCreated;
  private int expensesCreated;
  private int notificationsCreated;
  private int documentsCreated;

  public Map<String, UUID> getTeamIds() {
    return teamIds;
  }

  public void putIdentifier(UUID id, String identifier) {
    identifiers.put(id, identifier);
  }

  public String getIdentifier(UUID id) {
    return identifiers.getOrDefault(id, id.toString());
  }

  public Map<String, UUID> getUserIds() {
    return userIds;
  }

  public Map<String, String> getKeycloakIds() {
    return keycloakIds;
  }

  public Map<UUID, List<UUID>> getPropertyIdsByTeam() {
    return propertyIdsByTeam;
  }

  public Map<UUID, List<UUID>> getTenantIdsByTeam() {
    return tenantIdsByTeam;
  }

  public Map<UUID, List<UUID>> getContractIdsByTeam() {
    return contractIdsByTeam;
  }

  public Map<UUID, List<UUID>> getPaymentInstructionIdsByTeam() {
    return paymentInstructionIdsByTeam;
  }

  public Map<UUID, List<UUID>> getPaymentIdsByContract() {
    return paymentIdsByContract;
  }

  public int getTeamsCreated() {
    return teamsCreated;
  }

  public void incrementTeams() {
    teamsCreated++;
  }

  public int getUsersCreated() {
    return usersCreated;
  }

  public void incrementUsers() {
    usersCreated++;
  }

  public int getPropertiesCreated() {
    return propertiesCreated;
  }

  public void incrementProperties() {
    propertiesCreated++;
  }

  public int getTenantsCreated() {
    return tenantsCreated;
  }

  public void incrementTenants() {
    tenantsCreated++;
  }

  public int getContractsCreated() {
    return contractsCreated;
  }

  public void incrementContracts() {
    contractsCreated++;
  }

  public int getPaymentsCreated() {
    return paymentsCreated;
  }

  public void incrementPayments() {
    paymentsCreated++;
  }

  public int getExpensesCreated() {
    return expensesCreated;
  }

  public void incrementExpenses() {
    expensesCreated++;
  }

  public int getNotificationsCreated() {
    return notificationsCreated;
  }

  public void incrementNotifications(int count) {
    notificationsCreated += count;
  }

  public int getDocumentsCreated() {
    return documentsCreated;
  }

  public void incrementDocuments(int count) {
    documentsCreated += count;
  }

  /** Get the "admin" user UUID for a given team key (used as created_by/updated_by). */
  public UUID getAdminUserForTeam(String teamKey) {
    return switch (teamKey) {
      case "demo-team" -> userIds.get("demo.user@demo.buurman.io");
      case "team-alpha" -> userIds.get("admin@demo.buurman.io");
      case "team-beta" -> userIds.get("admin.team2@demo.buurman.io");
      default -> userIds.values().iterator().next();
    };
  }
}
