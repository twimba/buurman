package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static com.buurman.jooq.generated.Tables.TENANTS;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.buurman.domain.Document;
import com.buurman.repository.DocumentRepository;
import com.buurman.service.S3StorageService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemoDocumentGenerator {

  private static final String RESOURCE_PATH = "classpath:demo/documents/";

  private final DSLContext dsl;
  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final Clock clock;
  private final Random random = new Random(42);

  /** PDF files loaded from classpath, keyed by filename. */
  private final Map<String, byte[]> pdfPool;

  private record DocTemplate(String title, String pdfResource, @Nullable String notes) {}

  // --- Residential property documents ---
  private static final List<DocTemplate> RESIDENTIAL_PROPERTY_DOCS =
      List.of(
          new DocTemplate(
              "Building insurance policy",
              "insurance-certificate.pdf",
              "Annual building insurance certificate"),
          new DocTemplate(
              "Energy performance certificate", "energy-certificate.pdf", "EPC rating document"),
          new DocTemplate("Floor plan", "generic-form.pdf", null),
          new DocTemplate(
              "Property tax assessment",
              "tax-assessment.pdf",
              "WOZ value assessment for current year"),
          new DocTemplate(
              "Fire safety certificate",
              "insurance-cert-2.pdf",
              "Annual fire safety inspection report"),
          new DocTemplate(
              "Asbestos inspection report",
              "condition-report.pdf",
              "Pre-renovation asbestos inspection"),
          new DocTemplate(
              "Annual maintenance report",
              "condition-report.pdf",
              "Year-end property inspection and maintenance summary"),
          new DocTemplate(
              "Tenant handbook",
              "house-rules.pdf",
              "Welcome guide and building information for tenants"));

  // --- Commercial property documents ---
  private static final List<DocTemplate> COMMERCIAL_PROPERTY_DOCS =
      List.of(
          new DocTemplate(
              "Commercial lease agreement", "lease-agreement.pdf", "Standard commercial lease"),
          new DocTemplate(
              "Fire safety certificate",
              "insurance-cert-2.pdf",
              "Annual fire safety inspection report"),
          new DocTemplate("Building permit", "generic-form.pdf", "Municipal building permit"),
          new DocTemplate("Floor plan", "generic-form.pdf", "Office/retail floor layout"),
          new DocTemplate(
              "Environmental assessment",
              "condition-report.pdf",
              "Environmental impact assessment"),
          new DocTemplate(
              "Health and safety assessment",
              "condition-report.pdf",
              "Workplace health and safety inspection report"));

  // --- Industrial property documents ---
  private static final List<DocTemplate> INDUSTRIAL_PROPERTY_DOCS =
      List.of(
          new DocTemplate("Warehouse lease", "lease-agreement.pdf", "Industrial lease agreement"),
          new DocTemplate(
              "Environmental compliance certificate",
              "insurance-certificate.pdf",
              "Environmental compliance documentation"),
          new DocTemplate(
              "Structural inspection report",
              "condition-report.pdf",
              "Annual structural inspection"),
          new DocTemplate(
              "Loading dock specifications",
              "generic-form.pdf",
              "Loading dock dimensions and capacity"));

  // --- Agricultural property documents ---
  private static final List<DocTemplate> AGRICULTURAL_PROPERTY_DOCS =
      List.of(
          new DocTemplate(
              "Agricultural lease agreement", "lease-agreement.pdf", "Agricultural land lease"),
          new DocTemplate(
              "Soil analysis report", "condition-report.pdf", "Annual soil quality analysis"),
          new DocTemplate(
              "Water rights documentation",
              "generic-form.pdf",
              "Water extraction and irrigation rights"),
          new DocTemplate("Land survey", "generic-form.pdf", "Cadastral land survey report"));

  // --- Individual tenant documents ---
  private static final List<DocTemplate> INDIVIDUAL_TENANT_DOCS =
      List.of(
          new DocTemplate(
              "ID verification", "generic-form.pdf", "Copy of passport/ID for tenant verification"),
          new DocTemplate(
              "Proof of income", "invoice-template.pdf", "Recent payslips or employment letter"),
          new DocTemplate(
              "Previous landlord reference",
              "generic-form.pdf",
              "Reference letter from previous landlord"),
          new DocTemplate(
              "Bank statement",
              "generic-form.pdf",
              "3-month bank statement for affordability check"),
          new DocTemplate(
              "Rental application", "rental-application.pdf", "Completed rental application form"));

  // --- Business tenant documents ---
  private static final List<DocTemplate> BUSINESS_TENANT_DOCS =
      List.of(
          new DocTemplate(
              "Chamber of Commerce registration",
              "generic-form.pdf",
              "KvK extract or equivalent company registration"),
          new DocTemplate(
              "Company financial statements",
              "invoice-template.pdf",
              "Annual financial statements"),
          new DocTemplate(
              "VAT registration certificate",
              "tax-assessment.pdf",
              "VAT/BTW registration documentation"),
          new DocTemplate(
              "Director ID verification",
              "generic-form.pdf",
              "ID verification of company director"));

  private static final List<DocTemplate> CONTRACT_DOCS =
      List.of(
          new DocTemplate(
              "Signed rental agreement", "lease-agreement.pdf", "Fully executed rental agreement"),
          new DocTemplate(
              "Property condition report",
              "condition-report.pdf",
              "Move-in property condition report with photos"),
          new DocTemplate(
              "Deposit receipt", "deposit-receipt.pdf", "Confirmation of security deposit payment"),
          new DocTemplate(
              "House rules", "house-rules.pdf", "Building rules and regulations for tenant"));

  private static final List<DocTemplate> EXPENSE_DOCS =
      List.of(
          new DocTemplate("Invoice", "invoice-template.pdf", "Vendor invoice"),
          new DocTemplate("Receipt", "deposit-receipt.pdf", "Payment receipt from vendor"),
          new DocTemplate(
              "Repair estimate", "invoice-template.pdf", "Cost estimate before repair work"));

  public DemoDocumentGenerator(
      DSLContext dsl,
      DocumentRepository documentRepository,
      S3StorageService s3StorageService,
      Clock clock) {
    this.dsl = dsl;
    this.documentRepository = documentRepository;
    this.s3StorageService = s3StorageService;
    this.clock = clock;
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
      Optional<UUID> uploadedByOpt = ctx.getAdminUserForTeam(teamKey);
      if (uploadedByOpt.isEmpty()) {
        continue;
      }
      UUID uploadedBy = uploadedByOpt.get();
      int teamDocuments = 0;

      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().getOrDefault(teamId, List.of());
      List<UUID> tenantIds = ctx.getTenantIdsByTeam().getOrDefault(teamId, List.of());
      List<UUID> contractIds = ctx.getContractIdsByTeam().getOrDefault(teamId, List.of());

      // Property documents (scaled by acquisition age — older properties accumulate more)
      for (UUID propertyId : propertyIds) {
        String prefix = fetchPropertyStreet(propertyId).map(this::slugify).orElse("property");
        String category = ctx.getPropertyCategory(propertyId);
        List<DocTemplate> templates = propertyDocsForCategory(category);

        LocalDate acqDate = fetchAcquisitionDate(propertyId);
        int minDocs = acqDate.isBefore(LocalDate.of(2015, 1, 1)) ? 3 : 2;
        int maxDocs = acqDate.isBefore(LocalDate.of(2010, 1, 1)) ? 5 : 4;
        int docCount = random.nextInt(minDocs, maxDocs + 1);

        for (DocTemplate doc : pickRandom(templates, docCount)) {
          if (uploadDocument(
              ctx,
              teamId,
              uploadedBy,
              "PROPERTY",
              propertyId,
              doc.title,
              prefix + "-" + doc.pdfResource,
              doc.pdfResource,
              doc.notes)) {
            teamDocuments++;
          }
        }
      }

      // Tenant documents (1-2 per tenant, type-specific)
      for (UUID tenantId : tenantIds) {
        String prefix = fetchTenantName(tenantId).map(this::slugify).orElse("tenant");
        boolean isBusiness = ctx.isBusinessTenant(tenantId);
        List<DocTemplate> templates = isBusiness ? BUSINESS_TENANT_DOCS : INDIVIDUAL_TENANT_DOCS;
        for (DocTemplate doc : pickRandom(templates, random.nextInt(1, 3))) {
          if (uploadDocument(
              ctx,
              teamId,
              uploadedBy,
              "TENANT",
              tenantId,
              doc.title,
              prefix + "-" + doc.pdfResource,
              doc.pdfResource,
              doc.notes)) {
            teamDocuments++;
          }
        }
      }

      // Contract documents (varies by status/age)
      LocalDate fiveYearsAgo = LocalDate.now(clock).minusYears(5);
      for (UUID contractId : contractIds) {
        Record contract = dsl.selectFrom(CONTRACTS).where(CONTRACTS.ID.eq(contractId)).fetchOne();
        if (contract == null) {
          continue;
        }

        String status = contract.get(CONTRACTS.STATUS);

        if ("DRAFT".equals(status)) {
          // Draft: just the unsigned agreement
          if (uploadDocument(
              ctx,
              teamId,
              uploadedBy,
              "CONTRACT",
              contractId,
              "Draft rental agreement",
              "draft-rental-agreement.pdf",
              "hud-model-lease.pdf",
              "Unsigned draft for review")) {
            teamDocuments++;
          }
          continue;
        }

        // Expired contracts older than 5 years get only 1 doc
        LocalDate endDate = contract.get(CONTRACTS.END_DATE);
        boolean isOldExpired =
            "EXPIRED".equals(status) && endDate != null && endDate.isBefore(fiveYearsAgo);

        int docCount = isOldExpired ? 1 : random.nextInt(2, 4);

        for (DocTemplate doc : pickRandom(CONTRACT_DOCS, docCount)) {
          if (uploadDocument(
              ctx,
              teamId,
              uploadedBy,
              "CONTRACT",
              contractId,
              doc.title,
              doc.pdfResource,
              doc.pdfResource,
              doc.notes)) {
            teamDocuments++;
          }
        }
      }

      // Expense documents (~50% of RECENT expenses only — last 3 years)
      LocalDate expenseCutoff = LocalDate.now(clock).minusYears(3);
      var expenseRecords =
          dsl.select(EXPENSES.ID, EXPENSES.IDENTIFIER, EXPENSES.DESCRIPTION, EXPENSES.CATEGORY)
              .from(EXPENSES)
              .where(EXPENSES.TEAM_ID.eq(teamId))
              .and(EXPENSES.EXPENSE_DATE.greaterOrEqual(expenseCutoff))
              .fetch();

      for (var expense : expenseRecords) {
        if (random.nextBoolean()) {
          continue;
        }

        UUID expenseId = expense.get(EXPENSES.ID);
        ctx.putIdentifier(expenseId, expense.get(EXPENSES.IDENTIFIER));
        String category = expense.get(EXPENSES.CATEGORY);
        String description = expense.get(EXPENSES.DESCRIPTION);
        DocTemplate doc = EXPENSE_DOCS.get(random.nextInt(EXPENSE_DOCS.size()));
        String prefix = slugify(category.toLowerCase(Locale.ROOT));

        if (uploadDocument(
            ctx,
            teamId,
            uploadedBy,
            "EXPENSE",
            expenseId,
            doc.title + " — " + description,
            prefix + "-" + doc.pdfResource,
            doc.pdfResource,
            doc.notes)) {
          teamDocuments++;
        }
      }

      ctx.incrementDocuments(teamDocuments);
      log.info("Created {} documents for team {}", teamDocuments, teamKey);
    }
  }

  private List<DocTemplate> propertyDocsForCategory(String category) {
    return switch (category) {
      case "COMMERCIAL" -> COMMERCIAL_PROPERTY_DOCS;
      case "INDUSTRIAL" -> INDUSTRIAL_PROPERTY_DOCS;
      case "AGRICULTURAL" -> AGRICULTURAL_PROPERTY_DOCS;
      default -> RESIDENTIAL_PROPERTY_DOCS;
    };
  }

  private boolean uploadDocument(
      DemoDataContext ctx,
      UUID teamId,
      UUID uploadedBy,
      String entityType,
      UUID entityId,
      String title,
      String fileName,
      String pdfResource,
      @Nullable String notes) {
    byte[] pdfData = pdfPool.getOrDefault(pdfResource, pdfPool.values().iterator().next());

    try {
      String fileKey =
          s3StorageService.uploadFile(
              pdfData,
              "application/pdf",
              ctx.getIdentifier(teamId),
              entityType,
              ctx.getIdentifier(entityId),
              fileName);

      Document document = new Document();
      document.setTeamId(teamId);
      document.setEntityType(entityType);
      document.setEntityId(entityId);
      document.setFileKey(fileKey);
      document.setFileName(fileName);
      document.setFileSize((long) pdfData.length);
      document.setMimeType("application/pdf");
      document.setTitle(Optional.of(title));
      document.setNotes(Optional.ofNullable(notes));
      document.setUploadedBy(uploadedBy);

      documentRepository.save(document);
      return true;
    } catch (Exception e) {
      log.warn(
          "Failed to upload document '{}' for {} {}: {}",
          title,
          entityType,
          entityId,
          e.getMessage());
      return false;
    }
  }

  // --- helpers ---

  private Optional<String> fetchPropertyStreet(UUID propertyId) {
    return Optional.ofNullable(
        dsl.select(PROPERTIES.STREET)
            .from(PROPERTIES)
            .where(PROPERTIES.ID.eq(propertyId))
            .fetchOne(PROPERTIES.STREET));
  }

  private LocalDate fetchAcquisitionDate(UUID propertyId) {
    return Optional.ofNullable(
            dsl.select(PROPERTY_ACQUISITIONS.ACQUISITION_DATE)
                .from(PROPERTY_ACQUISITIONS)
                .where(PROPERTY_ACQUISITIONS.PROPERTY_ID.eq(propertyId))
                .fetchOne(PROPERTY_ACQUISITIONS.ACQUISITION_DATE))
        .orElse(LocalDate.now(clock));
  }

  private Optional<String> fetchTenantName(UUID tenantId) {
    Record r =
        dsl.select(TENANTS.FIRST_NAME, TENANTS.LAST_NAME)
            .from(TENANTS)
            .where(TENANTS.ID.eq(tenantId))
            .fetchOne();
    return r != null
        ? Optional.of(r.get(TENANTS.FIRST_NAME) + " " + r.get(TENANTS.LAST_NAME))
        : Optional.empty();
  }

  private List<DocTemplate> pickRandom(List<DocTemplate> templates, int count) {
    List<DocTemplate> shuffled = new ArrayList<>(templates);
    Collections.shuffle(shuffled, random);
    return shuffled.subList(0, Math.min(count, shuffled.size()));
  }

  private String slugify(String input) {
    return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
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
