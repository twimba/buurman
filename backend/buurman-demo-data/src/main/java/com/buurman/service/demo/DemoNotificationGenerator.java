package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.util.SidGenerator.newNotificationId;
import static java.time.temporal.ChronoUnit.DAYS;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import com.buurman.config.models.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemoNotificationGenerator {

  private final DSLContext dsl;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final Clock clock;
  private final Random random = new Random(42);

  public DemoNotificationGenerator(
      DSLContext dsl, ObjectMapper objectMapper, AppProperties appProperties, Clock clock) {
    this.dsl = dsl;
    this.objectMapper = objectMapper;
    this.baseUrl = appProperties.email().baseUrl();
    this.clock = clock;
  }

  @SuppressWarnings("NullAway")
  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);

    // --- Prefetch all data upfront to eliminate N+1 queries ---
    Collection<UUID> allTeamIds = ctx.getTeamIds().values();

    var usersById = dsl.selectFrom(USERS).fetchMap(USERS.ID);

    var propertiesById =
        dsl.selectFrom(PROPERTIES).where(PROPERTIES.TEAM_ID.in(allTeamIds)).fetchMap(PROPERTIES.ID);

    var contractsById =
        dsl.selectFrom(CONTRACTS).where(CONTRACTS.TEAM_ID.in(allTeamIds)).fetchMap(CONTRACTS.ID);

    var contactsById =
        dsl.selectFrom(CONTACTS).where(CONTACTS.TEAM_ID.in(allTeamIds)).fetchMap(CONTACTS.ID);

    var paymentsById =
        dsl.selectFrom(PAYMENTS).where(PAYMENTS.TEAM_ID.in(allTeamIds)).fetchMap(PAYMENTS.ID);

    // team_member user IDs per team
    Map<UUID, List<UUID>> memberUserIdsByTeam = new HashMap<>();
    dsl.select(TEAM_MEMBERS.TEAM_ID, TEAM_MEMBERS.USER_ID)
        .from(TEAM_MEMBERS)
        .where(TEAM_MEMBERS.TEAM_ID.in(allTeamIds))
        .fetch()
        .forEach(
            r ->
                memberUserIdsByTeam
                    .computeIfAbsent(r.get(TEAM_MEMBERS.TEAM_ID), k -> new ArrayList<>())
                    .add(r.get(TEAM_MEMBERS.USER_ID)));

    // contract_parties: contract_id -> primary tenant contact_id
    Map<UUID, UUID> primaryContactByContract = new HashMap<>();
    dsl.select(CONTRACT_PARTIES.CONTRACT_ID, CONTRACT_PARTIES.CONTACT_ID)
        .from(CONTRACT_PARTIES)
        .where(CONTRACT_PARTIES.ROLE.eq("PRIMARY_TENANT"))
        .and(CONTRACT_PARTIES.DELETED_AT.isNull())
        .fetch()
        .forEach(
            r ->
                primaryContactByContract.putIfAbsent(
                    r.get(CONTRACT_PARTIES.CONTRACT_ID), r.get(CONTRACT_PARTIES.CONTACT_ID)));

    // contact_id -> first contract_id (for failed notification section)
    Map<UUID, UUID> firstContractByContact = new HashMap<>();
    dsl.select(CONTRACT_PARTIES.CONTACT_ID, CONTRACT_PARTIES.CONTRACT_ID)
        .from(CONTRACT_PARTIES)
        .where(CONTRACT_PARTIES.DELETED_AT.isNull())
        .fetch()
        .forEach(
            r ->
                firstContractByContact.putIfAbsent(
                    r.get(CONTRACT_PARTIES.CONTACT_ID), r.get(CONTRACT_PARTIES.CONTRACT_ID)));

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      String teamCurrency = ctx.getCurrencyForTeam(teamKey);

      List<UUID> contractIds = ctx.getContractIdsByTeam().getOrDefault(teamId, List.of());
      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().getOrDefault(teamId, List.of());
      List<UUID> contactIds = ctx.getContactIdsByTeam().getOrDefault(teamId, List.of());

      // Collect all notifications for this team, then batch insert
      List<Object[]> pending = new ArrayList<>();

      // Resolve admin once per team
      Record adminRecord = createdBy != null ? usersById.get(createdBy) : null;
      String adminEmail =
          adminRecord != null ? adminRecord.get(USERS.EMAIL) : "admin@demo.buurman.io";
      String adminName = adminRecord != null ? adminRecord.get(USERS.FIRST_NAME) : "Admin";

      // --- Welcome emails for all team members ---
      List<UUID> memberUserIds = memberUserIdsByTeam.getOrDefault(teamId, List.of());
      for (UUID userId : memberUserIds) {
        Record user = usersById.get(userId);
        if (user == null) {
          continue;
        }

        String email = user.get(USERS.EMAIL);
        String firstName = user.get(USERS.FIRST_NAME);

        Map<String, Object> vars =
            Map.of(
                "userName", firstName,
                "baseUrl", baseUrl);

        addEmailNotification(
            pending,
            teamId,
            createdBy,
            now.minusDays(random.nextInt(60, 90)),
            "WELCOME",
            "welcome",
            vars,
            email,
            userId,
            null,
            "DELIVERED",
            "delivered",
            null);
      }

      // --- Property created notifications ---
      for (int i = 0; i < Math.min(propertyIds.size(), 3); i++) {
        UUID propertyId = propertyIds.get(i);
        Record property = propertiesById.get(propertyId);
        if (property == null) {
          continue;
        }

        String propertyName =
            property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);
        String propertyType = property.get(PROPERTIES.PROPERTY_TYPE);

        Map<String, Object> vars =
            Map.of(
                "propertyName", propertyName,
                "propertyAddress", propertyName,
                "propertyType", propertyType != null ? propertyType : "N/A",
                "baseUrl", baseUrl);

        addEmailNotification(
            pending,
            teamId,
            createdBy,
            now.minusDays(random.nextInt(30, 60)),
            "PROPERTY_CREATED",
            "property-created",
            vars,
            adminEmail,
            createdBy,
            null,
            "DELIVERED",
            "delivered",
            null);
      }

      // --- Contract created + status change notifications ---
      for (UUID contractId : contractIds) {
        Record contract = contractsById.get(contractId);
        if (contract == null) {
          continue;
        }

        UUID primaryContactId = primaryContactByContract.get(contractId);
        UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
        String status = contract.get(CONTRACTS.STATUS);
        if (primaryContactId == null) {
          continue;
        }
        Record contact = contactsById.get(primaryContactId);
        Record property = propertiesById.get(propertyId);
        if (contact == null || property == null) {
          continue;
        }

        String contactName =
            contact.get(CONTACTS.FIRST_NAME) + " " + contact.get(CONTACTS.LAST_NAME);
        String propertyName =
            property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);

        // Contract created notification
        var startDate = contract.get(CONTRACTS.START_DATE);
        var endDate = contract.get(CONTRACTS.END_DATE);
        String currency =
            contract.get(CONTRACTS.RENT_AMOUNT_CURRENCY) != null
                ? contract.get(CONTRACTS.RENT_AMOUNT_CURRENCY)
                : teamCurrency;
        BigDecimal rentAmount = contract.get(CONTRACTS.RENT_AMOUNT);

        Map<String, Object> contractVars = new HashMap<>();
        contractVars.put("propertyName", propertyName);
        contractVars.put("contactName", contactName);
        contractVars.put("rentAmount", currency + " " + rentAmount);
        contractVars.put("startDate", startDate != null ? startDate.toString() : "N/A");
        contractVars.put("endDate", endDate != null ? endDate.toString() : "");
        contractVars.put("baseUrl", baseUrl);

        addEmailNotification(
            pending,
            teamId,
            createdBy,
            now.minusDays(random.nextInt(20, 50)),
            "CONTRACT_CREATED",
            "contract-created",
            contractVars,
            adminEmail,
            createdBy,
            null,
            "DELIVERED",
            "delivered",
            null);

        // Contract status change notification for non-draft, non-active contracts
        if (!"DRAFT".equals(status) && !"ACTIVE".equals(status)) {
          Map<String, Object> statusVars =
              Map.of(
                  "propertyName", propertyName,
                  "contactName", contactName,
                  "oldStatus", "ACTIVE",
                  "newStatus", status,
                  "baseUrl", baseUrl);

          addEmailNotification(
              pending,
              teamId,
              createdBy,
              now.minusDays(random.nextInt(5, 30)),
              "CONTRACT_STATUS_CHANGED",
              "contract-status-changed",
              statusVars,
              adminEmail,
              createdBy,
              null,
              "DELIVERED",
              "delivered",
              null);
        }
      }

      // --- Payment reminder notifications (for upcoming/overdue payments) ---
      for (UUID contractId : contractIds) {
        List<UUID> paymentIds = ctx.getPaymentIdsByContract().getOrDefault(contractId, List.of());
        Record contract = contractsById.get(contractId);
        if (contract == null) {
          continue;
        }

        UUID primaryContactId = primaryContactByContract.get(contractId);
        UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
        if (primaryContactId == null) {
          continue;
        }
        Record contact = contactsById.get(primaryContactId);
        Record property = propertiesById.get(propertyId);
        if (contact == null || property == null) {
          continue;
        }

        String contactName =
            contact.get(CONTACTS.FIRST_NAME) + " " + contact.get(CONTACTS.LAST_NAME);
        String contactEmail = contact.get(CONTACTS.EMAIL);
        String contactPhone = contact.get(CONTACTS.PHONE);
        String propertyName =
            property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);

        int reminderCount = 0;
        for (UUID paymentId : paymentIds) {
          if (reminderCount >= 2) {
            break;
          }
          Record payment = paymentsById.get(paymentId);
          if (payment == null) {
            continue;
          }

          String paymentStatus = payment.get(PAYMENTS.STATUS);
          if ("PENDING".equals(paymentStatus) || "OVERDUE".equals(paymentStatus)) {
            String payCurrency =
                payment.get(PAYMENTS.CURRENCY) != null
                    ? payment.get(PAYMENTS.CURRENCY)
                    : teamCurrency;
            String amount = payCurrency + " " + payment.get(PAYMENTS.AMOUNT).toPlainString();
            String dueDate = payment.get(PAYMENTS.DUE_DATE).toString();

            Map<String, Object> vars =
                Map.of(
                    "userName", contactName,
                    "propertyName", propertyName,
                    "amount", amount,
                    "dueDate", dueDate,
                    "baseUrl", baseUrl);

            // Email reminder
            addEmailNotification(
                pending,
                teamId,
                createdBy,
                now.minusDays(random.nextInt(1, 10)),
                "PAYMENT_REMINDER",
                "payment-reminder",
                vars,
                contactEmail,
                null,
                primaryContactId,
                "DELIVERED",
                "delivered",
                null);

            // SMS reminder for overdue
            if ("OVERDUE".equals(paymentStatus) && contactPhone != null) {
              addSmsNotification(
                  pending,
                  teamId,
                  createdBy,
                  now.minusDays(random.nextInt(1, 5)),
                  "PAYMENT_REMINDER",
                  "payment-reminder",
                  vars,
                  contactPhone,
                  null,
                  primaryContactId,
                  "SENT",
                  "sent",
                  null);
            }

            reminderCount++;
          }
        }
      }

      // --- Payment paid notifications ---
      int paidCount = 0;
      for (UUID contractId : contractIds) {
        if (paidCount >= 4) {
          break;
        }
        List<UUID> paymentIds = ctx.getPaymentIdsByContract().getOrDefault(contractId, List.of());
        Record contract = contractsById.get(contractId);
        if (contract == null) {
          continue;
        }

        UUID primaryContactId = primaryContactByContract.get(contractId);
        UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
        if (primaryContactId == null) {
          continue;
        }
        Record contact = contactsById.get(primaryContactId);
        Record property = propertiesById.get(propertyId);
        if (contact == null || property == null) {
          continue;
        }

        String contactName =
            contact.get(CONTACTS.FIRST_NAME) + " " + contact.get(CONTACTS.LAST_NAME);
        String propertyName =
            property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);

        for (UUID paymentId : paymentIds) {
          if (paidCount >= 4) {
            break;
          }
          Record payment = paymentsById.get(paymentId);
          if (payment == null || !"PAID".equals(payment.get(PAYMENTS.STATUS))) {
            continue;
          }

          String rcptCurrency =
              payment.get(PAYMENTS.CURRENCY) != null
                  ? payment.get(PAYMENTS.CURRENCY)
                  : teamCurrency;
          String amount = rcptCurrency + " " + payment.get(PAYMENTS.AMOUNT).toPlainString();
          var paymentDate = payment.get(PAYMENTS.PAYMENT_DATE);

          Map<String, Object> vars =
              Map.of(
                  "propertyName", propertyName,
                  "contactName", contactName,
                  "amount", amount,
                  "paymentDate", paymentDate != null ? paymentDate.toString() : "N/A",
                  "baseUrl", baseUrl);

          addEmailNotification(
              pending,
              teamId,
              createdBy,
              now.minusDays(random.nextInt(1, 30)),
              "PAYMENT_PAID",
              "payment-paid",
              vars,
              adminEmail,
              createdBy,
              null,
              "DELIVERED",
              "delivered",
              null);
          paidCount++;
        }
      }

      // --- Contract expiry notifications (for contracts expiring soon) ---
      for (UUID contractId : contractIds) {
        Record contract = contractsById.get(contractId);
        if (contract == null || !"ACTIVE".equals(contract.get(CONTRACTS.STATUS))) {
          continue;
        }

        var endDate = contract.get(CONTRACTS.END_DATE);
        if (endDate == null) {
          continue;
        }

        UUID primaryContactId = primaryContactByContract.get(contractId);
        UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
        if (primaryContactId == null) {
          continue;
        }
        Record contact = contactsById.get(primaryContactId);
        Record property = propertiesById.get(propertyId);
        if (contact == null || property == null) {
          continue;
        }

        String propertyName =
            property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);
        long daysUntilExpiry = DAYS.between(LocalDate.now(clock), endDate);

        Map<String, Object> vars =
            Map.of(
                "userName", adminName,
                "propertyName", propertyName,
                "daysUntilExpiry", daysUntilExpiry,
                "expiryDate", endDate.toString(),
                "baseUrl", baseUrl);

        addEmailNotification(
            pending,
            teamId,
            createdBy,
            now.minusDays(random.nextInt(1, 14)),
            "CONTRACT_EXPIRY",
            "contract-expiry",
            vars,
            adminEmail,
            createdBy,
            null,
            "DELIVERED",
            "delivered",
            null);
      }

      // --- A couple of failed notifications for realism ---
      if (!contactIds.isEmpty()) {
        UUID failedContactId = contactIds.get(random.nextInt(contactIds.size()));
        Record failedContact = contactsById.get(failedContactId);
        if (failedContact != null) {
          String contactEmail = failedContact.get(CONTACTS.EMAIL);
          String contactName =
              failedContact.get(CONTACTS.FIRST_NAME) + " " + failedContact.get(CONTACTS.LAST_NAME);

          // Find a property for this contact via prefetched contract_parties
          String propertyName = "your property";
          UUID contactContractId = firstContractByContact.get(failedContactId);
          if (contactContractId != null) {
            Record contactContract = contractsById.get(contactContractId);
            if (contactContract != null && teamId.equals(contactContract.get(CONTRACTS.TEAM_ID))) {
              Record prop = propertiesById.get(contactContract.get(CONTRACTS.PROPERTY_ID));
              if (prop != null) {
                propertyName = prop.get(PROPERTIES.STREET) + ", " + prop.get(PROPERTIES.CITY);
              }
            }
          }

          Map<String, Object> vars =
              Map.of(
                  "userName", contactName,
                  "propertyName", propertyName,
                  "amount", teamCurrency + " 1200.00",
                  "dueDate", LocalDate.now(clock).minusDays(15).toString(),
                  "baseUrl", baseUrl);

          // Bounced email
          addEmailNotification(
              pending,
              teamId,
              createdBy,
              now.minusDays(random.nextInt(5, 20)),
              "PAYMENT_REMINDER",
              "payment-reminder",
              vars,
              contactEmail,
              null,
              failedContactId,
              "BOUNCED",
              null,
              "550 5.1.1 The email account does not exist");

          // Failed SMS
          addSmsNotification(
              pending,
              teamId,
              createdBy,
              now.minusDays(random.nextInt(3, 15)),
              "PAYMENT_REMINDER",
              "payment-reminder",
              vars,
              "+31600000000",
              null,
              failedContactId,
              "FAILED",
              null,
              "Invalid phone number");
        }
      }

      // --- Batch insert all notifications for this team ---
      if (!pending.isEmpty()) {
        batchInsertNotifications(pending);
      }

      ctx.incrementNotifications(pending.size());
      log.info("Created {} notifications for team {}", pending.size(), teamKey);
    }
  }

  /** Collects an email notification row into the pending batch. */
  private void addEmailNotification(
      List<Object[]> pending,
      UUID teamId,
      UUID createdBy,
      LocalDateTime createdAt,
      String type,
      String template,
      Map<String, Object> templateVars,
      String recipientEmail,
      UUID recipientUserId,
      UUID recipientContactId,
      String status,
      String providerStatus,
      String providerError) {
    String subject = deriveSubject(template, templateVars);
    String body = renderSimpleBody(subject);
    String varsJson = toJson(templateVars);
    LocalDateTime statusUpdatedAt = createdAt.plusMinutes(random.nextInt(1, 30));

    pending.add(
        new Object[] {
          UUID.randomUUID(),
          newNotificationId(),
          teamId,
          type,
          "EMAIL",
          template,
          subject,
          body,
          recipientEmail,
          null,
          recipientUserId,
          recipientContactId,
          varsJson != null ? JSONB.jsonb(varsJson) : null,
          status,
          providerStatus,
          providerError,
          statusUpdatedAt,
          createdAt,
          createdBy
        });
  }

  /** Collects an SMS notification row into the pending batch. */
  private void addSmsNotification(
      List<Object[]> pending,
      UUID teamId,
      UUID createdBy,
      LocalDateTime createdAt,
      String type,
      String template,
      Map<String, Object> templateVars,
      String recipientPhone,
      UUID recipientUserId,
      UUID recipientContactId,
      String status,
      String providerStatus,
      String providerError) {
    String smsBody = renderSmsBody(template, templateVars);
    String varsJson = toJson(templateVars);
    LocalDateTime statusUpdatedAt = createdAt.plusMinutes(random.nextInt(1, 30));

    pending.add(
        new Object[] {
          UUID.randomUUID(),
          newNotificationId(),
          teamId,
          type,
          "SMS",
          template,
          null,
          smsBody,
          null,
          recipientPhone,
          recipientUserId,
          recipientContactId,
          varsJson != null ? JSONB.jsonb(varsJson) : null,
          status,
          providerStatus,
          providerError,
          statusUpdatedAt,
          createdAt,
          createdBy
        });
  }

  /** Batch inserts all collected notification rows in a single round-trip. */
  @SuppressWarnings("unchecked")
  private void batchInsertNotifications(List<Object[]> rows) {
    var queries =
        rows.stream()
            .map(
                r ->
                    dsl.insertInto(NOTIFICATIONS)
                        .set(NOTIFICATIONS.ID, (UUID) r[0])
                        .set(NOTIFICATIONS.IDENTIFIER, (com.buurman.domain.Sid) r[1])
                        .set(NOTIFICATIONS.TEAM_ID, (UUID) r[2])
                        .set(NOTIFICATIONS.NOTIFICATION_TYPE, (String) r[3])
                        .set(NOTIFICATIONS.CHANNEL, (String) r[4])
                        .set(NOTIFICATIONS.CONTENT_TEMPLATE, (String) r[5])
                        .set(NOTIFICATIONS.SUBJECT, (String) r[6])
                        .set(NOTIFICATIONS.BODY, (String) r[7])
                        .set(NOTIFICATIONS.RECIPIENT_EMAIL, (String) r[8])
                        .set(NOTIFICATIONS.RECIPIENT_PHONE, (String) r[9])
                        .set(NOTIFICATIONS.RECIPIENT_USER_ID, (UUID) r[10])
                        .set(NOTIFICATIONS.RECIPIENT_CONTACT_ID, (UUID) r[11])
                        .set(NOTIFICATIONS.CONTENT_VARIABLES, (JSONB) r[12])
                        .set(NOTIFICATIONS.STATUS, (String) r[13])
                        .set(NOTIFICATIONS.PROVIDER_STATUS, (String) r[14])
                        .set(NOTIFICATIONS.PROVIDER_ERROR, (String) r[15])
                        .set(NOTIFICATIONS.STATUS_UPDATED_AT, (LocalDateTime) r[16])
                        .set(NOTIFICATIONS.CREATED_AT, (LocalDateTime) r[17])
                        .set(NOTIFICATIONS.CREATED_BY, (UUID) r[18]))
            .toList();
    dsl.batch(queries).execute();
  }

  /** Simple static HTML body — skips Thymeleaf rendering for demo data. */
  private String renderSimpleBody(String subject) {
    return "<div style='font-family:sans-serif;padding:20px'>"
        + "<h2>"
        + subject
        + "</h2>"
        + "<p>This is a demo notification.</p></div>";
  }

  private String deriveSubject(String templateName, Map<String, Object> variables) {
    return switch (templateName) {
      case "welcome" -> "Welcome to Buurman!";
      case "payment-reminder" ->
          "Payment reminder for " + getVar(variables, "propertyName", "your property");
      case "contract-expiry" ->
          "Contract expiring soon for " + getVar(variables, "propertyName", "your property");
      case "property-created" ->
          "Property created: " + getVar(variables, "propertyName", "New property");
      case "contract-created" ->
          "New contract for " + getVar(variables, "propertyName", "your property");
      case "contract-status-changed" ->
          "Contract status changed to " + getVar(variables, "newStatus", "updated");
      case "payment-paid" ->
          "Payment marked as paid for " + getVar(variables, "propertyName", "your property");
      default -> "Notification from Buurman";
    };
  }

  private String renderSmsBody(String templateName, Map<String, Object> variables) {
    return switch (templateName) {
      case "payment-reminder" ->
          "Buurman: Your rent payment of "
              + getVar(variables, "amount", "N/A")
              + " (due "
              + getVar(variables, "dueDate", "N/A")
              + ") is overdue. Please arrange payment.";
      default -> "Buurman: You have a new notification.";
    };
  }

  private String getVar(Map<String, Object> variables, String key, String defaultValue) {
    Object val = variables.get(key);
    return val != null ? val.toString() : defaultValue;
  }

  private String toJson(Map<String, Object> vars) {
    try {
      return objectMapper.writeValueAsString(vars);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize template variables: {}", e.getMessage());
      return "{}";
    }
  }
}
