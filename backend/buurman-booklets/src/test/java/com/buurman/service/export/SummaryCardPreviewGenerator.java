package com.buurman.service.export;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Not a test — a preview generator. Renders the three landscape summary-card templates with
 * realistic sample data + real QR codes and writes them as standalone HTML to a directory, for
 * visual evaluation (print to PDF with headless Chromium, the same engine Gotenberg uses).
 *
 * <p>Run with: {@code mvn -pl buurman-booklets test -Dtest=SummaryCardPreviewGenerator
 * -Dbooklet.preview=true -Dbooklet.preview.dir=/abs/out -Dbooklet.preview.font=/abs/Satoshi.woff2}
 */
@EnabledIfSystemProperty(named = "booklet.preview", matches = "true")
class SummaryCardPreviewGenerator {

  private final QrCodeGenerator qr = new QrCodeGenerator();

  private static final Locale LOCALE =
      Locale.forLanguageTag(System.getProperty("booklet.preview.locale", "en"));

  @Test
  void generate() throws Exception {
    SpringTemplateEngine engine = engine();
    Path dir = Path.of(System.getProperty("booklet.preview.dir", "build/booklet-preview"));
    Files.createDirectories(dir);

    write(dir, "contract", engine.process("contract-summary/generic", ctx(contract())));
    write(dir, "property", engine.process("property-summary/generic", ctx(property())));
    write(dir, "contact", engine.process("contact-summary/generic", ctx(contact())));
    write(
        dir,
        "contract-booklet",
        bookletEngine("document-contract-booklet")
            .process("contract-booklet/generic", ctx(contractBooklet())));
    write(
        dir,
        "contact-booklet",
        bookletEngine("document-contact-booklet")
            .process("contact-booklet/generic", ctx(contactBooklet())));
    write(
        dir,
        "property-booklet",
        bookletEngine("document-property-booklet")
            .process("property-booklet/generic", ctx(propertyBooklet())));
    System.out.println(
        "[preview] wrote summary cards + multi-page booklet to " + dir.toAbsolutePath());
  }

  // ── sample data ──────────────────────────────────────────────────

  private Map<String, Object> contract() {
    Map<String, Object> v = base("Tenancy Summary");
    v.put("propertyAddress", "Kerkstraat 14, 1017 GC Amsterdam");
    v.put("contractIdentifier", "CON01KWARB2PX4W0J4WGV7RG3S6P6");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("landlordName", "Vastgoed Bakker B.V.");
    v.put("tenantName", "Luís Santos");
    v.put("heroSizeClass", "hero-m");
    v.put("rent", "€1,450.00");
    v.put("rentPeriodUnit", "/mo");
    v.put("deposit", "€2,900.00");
    v.put("frequencyLabel", "Monthly");
    v.put("termLabel", "12 months");
    v.put("nextPaymentAmount", "€1,450.00");
    v.put("nextPaymentWhen", "1 Jul");
    v.put("nextPaymentDanger", false);
    v.put("paidPct", 75);
    v.put("pendingPct", 17);
    v.put("overduePct", 8);
    v.put("paidCount", 9);
    v.put("pendingCount", 2);
    v.put("overdueCount", 1);
    v.put("paymentInstruction", "NL91 ABNA 0417 1643 00 · Vastgoed Bakker B.V.");
    v.put("leaseStart", "1 Jan 2024");
    v.put("leaseEnd", "31 Dec 2024");
    v.put("termElapsedPct", 49);
    v.put("termElapsedLabel", "49% elapsed");
    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/c/C-2024-0187"));
    return v;
  }

  private Map<String, Object> property() {
    Map<String, Object> v = base("Property Summary");
    v.put("propertyAddress", "Kerkstraat 14");
    v.put("propertyTypeLabel", "Apartment");
    v.put("propertyCategoryLabel", "Residential");
    v.put("propertyIdentifier", "PRO01KWARB1XZVENQXM5JYS9N5ZK7");
    v.put("statusCode", "OCCUPIED");
    v.put("statusLabel", "Occupied");
    v.put("heroGroundClass", "hs-occupied");
    v.put("heroStatusWord", "OCCUPIED");
    v.put("heroSizeClass", "hero-l");
    v.put("tenure", "Tenant since 2023 · Lease ends 2026");
    v.put("area", "84");
    v.put("areaUnit", "m²");
    v.put("bedBath", "2 / 1");
    v.put("energyLabel", "A");
    v.put("yearBuilt", "2019");
    v.put("isVacant", false);
    v.put("headlineMoney", "€1,450.00");
    v.put("headlineUnit", "/mo");
    v.put("parking", "1 space · Garage");
    v.put("energyExpiry", "Valid to 2030");
    v.put("safety", "Smoke · CO · Alarm");
    v.put("utilities", "Gas · Fibre 1 Gbps");
    v.put("accessibility", "Elevator · Step-free");
    v.put("renovated", "2021");
    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/p/P-001"));
    return v;
  }

  private Map<String, Object> contact() {
    Map<String, Object> v = base("Contact Summary");
    v.put("contactName", "Luís Santos");
    v.put("contactTypeLabel", "Individual");
    v.put("contactIdentifier", "CTC01KWARB1RQ8M2Q9JK6PHQXMN8H");
    v.put("tags", List.of("Tenant", "Primary", "Reliable payer"));
    v.put("activeContracts", "3");
    v.put("heroSizeClass", "hero-l");
    v.put("roleLabel", "Tenant");
    v.put("contractsCount", "5");
    v.put("propertiesCount", "2");
    v.put("lifetimePaid", "€45,920.00");
    v.put("onTimeRate", "98%");
    v.put("primaryChannelValue", "+43631187167");
    v.put("email", "luis.santos@example.com");
    v.put("phone", "+43631187167");
    v.put("website", null);
    v.put("mailingAddress", "Kerkstraat 14, 1017 GC Amsterdam");
    v.put("taxId", "NL0000.00.000.B01");
    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/ct/CT-5001"));
    return v;
  }

  private Map<String, Object> contractBooklet() {
    Map<String, Object> v = base("Tenancy");
    v.put("propertyAddress", "Kerkstraat 14, 1017 GC Amsterdam");
    v.put("contractIdentifier", "C-2024-0187");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("contractTypeLabel", "Fixed term");
    v.put("tenantName", "Luís Santos");
    v.put("rent", "€1,450.00");
    v.put("frequencyLabel", "Monthly");
    v.put("period", "1 Jan 2024 — 31 Dec 2024");
    v.put("startDate", "1 Jan 2024");
    v.put("endDate", "31 Dec 2024");
    v.put("signedDate", "18 Dec 2023");
    v.put("deposit", "€2,900.00");
    v.put("securityDeposit", "€1,450.00");
    v.put("dueDay", "1");
    v.put("renewalMode", "Automatic");
    v.put("lateFee", "5%");
    v.put("propertyTypeLabel", "Apartment");
    v.put("propertyCategoryLabel", "Residential");
    v.put("propertyArea", "84 m²");
    v.put(
        "rentPeriods",
        List.of(
            Map.of("from", "1 Jan 2024", "to", "—", "amount", "€1,450.00"),
            Map.of("from", "1 Jan 2023", "to", "31 Dec 2023", "amount", "€1,400.00")));
    v.put(
        "termsHtml",
        "<p>The tenant shall use the premises solely as a private residence. Subletting requires"
            + " the landlord's written consent.</p>");
    v.put("notesHtml", "<p>Keys handed over on 1 Jan 2024 (2× front door, 1× mailbox).</p>");
    v.put(
        "parties",
        List.of(
            Map.of(
                "role",
                "Primary tenant",
                "name",
                "Luís Santos",
                "contact",
                "luis.santos@example.com · +31 6 12 34 56 78 · CT-5001"),
            Map.of(
                "role",
                "Guarantor",
                "name",
                "Maria Santos",
                "contact",
                "maria.santos@example.com · CT-5002")));
    v.put(
        "instructions",
        List.of(
            Map.of(
                "method", "Bank transfer",
                "current", true,
                "name", "Primary account",
                "bankName", "ABN AMRO",
                "accountHolder", "Vastgoed Bakker B.V.",
                "iban", "NL91 ABNA 0417 1643 00",
                "bic", "ABNANL2A",
                "reference", "Rent C-2024-0187")));
    v.put(
        "payments",
        List.of(
            Map.of(
                "due",
                "1 Jul 2026",
                "status",
                "Pending",
                "statusCode",
                "PENDING",
                "amount",
                "€1,450.00",
                "paid",
                "€0.00"),
            Map.of(
                "due",
                "1 Jun 2026",
                "status",
                "Overdue",
                "statusCode",
                "OVERDUE",
                "amount",
                "€1,450.00",
                "paid",
                "€0.00"),
            Map.of(
                "due",
                "1 May 2026",
                "status",
                "Paid",
                "statusCode",
                "PAID",
                "amount",
                "€1,450.00",
                "paid",
                "€1,450.00"),
            Map.of(
                "due",
                "1 Apr 2026",
                "status",
                "Paid",
                "statusCode",
                "PAID",
                "amount",
                "€1,450.00",
                "paid",
                "€1,450.00")));
    v.put("totalPaid", "€13,050.00");
    v.put("totalPending", "€1,450.00");
    v.put("totalOverdue", "€1,450.00");
    v.put("overdueCount", 1);
    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/contracts/C-2024-0187"));
    return v;
  }

  private Map<String, Object> contactBooklet() {
    Map<String, Object> v = base("Contact");
    v.put("contactName", "Luís Santos");
    v.put("contactTypeLabel", "Individual");
    v.put("contactIdentifier", "CT-5001");
    v.put("isIndividual", true);
    v.put("email", "luis.santos@example.com");
    v.put("phone", "+31 6 12 34 56 78");
    v.put("website", null);
    v.put("companyName", null);
    v.put("tradeName", null);
    v.put("industry", null);
    v.put("dateOfBirth", "4 Mar 1989");
    v.put("taxNumber", "NL0000.00.000.B01");
    v.put("idNumber", null);
    v.put("currentProperty", "Kerkstraat 14, Amsterdam");
    v.put("tags", List.of("Tenant", "Primary", "Reliable payer"));
    v.put("profileNotesHtml", "<p>Long-standing tenant; prefers contact by email. No arrears.</p>");
    v.put("totalPaid", "€34,800.00");
    v.put("outstanding", "€1,450.00");
    v.put("hasOutstanding", true);
    v.put("activeContracts", "1");
    v.put("totalContracts", "2");
    v.put(
        "addresses",
        List.of(
            Map.of(
                "type",
                "Current",
                "active",
                true,
                "street",
                "Kerkstraat 14",
                "city",
                "Amsterdam, 1017 GC",
                "country",
                "NL"),
            Map.of(
                "type",
                "Mailing",
                "active",
                false,
                "street",
                "Postbus 123",
                "city",
                "Amsterdam, 1000 AA",
                "country",
                "NL")));
    v.put(
        "rentals",
        List.of(
            Map.of(
                "role",
                "Primary tenant",
                "status",
                "Active",
                "statusCode",
                "ACTIVE",
                "property",
                "Kerkstraat 14, Amsterdam",
                "contractId",
                "C-2024-0187",
                "type",
                "Fixed term",
                "start",
                "1 Jan 2024",
                "end",
                "Ongoing",
                "rent",
                "€1,450.00",
                "frequency",
                "Monthly"),
            Map.of(
                "role",
                "Primary tenant",
                "status",
                "Expired",
                "statusCode",
                "EXPIRED",
                "property",
                "Lindenlaan 7, Utrecht",
                "contractId",
                "C-2021-0042",
                "type",
                "Fixed term",
                "start",
                "1 Jan 2021",
                "end",
                "31 Dec 2023",
                "rent",
                "€1,200.00",
                "frequency",
                "Monthly")));
    v.put(
        "payments",
        List.of(
            Map.of(
                "due",
                "1 Jun 2026",
                "paidOn",
                "—",
                "status",
                "Overdue",
                "statusCode",
                "OVERDUE",
                "amount",
                "€1,450.00"),
            Map.of(
                "due",
                "1 May 2026",
                "paidOn",
                "1 May 2026",
                "status",
                "Paid",
                "statusCode",
                "PAID",
                "amount",
                "€1,450.00"),
            Map.of(
                "due",
                "1 Apr 2026",
                "paidOn",
                "2 Apr 2026",
                "status",
                "Paid",
                "statusCode",
                "PAID",
                "amount",
                "€1,450.00")));
    v.put(
        "notes",
        List.of(
            Map.of(
                "type",
                "Call",
                "pinned",
                true,
                "date",
                "12 May 2026",
                "author",
                "A. Bakker",
                "subject",
                "Annual review",
                "body",
                "Discussed rent indexation for 2027. Tenant agreeable."),
            Map.of(
                "type",
                "Email",
                "pinned",
                false,
                "date",
                "3 Apr 2026",
                "author",
                "A. Bakker",
                "subject",
                "—",
                "body",
                "Confirmed payment received.")));
    v.put(
        "relationships",
        List.of(Map.of("name", "Maria Santos", "type", "Spouse", "notes", "Co-occupant")));
    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/contacts/CT-5001"));
    return v;
  }

  private Map<String, Object> propertyBooklet() {
    Map<String, Object> v = base("Property");
    v.put("identifier", "P-0001");
    v.put("street", "Kerkstraat 14");
    v.put("location", "Amsterdam, 1017 GC, NL");
    v.put("propertyTypeLabel", "Apartment");
    v.put("statusCode", "OCCUPIED");
    v.put("statusLabel", "Occupied");
    v.put(
        "coverRows",
        List.of(
            Map.of("k", "Bedrooms / Bathrooms", "v", "3 bed / 2 bath"),
            Map.of("k", "Total area", "v", "120 m²"),
            Map.of("k", "Year built", "v", "1998")));
    v.put(
        "detailsFields",
        List.of(
            Map.of("l", "Property type", "v", "Apartment"),
            Map.of("l", "Status", "v", "Occupied"),
            Map.of("l", "Total area", "v", "120 m²"),
            Map.of("l", "Number of floors", "v", "4"),
            Map.of("l", "Year built", "v", "1998"),
            Map.of("l", "Last renovated", "v", "2021")));
    v.put(
        "constructionFields",
        List.of(
            Map.of("l", "Construction type", "v", "Brick"),
            Map.of("l", "Roof type", "v", "Pitched Tile"),
            Map.of("l", "Window type", "v", "Double Glazed")));
    v.put("categoryTitle", "Residential details");
    v.put(
        "categoryFields",
        List.of(
            Map.of("l", "Bedrooms", "v", "3"),
            Map.of("l", "Bathrooms", "v", "2"),
            Map.of("l", "Furnished", "v", "Yes"),
            Map.of("l", "Pet policy", "v", "Allowed")));
    v.put("structuralNotes", "Load-bearing façade renovated in 2021; new roof membrane installed.");

    v.put("hasBuildingSpecs", true);
    v.put("energyRating", "A");
    v.put("energyColor", "#059669");
    v.put(
        "energyFields",
        List.of(
            Map.of("l", "Heating system", "v", "District Heating"),
            Map.of("l", "Hot water system", "v", "Central"),
            Map.of("l", "Certificate expiry", "v", "31 Dec 2030")));
    v.put("insulationNotes", "Triple glazing throughout; roof insulated to current code.");
    v.put(
        "utilitiesFields",
        List.of(
            Map.of("l", "Electricity", "v", "Grid"),
            Map.of("l", "Capacity", "v", "25 A"),
            Map.of("l", "Gas connection", "v", "Connected"),
            Map.of("l", "Internet", "v", "Fibre"),
            Map.of("l", "Max speed", "v", "1000 Mbps")));
    v.put(
        "parkingFields",
        List.of(
            Map.of("l", "Parking type", "v", "Garage"), Map.of("l", "Parking spaces", "v", "1")));

    v.put(
        "amenityGroups",
        List.of(
            Map.of(
                "category",
                "Indoor",
                "items",
                List.of("Dishwasher", "Washing Machine", "Built-in Wardrobes")),
            Map.of(
                "category",
                "Building",
                "items",
                List.of("Elevator", "Bicycle Storage", "Intercom"))));
    v.put(
        "outdoorAreas",
        List.of(
            Map.of("type", "Balcony", "area", "8 m²"),
            Map.of("type", "Shared Garden", "area", "120 m²")));
    v.put("hasFeatures", true);

    v.put("hasSafety", true);
    v.put(
        "safetyChecks",
        List.of(
            Map.of("label", "Smoke detectors", "ok", true),
            Map.of("label", "CO detectors", "ok", true),
            Map.of("label", "Fire extinguisher", "ok", true),
            Map.of("label", "Sprinkler system", "ok", false),
            Map.of("label", "Alarm system", "ok", true),
            Map.of("label", "Security cameras", "ok", false),
            Map.of("label", "Secure entry", "ok", true)));
    v.put("safetyNotes", "Fire safety inspection completed January 2026.");
    v.put(
        "accessibilityChecks",
        List.of(
            Map.of("label", "Wheelchair accessible", "ok", true),
            Map.of("label", "Elevator", "ok", true),
            Map.of("label", "Step-free entrance", "ok", true),
            Map.of("label", "Adapted bathroom", "ok", false)));
    v.put("accessibilityNotes", null);

    v.put(
        "photos",
        List.of(
            Map.of("src", PLACEHOLDER_IMG, "label", "Front facade", "main", true),
            Map.of("src", PLACEHOLDER_IMG, "label", "Living room", "main", false),
            Map.of("src", PLACEHOLDER_IMG, "label", "Kitchen", "main", false),
            Map.of("src", PLACEHOLDER_IMG, "label", "Master bedroom", "main", false),
            Map.of("src", PLACEHOLDER_IMG, "label", "Balcony view", "main", false)));

    v.put(
        "financialYears",
        List.of(
            Map.of(
                "year",
                "2025",
                "income",
                "€17,400.00",
                "expenses",
                "€4,180.00",
                "net",
                "€13,220.00"),
            Map.of(
                "year",
                "2024",
                "income",
                "€16,800.00",
                "expenses",
                "€5,940.00",
                "net",
                "€10,860.00")));

    v.put(
        "contracts",
        List.of(
            Map.of(
                "id",
                "C-2024-0187",
                "contact",
                "Luís Santos",
                "start",
                "1 Jan 2024",
                "end",
                "Ongoing",
                "rent",
                "€1,450.00",
                "statusCode",
                "ACTIVE",
                "statusLabel",
                "Active"),
            Map.of(
                "id",
                "C-2021-0042",
                "contact",
                "Anna de Vries",
                "start",
                "1 Feb 2021",
                "end",
                "31 Dec 2023",
                "rent",
                "€1,300.00",
                "statusCode",
                "EXPIRED",
                "statusLabel",
                "Expired")));

    v.put("hasDashboard", true);
    v.put(
        "dashMetrics",
        List.of(
            Map.of("l", "Total ROI", "v", "42.8%"),
            Map.of("l", "Annualized ROI", "v", "9.1%"),
            Map.of("l", "Cap rate", "v", "5.4%"),
            Map.of("l", "Cash-on-cash", "v", "7.2%"),
            Map.of("l", "Monthly cash flow", "v", "€640.00"),
            Map.of("l", "Annual NOI", "v", "€12,900.00"),
            Map.of("l", "Total equity", "v", "€180,000.00"),
            Map.of("l", "Occupancy", "v", "100%")));
    v.put(
        "cashFlow",
        List.of(
            cashRow("Jan 2026", "€1,450.00", "€320.00", "€600.00", "€530.00", true),
            cashRow("Feb 2026", "€1,450.00", "€980.00", "€600.00", "-€130.00", false),
            cashRow("Mar 2026", "€1,450.00", "€280.00", "€600.00", "€570.00", true)));
    v.put(
        "expenseBreakdown",
        List.of(
            Map.of("category", "Maintenance", "amount", "€1,200.00"),
            Map.of("category", "Insurance", "amount", "€640.00"),
            Map.of("category", "Property Tax", "amount", "€980.00")));
    v.put(
        "equityRows",
        List.of(
            Map.of("l", "Purchase price", "v", "€380,000.00"),
            Map.of("l", "Current market value", "v", "€465,000.00"),
            Map.of("l", "Mortgage balance", "v", "€285,000.00")));

    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/properties/P-0001"));
    return v;
  }

  private static Map<String, Object> cashRow(
      String month, String income, String expenses, String mortgage, String net, boolean positive) {
    Map<String, Object> m = new HashMap<>();
    m.put("month", month);
    m.put("income", income);
    m.put("expenses", expenses);
    m.put("mortgage", mortgage);
    m.put("net", net);
    m.put("net_positive", positive);
    return m;
  }

  /** 2×2 grey PNG — keeps the preview self-contained (no S3) while exercising the photo grid. */
  private static final String PLACEHOLDER_IMG =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAEklEQVR4nGP8//"
          + "8/AzZAjBgAJ54G/2zZ0sUAAAAASUVORK5CYII=";

  private static Map<String, Object> base(String kicker) {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("dir", "ltr");
    v.put("kicker", kicker);
    v.put("generatedDate", "28 June 2026");
    return v;
  }

  // ── plumbing ─────────────────────────────────────────────────────

  private static Context ctx(Map<String, Object> vars) {
    Context c = new Context(LOCALE);
    c.setVariables(vars);
    return c;
  }

  /** Inject a Satoshi @font-face (real brand font) so the preview matches production output. */
  private static void write(Path dir, String name, String html) throws Exception {
    String font = System.getProperty("booklet.preview.font", "");
    if (!font.isBlank()) {
      String face =
          "<style>@font-face{font-family:'Satoshi';src:url('file://"
              + font
              + "') format('woff2');font-weight:300 900;font-display:swap;}</style>";
      html = html.replace("</head>", face + "</head>");
    }
    Files.writeString(dir.resolve(name + ".html"), html, StandardCharsets.UTF_8);
  }

  private static SpringTemplateEngine engine() {
    return engineFor("classpath:messages/document-summary-card");
  }

  /**
   * Per-entity booklet engine mirroring production: the entity bundle (loaded from the app module
   * via file:) + the shared enum-label & summary-card chrome (classpath, in this module).
   */
  private static SpringTemplateEngine bookletEngine(String entityBundle) {
    String appMessages =
        System.getProperty(
            "booklet.preview.appmessages", "../buurman-app/src/main/resources/messages");
    return engineFor(
        "file:" + appMessages + "/" + entityBundle,
        "classpath:messages/document-enum-labels",
        "classpath:messages/document-summary-card");
  }

  private static SpringTemplateEngine engineFor(String... basenames) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames(basenames);
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(false);
    ms.setUseCodeAsDefaultMessage(true);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
    return engine;
  }
}
