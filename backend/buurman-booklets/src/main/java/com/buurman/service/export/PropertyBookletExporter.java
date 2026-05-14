package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Property.PropertyCategory.AGRICULTURAL;
import static com.buurman.domain.Property.PropertyCategory.COMMERCIAL;
import static com.buurman.domain.Property.PropertyCategory.INDUSTRIAL;
import static com.buurman.domain.Property.PropertyCategory.RESIDENTIAL;
import static com.buurman.service.export.BookletHelper.appendCheckItem;
import static com.buurman.service.export.BookletHelper.appendCoverCell;
import static com.buurman.service.export.BookletHelper.appendCoverEnd;
import static com.buurman.service.export.BookletHelper.appendCoverStart;
import static com.buurman.service.export.BookletHelper.appendDocumentEnd;
import static com.buurman.service.export.BookletHelper.appendDocumentStart;
import static com.buurman.service.export.BookletHelper.appendField;
import static com.buurman.service.export.BookletHelper.appendPageEnd;
import static com.buurman.service.export.BookletHelper.appendPageStart;
import static com.buurman.service.export.BookletHelper.appendRunningFooter;
import static com.buurman.service.export.BookletHelper.appendSectionTitle;
import static com.buurman.service.export.BookletHelper.appendStatusBadge;
import static com.buurman.service.export.BookletHelper.appendTextBlock;
import static com.buurman.service.export.BookletHelper.escapeHtml;
import static com.buurman.service.export.BookletHelper.formatEnumValue;
import static com.buurman.service.export.BookletHelper.isTrue;
import static com.buurman.service.export.BookletHelper.propertyTypeIconHtml;
import static java.math.BigDecimal.ZERO;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

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
import com.buurman.service.ContractPartyService;
import com.buurman.service.EffectiveEndDateHelper;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;
import com.buurman.service.S3StorageService;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.FeatureFlags;

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
  private final PdfRenderer pdfRenderer;
  private final MessageSource messageSource;
  private final Clock clock;

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
      PdfRenderer pdfRenderer,
      @Qualifier("bookletMessageSource") MessageSource messageSource,
      Clock clock) {
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
    this.pdfRenderer = pdfRenderer;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  private String msg(String key, Locale locale) {
    return java.util.Objects.requireNonNullElse(
        messageSource.getMessage(key, null, key, locale), key);
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

    PropertyDashboardResponse dashboard = null;
    if (featureFlagService.isEnabled(FeatureFlags.REPORTS)) {
      try {
        dashboard = propertyDashboardService.getDashboardData(propertyIdentifier, 0, teamId);
      } catch (Exception e) {
        // Non-critical: booklet still generates without dashboard section
      }
    }

    String html =
        buildHtml(
            property,
            category,
            residentialDetails,
            commercialDetails,
            industrialDetails,
            agriculturalDetails,
            contracts,
            payments,
            expenses,
            yearSummaries,
            teamId,
            outdoorAreas,
            propertyAmenities,
            allAmenities,
            photos,
            dashboard,
            locale);
    return pdfRenderer.renderHtml(html);
  }

  // ── HTML building ───────────────────────────────────────────────

  private String buildHtml(
      Property property,
      @Nullable PropertyCategory category,
      @Nullable PropertyResidentialDetails residentialDetails,
      @Nullable PropertyCommercialDetails commercialDetails,
      @Nullable PropertyIndustrialDetails industrialDetails,
      @Nullable PropertyAgriculturalDetails agriculturalDetails,
      List<Contract> contracts,
      List<Payment> payments,
      List<Expense> expenses,
      Map<Integer, FinancialYearSummary> yearSummaries,
      UUID teamId,
      List<PropertyOutdoorArea> outdoorAreas,
      List<PropertyAmenity> propertyAmenities,
      List<Amenity> allAmenities,
      List<Photo> photos,
      @Nullable PropertyDashboardResponse dashboard,
      Locale locale) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    String generatedDate = LocalDate.now(clock).format(dateFmt);
    Map<UUID, Amenity> amenityMap =
        allAmenities.stream().collect(Collectors.toMap(Amenity::getId, a -> a));

    String location = buildLocationString(property);
    String area = buildAreaString(property);

    String css =
        BookletCss.base()
            + BookletCss.propertyStatusBadges()
            + BookletCss.checkItems()
            + BookletCss.paymentTable();

    StringBuilder html = new StringBuilder(8192);
    appendDocumentStart(html, css);
    appendRunningFooter(html, generatedDate);

    appendCoverPage(
        html,
        property,
        category,
        residentialDetails,
        commercialDetails,
        industrialDetails,
        agriculturalDetails,
        generatedDate,
        location,
        area,
        locale);
    appendPropertyOverviewPage(
        html,
        property,
        category,
        residentialDetails,
        commercialDetails,
        industrialDetails,
        agriculturalDetails,
        area,
        locale);

    if (category != AGRICULTURAL) {
      appendBuildingSpecsPage(html, property, locale);
    }

    appendFeaturesPage(html, propertyAmenities, amenityMap, outdoorAreas, locale);

    if (category != AGRICULTURAL) {
      appendSafetyPage(html, property, category, locale);
    }

    appendPhotoGalleryPage(html, photos);
    String currency = dashboard != null ? dashboard.summary().currency().orElse(null) : null;
    appendFinancialOverviewPage(html, yearSummaries, currency);

    if (dashboard != null) {
      appendDashboardPage(html, dashboard);
    }

    appendContractsPage(html, contracts, teamId);

    appendDocumentEnd(html);
    return html.toString();
  }

  // ── Page: Cover ─────────────────────────────────────────────────

  private void appendCoverPage(
      StringBuilder html,
      Property property,
      @Nullable PropertyCategory category,
      @Nullable PropertyResidentialDetails residentialDetails,
      @Nullable PropertyCommercialDetails commercialDetails,
      @Nullable PropertyIndustrialDetails industrialDetails,
      @Nullable PropertyAgriculturalDetails agriculturalDetails,
      String generatedDate,
      String location,
      String area,
      Locale locale) {
    appendCoverStart(
        html, msg("cover.title", locale), escapeHtml(property.getStreet()), generatedDate);

    String statusStr = property.getStatus() != null ? property.getStatus().name() : "VACANT";
    appendStatusBadge(html, statusStr);

    html.append("<table class='cover-summary'>");

    if (category == COMMERCIAL && commercialDetails != null) {
      appendCoverSummaryCommercial(html, property, commercialDetails, location, locale);
    } else if (category == INDUSTRIAL && industrialDetails != null) {
      appendCoverSummaryIndustrial(html, property, industrialDetails, location, area, locale);
    } else if (category == AGRICULTURAL && agriculturalDetails != null) {
      appendCoverSummaryAgricultural(html, property, agriculturalDetails, location, locale);
    } else if (category == RESIDENTIAL && residentialDetails != null) {
      appendCoverSummaryResidential(html, property, residentialDetails, location, area, locale);
    } else {
      // MIXED_USE or fallback (no detail record)
      appendCoverSummaryMixedUse(html, property, location, area, locale);
    }

    html.append("</table>");
    appendCoverEnd(html);
  }

  private void appendCoverSummaryResidential(
      StringBuilder html,
      Property property,
      PropertyResidentialDetails details,
      String location,
      String area,
      Locale locale) {
    html.append("<tr>");
    appendCoverCell(
        html,
        msg("cover.property.type", locale),
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, msg("cover.location", locale), location);
    html.append("</tr><tr>");
    String bedBath =
        details.getBedrooms().map(v -> v + " bed").orElse("—")
            + " / "
            + details.getBathrooms().map(v -> v + " bath").orElse("—");
    appendCoverCell(html, msg("cover.bedrooms.bathrooms", locale), bedBath);
    appendCoverCell(html, msg("cover.total.area", locale), area);
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        msg("cover.year.built", locale),
        property.getYearBuilt().map(Object::toString).orElse("—"));
    appendCoverCell(
        html, msg("cover.reference", locale), property.getIdentifier().orElseThrow().value());
    html.append("</tr>");
  }

  private void appendCoverSummaryCommercial(
      StringBuilder html,
      Property property,
      PropertyCommercialDetails details,
      String location,
      Locale locale) {
    html.append("<tr>");
    appendCoverCell(
        html,
        msg("cover.property.type", locale),
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, msg("cover.location", locale), location);
    html.append("</tr><tr>");
    String usable =
        buildAreaDisplay(
            details.getUsableAreaValue().orElse(null), details.getUsableAreaUnit().orElse(null));
    String common =
        buildAreaDisplay(
            details.getCommonAreaValue().orElse(null), details.getCommonAreaUnit().orElse(null));
    appendCoverCell(html, msg("cover.usable.common.area", locale), usable + " / " + common);
    appendCoverCell(
        html,
        msg("cover.floor.level", locale),
        details.getFloorLevel().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        msg("cover.ceiling.height", locale),
        buildMeasureDisplay(
            details.getCeilingHeightValue().orElse(null),
            details.getCeilingHeightUnit().orElse(null),
            "m"));
    appendCoverCell(
        html, msg("cover.reference", locale), property.getIdentifier().orElseThrow().value());
    html.append("</tr>");
  }

  private void appendCoverSummaryIndustrial(
      StringBuilder html,
      Property property,
      PropertyIndustrialDetails details,
      String location,
      String area,
      Locale locale) {
    html.append("<tr>");
    appendCoverCell(
        html,
        msg("cover.property.type", locale),
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, msg("cover.location", locale), location);
    html.append("</tr><tr>");
    appendCoverCell(html, msg("cover.total.area", locale), area);
    appendCoverCell(
        html,
        msg("cover.clear.height", locale),
        buildMeasureDisplay(
            details.getClearHeightValue().orElse(null),
            details.getClearHeightUnit().orElse(null),
            "m"));
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        msg("cover.loading.docks", locale),
        details.getLoadingDocks().map(Object::toString).orElse("—"));
    appendCoverCell(
        html,
        msg("cover.power.capacity", locale),
        details
            .getPowerCapacityValue()
            .map(v -> v + " " + details.getPowerCapacityUnit().orElse("kVA"))
            .orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(
        html, msg("cover.reference", locale), property.getIdentifier().orElseThrow().value());
    html.append("<td></td>");
    html.append("</tr>");
  }

  private void appendCoverSummaryAgricultural(
      StringBuilder html,
      Property property,
      PropertyAgriculturalDetails details,
      String location,
      Locale locale) {
    html.append("<tr>");
    appendCoverCell(
        html,
        msg("cover.property.type", locale),
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, msg("cover.location", locale), location);
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        msg("cover.total.land.area", locale),
        buildAreaDisplay(
            details.getTotalLandAreaValue().orElse(null),
            details.getTotalLandAreaUnit().orElse(null)));
    appendCoverCell(
        html,
        msg("cover.arable.area", locale),
        buildAreaDisplay(
            details.getArableAreaValue().orElse(null), details.getArableAreaUnit().orElse(null)));
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        msg("cover.soil.type", locale),
        details.getSoilType().map(BookletHelper::formatEnumValue).orElse("—"));
    appendCoverCell(
        html,
        msg("cover.current.use", locale),
        details.getCurrentUse().map(BookletHelper::formatEnumValue).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(
        html, msg("cover.reference", locale), property.getIdentifier().orElseThrow().value());
    html.append("<td></td>");
    html.append("</tr>");
  }

  private void appendCoverSummaryMixedUse(
      StringBuilder html, Property property, String location, String area, Locale locale) {
    html.append("<tr>");
    appendCoverCell(
        html,
        msg("cover.property.type", locale),
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, msg("cover.location", locale), location);
    html.append("</tr><tr>");
    appendCoverCell(html, msg("cover.total.area", locale), area);
    appendCoverCell(
        html,
        msg("cover.year.built", locale),
        property.getYearBuilt().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(
        html, msg("cover.reference", locale), property.getIdentifier().orElseThrow().value());
    html.append("<td></td>");
    html.append("</tr>");
  }

  // ── Page: Property Overview ─────────────────────────────────────

  private void appendPropertyOverviewPage(
      StringBuilder html,
      Property property,
      @Nullable PropertyCategory category,
      @Nullable PropertyResidentialDetails residentialDetails,
      @Nullable PropertyCommercialDetails commercialDetails,
      @Nullable PropertyIndustrialDetails industrialDetails,
      @Nullable PropertyAgriculturalDetails agriculturalDetails,
      String area,
      Locale locale) {
    appendPageStart(html, msg("page.property.overview", locale));

    appendSectionTitle(html, msg("section.property.details", locale));
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        msg("field.property.type", locale),
        formatEnumValue(property.getPropertyType().name()));
    appendField(html, msg("field.status", locale), formatEnumValue(property.getStatus().name()));
    html.append("</tr><tr>");
    appendField(html, msg("field.total.area", locale), area);
    appendField(
        html,
        msg("field.number.of.floors", locale),
        property.getNumberOfFloors().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.year.built", locale),
        property.getYearBuilt().map(Object::toString).orElse("—"));
    appendField(
        html,
        msg("field.last.renovated", locale),
        property.getYearLastRenovated().map(Object::toString).orElse("—"));
    html.append("</tr>");
    html.append("</table>");

    appendConstructionSection(html, property, locale);

    if (category == RESIDENTIAL && residentialDetails != null) {
      appendResidentialDetailsSection(html, residentialDetails, locale);
    } else if (category == COMMERCIAL && commercialDetails != null) {
      appendCommercialDetailsSection(html, commercialDetails, locale);
    } else if (category == INDUSTRIAL && industrialDetails != null) {
      appendIndustrialDetailsSection(html, industrialDetails, locale);
    } else if (category == AGRICULTURAL && agriculturalDetails != null) {
      appendAgriculturalDetailsSection(html, agriculturalDetails, locale);
    }

    appendTextBlock(
        html, msg("field.structural.notes", locale), property.getStructuralNotes().orElse(null));

    appendPageEnd(html);
  }

  private void appendResidentialDetailsSection(
      StringBuilder html, PropertyResidentialDetails details, Locale locale) {
    appendSectionTitle(html, msg("section.residential.details", locale));
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        msg("field.bedrooms", locale),
        details.getBedrooms().map(Object::toString).orElse("—"));
    appendField(
        html,
        msg("field.bathrooms", locale),
        details.getBathrooms().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.furnished", locale),
        details
            .getFurnished()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    appendField(
        html,
        msg("field.pet.policy", locale),
        details.getPetPolicy().map(BookletHelper::formatEnumValue).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendCommercialDetailsSection(
      StringBuilder html, PropertyCommercialDetails details, Locale locale) {
    appendSectionTitle(html, msg("section.commercial.details", locale));
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        msg("field.usable.area", locale),
        buildAreaDisplay(
            details.getUsableAreaValue().orElse(null), details.getUsableAreaUnit().orElse(null)));
    appendField(
        html,
        msg("field.common.area", locale),
        buildAreaDisplay(
            details.getCommonAreaValue().orElse(null), details.getCommonAreaUnit().orElse(null)));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.floor.level", locale),
        details.getFloorLevel().map(Object::toString).orElse("—"));
    appendField(
        html,
        msg("field.ceiling.height", locale),
        buildMeasureDisplay(
            details.getCeilingHeightValue().orElse(null),
            details.getCeilingHeightUnit().orElse(null),
            "m"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.storefront", locale),
        details
            .getHasStorefront()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    appendField(
        html,
        msg("field.signage.rights", locale),
        details
            .getHasSignageRights()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.max.occupancy", locale),
        details.getMaxOccupancy().map(Object::toString).orElse("—"));
    appendField(
        html,
        msg("field.restrooms", locale),
        details.getRestroomCount().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.kitchen.facility", locale),
        details
            .getHasKitchenFacility()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    appendField(
        html,
        msg("field.accessibility.compliant", locale),
        details
            .getAccessibilityCompliant()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.zoning", locale),
        details.getZoningClassification().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("<td></td>");
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendIndustrialDetailsSection(
      StringBuilder html, PropertyIndustrialDetails details, Locale locale) {
    appendSectionTitle(html, msg("section.industrial.details", locale));
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        msg("field.clear.height", locale),
        buildMeasureDisplay(
            details.getClearHeightValue().orElse(null),
            details.getClearHeightUnit().orElse(null),
            "m"));
    appendField(
        html,
        msg("field.loading.docks", locale),
        details.getLoadingDocks().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.drive.in.doors", locale),
        details.getDriveInDoors().map(Object::toString).orElse("—"));
    appendField(
        html,
        msg("field.floor.load.capacity", locale),
        buildMeasureDisplay(
            details.getFloorLoadCapacityValue().orElse(null),
            details.getFloorLoadCapacityUnit().orElse(null),
            "kg/m²"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.power.capacity", locale),
        details
            .getPowerCapacityValue()
            .map(v -> v + " " + details.getPowerCapacityUnit().orElse("kVA"))
            .orElse("—"));
    appendField(
        html,
        msg("field.three.phase.power", locale),
        details
            .getHasThreePhasePower()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.crane", locale),
        details
            .getHasCrane()
            .map(
                v ->
                    isTrue(v)
                        ? msg("value.yes", locale)
                            + details
                                .getCraneCapacityValue()
                                .map(
                                    t ->
                                        " ("
                                            + t
                                            + " "
                                            + details.getCraneCapacityUnit().orElse("metric_tons")
                                            + ")")
                                .orElse("")
                        : msg("value.no", locale))
            .orElse("—"));
    appendField(
        html,
        msg("field.hazmat.certification", locale),
        details
            .getHasHazmatCertification()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.ventilation.system", locale),
        details
            .getHasVentilationSystem()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    appendField(
        html,
        msg("field.climate.control", locale),
        details
            .getHasClimateControl()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.yard.area", locale),
        buildAreaDisplay(
            details.getYardAreaValue().orElse(null), details.getYardAreaUnit().orElse(null)));
    appendField(
        html,
        msg("field.zoning", locale),
        details.getZoningClassification().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendAgriculturalDetailsSection(
      StringBuilder html, PropertyAgriculturalDetails details, Locale locale) {
    appendSectionTitle(html, msg("section.agricultural.details", locale));
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        msg("field.total.land.area", locale),
        buildAreaDisplay(
            details.getTotalLandAreaValue().orElse(null),
            details.getTotalLandAreaUnit().orElse(null)));
    appendField(
        html,
        msg("field.arable.area", locale),
        buildAreaDisplay(
            details.getArableAreaValue().orElse(null), details.getArableAreaUnit().orElse(null)));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.soil.type", locale),
        details.getSoilType().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        msg("field.water.rights", locale),
        details
            .getHasWaterRights()
            .map(v -> isTrue(v) ? msg("value.yes", locale) : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.water.source", locale),
        details.getWaterSource().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        msg("field.irrigation", locale),
        details.getIrrigationType().map(BookletHelper::formatEnumValue).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.fencing", locale),
        details.getFencingType().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        msg("field.outbuildings", locale),
        details
            .getHasOutbuildings()
            .map(
                v ->
                    isTrue(v)
                        ? msg("value.yes", locale)
                            + details
                                .getOutbuildingDetails()
                                .map(d -> " — " + escapeHtml(d))
                                .orElse("")
                        : msg("value.no", locale))
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        msg("field.current.use", locale),
        details.getCurrentUse().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        msg("field.zoning", locale),
        details.getZoningClassification().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendConstructionSection(StringBuilder html, Property property, Locale locale) {
    boolean hasConstruction =
        property.getConstructionType().isPresent()
            || property.getFoundationType().isPresent()
            || property.getRoofType().isPresent()
            || property.getWindowType().isPresent()
            || property.getWallConstruction().isPresent()
            || property.getFlooringType().isPresent();

    if (!hasConstruction) {
      return;
    }

    appendSectionTitle(html, msg("section.construction", locale));
    html.append("<table class='detail-grid'>");
    if (property.getConstructionType().isPresent() || property.getFoundationType().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.construction.type", locale),
          formatEnumValue(property.getConstructionType().orElse(null)));
      appendField(
          html,
          msg("field.foundation", locale),
          formatEnumValue(property.getFoundationType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getRoofType().isPresent() || property.getWindowType().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.roof.type", locale),
          formatEnumValue(property.getRoofType().orElse(null)));
      appendField(
          html,
          msg("field.window.type", locale),
          formatEnumValue(property.getWindowType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getWallConstruction().isPresent() || property.getFlooringType().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.wall.construction", locale),
          formatEnumValue(property.getWallConstruction().orElse(null)));
      appendField(
          html,
          msg("field.flooring", locale),
          formatEnumValue(property.getFlooringType().orElse(null)));
      html.append("</tr>");
    }
    html.append("</table>");
  }

  // ── Page: Building Specifications ───────────────────────────────

  private void appendBuildingSpecsPage(StringBuilder html, Property property, Locale locale) {
    boolean hasEnergyData =
        property.getEnergyEfficiencyRating().isPresent()
            || property.getHeatingType().isPresent()
            || property.getCoolingType().isPresent()
            || property.getHotWaterSystem().isPresent()
            || property.getEnergyCertificateExpiryDate().isPresent()
            || property.getInsulationNotes().isPresent();
    boolean hasUtilitiesData =
        property.getElectricityConnectionType().isPresent()
            || property.getWaterConnectionType().isPresent()
            || property.getHasGasConnection().isPresent()
            || property.getSewageType().isPresent()
            || property.getInternetConnectionType().isPresent();
    boolean hasParkingData =
        property.getParkingType().isPresent() || property.getParkingSpaces().isPresent();

    if (!hasEnergyData && !hasUtilitiesData && !hasParkingData) {
      return;
    }

    appendPageStart(html, msg("page.building.specs", locale));

    if (hasEnergyData) {
      appendEnergySection(html, property, locale);
    }
    if (hasUtilitiesData) {
      appendUtilitiesSection(html, property, locale);
    }
    if (hasParkingData) {
      appendParkingSection(html, property, locale);
    }

    appendPageEnd(html);
  }

  private void appendEnergySection(StringBuilder html, Property property, Locale locale) {
    appendSectionTitle(html, msg("section.energy.climate", locale));

    if (property.getEnergyEfficiencyRating().isPresent()) {
      String ratingColor = getEnergyRatingColor(property.getEnergyEfficiencyRating().orElse(null));
      html.append("<div style='margin-bottom: 16px;'>");
      html.append(
              "<span style='display:inline-block;padding:8px"
                  + " 20px;border-radius:6px;color:white;"
                  + "font-size:22px;font-weight:700;letter-spacing:1px;background-color:")
          .append(ratingColor)
          .append(";'>")
          .append(escapeHtml(property.getEnergyEfficiencyRating().orElse(null)))
          .append("</span>");
      html.append("<span style='margin-left:12px;font-size:13px;color:#78716c;'>")
          .append(msg("field.energy.efficiency.rating", locale))
          .append("</span>");
      html.append("</div>");
    }

    html.append("<table class='detail-grid'>");
    if (property.getHeatingType().isPresent() || property.getCoolingType().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.heating.system", locale),
          formatEnumValue(property.getHeatingType().orElse(null)));
      appendField(
          html,
          msg("field.cooling.system", locale),
          formatEnumValue(property.getCoolingType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getHotWaterSystem().isPresent()
        || property.getEnergyCertificateExpiryDate().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.hot.water.system", locale),
          formatEnumValue(property.getHotWaterSystem().orElse(null)));
      appendField(
          html,
          msg("field.certificate.expiry", locale),
          property
              .getEnergyCertificateExpiryDate()
              .map(d -> d.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
              .orElse("—"));
      html.append("</tr>");
    }
    html.append("</table>");

    appendTextBlock(
        html, msg("field.insulation.notes", locale), property.getInsulationNotes().orElse(null));
  }

  private void appendUtilitiesSection(StringBuilder html, Property property, Locale locale) {
    appendSectionTitle(html, msg("section.utilities.infrastructure", locale));
    html.append("<table class='detail-grid'>");

    if (property.getElectricityConnectionType().isPresent()
        || property.getElectricityCapacityValue().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.electricity", locale),
          formatEnumValue(property.getElectricityConnectionType().orElse(null)));
      appendField(
          html,
          msg("field.capacity", locale),
          property
              .getElectricityCapacityValue()
              .map(a -> a + " " + property.getElectricityCapacityUnit().orElse("A"))
              .orElse("—"));
      html.append("</tr>");
    }
    if (property.getWaterConnectionType().isPresent()
        || property.getHasGasConnection().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.water", locale),
          formatEnumValue(property.getWaterConnectionType().orElse(null)));
      String gasText =
          property
              .getHasGasConnection()
              .map(
                  g ->
                      isTrue(g)
                          ? msg("value.connected", locale)
                          : msg("value.not.connected", locale))
              .orElse("—");
      appendField(html, msg("field.gas.connection", locale), gasText);
      html.append("</tr>");
    }
    if (property.getSewageType().isPresent() || property.getInternetConnectionType().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.sewage", locale),
          formatEnumValue(property.getSewageType().orElse(null)));
      appendField(
          html,
          msg("field.internet", locale),
          formatEnumValue(property.getInternetConnectionType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getInternetMaxSpeedValue().isPresent()
        || property.getInternetStatus().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          msg("field.max.speed", locale),
          property
              .getInternetMaxSpeedValue()
              .map(s -> s + " " + property.getInternetMaxSpeedUnit().orElse("Mbps"))
              .orElse("—"));
      appendField(
          html,
          msg("field.internet.status", locale),
          formatEnumValue(property.getInternetStatus().orElse(null)));
      html.append("</tr>");
    }
    html.append("</table>");
  }

  private void appendParkingSection(StringBuilder html, Property property, Locale locale) {
    appendSectionTitle(html, msg("section.parking", locale));
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        msg("field.parking.type", locale),
        formatEnumValue(property.getParkingType().orElse(null)));
    appendField(
        html,
        msg("field.parking.spaces", locale),
        property.getParkingSpaces().map(Object::toString).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  // ── Page: Features & Outdoor Spaces ─────────────────────────────

  private void appendFeaturesPage(
      StringBuilder html,
      List<PropertyAmenity> propertyAmenities,
      Map<UUID, Amenity> amenityMap,
      List<PropertyOutdoorArea> outdoorAreas,
      Locale locale) {
    boolean hasAmenities = !propertyAmenities.isEmpty();
    boolean hasOutdoorAreas = !outdoorAreas.isEmpty();
    if (!hasAmenities && !hasOutdoorAreas) {
      return;
    }

    appendPageStart(html, msg("page.features.outdoor", locale));

    if (hasAmenities) {
      appendSectionTitle(html, msg("section.amenities", locale));

      Map<String, List<Amenity>> grouped = new LinkedHashMap<>();
      for (PropertyAmenity pa : propertyAmenities) {
        Amenity amenity = amenityMap.get(pa.getAmenityId());
        if (amenity != null) {
          String category = amenity.getCategory() != null ? amenity.getCategory() : "Other";
          grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(amenity);
        }
      }

      for (Map.Entry<String, List<Amenity>> entry : grouped.entrySet()) {
        html.append(
                "<div style='font-size:11px;font-weight:600;text-transform:uppercase;"
                    + "letter-spacing:1.2px;color:#57534e;margin:16px 0 6px 0;padding-bottom:4px;"
                    + "border-bottom:1px solid #e7e5e4;'>")
            .append(escapeHtml(formatEnumValue(entry.getKey())))
            .append("</div>");
        html.append("<div style='margin-bottom:10px;'>");
        for (Amenity amenity : entry.getValue()) {
          html.append(
                  "<span style='display:inline-block;background-color:#f0f9ff;border:1px solid"
                      + " #bae6fd;color:#0284c7;padding:4px"
                      + " 10px;border-radius:12px;font-size:11px;font-weight:500;margin:2px"
                      + " 3px;'>")
              .append(escapeHtml(amenity.getName()))
              .append("</span>");
        }
        html.append("</div>");
      }
    }

    if (hasOutdoorAreas) {
      appendSectionTitle(html, msg("section.outdoor.spaces", locale));
      for (PropertyOutdoorArea oa : outdoorAreas) {
        html.append(
            "<div style='border:1px solid #e7e5e4;border-radius:6px;padding:12px 16px;"
                + "margin-bottom:8px;background-color:#f0fdf4;'>");
        html.append("<div style='font-size:15px;font-weight:600;color:#166534;'>")
            .append(escapeHtml(formatEnumValue(oa.getType())))
            .append("</div>");
        if (oa.getAreaValue().isPresent()) {
          String unit = oa.getAreaUnit() != null ? oa.getAreaUnit() : "sqm";
          html.append("<div style='font-size:13px;color:#57534e;margin-top:2px;'>")
              .append(oa.getAreaValue().get())
              .append(" ")
              .append(unit)
              .append("</div>");
        }
        html.append("</div>");
      }
    }

    appendPageEnd(html);
  }

  // ── Page: Safety & Accessibility ────────────────────────────────

  private void appendSafetyPage(
      StringBuilder html, Property property, @Nullable PropertyCategory category, Locale locale) {
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
    boolean hasAccessibilityData =
        !skipAccessibility
            && (isTrue(property.getIsWheelchairAccessible().orElse(null))
                || isTrue(property.getHasElevator().orElse(null))
                || isTrue(property.getHasStepFreeEntrance().orElse(null))
                || isTrue(property.getHasAdaptedBathroom().orElse(null))
                || property.getAccessibilityNotes().filter(s -> !s.isBlank()).isPresent());

    if (!hasSafetyData && !hasAccessibilityData) {
      return;
    }

    appendPageStart(html, msg("page.safety.accessibility", locale));

    if (hasSafetyData) {
      appendSectionTitle(html, msg("section.safety.security", locale));
      html.append(
          "<div style='background-color:#fafaf9;border:1px solid #e7e5e4;border-radius:6px;"
              + "padding:16px 18px;margin-bottom:10px;'>");
      appendCheckItem(
          html, msg("check.smoke.detectors", locale), property.getHasSmokeDetectors().orElse(null));
      appendCheckItem(
          html, msg("check.co.detectors", locale), property.getHasCoDetectors().orElse(null));
      appendCheckItem(
          html,
          msg("check.fire.extinguisher", locale),
          property.getHasFireExtinguisher().orElse(null));
      appendCheckItem(
          html,
          msg("check.sprinkler.system", locale),
          property.getHasSprinklerSystem().orElse(null));
      appendCheckItem(
          html, msg("check.alarm.system", locale), property.getHasAlarmSystem().orElse(null));
      appendCheckItem(
          html,
          msg("check.security.cameras", locale),
          property.getHasSecurityCameras().orElse(null));
      appendCheckItem(
          html, msg("check.secure.entry", locale), property.getHasSecureEntry().orElse(null));
      html.append("</div>");

      appendTextBlock(
          html, msg("field.safety.notes", locale), property.getSafetyNotes().orElse(null));
    }

    if (hasAccessibilityData) {
      appendSectionTitle(html, msg("section.accessibility", locale));
      html.append(
          "<div style='background-color:#fafaf9;border:1px solid #e7e5e4;border-radius:6px;"
              + "padding:16px 18px;margin-bottom:10px;'>");
      appendCheckItem(
          html,
          msg("check.wheelchair.accessible", locale),
          property.getIsWheelchairAccessible().orElse(null));
      appendCheckItem(html, msg("check.elevator", locale), property.getHasElevator().orElse(null));
      appendCheckItem(
          html,
          msg("check.step.free.entrance", locale),
          property.getHasStepFreeEntrance().orElse(null));
      appendCheckItem(
          html,
          msg("check.adapted.bathroom", locale),
          property.getHasAdaptedBathroom().orElse(null));
      html.append("</div>");

      appendTextBlock(
          html,
          msg("field.accessibility.notes", locale),
          property.getAccessibilityNotes().orElse(null));
    }

    appendPageEnd(html);
  }

  // ── Page: Photo Gallery ─────────────────────────────────────────

  private void appendPhotoGalleryPage(StringBuilder html, List<Photo> photos) {
    if (photos.isEmpty()) {
      return;
    }

    List<String[]> photoEntries = new ArrayList<>();
    for (Photo photo : photos) {
      String dataUri = photoToBase64DataUri(photo);
      if (dataUri != null) {
        String label = photo.getTitle().orElse(photo.getFileName());
        photoEntries.add(new String[] {dataUri, label, isTrue(photo.getIsMainPhoto()) ? "1" : "0"});
      }
    }
    if (photoEntries.isEmpty()) {
      return;
    }

    appendPageStart(html, "Photo Gallery");
    html.append("<p style='font-size:13px;color:#78716c;margin-bottom:16px;'>")
        .append(photoEntries.size())
        .append(" photo")
        .append(photoEntries.size() != 1 ? "s" : "")
        .append("</p>");

    html.append(
        "<table style='width:100%;border-collapse:collapse;table-layout:fixed;'>");
    for (int i = 0; i < photoEntries.size(); i++) {
      if (i % 3 == 0) {
        html.append("<tr>");
      }
      String[] entry = photoEntries.get(i);
      boolean isMain = "1".equals(entry[2]);

      html.append(
          "<td style='width:33.33%;padding:4px;vertical-align:top;"
              + "box-sizing:border-box;overflow:hidden;'>");
      html.append(
          "<div style='border:1px solid #e7e5e4;border-radius:6px;overflow:hidden;"
              + "background-color:#fafaf9;'>");
      html.append("<img src='")
          .append(entry[0])
          .append(
              "' style='width:100%;max-width:100%;height:140px;object-fit:cover;display:block;'/>");
      html.append("<div style='padding:6px 8px;font-size:11px;color:#44403c;'>");
      if (isMain) {
        html.append(
            "<span"
                + " style='display:inline-block;background-color:#f0f9ff;color:#0284c7;padding:1px"
                + " 6px;border-radius:3px;font-size:9px;font-weight:600;"
                + "text-transform:uppercase;letter-spacing:0.5px;margin-right:4px;'>Main</span>");
      }
      html.append(escapeHtml(entry[1]));
      html.append("</div></div></td>");

      if (i % 3 == 2 || i == photoEntries.size() - 1) {
        if (i == photoEntries.size() - 1) {
          html.append("<td style='width:33%;'></td>".repeat(3 - ((i % 3) + 1)));
        }
        html.append("</tr>");
      }
    }
    html.append("</table>");
    appendPageEnd(html);
  }

  // ── Page: Financial Overview ────────────────────────────────────

  private void appendFinancialOverviewPage(
      StringBuilder html,
      Map<Integer, FinancialYearSummary> yearSummaries,
      @Nullable String currency) {
    if (yearSummaries.isEmpty()) {
      return;
    }
    String ccy = currency != null ? currency : "EUR";

    appendPageStart(html, "Financial Overview");

    for (Map.Entry<Integer, FinancialYearSummary> entry : yearSummaries.entrySet()) {
      FinancialYearSummary summary = entry.getValue();
      html.append(
          "<div style='background-color:#fafaf9;border:1px solid #e7e5e4;border-radius:6px;"
              + "padding:20px;margin:15px 0;'>");
      html.append("<h3 style='margin:0 0 15px 0;color:#0c4a6e;font-size:18px;'>Year ")
          .append(entry.getKey())
          .append("</h3>");
      html.append("<table style='width:100%;border-collapse:collapse;'><tr>");
      html.append(
              "<td style='text-align:center;padding:10px;'><div"
                  + " style='font-size:10px;color:#78716c;text-transform:uppercase;"
                  + "letter-spacing:1px;'>Income</div><div"
                  + " style='font-size:22px;font-weight:700;margin-top:4px;color:#059669;'>")
          .append(CurrencyUtils.formatCurrency(summary.income, ccy))
          .append("</div></td>");
      html.append(
              "<td style='text-align:center;padding:10px;'><div"
                  + " style='font-size:10px;color:#78716c;text-transform:uppercase;"
                  + "letter-spacing:1px;'>Expenses</div><div"
                  + " style='font-size:22px;font-weight:700;margin-top:4px;color:#dc2626;'>")
          .append(CurrencyUtils.formatCurrency(summary.expenses, ccy))
          .append("</div></td>");
      html.append(
              "<td style='text-align:center;padding:10px;'><div"
                  + " style='font-size:10px;color:#78716c;text-transform:uppercase;letter-spacing:1px;'>Net"
                  + " Profit</div><div"
                  + " style='font-size:22px;font-weight:700;margin-top:4px;color:#0284c7;'>")
          .append(CurrencyUtils.formatCurrency(summary.getNetProfit(), ccy))
          .append("</div></td>");
      html.append("</tr></table>");
      html.append("</div>");
    }

    appendPageEnd(html);
  }

  // ── Page: Contracts ─────────────────────────────────────────────

  private void appendContractsPage(StringBuilder html, List<Contract> contracts, UUID teamId) {
    if (contracts.isEmpty()) {
      return;
    }

    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    Map<UUID, Contact> primaryContacts =
        contractPartyService.getPrimaryContactsForContracts(contractIds, teamId);

    // Bulk-load extensions and group by contract ID
    List<ContractExtension> allExtensions =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream().collect(Collectors.groupingBy(ContractExtension::getContractId));

    appendPageStart(html, "Contracts");
    html.append("<p style='font-size:13px;color:#78716c;margin-bottom:12px;'>")
        .append(contracts.size())
        .append(" contract(s) on record</p>");

    html.append("<table class='payment-table'><thead><tr>");
    html.append(
        "<th>Contract ID</th><th>Contact</th><th>Start Date</th><th>End"
            + " Date</th><th>Rent</th><th>Status</th>");
    html.append("</tr></thead><tbody>");

    for (Contract contract : contracts) {
      Contact contact = primaryContacts.get(contract.getId());
      String contactName = contact != null ? contact.getDisplayName() : "Unknown";

      html.append("<tr>");
      html.append("<td>#").append(contract.getIdentifier().orElseThrow().value()).append("</td>");
      html.append("<td>").append(escapeHtml(contactName)).append("</td>");
      List<ContractExtension> extensions =
          extensionsByContract.getOrDefault(contract.getId(), List.of());
      Optional<LocalDate> effectiveEndDate =
          EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);

      html.append("<td>").append(contract.getStartDate()).append("</td>");
      html.append("<td>")
          .append(effectiveEndDate.map(Object::toString).orElse("Ongoing"))
          .append("</td>");
      html.append("<td>")
          .append(
              CurrencyUtils.formatCurrency(
                  contract.getRentAmount().value(), contract.getRentAmount().currency()))
          .append("</td>");
      html.append("<td>")
          .append(
              formatEnumValue(
                  contract.getStatus() != null ? contract.getStatus().toString() : null))
          .append("</td>");
      html.append("</tr>");
    }

    html.append("</tbody></table>");
    appendPageEnd(html);
  }

  // ── Page: Investment Dashboard ──────────────────────────────────

  private void appendDashboardPage(StringBuilder html, PropertyDashboardResponse dashboard) {
    SummaryMetrics s = dashboard.summary();
    String currency = s.currency().map(BookletHelper::escapeHtml).orElse("");

    appendPageStart(html, "Investment Dashboard");

    // Summary metrics grid
    appendSectionTitle(html, "Key Metrics");
    html.append("<table style='width:100%;border-collapse:collapse;margin-bottom:16px;'>");
    html.append("<tr>");
    appendMetricCell(html, "Total ROI", fmtPct(s.totalRoiPercent().orElse(null)));
    appendMetricCell(html, "Annualized ROI", fmtPct(s.annualizedRoiPercent().orElse(null)));
    appendMetricCell(html, "Cap Rate", fmtPct(s.capRatePercent().orElse(null)));
    appendMetricCell(html, "Cash-on-Cash", fmtPct(s.cashOnCashPercent().orElse(null)));
    html.append("</tr><tr>");
    appendMetricCell(
        html, "Monthly Cash Flow", fmtMoney(s.monthlyCashFlow().orElse(null), currency));
    appendMetricCell(html, "Annual NOI", fmtMoney(s.annualNoi().orElse(null), currency));
    appendMetricCell(html, "Total Equity", fmtMoney(s.totalEquity().orElse(null), currency));
    appendMetricCell(html, "Occupancy", fmtPct(s.occupancyRatePercent().orElse(null)));
    html.append("</tr>");
    html.append("</table>");

    // Cash flow table
    if (!dashboard.cashFlow().months().isEmpty()) {
      appendSectionTitle(html, "Monthly Cash Flow");
      html.append("<table class='payment-table'><thead><tr>");
      html.append("<th>Month</th><th>Income</th><th>Expenses</th><th>Mortgage</th><th>Net</th>");
      html.append("</tr></thead><tbody>");
      for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
        html.append("<tr>");
        html.append("<td>").append(escapeHtml(m.month())).append("</td>");
        html.append("<td style='text-align:right;'>")
            .append(fmtMoney(m.income(), currency))
            .append("</td>");
        html.append("<td style='text-align:right;'>")
            .append(fmtMoney(m.expenses(), currency))
            .append("</td>");
        html.append("<td style='text-align:right;'>")
            .append(fmtMoney(m.mortgage(), currency))
            .append("</td>");
        String netColor = m.net().signum() >= 0 ? "#059669" : "#dc2626";
        html.append("<td style='text-align:right;color:")
            .append(netColor)
            .append(";font-weight:600;'>")
            .append(fmtMoney(m.net(), currency))
            .append("</td>");
        html.append("</tr>");
      }
      html.append("</tbody></table>");
    }

    // Expense breakdown
    if (!dashboard.expenseBreakdown().categories().isEmpty()) {
      appendSectionTitle(html, "Expense Breakdown");
      html.append("<table class='payment-table'><thead><tr>");
      html.append("<th>Category</th><th>Amount</th>");
      html.append("</tr></thead><tbody>");
      for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
        html.append("<tr>");
        html.append("<td>").append(escapeHtml(formatEnumValue(c.category()))).append("</td>");
        html.append("<td style='text-align:right;'>")
            .append(fmtMoney(c.amount(), currency))
            .append("</td>");
        html.append("</tr>");
      }
      html.append("</tbody></table>");
    }

    // Equity overview
    appendSectionTitle(html, "Equity Overview");
    html.append("<table class='payment-table'><thead><tr>");
    html.append("<th>Item</th><th>Amount</th>");
    html.append("</tr></thead><tbody>");
    appendEquityRow(
        html, "Purchase Price", dashboard.equity().purchasePrice().orElse(null), currency);
    appendEquityRow(
        html,
        "Current Market Value",
        dashboard.equity().currentMarketValue().orElse(null),
        currency);
    appendEquityRow(
        html, "Mortgage Balance", dashboard.equity().mortgageBalance().orElse(null), currency);
    html.append("</tbody></table>");

    appendPageEnd(html);
  }

  private void appendMetricCell(StringBuilder html, String label, String value) {
    html.append(
            "<td style='padding:8px 10px;text-align:center;'>"
                + "<div style='font-size:9px;color:#78716c;text-transform:uppercase;"
                + "letter-spacing:0.8px;'>")
        .append(escapeHtml(label))
        .append("</div><div style='font-size:16px;font-weight:700;color:#0c4a6e;margin-top:2px;'>")
        .append(escapeHtml(value))
        .append("</div></td>");
  }

  private void appendEquityRow(
      StringBuilder html, String label, @Nullable BigDecimal value, String currency) {
    html.append("<tr><td>").append(escapeHtml(label)).append("</td>");
    html.append("<td style='text-align:right;'>")
        .append(fmtMoney(value, currency))
        .append("</td></tr>");
  }

  private static String fmtPct(@Nullable BigDecimal value) {
    return value != null ? value.toPlainString() + "%" : "N/A";
  }

  private static String fmtMoney(@Nullable BigDecimal value, String currencyCode) {
    return CurrencyUtils.formatCurrency(value, currencyCode);
  }

  // ── Helpers ─────────────────────────────────────────────────────

  private String buildLocationString(Property property) {
    StringBuilder location = new StringBuilder();
    location.append(escapeHtml(property.getCity()));
    if (property.getPostalCode() != null) {
      location.append(", ").append(escapeHtml(property.getPostalCode()));
    }
    if (property.getCountryCode() != null) {
      location.append(", ").append(escapeHtml(property.getCountryCode()));
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

  private String buildAreaDisplay(@Nullable BigDecimal value, @Nullable String unit) {
    return buildMeasureDisplay(value, unit, "sqm");
  }

  private String buildMeasureDisplay(
      @Nullable BigDecimal value, @Nullable String unit, String defaultUnit) {
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

  // ── Inner types ─────────────────────────────────────────────────

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
