package com.buurman.service.demo;

import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

import static com.buurman.jooq.generated.Tables.*;

@Component
public class DemoDocumentGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoDocumentGenerator.class);

    private final DSLContext dsl;
    private final Random random = new Random(42);

    // Document templates per entity type
    private record DocTemplate(String title, String fileName, String mimeType, long minSize, long maxSize, String notes) {}

    private static final List<DocTemplate> PROPERTY_DOCS = List.of(
            new DocTemplate("Building insurance policy", "building-insurance-policy.pdf", "application/pdf", 150_000, 800_000, "Annual building insurance certificate"),
            new DocTemplate("Energy performance certificate", "energy-certificate.pdf", "application/pdf", 200_000, 500_000, "EPC rating document"),
            new DocTemplate("Floor plan", "floor-plan.pdf", "application/pdf", 500_000, 2_000_000, null),
            new DocTemplate("Property tax assessment", "property-tax-assessment.pdf", "application/pdf", 100_000, 400_000, "WOZ value assessment for current year"),
            new DocTemplate("Fire safety certificate", "fire-safety-certificate.pdf", "application/pdf", 80_000, 300_000, "Annual fire safety inspection report"),
            new DocTemplate("Asbestos inspection report", "asbestos-report.pdf", "application/pdf", 300_000, 1_200_000, "Pre-renovation asbestos inspection")
    );

    private static final List<DocTemplate> TENANT_DOCS = List.of(
            new DocTemplate("ID verification", "id-verification.pdf", "application/pdf", 200_000, 600_000, "Copy of passport/ID for tenant verification"),
            new DocTemplate("Proof of income", "income-proof.pdf", "application/pdf", 100_000, 400_000, "Recent payslips or employment letter"),
            new DocTemplate("Previous landlord reference", "landlord-reference.pdf", "application/pdf", 50_000, 200_000, "Reference letter from previous landlord"),
            new DocTemplate("Bank statement", "bank-statement.pdf", "application/pdf", 150_000, 500_000, "3-month bank statement for affordability check")
    );

    private static final List<DocTemplate> CONTRACT_DOCS = List.of(
            new DocTemplate("Signed rental agreement", "rental-agreement-signed.pdf", "application/pdf", 300_000, 1_500_000, "Fully executed rental agreement"),
            new DocTemplate("Property condition report", "condition-report.pdf", "application/pdf", 500_000, 3_000_000, "Move-in property condition report with photos"),
            new DocTemplate("Deposit receipt", "deposit-receipt.pdf", "application/pdf", 50_000, 150_000, "Confirmation of security deposit payment"),
            new DocTemplate("House rules", "house-rules.pdf", "application/pdf", 40_000, 120_000, "Building rules and regulations for tenant")
    );

    private static final List<DocTemplate> EXPENSE_DOCS = List.of(
            new DocTemplate("Invoice", "invoice.pdf", "application/pdf", 80_000, 400_000, "Vendor invoice"),
            new DocTemplate("Receipt", "receipt.pdf", "application/pdf", 30_000, 150_000, "Payment receipt from vendor"),
            new DocTemplate("Repair estimate", "repair-estimate.pdf", "application/pdf", 60_000, 250_000, "Cost estimate before repair work")
    );

    public DemoDocumentGenerator(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now();

        for (var teamEntry : ctx.getTeamIds().entrySet()) {
            String teamKey = teamEntry.getKey();
            UUID teamId = teamEntry.getValue();
            UUID uploadedBy = ctx.getAdminUserForTeam(teamKey);

            List<UUID> propertyIds = ctx.getPropertyIdsByTeam().getOrDefault(teamId, List.of());
            List<UUID> tenantIds = ctx.getTenantIdsByTeam().getOrDefault(teamId, List.of());
            List<UUID> contractIds = ctx.getContractIdsByTeam().getOrDefault(teamId, List.of());

            int teamDocuments = 0;

            // --- Property documents (2-3 per property) ---
            for (UUID propertyId : propertyIds) {
                List<DocTemplate> selected = pickRandom(PROPERTY_DOCS, random.nextInt(2, 4));
                for (DocTemplate doc : selected) {
                    String street = fetchPropertyStreet(propertyId);
                    String prefix = street != null ? slugify(street) : "property";
                    insertDocument(teamId, uploadedBy, "PROPERTY", propertyId,
                            now.minusDays(random.nextInt(30, 365)),
                            doc.title, prefix + "-" + doc.fileName, doc.mimeType,
                            randomSize(doc.minSize, doc.maxSize), doc.notes);
                    teamDocuments++;
                }
            }

            // --- Tenant documents (1-2 per tenant) ---
            for (UUID tenantId : tenantIds) {
                List<DocTemplate> selected = pickRandom(TENANT_DOCS, random.nextInt(1, 3));
                for (DocTemplate doc : selected) {
                    String tenantName = fetchTenantName(tenantId);
                    String prefix = tenantName != null ? slugify(tenantName) : "tenant";
                    insertDocument(teamId, uploadedBy, "TENANT", tenantId,
                            now.minusDays(random.nextInt(30, 180)),
                            doc.title, prefix + "-" + doc.fileName, doc.mimeType,
                            randomSize(doc.minSize, doc.maxSize), doc.notes);
                    teamDocuments++;
                }
            }

            // --- Contract documents (2-3 per contract, except DRAFT) ---
            for (UUID contractId : contractIds) {
                Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
                if (contract == null) continue;

                String status = contract.get(CONTRACTS.STATUS);
                if ("DRAFT".equals(status)) {
                    // Draft contracts only get the unsigned agreement
                    insertDocument(teamId, uploadedBy, "CONTRACT", contractId,
                            now.minusDays(random.nextInt(1, 14)),
                            "Draft rental agreement", "draft-rental-agreement.pdf", "application/pdf",
                            randomSize(250_000, 1_000_000), "Unsigned draft for review");
                    teamDocuments++;
                    continue;
                }

                List<DocTemplate> selected = pickRandom(CONTRACT_DOCS, random.nextInt(2, 4));
                for (DocTemplate doc : selected) {
                    insertDocument(teamId, uploadedBy, "CONTRACT", contractId,
                            now.minusDays(random.nextInt(14, 200)),
                            doc.title, doc.fileName, doc.mimeType,
                            randomSize(doc.minSize, doc.maxSize), doc.notes);
                    teamDocuments++;
                }
            }

            // --- Expense documents (invoice/receipt for ~50% of expenses) ---
            var expenseRecords = dsl.select(EXPENSES.ID, EXPENSES.DESCRIPTION, EXPENSES.CATEGORY)
                    .from(EXPENSES)
                    .where(EXPENSES.TEAM_ID.eq(teamId))
                    .fetch();

            for (var expense : expenseRecords) {
                if (random.nextBoolean()) continue; // ~50% have documents

                UUID expenseId = expense.get(EXPENSES.ID);
                String category = expense.get(EXPENSES.CATEGORY);
                String description = expense.get(EXPENSES.DESCRIPTION);

                DocTemplate doc = EXPENSE_DOCS.get(random.nextInt(EXPENSE_DOCS.size()));
                String prefix = slugify(category.toLowerCase());

                insertDocument(teamId, uploadedBy, "EXPENSE", expenseId,
                        now.minusDays(random.nextInt(7, 180)),
                        doc.title + " — " + description,
                        prefix + "-" + doc.fileName, doc.mimeType,
                        randomSize(doc.minSize, doc.maxSize), doc.notes);
                teamDocuments++;
            }

            ctx.incrementDocuments(teamDocuments);
            log.info("Created {} documents for team {}", teamDocuments, teamKey);
        }
    }

    private void insertDocument(UUID teamId, UUID uploadedBy, String entityType, UUID entityId,
                                LocalDateTime uploadedAt, String title, String fileName,
                                String mimeType, long fileSize, String notes) {
        String fileKey = "demo/" + entityType.toLowerCase() + "/" + entityId + "/" + fileName;

        dsl.insertInto(DOCUMENTS)
                .set(DOCUMENTS.ID, UUID.randomUUID())
                .set(DOCUMENTS.IDENTIFIER, UlidGenerator.generate(EntityPrefix.DOC))
                .set(DOCUMENTS.TEAM_ID, teamId)
                .set(DOCUMENTS.ENTITY_TYPE, entityType)
                .set(DOCUMENTS.ENTITY_ID, entityId)
                .set(DOCUMENTS.FILE_KEY, fileKey)
                .set(DOCUMENTS.FILE_NAME, fileName)
                .set(DOCUMENTS.FILE_SIZE, fileSize)
                .set(DOCUMENTS.MIME_TYPE, mimeType)
                .set(DOCUMENTS.TITLE, title)
                .set(DOCUMENTS.NOTES, notes)
                .set(DOCUMENTS.UPLOADED_BY, uploadedBy)
                .set(DOCUMENTS.UPLOADED_AT, uploadedAt)
                .execute();
    }

    private String fetchPropertyStreet(UUID propertyId) {
        return dsl.select(PROPERTIES.STREET).from(PROPERTIES)
                .where(PROPERTIES.ID.eq(propertyId))
                .fetchOne(PROPERTIES.STREET);
    }

    private String fetchTenantName(UUID tenantId) {
        Record r = dsl.select(TENANTS.FIRST_NAME, TENANTS.LAST_NAME).from(TENANTS)
                .where(TENANTS.ID.eq(tenantId))
                .fetchOne();
        return r != null ? r.get(TENANTS.FIRST_NAME) + " " + r.get(TENANTS.LAST_NAME) : null;
    }

    private List<DocTemplate> pickRandom(List<DocTemplate> templates, int count) {
        List<DocTemplate> shuffled = new ArrayList<>(templates);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private long randomSize(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min));
    }

    private String slugify(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
