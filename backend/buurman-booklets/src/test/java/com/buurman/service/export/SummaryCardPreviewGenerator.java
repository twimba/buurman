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
        engine.process("contract-booklet-v2/generic", ctx(contractBooklet())));
    System.out.println(
        "[preview] wrote summary cards + multi-page booklet to " + dir.toAbsolutePath());
  }

  // ── sample data ──────────────────────────────────────────────────

  private Map<String, Object> contract() {
    Map<String, Object> v = base("Tenancy Summary");
    v.put("propertyAddress", "Kerkstraat 14, 1017 GC Amsterdam");
    v.put("contractIdentifier", "C-2024-0187");
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
    v.put("propertyIdentifier", "P-001");
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
    v.put("contactIdentifier", "CT-5001");
    v.put("tags", List.of("Tenant", "Primary", "Reliable payer"));
    v.put("activeContracts", "3");
    v.put("heroSizeClass", "hero-l");
    v.put("roleLabel", "Tenant");
    v.put("contractsCount", "5");
    v.put("propertiesCount", "2");
    v.put("lifetimePaid", "€34,800.00");
    v.put("onTimeRate", "98%");
    v.put("primaryChannelValue", "+31 6 12 34 56 78");
    v.put("email", "luis.santos@example.com");
    v.put("phone", "+31 6 12 34 56 78");
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
    v.put("contractType", "Fixed-term tenancy");
    v.put("statusCode", "ACTIVE");
    v.put("statusLabel", "Active");
    v.put("landlordName", "Vastgoed Bakker B.V.");
    v.put("tenantName", "Luís Santos");
    v.put("rent", "€1,450.00 / mo");
    v.put("deposit", "€2,900.00");
    v.put("frequencyLabel", "Monthly");
    v.put("termRange", "1 Jan 2024 → 31 Dec 2024");
    v.put("totalPaid", "€13,050.00");
    v.put("totalPending", "€1,450.00");
    v.put("totalOverdue", "€1,450.00");
    v.put("overdueCount", 1);
    v.put(
        "parties",
        List.of(
            Map.of(
                "role",
                "Primary tenant",
                "name",
                "Luís Santos",
                "contact",
                "luis.santos@example.com · +31 6 12 34 56 78"),
            Map.of(
                "role",
                "Guarantor",
                "name",
                "Maria Santos",
                "contact",
                "maria.santos@example.com")));
    v.put(
        "payments",
        List.of(
            Map.of(
                "date",
                "1 Apr 2026",
                "status",
                "Paid",
                "statusCode",
                "PAID",
                "amount",
                "€1,450.00"),
            Map.of(
                "date",
                "1 May 2026",
                "status",
                "Paid",
                "statusCode",
                "PAID",
                "amount",
                "€1,450.00"),
            Map.of(
                "date",
                "1 Jun 2026",
                "status",
                "Overdue",
                "statusCode",
                "OVERDUE",
                "amount",
                "€1,450.00"),
            Map.of(
                "date",
                "1 Jul 2026",
                "status",
                "Pending",
                "statusCode",
                "PENDING",
                "amount",
                "€1,450.00")));
    v.put("qrDataUri", qr.toSvgDataUri("https://app.buurman.io/contracts/C-2024-0187"));
    return v;
  }

  private static Map<String, Object> base(String kicker) {
    Map<String, Object> v = new HashMap<>();
    v.put("lang", "en");
    v.put("dir", "ltr");
    v.put("kicker", kicker);
    v.put("generatedMeta", "Generated 28 June 2026 · Confidential");
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
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/document-summary-card");
    ms.setDefaultEncoding("UTF-8");
    ms.setFallbackToSystemLocale(false);
    ms.setUseCodeAsDefaultMessage(true);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(ms);
    return engine;
  }
}
