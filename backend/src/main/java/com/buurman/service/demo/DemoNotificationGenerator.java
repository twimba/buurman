package com.buurman.service.demo;

import com.buurman.config.models.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.util.UlidGenerator.newNotificationId;
import static java.time.temporal.ChronoUnit.DAYS;

@Component
@Slf4j
public class DemoNotificationGenerator {

    private final DSLContext dsl;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Clock clock;
    private final Random random = new Random(42);

    public DemoNotificationGenerator(DSLContext dsl, TemplateEngine templateEngine,
                                     ObjectMapper objectMapper, AppProperties appProperties,
                                     Clock clock) {
        this.dsl = dsl;
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
        this.baseUrl = appProperties.email().baseUrl();
        this.clock = clock;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now(clock);

        for (var teamEntry : ctx.getTeamIds().entrySet()) {
            String teamKey = teamEntry.getKey();
            UUID teamId = teamEntry.getValue();
            UUID createdBy = ctx.getAdminUserForTeam(teamKey);

            List<UUID> contractIds = ctx.getContractIdsByTeam().getOrDefault(teamId, List.of());
            List<UUID> propertyIds = ctx.getPropertyIdsByTeam().getOrDefault(teamId, List.of());
            List<UUID> tenantIds = ctx.getTenantIdsByTeam().getOrDefault(teamId, List.of());

            int teamNotifications = 0;

            // --- Welcome emails for all team members ---
            var members = dsl.select(TEAM_MEMBERS.USER_ID)
                    .from(TEAM_MEMBERS)
                    .where(TEAM_MEMBERS.TEAM_ID.eq(teamId))
                    .fetch();

            for (var member : members) {
                UUID userId = member.get(TEAM_MEMBERS.USER_ID);
                Record user = dsl.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOne();
                if (user == null) {
                    continue;
                }

                String email = user.get(USERS.EMAIL);
                String firstName = user.get(USERS.FIRST_NAME);

                Map<String, Object> vars = Map.of(
                        "userName", firstName,
                        "baseUrl", baseUrl
                );

                insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(60, 90)),
                        "WELCOME", "EMAIL", "welcome", vars,
                        email, null, userId, null,
                        "DELIVERED", "delivered", null);
                teamNotifications++;
            }

            // --- Property created notifications ---
            for (int i = 0; i < Math.min(propertyIds.size(), 3); i++) {
                UUID propertyId = propertyIds.get(i);
                Record property = dsl.selectFrom(PROPERTIES).where(PROPERTIES.ID.eq(propertyId)).fetchOne();
                if (property == null) {
                    continue;
                }

                String street = property.get(PROPERTIES.STREET);
                String city = property.get(PROPERTIES.CITY);
                String propertyName = street + ", " + city;
                String propertyType = property.get(PROPERTIES.PROPERTY_TYPE);

                Record admin = dsl.selectFrom(USERS).where(USERS.ID.eq(createdBy)).fetchOne();
                String adminEmail = admin != null ? admin.get(USERS.EMAIL) : "admin@demo.buurman.io";

                Map<String, Object> vars = Map.of(
                        "propertyName", propertyName,
                        "propertyAddress", propertyName,
                        "propertyType", propertyType != null ? propertyType : "N/A",
                        "baseUrl", baseUrl
                );

                insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(30, 60)),
                        "PROPERTY_CREATED", "EMAIL", "property-created", vars,
                        adminEmail, null, createdBy, null,
                        "DELIVERED", "delivered", null);
                teamNotifications++;
            }

            // --- Contract created + status change notifications ---
            for (UUID contractId : contractIds) {
                Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
                if (contract == null) {
                    continue;
                }

                UUID tenantId = contract.get(CONTRACTS.TENANT_ID);
                UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
                String status = contract.get(CONTRACTS.STATUS);
                Record tenant = dsl.selectFrom(TENANTS).where(TENANTS.ID.eq(tenantId)).fetchOne();
                Record property = dsl.selectFrom(PROPERTIES).where(PROPERTIES.ID.eq(propertyId)).fetchOne();
                if (tenant == null || property == null) {
                    continue;
                }

                String tenantName = tenant.get(TENANTS.FIRST_NAME) + " " + tenant.get(TENANTS.LAST_NAME);
                String street = property.get(PROPERTIES.STREET);
                String city = property.get(PROPERTIES.CITY);
                String propertyName = street + ", " + city;

                Record admin = dsl.selectFrom(USERS).where(USERS.ID.eq(createdBy)).fetchOne();
                String adminEmail = admin != null ? admin.get(USERS.EMAIL) : "admin@demo.buurman.io";

                // Contract created notification
                var startDate = contract.get(CONTRACTS.START_DATE);
                var endDate = contract.get(CONTRACTS.END_DATE);
                var rentAmount = contract.get(CONTRACTS.RENT_AMOUNT);
                String currency = contract.get(CONTRACTS.CURRENCY) != null ? contract.get(CONTRACTS.CURRENCY) : "EUR";

                Map<String, Object> contractVars = new HashMap<>();
                contractVars.put("propertyName", propertyName);
                contractVars.put("tenantName", tenantName);
                contractVars.put("rentAmount", currency + " " + rentAmount);
                contractVars.put("startDate", startDate != null ? startDate.toString() : "N/A");
                contractVars.put("endDate", endDate != null ? endDate.toString() : "");
                contractVars.put("baseUrl", baseUrl);

                insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(20, 50)),
                        "CONTRACT_CREATED", "EMAIL", "contract-created", contractVars,
                        adminEmail, null, createdBy, null,
                        "DELIVERED", "delivered", null);
                teamNotifications++;

                // Contract status change notification for non-draft, non-active contracts
                if (!"DRAFT".equals(status) && !"ACTIVE".equals(status)) {
                    Map<String, Object> statusVars = Map.of(
                            "propertyName", propertyName,
                            "tenantName", tenantName,
                            "oldStatus", "ACTIVE",
                            "newStatus", status,
                            "baseUrl", baseUrl
                    );

                    insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(5, 30)),
                            "CONTRACT_STATUS_CHANGED", "EMAIL", "contract-status-changed", statusVars,
                            adminEmail, null, createdBy, null,
                            "DELIVERED", "delivered", null);
                    teamNotifications++;
                }
            }

            // --- Payment reminder notifications (for upcoming/overdue payments) ---
            for (UUID contractId : contractIds) {
                List<UUID> paymentIds = ctx.getPaymentIdsByContract().getOrDefault(contractId, List.of());
                Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
                if (contract == null) {
                    continue;
                }

                UUID tenantId = contract.get(CONTRACTS.TENANT_ID);
                UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
                Record tenant = dsl.selectFrom(TENANTS).where(TENANTS.ID.eq(tenantId)).fetchOne();
                Record property = dsl.selectFrom(PROPERTIES).where(PROPERTIES.ID.eq(propertyId)).fetchOne();
                if (tenant == null || property == null) {
                    continue;
                }

                String tenantName = tenant.get(TENANTS.FIRST_NAME) + " " + tenant.get(TENANTS.LAST_NAME);
                String tenantEmail = tenant.get(TENANTS.EMAIL);
                String tenantPhone = tenant.get(TENANTS.PHONE);
                String propertyName = property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);

                int reminderCount = 0;
                for (UUID paymentId : paymentIds) {
                    if (reminderCount >= 2) {
                        break;
                    }
                    Record payment = dsl.selectFrom(PAYMENTS).where(PAYMENTS.ID.eq(paymentId)).fetchOne();
                    if (payment == null) {
                        continue;
                    }

                    String paymentStatus = payment.get(PAYMENTS.STATUS);
                    if ("PENDING".equals(paymentStatus) || "OVERDUE".equals(paymentStatus)) {
                        String amount = "EUR " + payment.get(PAYMENTS.AMOUNT).toPlainString();
                        String dueDate = payment.get(PAYMENTS.DUE_DATE).toString();

                        Map<String, Object> vars = Map.of(
                                "userName", tenantName,
                                "propertyName", propertyName,
                                "amount", amount,
                                "dueDate", dueDate,
                                "baseUrl", baseUrl
                        );

                        // Email reminder
                        insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(1, 10)),
                                "PAYMENT_REMINDER", "EMAIL", "payment-reminder", vars,
                                tenantEmail, null, null, tenantId,
                                "DELIVERED", "delivered", null);
                        teamNotifications++;

                        // SMS reminder for overdue
                        if ("OVERDUE".equals(paymentStatus) && tenantPhone != null) {
                            insertSmsNotification(teamId, createdBy, now.minusDays(random.nextInt(1, 5)),
                                    "PAYMENT_REMINDER", "payment-reminder", vars,
                                    tenantPhone, null, tenantId,
                                    "SENT", "sent", null);
                            teamNotifications++;
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
                Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
                if (contract == null) {
                    continue;
                }

                UUID tenantId = contract.get(CONTRACTS.TENANT_ID);
                UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
                Record tenant = dsl.selectFrom(TENANTS).where(TENANTS.ID.eq(tenantId)).fetchOne();
                Record property = dsl.selectFrom(PROPERTIES).where(PROPERTIES.ID.eq(propertyId)).fetchOne();
                if (tenant == null || property == null) {
                    continue;
                }

                String tenantName = tenant.get(TENANTS.FIRST_NAME) + " " + tenant.get(TENANTS.LAST_NAME);
                String propertyName = property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);

                for (UUID paymentId : paymentIds) {
                    if (paidCount >= 4) {
                        break;
                    }
                    Record payment = dsl.selectFrom(PAYMENTS).where(PAYMENTS.ID.eq(paymentId)).fetchOne();
                    if (payment == null || !"PAID".equals(payment.get(PAYMENTS.STATUS))) {
                        continue;
                    }

                    String amount = "EUR " + payment.get(PAYMENTS.AMOUNT).toPlainString();
                    var paymentDate = payment.get(PAYMENTS.PAYMENT_DATE);
                    Record admin = dsl.selectFrom(USERS).where(USERS.ID.eq(createdBy)).fetchOne();
                    String adminEmail = admin != null ? admin.get(USERS.EMAIL) : "admin@demo.buurman.io";

                    Map<String, Object> vars = Map.of(
                            "propertyName", propertyName,
                            "tenantName", tenantName,
                            "amount", amount,
                            "paymentDate", paymentDate != null ? paymentDate.toString() : "N/A",
                            "baseUrl", baseUrl
                    );

                    insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(1, 30)),
                            "PAYMENT_PAID", "EMAIL", "payment-paid", vars,
                            adminEmail, null, createdBy, null,
                            "DELIVERED", "delivered", null);
                    teamNotifications++;
                    paidCount++;
                }
            }

            // --- Contract expiry notifications (for contracts expiring soon) ---
            for (UUID contractId : contractIds) {
                Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
                if (contract == null || !"ACTIVE".equals(contract.get(CONTRACTS.STATUS))) {
                    continue;
                }

                var endDate = contract.get(CONTRACTS.END_DATE);
                if (endDate == null) {
                    continue;
                }

                UUID tenantId = contract.get(CONTRACTS.TENANT_ID);
                UUID propertyId = contract.get(CONTRACTS.PROPERTY_ID);
                Record tenant = dsl.selectFrom(TENANTS).where(TENANTS.ID.eq(tenantId)).fetchOne();
                Record property = dsl.selectFrom(PROPERTIES).where(PROPERTIES.ID.eq(propertyId)).fetchOne();
                if (tenant == null || property == null) {
                    continue;
                }

                String propertyName = property.get(PROPERTIES.STREET) + ", " + property.get(PROPERTIES.CITY);
                Record admin = dsl.selectFrom(USERS).where(USERS.ID.eq(createdBy)).fetchOne();
                String adminEmail = admin != null ? admin.get(USERS.EMAIL) : "admin@demo.buurman.io";
                String adminName = admin != null ? admin.get(USERS.FIRST_NAME) : "Admin";
                long daysUntilExpiry = DAYS.between(LocalDate.now(clock), endDate);

                Map<String, Object> vars = Map.of(
                        "userName", adminName,
                        "propertyName", propertyName,
                        "daysUntilExpiry", daysUntilExpiry,
                        "expiryDate", endDate.toString(),
                        "baseUrl", baseUrl
                );

                insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(1, 14)),
                        "CONTRACT_EXPIRY", "EMAIL", "contract-expiry", vars,
                        adminEmail, null, createdBy, null,
                        "DELIVERED", "delivered", null);
                teamNotifications++;
            }

            // --- A couple of failed notifications for realism ---
            if (!tenantIds.isEmpty()) {
                UUID tenantId = tenantIds.get(random.nextInt(tenantIds.size()));
                Record tenant = dsl.selectFrom(TENANTS).where(TENANTS.ID.eq(tenantId)).fetchOne();
                if (tenant != null) {
                    String tenantEmail = tenant.get(TENANTS.EMAIL);
                    String tenantName = tenant.get(TENANTS.FIRST_NAME) + " " + tenant.get(TENANTS.LAST_NAME);

                    // Find a property for this tenant via contract
                    String propertyName = "your property";
                    var tenantContract = dsl.selectFrom(CONTRACTS)
                            .where(CONTRACTS.TENANT_ID.eq(tenantId).and(CONTRACTS.TEAM_ID.eq(teamId)))
                            .fetchAny();
                    if (tenantContract != null) {
                        Record prop = dsl.selectFrom(PROPERTIES)
                                .where(PROPERTIES.ID.eq(tenantContract.get(CONTRACTS.PROPERTY_ID)))
                                .fetchOne();
                        if (prop != null) {
                            propertyName = prop.get(PROPERTIES.STREET) + ", " + prop.get(PROPERTIES.CITY);
                        }
                    }

                    Map<String, Object> vars = Map.of(
                            "userName", tenantName,
                            "propertyName", propertyName,
                            "amount", "EUR 1200.00",
                            "dueDate", LocalDate.now(clock).minusDays(15).toString(),
                            "baseUrl", baseUrl
                    );

                    // Bounced email
                    insertRenderedNotification(teamId, createdBy, now.minusDays(random.nextInt(5, 20)),
                            "PAYMENT_REMINDER", "EMAIL", "payment-reminder", vars,
                            tenantEmail, null, null, tenantId,
                            "BOUNCED", null, "550 5.1.1 The email account does not exist");
                    teamNotifications++;

                    // Failed SMS
                    insertSmsNotification(teamId, createdBy, now.minusDays(random.nextInt(3, 15)),
                            "PAYMENT_REMINDER", "payment-reminder", vars,
                            "+31600000000", null, tenantId,
                            "FAILED", null, "Invalid phone number");
                    teamNotifications++;
                }
            }

            ctx.incrementNotifications(teamNotifications);
            log.info("Created {} notifications for team {}", teamNotifications, teamKey);
        }
    }

    /**
     * Renders the Thymeleaf email template, derives the subject, and inserts the notification.
     */
    private void insertRenderedNotification(UUID teamId, UUID createdBy, LocalDateTime createdAt,
                                            String type, String channel, String template,
                                            Map<String, Object> templateVars,
                                            String recipientEmail, String recipientPhone,
                                            UUID recipientUserId, UUID recipientTenantId,
                                            String status, String providerStatus, String providerError) {
        String subject = deriveSubject(template, templateVars);
        String body = renderTemplate(template, templateVars);
        String varsJson = toJson(templateVars);

        insertNotification(teamId, createdBy, createdAt, type, channel, template,
                subject, body, recipientEmail, recipientPhone, recipientUserId, recipientTenantId,
                varsJson, status, providerStatus, providerError);
    }

    /**
     * Inserts an SMS notification with a simple text body (no Thymeleaf rendering).
     */
    private void insertSmsNotification(UUID teamId, UUID createdBy, LocalDateTime createdAt,
                                       String type, String template,
                                       Map<String, Object> templateVars,
                                       String recipientPhone,
                                       UUID recipientUserId, UUID recipientTenantId,
                                       String status, String providerStatus, String providerError) {
        String smsBody = renderSmsBody(template, templateVars);
        String varsJson = toJson(templateVars);

        insertNotification(teamId, createdBy, createdAt, type, "SMS", template,
                null, smsBody, null, recipientPhone, recipientUserId, recipientTenantId,
                varsJson, status, providerStatus, providerError);
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        return templateEngine.process("email/" + templateName, context);
    }

    private String deriveSubject(String templateName, Map<String, Object> variables) {
        return switch (templateName) {
            case "welcome" -> "Welcome to Buurman!";
            case "payment-reminder" -> "Payment reminder for " + getVar(variables, "propertyName", "your property");
            case "contract-expiry" -> "Contract expiring soon for " + getVar(variables, "propertyName", "your property");
            case "property-created" -> "Property created: " + getVar(variables, "propertyName", "New property");
            case "contract-created" -> "New contract for " + getVar(variables, "propertyName", "your property");
            case "contract-status-changed" -> "Contract status changed to " + getVar(variables, "newStatus", "updated");
            case "payment-paid" -> "Payment marked as paid for " + getVar(variables, "propertyName", "your property");
            default -> "Notification from Buurman";
        };
    }

    private String renderSmsBody(String templateName, Map<String, Object> variables) {
        return switch (templateName) {
            case "payment-reminder" ->
                    "Buurman: Your rent payment of " + getVar(variables, "amount", "N/A")
                            + " (due " + getVar(variables, "dueDate", "N/A")
                            + ") is overdue. Please arrange payment.";
            default -> "Buurman: You have a new notification.";
        };
    }

    private String getVar(Map<String, Object> variables, String key, String defaultValue) {
        if (variables == null) {
            return defaultValue;
        }
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

    private void insertNotification(UUID teamId, UUID createdBy, LocalDateTime createdAt,
                                    String type, String channel, String template,
                                    String subject, String body,
                                    String recipientEmail, String recipientPhone,
                                    UUID recipientUserId, UUID recipientTenantId,
                                    String contentVariablesJson,
                                    String status, String providerStatus, String providerError) {
        LocalDateTime statusUpdatedAt = createdAt.plusMinutes(random.nextInt(1, 30));

        dsl.insertInto(NOTIFICATIONS)
                .set(NOTIFICATIONS.ID, UUID.randomUUID())
                .set(NOTIFICATIONS.IDENTIFIER, newNotificationId().value())
                .set(NOTIFICATIONS.TEAM_ID, teamId)
                .set(NOTIFICATIONS.NOTIFICATION_TYPE, type)
                .set(NOTIFICATIONS.CHANNEL, channel)
                .set(NOTIFICATIONS.CONTENT_TEMPLATE, template)
                .set(NOTIFICATIONS.SUBJECT, subject)
                .set(NOTIFICATIONS.BODY, body)
                .set(NOTIFICATIONS.RECIPIENT_EMAIL, recipientEmail)
                .set(NOTIFICATIONS.RECIPIENT_PHONE, recipientPhone)
                .set(NOTIFICATIONS.RECIPIENT_USER_ID, recipientUserId)
                .set(NOTIFICATIONS.RECIPIENT_TENANT_ID, recipientTenantId)
                .set(NOTIFICATIONS.CONTENT_VARIABLES, contentVariablesJson != null ? JSONB.jsonb(contentVariablesJson) : null)
                .set(NOTIFICATIONS.STATUS, status)
                .set(NOTIFICATIONS.PROVIDER_STATUS, providerStatus)
                .set(NOTIFICATIONS.PROVIDER_ERROR, providerError)
                .set(NOTIFICATIONS.STATUS_UPDATED_AT, statusUpdatedAt)
                .set(NOTIFICATIONS.CREATED_AT, createdAt)
                .set(NOTIFICATIONS.CREATED_BY, createdBy)
                .execute();
    }
}
