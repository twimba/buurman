package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Property.PropertyCategory.AGRICULTURAL;
import static com.buurman.domain.Property.PropertyCategory.COMMERCIAL;
import static com.buurman.domain.Property.PropertyCategory.INDUSTRIAL;
import static com.buurman.domain.Property.PropertyCategory.RESIDENTIAL;
import static com.buurman.service.export.BookletHelper.formatEnumValue;
import static com.buurman.service.export.BookletHelper.isTrue;
import static java.math.BigDecimal.ZERO;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.domain.Amenity;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Expense;
import com.buurman.domain.Payment;
import com.buurman.domain.Photo;
import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.PropertyAgriculturalDetails;
import com.buurman.domain.PropertyAmenity;
import com.buurman.domain.PropertyCommercialDetails;
import com.buurman.domain.PropertyIndustrialDetails;
import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.domain.PropertyResidentialDetails;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;
import com.buurman.repository.AmenityRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyAgriculturalDetailsRepository;
import com.buurman.repository.PropertyAmenityRepository;
import com.buurman.repository.PropertyCommercialDetailsRepository;
import com.buurman.repository.PropertyIndustrialDetailsRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyResidentialDetailsRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.EffectiveEndDateHelper;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;
import com.buurman.service.S3StorageService;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.FeatureFlags;

/**
 * Generates the multi-page property dossier PDF. Loads the property + category-specific details +
 * amenities + outdoor areas + photos + financials + contracts (+ optional investment dashboard),
 * projects everything into a localized, list-driven view-model ({@link #buildModel}) and renders the
 * {@code property-booklet/generic} Thymeleaf template via Gotenberg/Chromium. All money/dates/enum
 * labels are pre-formatted &amp; localized here; the template only lays out (no category branching,
 * no hardcoded English).
 */
@Component
public class PropertyBookletExporter {

  private final PropertyRepository propertyRepository;
  private final PropertyResidentialDetailsRepository residentialDetailsRepository;
  private final PropertyCommercialDetailsRepository commercialDetailsRepository;
  private final PropertyIndustrialDetailsRepository industrialDetailsRepository;
  private final PropertyAgriculturalDetailsRepository agriculturalDetailsRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;
  private final PropertyAmenityRepository propertyAmenityRepository;
  private final PropertyOutdoorAreaRepository propertyOutdoorAreaRepository;
  private final AmenityRepository amenityRepository;
  private final PhotoRepository photoRepository;
  private final ContractPartyService contractPartyService;
  private final S3StorageService s3StorageService;
  private final FeatureFlagService featureFlagService;
  private final PropertyDashboardService propertyDashboardService;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final DocumentRenderer pdfRenderer;
  private final TemplateEngine templateEngine;
  private final MessageSource messageSource;
  private final EnumLabelResolver enumLabels;
  private final BookletFormatter formatter;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public PropertyBookletExporter(
      PropertyRepository propertyRepository,
      PropertyResidentialDetailsRepository residentialDetailsRepository,
      PropertyCommercialDetailsRepository commercialDetailsRepository,
      PropertyIndustrialDetailsRepository industrialDetailsRepository,
      PropertyAgriculturalDetailsRepository agriculturalDetailsRepository,
      ContractRepository contractRepository,
      ContractExtensionRepository contractExtensionRepository,
      PaymentRepository paymentRepository,
      ExpenseRepository expenseRepository,
      PropertyAmenityRepository propertyAmenityRepository,
      PropertyOutdoorAreaRepository propertyOutdoorAreaRepository,
      AmenityRepository amenityRepository,
      PhotoRepository photoRepository,
      ContractPartyService contractPartyService,
      S3StorageService s3StorageService,
      FeatureFlagService featureFlagService,
      PropertyDashboardService propertyDashboardService,
      TeamPreferencesRepository teamPreferencesRepository,
      DocumentRenderer pdfRenderer,
      @Qualifier("propertyBookletTemplateEngine") TemplateEngine templateEngine,
      @Qualifier("propertyBookletMessageSource") MessageSource messageSource,
      EnumLabelResolver enumLabels,
      BookletFormatter formatter,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.propertyRepository = propertyRepository;
    this.residentialDetailsRepository = residentialDetailsRepository;
    this.commercialDetailsRepository = commercialDetailsRepository;
    this.industrialDetailsRepository = industrialDetailsRepository;
    this.agriculturalDetailsRepository = agriculturalDetailsRepository;
    this.contractRepository = contractRepository;
    this.contractExtensionRepository = contractExtensionRepository;
    this.paymentRepository = paymentRepository;
    this.expenseRepository = expenseRepository;
    this.propertyAmenityRepository = propertyAmenityRepository;
    this.propertyOutdoorAreaRepository = propertyOutdoorAreaRepository;
    this.amenityRepository = amenityRepository;
    this.photoRepository = photoRepository;
    this.contractPartyService = contractPartyService;
    this.s3StorageService = s3StorageService;
    this.featureFlagService = featureFlagService;
    this.propertyDashboardService = propertyDashboardService;
    this.teamPreferencesRepository = teamPreferencesRepository;
    this.pdfRenderer = pdfRenderer;
    this.templateEngine = templateEngine;
    this.messageSource = messageSource;
    this.enumLabels = enumLabels;
    this.formatter = formatter;
    this.qrCodeGenerator = qrCodeGenerator;
    this.clock = clock;
    this.appBaseUrl = appBaseUrl;
  }

  private String msg(String key, Locale locale) {
    return Objects.requireNonNullElse(messageSource.getMessage(key, null, key, locale), key);
  }

  public byte[] generate(PropertyIdentifier propertyIdentifier, UUID teamId, Locale locale) {
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    UUID propertyId = property.getId();

    PropertyCategory category = property.getPropertyCategory();
    PropertyResidentialDetails residentialDetails = null;
    PropertyCommercialDetails commercialDetails = null;
    PropertyIndustrialDetails industrialDetails = null;
    PropertyAgriculturalDetails agriculturalDetails = null;

    if (category != null) {
      switch (category) {
        case RESIDENTIAL ->
            residentialDetails =
                residentialDetailsRepository
                    .findByPropertyIdAndTeamId(propertyId, teamId)
                    .orElse(null);
        case COMMERCIAL ->
            commercialDetails =
                commercialDetailsRepository
                    .findByPropertyIdAndTeamId(propertyId, teamId)
                    .orElse(null);
        case INDUSTRIAL ->
            industrialDetails =
                industrialDetailsRepository
                    .findByPropertyIdAndTeamId(propertyId, teamId)
                    .orElse(null);
        case AGRICULTURAL ->
            agriculturalDetails =
                agriculturalDetailsRepository
                    .findByPropertyIdAndTeamId(propertyId, teamId)
                    .orElse(null);
        case MIXED_USE -> {
          // No category-specific details for mixed-use
        }
      }
    }

    List<Contract> contracts = contractRepository.findByPropertyId(propertyId, teamId);
    List<Payment> payments =
        paymentRepository.findAllByTeamId(teamId).stream()
            .filter(
                p ->
                    p.getContractId() != null
                        && contracts.stream().anyMatch(c -> c.getId().equals(p.getContractId())))
            .toList();
    List<Expense> expenses = expenseRepository.findByPropertyId(propertyId, teamId);
    List<PropertyOutdoorArea> outdoorAreas =
        propertyOutdoorAreaRepository.findByPropertyIdAndTeamId(propertyId, teamId);
    List<PropertyAmenity> propertyAmenities =
        propertyAmenityRepository.findByPropertyIdAndTeamId(propertyId, teamId);
    List<Amenity> allAmenities = amenityRepository.findAll();
    List<Photo> photos = photoRepository.findByEntityAndTeamId("PROPERTY", propertyId, teamId);

    Map<Integer, FinancialYearSummary> yearSummaries = calculateYearSummaries(payments, expenses);

    String teamCurrency = teamPreferencesRepository.getByTeamId(teamId).getDefaultCurrency();

    PropertyDashboardResponse dashboard = null;
    if (featureFlagService.isEnabled(FeatureFlags.REPORTS)) {
      try {
        dashboard = propertyDashboardService.getDashboardData(propertyIdentifier, 0, teamId);
      } catch (Exception e) {
        // Non-critical: booklet still generates without dashboard section
      }
    }

    Map<String, Object> model =
        buildModel(
            property,
            category,
            residentialDetails,
            commercialDetails,
            industrialDetails,
            agriculturalDetails,
            contracts,
            yearSummaries,
            teamId,
            outdoorAreas,
            propertyAmenities,
            allAmenities,
            photos,
            dashboard,
            teamCurrency,
            locale);
    Context context = new Context(locale);
    context.setVariables(model);
    String html = templateEngine.process("property-booklet/generic", context);
    return pdfRenderer.render(html, PageSpec.A4_PORTRAIT);
  }

  // ── View-model ──────────────────────────────────────────────────

  private Map<String, Object> buildModel(
      Property property,
      @Nullable PropertyCategory category,
      @Nullable PropertyResidentialDetails residentialDetails,
      @Nullable PropertyCommercialDetails commercialDetails,
      @Nullable PropertyIndustrialDetails industrialDetails,
      @Nullable PropertyAgriculturalDetails agriculturalDetails,
      List<Contract> contracts,
      Map<Integer, FinancialYearSummary> yearSummaries,
      UUID teamId,
      List<PropertyOutdoorArea> outdoorAreas,
      List<PropertyAmenity> propertyAmenities,
      List<Amenity> allAmenities,
      List<Photo> photos,
      @Nullable PropertyDashboardResponse dashboard,
      String teamCurrency,
      Locale locale) {
    String identifier = property.getIdentifier().map(Sid::value).orElse("—");
    String area = buildAreaString(property);

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("identifier", identifier);
    v.put("street", property.getStreet());
    v.put("location", buildLocationString(property));
    v.put("propertyTypeLabel", enumLabels.label(property.getPropertyType(), locale));
    v.put("statusCode", property.getStatus() != null ? property.getStatus().name() : "VACANT");
    v.put(
        "statusLabel",
        property.getStatus() != null ? enumLabels.label(property.getStatus(), locale) : "—");

    v.put(
        "coverRows",
        buildCoverRows(
            property,
            category,
            residentialDetails,
            commercialDetails,
            industrialDetails,
            agriculturalDetails,
            area,
            locale));

    // ── Property overview ──
    List<Map<String, Object>> details = new ArrayList<>();
    addAlways(details, msg("field.property.type", locale), enumLabels.label(property.getPropertyType(), locale));
    addAlways(details, msg("field.status", locale), enumLabels.label(property.getStatus(), locale));
    addAlways(details, msg("field.total.area", locale), area);
    addAlways(details, msg("field.number.of.floors", locale), property.getNumberOfFloors().map(Object::toString).orElse(null));
    addAlways(details, msg("field.year.built", locale), property.getYearBuilt().map(Object::toString).orElse(null));
    addAlways(details, msg("field.last.renovated", locale), property.getYearLastRenovated().map(Object::toString).orElse(null));
    v.put("detailsFields", details);

    v.put("constructionFields", buildConstructionFields(property, locale));

    CategorySection cat =
        buildCategorySection(
            category, residentialDetails, commercialDetails, industrialDetails, agriculturalDetails, locale);
    v.put("categoryTitle", cat.title);
    v.put("categoryFields", cat.fields);

    v.put("structuralNotes", property.getStructuralNotes().filter(s -> !s.isBlank()).orElse(null));

    // ── Building specifications (skipped for agricultural) ──
    String energyRating = property.getEnergyEfficiencyRating().filter(s -> !s.isBlank()).orElse(null);
    List<Map<String, Object>> energyFields = buildEnergyFields(property, locale);
    String insulationNotes = property.getInsulationNotes().filter(s -> !s.isBlank()).orElse(null);
    List<Map<String, Object>> utilitiesFields = buildUtilitiesFields(property, locale);
    List<Map<String, Object>> parkingFields = buildParkingFields(property, locale);
    boolean hasBuildingSpecs =
        category != AGRICULTURAL
            && (energyRating != null
                || !energyFields.isEmpty()
                || insulationNotes != null
                || !utilitiesFields.isEmpty()
                || !parkingFields.isEmpty());
    v.put("hasBuildingSpecs", hasBuildingSpecs);
    v.put("energyRating", energyRating);
    v.put("energyColor", getEnergyRatingColor(energyRating));
    v.put("energyFields", energyFields);
    v.put("insulationNotes", insulationNotes);
    v.put("utilitiesFields", utilitiesFields);
    v.put("parkingFields", parkingFields);

    // ── Features & outdoor ──
    List<Map<String, Object>> amenityGroups = buildAmenityGroups(propertyAmenities, allAmenities);
    List<Map<String, Object>> outdoor = buildOutdoorAreas(outdoorAreas);
    v.put("amenityGroups", amenityGroups);
    v.put("outdoorAreas", outdoor);
    v.put("hasFeatures", !amenityGroups.isEmpty() || !outdoor.isEmpty());

    // ── Safety & accessibility (skipped for agricultural; accessibility skipped for industrial) ──
    buildSafetySection(v, property, category, locale);

    // ── Photo gallery ──
    v.put("photos", buildPhotos(photos));

    // ── Financial overview ──
    String currency =
        dashboard != null ? dashboard.summary().currency().orElse(teamCurrency) : teamCurrency;
    v.put("financialYears", buildFinancialYears(yearSummaries, currency, locale));

    // ── Contracts ──
    v.put("contracts", buildContracts(contracts, teamId, locale));

    // ── Investment dashboard (optional, feature-flagged) ──
    if (dashboard != null) {
      buildDashboard(v, dashboard, teamCurrency, locale);
    } else {
      v.put("hasDashboard", false);
      v.put("dashMetrics", List.of());
      v.put("cashFlow", List.of());
      v.put("expenseBreakdown", List.of());
      v.put("equityRows", List.of());
    }

    v.put("qrDataUri", qrCodeGenerator.toSvgDataUri(appBaseUrl + "/properties/" + identifier));
    v.put("generatedDate", formatter.date(LocalDate.now(clock), locale));
    return v;
  }

  // ── Cover ────────────────────────────────────────────────────────

  private List<Map<String, Object>> buildCoverRows(
      Property property,
      @Nullable PropertyCategory category,
      @Nullable PropertyResidentialDetails residential,
      @Nullable PropertyCommercialDetails commercial,
      @Nullable PropertyIndustrialDetails industrial,
      @Nullable PropertyAgriculturalDetails agricultural,
      String area,
      Locale locale) {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (category == COMMERCIAL && commercial != null) {
      addRow(rows, msg("cover.usable.common.area", locale),
          areaDisplay(commercial.getUsableAreaValue().orElse(null), commercial.getUsableAreaUnit().orElse(null))
              + " / "
              + areaDisplay(commercial.getCommonAreaValue().orElse(null), commercial.getCommonAreaUnit().orElse(null)));
      addRow(rows, msg("cover.floor.level", locale), commercial.getFloorLevel().map(Object::toString).orElse(null));
      addRow(rows, msg("cover.ceiling.height", locale),
          measureDisplay(commercial.getCeilingHeightValue().orElse(null), commercial.getCeilingHeightUnit().orElse(null), "m"));
    } else if (category == INDUSTRIAL && industrial != null) {
      addRow(rows, msg("cover.total.area", locale), area);
      addRow(rows, msg("cover.clear.height", locale),
          measureDisplay(industrial.getClearHeightValue().orElse(null), industrial.getClearHeightUnit().orElse(null), "m"));
      addRow(rows, msg("cover.loading.docks", locale), industrial.getLoadingDocks().map(Object::toString).orElse(null));
      addRow(rows, msg("cover.power.capacity", locale),
          industrial.getPowerCapacityValue().map(p -> p + " " + industrial.getPowerCapacityUnit().orElse("kVA")).orElse(null));
    } else if (category == AGRICULTURAL && agricultural != null) {
      addRow(rows, msg("cover.total.land.area", locale),
          areaDisplay(agricultural.getTotalLandAreaValue().orElse(null), agricultural.getTotalLandAreaUnit().orElse(null)));
      addRow(rows, msg("cover.arable.area", locale),
          areaDisplay(agricultural.getArableAreaValue().orElse(null), agricultural.getArableAreaUnit().orElse(null)));
      addRow(rows, msg("cover.soil.type", locale), agricultural.getSoilType().map(BookletHelper::formatEnumValue).orElse(null));
      addRow(rows, msg("cover.current.use", locale), agricultural.getCurrentUse().map(BookletHelper::formatEnumValue).orElse(null));
    } else if (category == RESIDENTIAL && residential != null) {
      String bedBath =
          residential.getBedrooms().map(b -> b + " " + msg("cover.bed", locale)).orElse("—")
              + " / "
              + residential.getBathrooms().map(b -> b + " " + msg("cover.bath", locale)).orElse("—");
      addRow(rows, msg("cover.bedrooms.bathrooms", locale), bedBath);
      addRow(rows, msg("cover.total.area", locale), area);
      addRow(rows, msg("cover.year.built", locale), property.getYearBuilt().map(Object::toString).orElse(null));
    } else {
      addRow(rows, msg("cover.total.area", locale), area);
      addRow(rows, msg("cover.year.built", locale), property.getYearBuilt().map(Object::toString).orElse(null));
      addRow(rows, msg("cover.floor.level", locale), property.getNumberOfFloors().map(Object::toString).orElse(null));
    }
    return rows;
  }

  // ── Property overview ────────────────────────────────────────────

  private List<Map<String, Object>> buildConstructionFields(Property property, Locale locale) {
    List<Map<String, Object>> f = new ArrayList<>();
    addField(f, msg("field.construction.type", locale), enumStr(property.getConstructionType()));
    addField(f, msg("field.foundation", locale), enumStr(property.getFoundationType()));
    addField(f, msg("field.roof.type", locale), enumStr(property.getRoofType()));
    addField(f, msg("field.window.type", locale), enumStr(property.getWindowType()));
    addField(f, msg("field.wall.construction", locale), enumStr(property.getWallConstruction()));
    addField(f, msg("field.flooring", locale), enumStr(property.getFlooringType()));
    return f;
  }

  private CategorySection buildCategorySection(
      @Nullable PropertyCategory category,
      @Nullable PropertyResidentialDetails residential,
      @Nullable PropertyCommercialDetails commercial,
      @Nullable PropertyIndustrialDetails industrial,
      @Nullable PropertyAgriculturalDetails agricultural,
      Locale locale) {
    List<Map<String, Object>> f = new ArrayList<>();
    if (category == RESIDENTIAL && residential != null) {
      addField(f, msg("field.bedrooms", locale), residential.getBedrooms().map(Object::toString).orElse(null));
      addField(f, msg("field.bathrooms", locale), residential.getBathrooms().map(Object::toString).orElse(null));
      addField(f, msg("field.furnished", locale), yesNo(residential.getFurnished(), locale));
      addField(f, msg("field.pet.policy", locale), residential.getPetPolicy().map(BookletHelper::formatEnumValue).orElse(null));
      return new CategorySection(msg("section.residential.details", locale), f);
    }
    if (category == COMMERCIAL && commercial != null) {
      addField(f, msg("field.usable.area", locale), areaDisplay(commercial.getUsableAreaValue().orElse(null), commercial.getUsableAreaUnit().orElse(null)));
      addField(f, msg("field.common.area", locale), areaDisplay(commercial.getCommonAreaValue().orElse(null), commercial.getCommonAreaUnit().orElse(null)));
      addField(f, msg("field.floor.level", locale), commercial.getFloorLevel().map(Object::toString).orElse(null));
      addField(f, msg("field.ceiling.height", locale), measureDisplay(commercial.getCeilingHeightValue().orElse(null), commercial.getCeilingHeightUnit().orElse(null), "m"));
      addField(f, msg("field.storefront", locale), yesNo(commercial.getHasStorefront(), locale));
      addField(f, msg("field.signage.rights", locale), yesNo(commercial.getHasSignageRights(), locale));
      addField(f, msg("field.max.occupancy", locale), commercial.getMaxOccupancy().map(Object::toString).orElse(null));
      addField(f, msg("field.restrooms", locale), commercial.getRestroomCount().map(Object::toString).orElse(null));
      addField(f, msg("field.kitchen.facility", locale), yesNo(commercial.getHasKitchenFacility(), locale));
      addField(f, msg("field.accessibility.compliant", locale), yesNo(commercial.getAccessibilityCompliant(), locale));
      addField(f, msg("field.zoning", locale), commercial.getZoningClassification().orElse(null));
      return new CategorySection(msg("section.commercial.details", locale), f);
    }
    if (category == INDUSTRIAL && industrial != null) {
      addField(f, msg("field.clear.height", locale), measureDisplay(industrial.getClearHeightValue().orElse(null), industrial.getClearHeightUnit().orElse(null), "m"));
      addField(f, msg("field.loading.docks", locale), industrial.getLoadingDocks().map(Object::toString).orElse(null));
      addField(f, msg("field.drive.in.doors", locale), industrial.getDriveInDoors().map(Object::toString).orElse(null));
      addField(f, msg("field.floor.load.capacity", locale), measureDisplay(industrial.getFloorLoadCapacityValue().orElse(null), industrial.getFloorLoadCapacityUnit().orElse(null), "kg/m²"));
      addField(f, msg("field.power.capacity", locale), industrial.getPowerCapacityValue().map(p -> p + " " + industrial.getPowerCapacityUnit().orElse("kVA")).orElse(null));
      addField(f, msg("field.three.phase.power", locale), yesNo(industrial.getHasThreePhasePower(), locale));
      addField(f, msg("field.crane", locale), craneText(industrial, locale));
      addField(f, msg("field.hazmat.certification", locale), yesNo(industrial.getHasHazmatCertification(), locale));
      addField(f, msg("field.ventilation.system", locale), yesNo(industrial.getHasVentilationSystem(), locale));
      addField(f, msg("field.climate.control", locale), yesNo(industrial.getHasClimateControl(), locale));
      addField(f, msg("field.yard.area", locale), areaDisplay(industrial.getYardAreaValue().orElse(null), industrial.getYardAreaUnit().orElse(null)));
      addField(f, msg("field.zoning", locale), industrial.getZoningClassification().orElse(null));
      return new CategorySection(msg("section.industrial.details", locale), f);
    }
    if (category == AGRICULTURAL && agricultural != null) {
      addField(f, msg("field.total.land.area", locale), areaDisplay(agricultural.getTotalLandAreaValue().orElse(null), agricultural.getTotalLandAreaUnit().orElse(null)));
      addField(f, msg("field.arable.area", locale), areaDisplay(agricultural.getArableAreaValue().orElse(null), agricultural.getArableAreaUnit().orElse(null)));
      addField(f, msg("field.soil.type", locale), agricultural.getSoilType().map(BookletHelper::formatEnumValue).orElse(null));
      addField(f, msg("field.water.rights", locale), yesNo(agricultural.getHasWaterRights(), locale));
      addField(f, msg("field.water.source", locale), agricultural.getWaterSource().map(BookletHelper::formatEnumValue).orElse(null));
      addField(f, msg("field.irrigation", locale), agricultural.getIrrigationType().map(BookletHelper::formatEnumValue).orElse(null));
      addField(f, msg("field.fencing", locale), agricultural.getFencingType().map(BookletHelper::formatEnumValue).orElse(null));
      addField(f, msg("field.outbuildings", locale), outbuildingsText(agricultural, locale));
      addField(f, msg("field.current.use", locale), agricultural.getCurrentUse().map(BookletHelper::formatEnumValue).orElse(null));
      addField(f, msg("field.zoning", locale), agricultural.getZoningClassification().orElse(null));
      return new CategorySection(msg("section.agricultural.details", locale), f);
    }
    return new CategorySection(null, f);
  }

  private @Nullable String craneText(PropertyIndustrialDetails d, Locale locale) {
    return d.getHasCrane()
        .map(
            v ->
                isTrue(v)
                    ? msg("value.yes", locale)
                        + d.getCraneCapacityValue()
                            .map(t -> " (" + t + " " + d.getCraneCapacityUnit().orElse("metric_tons") + ")")
                            .orElse("")
                    : msg("value.no", locale))
        .orElse(null);
  }

  private @Nullable String outbuildingsText(PropertyAgriculturalDetails d, Locale locale) {
    return d.getHasOutbuildings()
        .map(
            v ->
                isTrue(v)
                    ? msg("value.yes", locale) + d.getOutbuildingDetails().map(t -> " — " + t).orElse("")
                    : msg("value.no", locale))
        .orElse(null);
  }

  // ── Building specifications ──────────────────────────────────────

  private List<Map<String, Object>> buildEnergyFields(Property property, Locale locale) {
    List<Map<String, Object>> f = new ArrayList<>();
    addField(f, msg("field.heating.system", locale), enumStr(property.getHeatingType()));
    addField(f, msg("field.cooling.system", locale), enumStr(property.getCoolingType()));
    addField(f, msg("field.hot.water.system", locale), enumStr(property.getHotWaterSystem()));
    addField(f, msg("field.certificate.expiry", locale),
        property.getEnergyCertificateExpiryDate().map(d -> formatter.date(d, locale)).orElse(null));
    return f;
  }

  private List<Map<String, Object>> buildUtilitiesFields(Property property, Locale locale) {
    List<Map<String, Object>> f = new ArrayList<>();
    addField(f, msg("field.electricity", locale), enumStr(property.getElectricityConnectionType()));
    addField(f, msg("field.capacity", locale),
        property.getElectricityCapacityValue().map(a -> a + " " + property.getElectricityCapacityUnit().orElse("A")).orElse(null));
    addField(f, msg("field.water", locale), enumStr(property.getWaterConnectionType()));
    addField(f, msg("field.gas.connection", locale),
        property.getHasGasConnection()
            .map(g -> isTrue(g) ? msg("value.connected", locale) : msg("value.not.connected", locale))
            .orElse(null));
    addField(f, msg("field.sewage", locale), enumStr(property.getSewageType()));
    addField(f, msg("field.internet", locale), enumStr(property.getInternetConnectionType()));
    addField(f, msg("field.max.speed", locale),
        property.getInternetMaxSpeedValue().map(s -> s + " " + property.getInternetMaxSpeedUnit().orElse("Mbps")).orElse(null));
    addField(f, msg("field.internet.status", locale), enumStr(property.getInternetStatus()));
    return f;
  }

  private List<Map<String, Object>> buildParkingFields(Property property, Locale locale) {
    List<Map<String, Object>> f = new ArrayList<>();
    addField(f, msg("field.parking.type", locale), enumStr(property.getParkingType()));
    addField(f, msg("field.parking.spaces", locale), property.getParkingSpaces().map(Object::toString).orElse(null));
    return f;
  }

  // ── Features & outdoor ───────────────────────────────────────────

  private List<Map<String, Object>> buildAmenityGroups(
      List<PropertyAmenity> propertyAmenities, List<Amenity> allAmenities) {
    Map<UUID, Amenity> amenityMap =
        allAmenities.stream().collect(Collectors.toMap(Amenity::getId, a -> a, (a, b) -> a));
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    for (PropertyAmenity pa : propertyAmenities) {
      Amenity amenity = amenityMap.get(pa.getAmenityId());
      if (amenity == null) {
        continue;
      }
      String category = amenity.getCategory() != null ? amenity.getCategory() : "Other";
      grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(amenity.getName());
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map.Entry<String, List<String>> e : grouped.entrySet()) {
      Map<String, Object> m = new HashMap<>();
      m.put("category", formatEnumValue(e.getKey()));
      m.put("items", e.getValue());
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildOutdoorAreas(List<PropertyOutdoorArea> areas) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (PropertyOutdoorArea oa : areas) {
      Map<String, Object> m = new HashMap<>();
      m.put("type", formatEnumValue(oa.getType()));
      m.put(
          "area",
          oa.getAreaValue()
              .map(val -> val + " " + (oa.getAreaUnit() != null ? oa.getAreaUnit() : "sqm"))
              .orElse(null));
      out.add(m);
    }
    return out;
  }

  // ── Safety & accessibility ───────────────────────────────────────

  private void buildSafetySection(
      Map<String, Object> v, Property property, @Nullable PropertyCategory category, Locale locale) {
    boolean hasSafetyData =
        isTrue(property.getHasSmokeDetectors().orElse(null))
            || isTrue(property.getHasCoDetectors().orElse(null))
            || isTrue(property.getHasFireExtinguisher().orElse(null))
            || isTrue(property.getHasSprinklerSystem().orElse(null))
            || isTrue(property.getHasAlarmSystem().orElse(null))
            || isTrue(property.getHasSecurityCameras().orElse(null))
            || isTrue(property.getHasSecureEntry().orElse(null))
            || property.getSafetyNotes().filter(s -> !s.isBlank()).isPresent();

    boolean skipAccessibility = category == INDUSTRIAL;
    boolean hasAccessData =
        !skipAccessibility
            && (isTrue(property.getIsWheelchairAccessible().orElse(null))
                || isTrue(property.getHasElevator().orElse(null))
                || isTrue(property.getHasStepFreeEntrance().orElse(null))
                || isTrue(property.getHasAdaptedBathroom().orElse(null))
                || property.getAccessibilityNotes().filter(s -> !s.isBlank()).isPresent());

    List<Map<String, Object>> safetyChecks = new ArrayList<>();
    List<Map<String, Object>> accessChecks = new ArrayList<>();
    if (hasSafetyData) {
      safetyChecks.add(check(msg("check.smoke.detectors", locale), property.getHasSmokeDetectors().orElse(null)));
      safetyChecks.add(check(msg("check.co.detectors", locale), property.getHasCoDetectors().orElse(null)));
      safetyChecks.add(check(msg("check.fire.extinguisher", locale), property.getHasFireExtinguisher().orElse(null)));
      safetyChecks.add(check(msg("check.sprinkler.system", locale), property.getHasSprinklerSystem().orElse(null)));
      safetyChecks.add(check(msg("check.alarm.system", locale), property.getHasAlarmSystem().orElse(null)));
      safetyChecks.add(check(msg("check.security.cameras", locale), property.getHasSecurityCameras().orElse(null)));
      safetyChecks.add(check(msg("check.secure.entry", locale), property.getHasSecureEntry().orElse(null)));
    }
    if (hasAccessData) {
      accessChecks.add(check(msg("check.wheelchair.accessible", locale), property.getIsWheelchairAccessible().orElse(null)));
      accessChecks.add(check(msg("check.elevator", locale), property.getHasElevator().orElse(null)));
      accessChecks.add(check(msg("check.step.free.entrance", locale), property.getHasStepFreeEntrance().orElse(null)));
      accessChecks.add(check(msg("check.adapted.bathroom", locale), property.getHasAdaptedBathroom().orElse(null)));
    }

    v.put("hasSafety", category != AGRICULTURAL && (hasSafetyData || hasAccessData));
    v.put("safetyChecks", safetyChecks);
    v.put("safetyNotes", hasSafetyData ? property.getSafetyNotes().filter(s -> !s.isBlank()).orElse(null) : null);
    v.put("accessibilityChecks", accessChecks);
    v.put("accessibilityNotes", hasAccessData ? property.getAccessibilityNotes().filter(s -> !s.isBlank()).orElse(null) : null);
  }

  private Map<String, Object> check(String label, @Nullable Boolean value) {
    Map<String, Object> m = new HashMap<>();
    m.put("label", label);
    m.put("ok", isTrue(value));
    return m;
  }

  // ── Photos ───────────────────────────────────────────────────────

  private List<Map<String, Object>> buildPhotos(List<Photo> photos) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Photo photo : photos) {
      String dataUri = photoToBase64DataUri(photo);
      if (dataUri == null) {
        continue;
      }
      Map<String, Object> m = new HashMap<>();
      m.put("src", dataUri);
      m.put("label", photo.getTitle().orElse(photo.getFileName()));
      m.put("main", isTrue(photo.getIsMainPhoto()));
      out.add(m);
    }
    return out;
  }

  // ── Financial overview ───────────────────────────────────────────

  private List<Map<String, Object>> buildFinancialYears(
      Map<Integer, FinancialYearSummary> yearSummaries, @Nullable String currency, Locale locale) {
    String ccy = currency != null ? currency : "EUR";
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map.Entry<Integer, FinancialYearSummary> e : yearSummaries.entrySet()) {
      FinancialYearSummary s = e.getValue();
      Map<String, Object> m = new HashMap<>();
      m.put("year", String.valueOf(e.getKey()));
      m.put("income", CurrencyUtils.formatCurrency(s.income, ccy, locale));
      m.put("expenses", CurrencyUtils.formatCurrency(s.expenses, ccy, locale));
      m.put("net", CurrencyUtils.formatCurrency(s.getNetProfit(), ccy, locale));
      out.add(m);
    }
    return out;
  }

  // ── Contracts ────────────────────────────────────────────────────

  private List<Map<String, Object>> buildContracts(List<Contract> contracts, UUID teamId, Locale locale) {
    if (contracts.isEmpty()) {
      return List.of();
    }
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    Map<UUID, Contact> primaryContacts =
        contractPartyService.getPrimaryContactsForContracts(contractIds, teamId);
    List<ContractExtension> allExtensions =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream().collect(Collectors.groupingBy(ContractExtension::getContractId));

    List<Map<String, Object>> out = new ArrayList<>();
    for (Contract contract : contracts) {
      Contact contact = primaryContacts.get(contract.getId());
      List<ContractExtension> extensions =
          extensionsByContract.getOrDefault(contract.getId(), List.of());
      Optional<LocalDate> effectiveEndDate =
          EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);
      Map<String, Object> m = new HashMap<>();
      m.put("id", contract.getIdentifier().map(Sid::value).orElse("—"));
      m.put("contact", contact != null ? contact.getDisplayName() : msg("value.unknown", locale));
      m.put("start", formatter.date(contract.getStartDate(), locale));
      m.put("end", effectiveEndDate.map(d -> formatter.date(d, locale)).orElse(msg("value.ongoing", locale)));
      m.put("rent", formatter.money(contract.getRentAmount(), locale));
      m.put("statusCode", contract.getStatus() != null ? contract.getStatus().name() : "DRAFT");
      m.put("statusLabel", contract.getStatus() != null ? enumLabels.label(contract.getStatus(), locale) : "—");
      out.add(m);
    }
    return out;
  }

  // ── Investment dashboard ─────────────────────────────────────────

  private void buildDashboard(
      Map<String, Object> v, PropertyDashboardResponse dashboard, String teamCurrency, Locale locale) {
    SummaryMetrics s = dashboard.summary();
    String ccy = s.currency().orElse(teamCurrency);

    List<Map<String, Object>> metrics = new ArrayList<>();
    metrics.add(kv(msg("dashboard.total.roi", locale), pct(s.totalRoiPercent().orElse(null), locale)));
    metrics.add(kv(msg("dashboard.annualized.roi", locale), pct(s.annualizedRoiPercent().orElse(null), locale)));
    metrics.add(kv(msg("dashboard.cap.rate", locale), pct(s.capRatePercent().orElse(null), locale)));
    metrics.add(kv(msg("dashboard.cash.on.cash", locale), pct(s.cashOnCashPercent().orElse(null), locale)));
    metrics.add(kv(msg("dashboard.monthly.cash.flow", locale), moneyOpt(s.monthlyCashFlow().orElse(null), ccy, locale)));
    metrics.add(kv(msg("dashboard.annual.noi", locale), moneyOpt(s.annualNoi().orElse(null), ccy, locale)));
    metrics.add(kv(msg("dashboard.total.equity", locale), moneyOpt(s.totalEquity().orElse(null), ccy, locale)));
    metrics.add(kv(msg("dashboard.occupancy", locale), pct(s.occupancyRatePercent().orElse(null), locale)));

    List<Map<String, Object>> cashFlow = new ArrayList<>();
    for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
      Map<String, Object> row = new HashMap<>();
      row.put("month", m.month());
      row.put("income", CurrencyUtils.formatCurrency(m.income(), ccy, locale));
      row.put("expenses", CurrencyUtils.formatCurrency(m.expenses(), ccy, locale));
      row.put("mortgage", CurrencyUtils.formatCurrency(m.mortgage(), ccy, locale));
      row.put("net", CurrencyUtils.formatCurrency(m.net(), ccy, locale));
      row.put("net_positive", m.net().signum() >= 0);
      cashFlow.add(row);
    }

    List<Map<String, Object>> breakdown = new ArrayList<>();
    for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
      Map<String, Object> row = new HashMap<>();
      row.put("category", formatEnumValue(c.category()));
      row.put("amount", CurrencyUtils.formatCurrency(c.amount(), ccy, locale));
      breakdown.add(row);
    }

    List<Map<String, Object>> equity = new ArrayList<>();
    addEquity(equity, msg("equity.purchase.price", locale), dashboard.equity().purchasePrice().orElse(null), ccy, locale);
    addEquity(equity, msg("equity.current.market.value", locale), dashboard.equity().currentMarketValue().orElse(null), ccy, locale);
    addEquity(equity, msg("equity.mortgage.balance", locale), dashboard.equity().mortgageBalance().orElse(null), ccy, locale);

    v.put("hasDashboard", true);
    v.put("dashMetrics", metrics);
    v.put("cashFlow", cashFlow);
    v.put("expenseBreakdown", breakdown);
    v.put("equityRows", equity);
  }

  private void addEquity(
      List<Map<String, Object>> list, String label, @Nullable BigDecimal value, String ccy, Locale locale) {
    if (value == null) {
      return;
    }
    list.add(kv(label, CurrencyUtils.formatCurrency(value, ccy, locale)));
  }

  private String pct(@Nullable BigDecimal value, Locale locale) {
    return value != null ? formatter.numberOrDash(value, locale) + "%" : "—";
  }

  private String moneyOpt(@Nullable BigDecimal value, String ccy, Locale locale) {
    return value != null ? CurrencyUtils.formatCurrency(value, ccy, locale) : "—";
  }

  // ── Field helpers ────────────────────────────────────────────────

  private Map<String, Object> kv(String label, String value) {
    Map<String, Object> m = new HashMap<>();
    m.put("l", label);
    m.put("v", value);
    return m;
  }

  /** Adds a field only when it carries a real value (skips null/blank/em-dash). */
  private void addField(List<Map<String, Object>> list, String label, @Nullable String value) {
    if (value != null && !value.isBlank() && !"—".equals(value)) {
      list.add(kv(label, value));
    }
  }

  /** Adds a field always, substituting an em-dash for a missing value (core identity rows). */
  private void addAlways(List<Map<String, Object>> list, String label, @Nullable String value) {
    list.add(kv(label, value != null && !value.isBlank() ? value : "—"));
  }

  private void addRow(List<Map<String, Object>> rows, String key, @Nullable String value) {
    Map<String, Object> m = new HashMap<>();
    m.put("k", key);
    m.put("v", value != null && !value.isBlank() ? value : "—");
    rows.add(m);
  }

  private @Nullable String yesNo(Optional<Boolean> opt, Locale locale) {
    return opt.map(b -> isTrue(b) ? msg("value.yes", locale) : msg("value.no", locale)).orElse(null);
  }

  private @Nullable String enumStr(Optional<String> opt) {
    return opt.filter(s -> !s.isBlank()).map(BookletHelper::formatEnumValue).orElse(null);
  }

  // ── Domain helpers (unchanged) ───────────────────────────────────

  private String buildLocationString(Property property) {
    StringBuilder location = new StringBuilder();
    location.append(property.getCity());
    if (property.getPostalCode() != null) {
      location.append(", ").append(property.getPostalCode());
    }
    if (property.getCountryCode() != null) {
      location.append(", ").append(property.getCountryCode());
    }
    return location.toString();
  }

  private String buildAreaString(Property property) {
    if (property.getAreaValue().isEmpty()) {
      return "—";
    }
    String unit = property.getAreaUnit().orElse("sqm");
    return property.getAreaValue().get() + " " + unit;
  }

  private String areaDisplay(@Nullable BigDecimal value, @Nullable String unit) {
    return measureDisplay(value, unit, "sqm");
  }

  private String measureDisplay(@Nullable BigDecimal value, @Nullable String unit, String defaultUnit) {
    if (value == null) {
      return "—";
    }
    return value + " " + (unit != null ? unit : defaultUnit);
  }

  private @Nullable String photoToBase64DataUri(Photo photo) {
    String fileKey = photo.getThumbnailFileKey().orElse(photo.getFileKey());
    try (InputStream is = s3StorageService.downloadFile(fileKey)) {
      byte[] bytes = is.readAllBytes();
      String mime = photo.getMimeType() != null ? photo.getMimeType() : "image/jpeg";
      return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      return null;
    }
  }

  private Map<Integer, FinancialYearSummary> calculateYearSummaries(
      List<Payment> payments, List<Expense> expenses) {
    Map<Integer, FinancialYearSummary> summaries = new TreeMap<>(Comparator.reverseOrder());
    for (Payment payment : payments) {
      if (payment.getStatus() == PAID) {
        payment
            .getPaymentDate()
            .ifPresent(
                paymentDate -> {
                  int year = paymentDate.getYear();
                  summaries.computeIfAbsent(year, FinancialYearSummary::new);
                  summaries.get(year).addIncome(payment.getAmount().value());
                });
      }
    }
    for (Expense expense : expenses) {
      int year = expense.getExpenseDate().getYear();
      summaries.computeIfAbsent(year, FinancialYearSummary::new);
      summaries.get(year).addExpense(expense.getAmount().value());
    }
    return summaries;
  }

  private String getEnergyRatingColor(@Nullable String rating) {
    if (rating == null) {
      return "#78716c";
    }
    return switch (rating) {
      case "A++++" -> "#052e16";
      case "A+++" -> "#064e3b";
      case "A++" -> "#065f46";
      case "A+" -> "#047857";
      case "A" -> "#059669";
      case "B" -> "#65a30d";
      case "C" -> "#ca8a04";
      case "D" -> "#ea580c";
      case "E" -> "#dc2626";
      case "F" -> "#b91c1c";
      case "G" -> "#991b1b";
      default -> "#78716c";
    };
  }

  // ── Inner types ──────────────────────────────────────────────────

  private record CategorySection(@Nullable String title, List<Map<String, Object>> fields) {}

  private static class FinancialYearSummary {
    private final int year;
    private BigDecimal income = ZERO;
    private BigDecimal expenses = ZERO;

    FinancialYearSummary(int year) {
      this.year = year;
    }

    void addIncome(BigDecimal amount) {
      this.income = this.income.add(amount);
    }

    void addExpense(BigDecimal amount) {
      this.expenses = this.expenses.add(amount);
    }

    BigDecimal getNetProfit() {
      return income.subtract(expenses);
    }
  }
}
