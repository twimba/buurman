package com.buurman.service.demo;

import com.buurman.domain.Document;
import com.buurman.repository.DocumentRepository;
import com.buurman.service.S3StorageService;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import static com.buurman.jooq.generated.Tables.*;

@Component
public class DemoDocumentGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoDocumentGenerator.class);
    private static final String RESOURCE_PATH = "classpath:demo/documents/";

    private final DSLContext dsl;
    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final Random random = new Random(42);

    /** PDF files loaded from classpath, keyed by filename. */
    private final Map<String, byte[]> pdfPool;

    private record DocTemplate(String title, String pdfResource, String notes) {}

    private static final List<DocTemplate> PROPERTY_DOCS = List.of(
            new DocTemplate("Building insurance policy", "insurance-certificate.pdf", "Annual building insurance certificate"),
            new DocTemplate("Energy performance certificate", "energy-certificate.pdf", "EPC rating document"),
            new DocTemplate("Floor plan", "generic-form.pdf", null),
            new DocTemplate("Property tax assessment", "tax-assessment.pdf", "WOZ value assessment for current year"),
            new DocTemplate("Fire safety certificate", "insurance-cert-2.pdf", "Annual fire safety inspection report"),
            new DocTemplate("Asbestos inspection report", "condition-report.pdf", "Pre-renovation asbestos inspection")
    );

    private static final List<DocTemplate> TENANT_DOCS = List.of(
            new DocTemplate("ID verification", "generic-form.pdf", "Copy of passport/ID for tenant verification"),
            new DocTemplate("Proof of income", "invoice-template.pdf", "Recent payslips or employment letter"),
            new DocTemplate("Previous landlord reference", "generic-form.pdf", "Reference letter from previous landlord"),
            new DocTemplate("Bank statement", "generic-form.pdf", "3-month bank statement for affordability check"),
            new DocTemplate("Rental application", "rental-application.pdf", "Completed rental application form")
    );

    private static final List<DocTemplate> CONTRACT_DOCS = List.of(
            new DocTemplate("Signed rental agreement", "lease-agreement.pdf", "Fully executed rental agreement"),
            new DocTemplate("Property condition report", "condition-report.pdf", "Move-in property condition report with photos"),
            new DocTemplate("Deposit receipt", "deposit-receipt.pdf", "Confirmation of security deposit payment"),
            new DocTemplate("House rules", "house-rules.pdf", "Building rules and regulations for tenant")
    );

    private static final List<DocTemplate> EXPENSE_DOCS = List.of(
            new DocTemplate("Invoice", "invoice-template.pdf", "Vendor invoice"),
            new DocTemplate("Receipt", "deposit-receipt.pdf", "Payment receipt from vendor"),
            new DocTemplate("Repair estimate", "invoice-template.pdf", "Cost estimate before repair work")
    );

    public DemoDocumentGenerator(DSLContext dsl, DocumentRepository documentRepository, S3StorageService s3StorageService) {
        this.dsl = dsl;
        this.documentRepository = documentRepository;
        this.s3StorageService = s3StorageService;
        this.pdfPool = loadPdfPool();
    }

    public void generate(DemoDataContext ctx) {
        if (pdfPool.isEmpty()) {
            log.warn("No demo PDFs found on classpath — skipping document generation");
            return;
        }

        for (var teamEntry : ctx.getTeamIds().entrySet()) {
            String teamKey = teamEntry.getKey();
            UUID teamId = teamEntry.getValue();
            UUID uploadedBy = ctx.getAdminUserForTeam(teamKey);
            int teamDocuments = 0;

            List<UUID> propertyIds = ctx.getPropertyIdsByTeam().getOrDefault(teamId, List.of());
            List<UUID> tenantIds = ctx.getTenantIdsByTeam().getOrDefault(teamId, List.of());
            List<UUID> contractIds = ctx.getContractIdsByTeam().getOrDefault(teamId, List.of());

            // Property documents (2-3 per property)
            for (UUID propertyId : propertyIds) {
                String street = fetchPropertyStreet(propertyId);
                String prefix = street != null ? slugify(street) : "property";
                for (DocTemplate doc : pickRandom(PROPERTY_DOCS, random.nextInt(2, 4))) {
                    if (uploadDocument(teamId, uploadedBy, "PROPERTY", propertyId,
                            doc.title, prefix + "-" + doc.pdfResource, doc.pdfResource, doc.notes)) {
                        teamDocuments++;
                    }
                }
            }

            // Tenant documents (1-2 per tenant)
            for (UUID tenantId : tenantIds) {
                String tenantName = fetchTenantName(tenantId);
                String prefix = tenantName != null ? slugify(tenantName) : "tenant";
                for (DocTemplate doc : pickRandom(TENANT_DOCS, random.nextInt(1, 3))) {
                    if (uploadDocument(teamId, uploadedBy, "TENANT", tenantId,
                            doc.title, prefix + "-" + doc.pdfResource, doc.pdfResource, doc.notes)) {
                        teamDocuments++;
                    }
                }
            }

            // Contract documents (2-3 per active contract, 1 for DRAFT)
            for (UUID contractId : contractIds) {
                Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
                if (contract == null) continue;

                String status = contract.get(CONTRACTS.STATUS);
                if ("DRAFT".equals(status)) {
                    if (uploadDocument(teamId, uploadedBy, "CONTRACT", contractId,
                            "Draft rental agreement", "draft-rental-agreement.pdf",
                            "hud-model-lease.pdf", "Unsigned draft for review")) {
                        teamDocuments++;
                    }
                    continue;
                }

                for (DocTemplate doc : pickRandom(CONTRACT_DOCS, random.nextInt(2, 4))) {
                    if (uploadDocument(teamId, uploadedBy, "CONTRACT", contractId,
                            doc.title, doc.pdfResource, doc.pdfResource, doc.notes)) {
                        teamDocuments++;
                    }
                }
            }

            // Expense documents (~50% of expenses)
            var expenseRecords = dsl.select(EXPENSES.ID, EXPENSES.DESCRIPTION, EXPENSES.CATEGORY)
                    .from(EXPENSES)
                    .where(EXPENSES.TEAM_ID.eq(teamId))
                    .fetch();

            for (var expense : expenseRecords) {
                if (random.nextBoolean()) continue;

                UUID expenseId = expense.get(EXPENSES.ID);
                String category = expense.get(EXPENSES.CATEGORY);
                String description = expense.get(EXPENSES.DESCRIPTION);
                DocTemplate doc = EXPENSE_DOCS.get(random.nextInt(EXPENSE_DOCS.size()));
                String prefix = slugify(category.toLowerCase());

                if (uploadDocument(teamId, uploadedBy, "EXPENSE", expenseId,
                        doc.title + " — " + description,
                        prefix + "-" + doc.pdfResource, doc.pdfResource, doc.notes)) {
                    teamDocuments++;
                }
            }

            ctx.incrementDocuments(teamDocuments);
            log.info("Created {} documents for team {}", teamDocuments, teamKey);
        }
    }

    private boolean uploadDocument(UUID teamId, UUID uploadedBy, String entityType, UUID entityId,
                                   String title, String fileName, String pdfResource, String notes) {
        byte[] pdfData = pdfPool.getOrDefault(pdfResource, pdfPool.values().iterator().next());

        try {
            String fileKey = s3StorageService.uploadFile(
                    pdfData, "application/pdf", teamId, entityType, entityId, fileName);

            Document document = new Document();
            document.setTeamId(teamId);
            document.setEntityType(entityType);
            document.setEntityId(entityId);
            document.setFileKey(fileKey);
            document.setFileName(fileName);
            document.setFileSize((long) pdfData.length);
            document.setMimeType("application/pdf");
            document.setTitle(title);
            document.setNotes(notes);
            document.setUploadedBy(uploadedBy);

            documentRepository.save(document);
            return true;
        } catch (Exception e) {
            log.warn("Failed to upload document '{}' for {} {}: {}", title, entityType, entityId, e.getMessage());
            return false;
        }
    }

    // --- helpers ---

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

    private String slugify(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static Map<String, byte[]> loadPdfPool() {
        Map<String, byte[]> pool = new LinkedHashMap<>();
        var resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(RESOURCE_PATH + "*.pdf");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null) {
                    pool.put(filename, resource.getContentAsByteArray());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load demo PDFs from classpath: {}", e.getMessage());
        }
        log.info("Loaded {} demo PDFs from classpath", pool.size());
        return pool;
    }
}
