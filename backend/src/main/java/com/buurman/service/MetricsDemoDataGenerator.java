package com.buurman.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;

/**
 * Generates simulated metric data for dashboard testing.
 * Only active when the "metrics-demo" profile is enabled.
 */
@Component
@Profile("metrics-demo")
public class MetricsDemoDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(MetricsDemoDataGenerator.class);

    private final MetricsService metricsService;
    private final Random random = new Random();

    private static final String[] CURRENCIES = {"EUR", "USD", "GBP"};
    private static final String[] PAYMENT_STATUSES = {"PENDING", "PAID", "OVERDUE", "PARTIALLY_PAID"};
    private static final String[] CONTRACT_STATUSES = {"DRAFT", "ACTIVE", "TERMINATED", "EXPIRED"};
    private static final String[] ENTITY_TYPES = {"PROPERTY", "TENANT", "CONTRACT", "PAYMENT", "EXPENSE"};
    private static final String[] EMAIL_TEMPLATES = {"welcome", "team-invitation", "invitation-accepted", "payment-reminder", "contract-expiry"};
    private static final String[] EXPORT_TYPES = {"transaction_csv", "transaction_pdf", "property_brochure", "contract_report", "tenant_report"};
    private static final String[] S3_OPERATIONS = {"upload", "download", "delete", "presign"};
    private static final String[] AUDIT_ACTIONS = {"CREATE", "UPDATE", "DELETE"};
    private static final String[] RECEIVAL_ACTIONS = {"registered", "updated", "deleted"};

    public MetricsDemoDataGenerator(MetricsService metricsService) {
        this.metricsService = metricsService;
        log.info("MetricsDemoDataGenerator initialized - generating simulated metrics");
    }

    @Scheduled(fixedRate = 5000)
    public void generatePaymentMetrics() {
        if (random.nextDouble() < 0.3) {
            metricsService.incrementCounter("payment.total");
            String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
            double amount = 500 + random.nextDouble() * 2500;
            metricsService.recordHistogram("payment.amount", amount,
                    "currency", currency, "status", "PENDING");
        }

        if (random.nextDouble() < 0.2) {
            metricsService.incrementCounter("payment.marked.paid.total");
            String fromStatus = random.nextBoolean() ? "PENDING" : "OVERDUE";
            metricsService.incrementCounter("payment.status.changed.total",
                    "from_status", fromStatus, "to_status", "PAID");
        }

        if (random.nextDouble() < 0.15) {
            metricsService.incrementCounter("payment.receival.total",
                    "action", RECEIVAL_ACTIONS[random.nextInt(RECEIVAL_ACTIONS.length)]);
        }

        if (random.nextDouble() < 0.05) {
            int count = 5 + random.nextInt(20);
            metricsService.incrementCounter("payment.bulk.generated.total");
            metricsService.recordHistogram("payment.bulk.generated.count", count);
        }
    }

    @Scheduled(fixedRate = 8000)
    public void generateContractMetrics() {
        if (random.nextDouble() < 0.15) {
            metricsService.incrementCounter("contract.total");
            double rent = 800 + random.nextDouble() * 3000;
            metricsService.recordHistogram("contract.rent.amount", rent, "currency", "EUR");
        }

        if (random.nextDouble() < 0.1) {
            String from = CONTRACT_STATUSES[random.nextInt(2)];
            String to = CONTRACT_STATUSES[1 + random.nextInt(CONTRACT_STATUSES.length - 1)];
            metricsService.incrementCounter("contract.status.changed.total",
                    "from_status", from, "to_status", to);
        }

        if (random.nextDouble() < 0.05) {
            metricsService.incrementCounter("contract.duplicated.total");
        }
    }

    @Scheduled(fixedRate = 10000)
    public void generatePropertyAndTenantMetrics() {
        if (random.nextDouble() < 0.1) {
            metricsService.incrementCounter("property.total");
        }

        if (random.nextDouble() < 0.12) {
            metricsService.incrementCounter("tenant.total");
        }

        if (random.nextDouble() < 0.08) {
            metricsService.incrementCounter("tenant.linked.total");
        }

        if (random.nextDouble() < 0.05) {
            metricsService.incrementCounter("tenant.unlinked.total");
        }
    }

    @Scheduled(fixedRate = 6000)
    public void generateDocumentMetrics() {
        if (random.nextDouble() < 0.25) {
            String entityType = ENTITY_TYPES[random.nextInt(ENTITY_TYPES.length)];
            String category = random.nextBoolean() ? "PHOTO" : "DOCUMENT";
            metricsService.incrementCounter("document.upload.total",
                    "entity_type", entityType, "category", category);
            long size = 50000 + random.nextLong(5000000);
            metricsService.recordHistogram("document.upload.bytes", size,
                    "entity_type", entityType);
        }

        if (random.nextDouble() < 0.3) {
            metricsService.incrementCounter("document.download.total",
                    "entity_type", ENTITY_TYPES[random.nextInt(ENTITY_TYPES.length)]);
        }

        if (random.nextDouble() < 0.05) {
            metricsService.incrementCounter("document.bulk.download.total");
        }
    }

    @Scheduled(fixedRate = 7000)
    public void generateS3Metrics() {
        String operation = S3_OPERATIONS[random.nextInt(S3_OPERATIONS.length)];
        String result = random.nextDouble() < 0.95 ? "success" : "failure";
        long durationMs = 20 + random.nextLong(500);
        metricsService.recordTimer("s3.operation.seconds",
                Duration.ofMillis(durationMs),
                "operation", operation, "result", result);
        metricsService.incrementCounter("s3.operation.total",
                "operation", operation, "result", result);
    }

    @Scheduled(fixedRate = 12000)
    public void generateEmailMetrics() {
        if (random.nextDouble() < 0.3) {
            String template = EMAIL_TEMPLATES[random.nextInt(EMAIL_TEMPLATES.length)];
            String result = random.nextDouble() < 0.9 ? "success" : "failure";
            long durationMs = 100 + random.nextLong(2000);
            metricsService.incrementCounter("email.sent.total",
                    "template", template, "result", result);
            metricsService.recordTimer("email.send.seconds",
                    Duration.ofMillis(durationMs),
                    "template", template);
        }
    }

    @Scheduled(fixedRate = 15000)
    public void generateExportMetrics() {
        if (random.nextDouble() < 0.2) {
            String type = EXPORT_TYPES[random.nextInt(EXPORT_TYPES.length)];
            String result = random.nextDouble() < 0.95 ? "success" : "failure";
            long durationMs = 500 + random.nextLong(5000);
            metricsService.recordTimer("export.generation.seconds",
                    Duration.ofMillis(durationMs),
                    "type", type, "result", result);
            metricsService.incrementCounter("export.generation.total",
                    "type", type, "result", result);
            if ("success".equals(result)) {
                long size = 10000 + random.nextLong(500000);
                metricsService.recordHistogram("export.size.bytes", size, "type", type);
            }
        }
    }

    @Scheduled(fixedRate = 4000)
    public void generateAuditMetrics() {
        String entityType = ENTITY_TYPES[random.nextInt(ENTITY_TYPES.length)];
        String action = AUDIT_ACTIONS[random.nextInt(AUDIT_ACTIONS.length)];
        metricsService.incrementCounter("audit.log.total",
                "entity_type", entityType, "action", action);
    }

    @Scheduled(fixedRate = 20000)
    public void generateTeamMetrics() {
        if (random.nextDouble() < 0.1) {
            metricsService.incrementCounter("team.registered.total");
        }
    }
}
