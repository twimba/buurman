package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.PAID;
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
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Amenity;
import com.buurman.domain.Contract;
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
import com.buurman.domain.Tenant;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;
import com.buurman.repository.AmenityRepository;
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
import com.buurman.service.FeatureFlagService;
import com.buurman.service.PropertyDashboardService;
import com.buurman.service.S3StorageService;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.FeatureFlags;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PropertyBookletExporter {

  private final PropertyRepository propertyRepository;
  private final PropertyResidentialDetailsRepository residentialDetailsRepository;
  private final PropertyCommercialDetailsRepository commercialDetailsRepository;
  private final PropertyIndustrialDetailsRepository industrialDetailsRepository;
  private final PropertyAgriculturalDetailsRepository agriculturalDetailsRepository;
  private final ContractRepository contractRepository;
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
  private final Clock clock;

  public byte[] generate(PropertyIdentifier propertyIdentifier, UUID teamId) {
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
            dashboard);
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
      @Nullable PropertyDashboardResponse dashboard) {
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
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
        area);
    appendPropertyOverviewPage(
        html,
        property,
        category,
        residentialDetails,
        commercialDetails,
        industrialDetails,
        agriculturalDetails,
        area);

    if (category != PropertyCategory.AGRICULTURAL) {
      appendBuildingSpecsPage(html, property);
    }

    appendFeaturesPage(html, propertyAmenities, amenityMap, outdoorAreas);

    if (category != PropertyCategory.AGRICULTURAL) {
      appendSafetyPage(html, property, category);
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
      String area) {
    appendCoverStart(html, "PROPERTY REPORT", escapeHtml(property.getStreet()), generatedDate);

    String statusStr = property.getStatus() != null ? property.getStatus().name() : "VACANT";
    appendStatusBadge(html, statusStr);

    html.append("<table class='cover-summary'>");

    if (category == PropertyCategory.COMMERCIAL && commercialDetails != null) {
      appendCoverSummaryCommercial(html, property, commercialDetails, location);
    } else if (category == PropertyCategory.INDUSTRIAL && industrialDetails != null) {
      appendCoverSummaryIndustrial(html, property, industrialDetails, location, area);
    } else if (category == PropertyCategory.AGRICULTURAL && agriculturalDetails != null) {
      appendCoverSummaryAgricultural(html, property, agriculturalDetails, location);
    } else if (category == PropertyCategory.RESIDENTIAL && residentialDetails != null) {
      appendCoverSummaryResidential(html, property, residentialDetails, location, area);
    } else {
      // MIXED_USE or fallback (no detail record)
      appendCoverSummaryMixedUse(html, property, location, area);
    }

    html.append("</table>");
    appendCoverEnd(html);
  }

  private void appendCoverSummaryResidential(
      StringBuilder html,
      Property property,
      PropertyResidentialDetails details,
      String location,
      String area) {
    html.append("<tr>");
    appendCoverCell(
        html,
        "Property Type",
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, "Location", location);
    html.append("</tr><tr>");
    String bedBath =
        details.getBedrooms().map(v -> v + " bed").orElse("—")
            + " / "
            + details.getBathrooms().map(v -> v + " bath").orElse("—");
    appendCoverCell(html, "Bedrooms / Bathrooms", bedBath);
    appendCoverCell(html, "Total Area", area);
    html.append("</tr><tr>");
    appendCoverCell(html, "Year Built", property.getYearBuilt().map(Object::toString).orElse("—"));
    appendCoverCell(html, "Reference", property.getIdentifier().orElseThrow().value());
    html.append("</tr>");
  }

  private void appendCoverSummaryCommercial(
      StringBuilder html, Property property, PropertyCommercialDetails details, String location) {
    html.append("<tr>");
    appendCoverCell(
        html,
        "Property Type",
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, "Location", location);
    html.append("</tr><tr>");
    String usable =
        buildAreaDisplay(
            details.getUsableAreaValue().orElse(null), details.getUsableAreaUnit().orElse(null));
    String common =
        buildAreaDisplay(
            details.getCommonAreaValue().orElse(null), details.getCommonAreaUnit().orElse(null));
    appendCoverCell(html, "Usable / Common Area", usable + " / " + common);
    appendCoverCell(html, "Floor Level", details.getFloorLevel().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        "Ceiling Height",
        buildMeasureDisplay(
            details.getCeilingHeightValue().orElse(null),
            details.getCeilingHeightUnit().orElse(null),
            "m"));
    appendCoverCell(html, "Reference", property.getIdentifier().orElseThrow().value());
    html.append("</tr>");
  }

  private void appendCoverSummaryIndustrial(
      StringBuilder html,
      Property property,
      PropertyIndustrialDetails details,
      String location,
      String area) {
    html.append("<tr>");
    appendCoverCell(
        html,
        "Property Type",
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, "Location", location);
    html.append("</tr><tr>");
    appendCoverCell(html, "Total Area", area);
    appendCoverCell(
        html,
        "Clear Height",
        buildMeasureDisplay(
            details.getClearHeightValue().orElse(null),
            details.getClearHeightUnit().orElse(null),
            "m"));
    html.append("</tr><tr>");
    appendCoverCell(
        html, "Loading Docks", details.getLoadingDocks().map(Object::toString).orElse("—"));
    appendCoverCell(
        html,
        "Power Capacity",
        details
            .getPowerCapacityValue()
            .map(v -> v + " " + details.getPowerCapacityUnit().orElse("kVA"))
            .orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(html, "Reference", property.getIdentifier().orElseThrow().value());
    html.append("<td></td>");
    html.append("</tr>");
  }

  private void appendCoverSummaryAgricultural(
      StringBuilder html, Property property, PropertyAgriculturalDetails details, String location) {
    html.append("<tr>");
    appendCoverCell(
        html,
        "Property Type",
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, "Location", location);
    html.append("</tr><tr>");
    appendCoverCell(
        html,
        "Total Land Area",
        buildAreaDisplay(
            details.getTotalLandAreaValue().orElse(null),
            details.getTotalLandAreaUnit().orElse(null)));
    appendCoverCell(
        html,
        "Arable Area",
        buildAreaDisplay(
            details.getArableAreaValue().orElse(null), details.getArableAreaUnit().orElse(null)));
    html.append("</tr><tr>");
    appendCoverCell(
        html, "Soil Type", details.getSoilType().map(BookletHelper::formatEnumValue).orElse("—"));
    appendCoverCell(
        html,
        "Current Use",
        details.getCurrentUse().map(BookletHelper::formatEnumValue).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(html, "Reference", property.getIdentifier().orElseThrow().value());
    html.append("<td></td>");
    html.append("</tr>");
  }

  private void appendCoverSummaryMixedUse(
      StringBuilder html, Property property, String location, String area) {
    html.append("<tr>");
    appendCoverCell(
        html,
        "Property Type",
        propertyTypeIconHtml(property.getPropertyType().name())
            + escapeHtml(formatEnumValue(property.getPropertyType().name())));
    appendCoverCell(html, "Location", location);
    html.append("</tr><tr>");
    appendCoverCell(html, "Total Area", area);
    appendCoverCell(html, "Year Built", property.getYearBuilt().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendCoverCell(html, "Reference", property.getIdentifier().orElseThrow().value());
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
      String area) {
    appendPageStart(html, "Property Overview");

    appendSectionTitle(html, "Property Details");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Property Type", formatEnumValue(property.getPropertyType().name()));
    appendField(html, "Status", formatEnumValue(property.getStatus().name()));
    html.append("</tr><tr>");
    appendField(html, "Total Area", area);
    appendField(
        html, "Number of Floors", property.getNumberOfFloors().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(html, "Year Built", property.getYearBuilt().map(Object::toString).orElse("—"));
    appendField(
        html, "Last Renovated", property.getYearLastRenovated().map(Object::toString).orElse("—"));
    html.append("</tr>");
    html.append("</table>");

    appendConstructionSection(html, property);

    if (category == PropertyCategory.RESIDENTIAL && residentialDetails != null) {
      appendResidentialDetailsSection(html, residentialDetails);
    } else if (category == PropertyCategory.COMMERCIAL && commercialDetails != null) {
      appendCommercialDetailsSection(html, commercialDetails);
    } else if (category == PropertyCategory.INDUSTRIAL && industrialDetails != null) {
      appendIndustrialDetailsSection(html, industrialDetails);
    } else if (category == PropertyCategory.AGRICULTURAL && agriculturalDetails != null) {
      appendAgriculturalDetailsSection(html, agriculturalDetails);
    }

    appendTextBlock(html, "Structural Notes", property.getStructuralNotes().orElse(null));

    appendPageEnd(html);
  }

  private void appendResidentialDetailsSection(
      StringBuilder html, PropertyResidentialDetails details) {
    appendSectionTitle(html, "Residential Details");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Bedrooms", details.getBedrooms().map(Object::toString).orElse("—"));
    appendField(html, "Bathrooms", details.getBathrooms().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html, "Furnished", details.getFurnished().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    appendField(
        html, "Pet Policy", details.getPetPolicy().map(BookletHelper::formatEnumValue).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendCommercialDetailsSection(
      StringBuilder html, PropertyCommercialDetails details) {
    appendSectionTitle(html, "Commercial Details");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        "Usable Area",
        buildAreaDisplay(
            details.getUsableAreaValue().orElse(null), details.getUsableAreaUnit().orElse(null)));
    appendField(
        html,
        "Common Area",
        buildAreaDisplay(
            details.getCommonAreaValue().orElse(null), details.getCommonAreaUnit().orElse(null)));
    html.append("</tr><tr>");
    appendField(html, "Floor Level", details.getFloorLevel().map(Object::toString).orElse("—"));
    appendField(
        html,
        "Ceiling Height",
        buildMeasureDisplay(
            details.getCeilingHeightValue().orElse(null),
            details.getCeilingHeightUnit().orElse(null),
            "m"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Storefront",
        details.getHasStorefront().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    appendField(
        html,
        "Signage Rights",
        details.getHasSignageRights().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    html.append("</tr><tr>");
    appendField(html, "Max Occupancy", details.getMaxOccupancy().map(Object::toString).orElse("—"));
    appendField(html, "Restrooms", details.getRestroomCount().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Kitchen Facility",
        details.getHasKitchenFacility().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    appendField(
        html,
        "Accessibility Compliant",
        details.getAccessibilityCompliant().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Zoning",
        details.getZoningClassification().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("<td></td>");
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendIndustrialDetailsSection(
      StringBuilder html, PropertyIndustrialDetails details) {
    appendSectionTitle(html, "Industrial Details");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        "Clear Height",
        buildMeasureDisplay(
            details.getClearHeightValue().orElse(null),
            details.getClearHeightUnit().orElse(null),
            "m"));
    appendField(html, "Loading Docks", details.getLoadingDocks().map(Object::toString).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html, "Drive-In Doors", details.getDriveInDoors().map(Object::toString).orElse("—"));
    appendField(
        html,
        "Floor Load Capacity",
        buildMeasureDisplay(
            details.getFloorLoadCapacityValue().orElse(null),
            details.getFloorLoadCapacityUnit().orElse(null),
            "kg/m²"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Power Capacity",
        details
            .getPowerCapacityValue()
            .map(v -> v + " " + details.getPowerCapacityUnit().orElse("kVA"))
            .orElse("—"));
    appendField(
        html,
        "Three-Phase Power",
        details.getHasThreePhasePower().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Crane",
        details
            .getHasCrane()
            .map(
                v ->
                    isTrue(v)
                        ? "Yes"
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
                        : "No")
            .orElse("—"));
    appendField(
        html,
        "Hazmat Certification",
        details.getHasHazmatCertification().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Ventilation System",
        details.getHasVentilationSystem().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    appendField(
        html,
        "Climate Control",
        details.getHasClimateControl().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Yard Area",
        buildAreaDisplay(
            details.getYardAreaValue().orElse(null), details.getYardAreaUnit().orElse(null)));
    appendField(
        html,
        "Zoning",
        details.getZoningClassification().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendAgriculturalDetailsSection(
      StringBuilder html, PropertyAgriculturalDetails details) {
    appendSectionTitle(html, "Agricultural Details");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(
        html,
        "Total Land Area",
        buildAreaDisplay(
            details.getTotalLandAreaValue().orElse(null),
            details.getTotalLandAreaUnit().orElse(null)));
    appendField(
        html,
        "Arable Area",
        buildAreaDisplay(
            details.getArableAreaValue().orElse(null), details.getArableAreaUnit().orElse(null)));
    html.append("</tr><tr>");
    appendField(
        html, "Soil Type", details.getSoilType().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        "Water Rights",
        details.getHasWaterRights().map(v -> isTrue(v) ? "Yes" : "No").orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Water Source",
        details.getWaterSource().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        "Irrigation",
        details.getIrrigationType().map(BookletHelper::formatEnumValue).orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html, "Fencing", details.getFencingType().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        "Outbuildings",
        details
            .getHasOutbuildings()
            .map(
                v ->
                    isTrue(v)
                        ? "Yes"
                            + details
                                .getOutbuildingDetails()
                                .map(d -> " — " + escapeHtml(d))
                                .orElse("")
                        : "No")
            .orElse("—"));
    html.append("</tr><tr>");
    appendField(
        html,
        "Current Use",
        details.getCurrentUse().map(BookletHelper::formatEnumValue).orElse("—"));
    appendField(
        html,
        "Zoning",
        details.getZoningClassification().map(BookletHelper::escapeHtml).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  private void appendConstructionSection(StringBuilder html, Property property) {
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

    appendSectionTitle(html, "Construction");
    html.append("<table class='detail-grid'>");
    if (property.getConstructionType().isPresent() || property.getFoundationType().isPresent()) {
      html.append("<tr>");
      appendField(
          html, "Construction Type", formatEnumValue(property.getConstructionType().orElse(null)));
      appendField(html, "Foundation", formatEnumValue(property.getFoundationType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getRoofType().isPresent() || property.getWindowType().isPresent()) {
      html.append("<tr>");
      appendField(html, "Roof Type", formatEnumValue(property.getRoofType().orElse(null)));
      appendField(html, "Window Type", formatEnumValue(property.getWindowType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getWallConstruction().isPresent() || property.getFlooringType().isPresent()) {
      html.append("<tr>");
      appendField(
          html, "Wall Construction", formatEnumValue(property.getWallConstruction().orElse(null)));
      appendField(html, "Flooring", formatEnumValue(property.getFlooringType().orElse(null)));
      html.append("</tr>");
    }
    html.append("</table>");
  }

  // ── Page: Building Specifications ───────────────────────────────

  private void appendBuildingSpecsPage(StringBuilder html, Property property) {
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

    appendPageStart(html, "Building Specifications");

    if (hasEnergyData) {
      appendEnergySection(html, property);
    }
    if (hasUtilitiesData) {
      appendUtilitiesSection(html, property);
    }
    if (hasParkingData) {
      appendParkingSection(html, property);
    }

    appendPageEnd(html);
  }

  private void appendEnergySection(StringBuilder html, Property property) {
    appendSectionTitle(html, "Energy &amp; Climate");

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
      html.append(
          "<span style='margin-left:12px;font-size:13px;color:#718096;'>Energy Efficiency"
              + " Rating</span>");
      html.append("</div>");
    }

    html.append("<table class='detail-grid'>");
    if (property.getHeatingType().isPresent() || property.getCoolingType().isPresent()) {
      html.append("<tr>");
      appendField(html, "Heating System", formatEnumValue(property.getHeatingType().orElse(null)));
      appendField(html, "Cooling System", formatEnumValue(property.getCoolingType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getHotWaterSystem().isPresent()
        || property.getEnergyCertificateExpiryDate().isPresent()) {
      html.append("<tr>");
      appendField(
          html, "Hot Water System", formatEnumValue(property.getHotWaterSystem().orElse(null)));
      appendField(
          html,
          "Certificate Expiry",
          property
              .getEnergyCertificateExpiryDate()
              .map(d -> d.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
              .orElse("—"));
      html.append("</tr>");
    }
    html.append("</table>");

    appendTextBlock(html, "Insulation Notes", property.getInsulationNotes().orElse(null));
  }

  private void appendUtilitiesSection(StringBuilder html, Property property) {
    appendSectionTitle(html, "Utilities &amp; Infrastructure");
    html.append("<table class='detail-grid'>");

    if (property.getElectricityConnectionType().isPresent()
        || property.getElectricityCapacityValue().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          "Electricity",
          formatEnumValue(property.getElectricityConnectionType().orElse(null)));
      appendField(
          html,
          "Capacity",
          property
              .getElectricityCapacityValue()
              .map(a -> a + " " + property.getElectricityCapacityUnit().orElse("A"))
              .orElse("—"));
      html.append("</tr>");
    }
    if (property.getWaterConnectionType().isPresent()
        || property.getHasGasConnection().isPresent()) {
      html.append("<tr>");
      appendField(html, "Water", formatEnumValue(property.getWaterConnectionType().orElse(null)));
      String gasText =
          property
              .getHasGasConnection()
              .map(g -> isTrue(g) ? "Connected" : "Not Connected")
              .orElse("—");
      appendField(html, "Gas Connection", gasText);
      html.append("</tr>");
    }
    if (property.getSewageType().isPresent() || property.getInternetConnectionType().isPresent()) {
      html.append("<tr>");
      appendField(html, "Sewage", formatEnumValue(property.getSewageType().orElse(null)));
      appendField(
          html, "Internet", formatEnumValue(property.getInternetConnectionType().orElse(null)));
      html.append("</tr>");
    }
    if (property.getInternetMaxSpeedValue().isPresent()
        || property.getInternetStatus().isPresent()) {
      html.append("<tr>");
      appendField(
          html,
          "Max Speed",
          property
              .getInternetMaxSpeedValue()
              .map(s -> s + " " + property.getInternetMaxSpeedUnit().orElse("Mbps"))
              .orElse("—"));
      appendField(
          html, "Internet Status", formatEnumValue(property.getInternetStatus().orElse(null)));
      html.append("</tr>");
    }
    html.append("</table>");
  }

  private void appendParkingSection(StringBuilder html, Property property) {
    appendSectionTitle(html, "Parking");
    html.append("<table class='detail-grid'>");
    html.append("<tr>");
    appendField(html, "Parking Type", formatEnumValue(property.getParkingType().orElse(null)));
    appendField(
        html, "Parking Spaces", property.getParkingSpaces().map(Object::toString).orElse("—"));
    html.append("</tr>");
    html.append("</table>");
  }

  // ── Page: Features & Outdoor Spaces ─────────────────────────────

  private void appendFeaturesPage(
      StringBuilder html,
      List<PropertyAmenity> propertyAmenities,
      Map<UUID, Amenity> amenityMap,
      List<PropertyOutdoorArea> outdoorAreas) {
    boolean hasAmenities = !propertyAmenities.isEmpty();
    boolean hasOutdoorAreas = !outdoorAreas.isEmpty();
    if (!hasAmenities && !hasOutdoorAreas) {
      return;
    }

    appendPageStart(html, "Features &amp; Outdoor Spaces");

    if (hasAmenities) {
      appendSectionTitle(html, "Amenities");

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
                    + "letter-spacing:1.2px;color:#4a5568;margin:16px 0 6px 0;padding-bottom:4px;"
                    + "border-bottom:1px solid #e2e8f0;'>")
            .append(escapeHtml(formatEnumValue(entry.getKey())))
            .append("</div>");
        html.append("<div style='margin-bottom:10px;'>");
        for (Amenity amenity : entry.getValue()) {
          html.append(
                  "<span style='display:inline-block;background-color:#ebf4ff;border:1px solid"
                      + " #bee3f8;color:#2b6cb0;padding:4px"
                      + " 10px;border-radius:12px;font-size:11px;font-weight:500;margin:2px"
                      + " 3px;'>")
              .append(escapeHtml(amenity.getName()))
              .append("</span>");
        }
        html.append("</div>");
      }
    }

    if (hasOutdoorAreas) {
      appendSectionTitle(html, "Outdoor Spaces");
      for (PropertyOutdoorArea oa : outdoorAreas) {
        html.append(
            "<div style='border:1px solid #e2e8f0;border-radius:6px;padding:12px 16px;"
                + "margin-bottom:8px;background-color:#f0fff4;'>");
        html.append("<div style='font-size:15px;font-weight:600;color:#276749;'>")
            .append(escapeHtml(formatEnumValue(oa.getType())))
            .append("</div>");
        if (oa.getAreaValue().isPresent()) {
          String unit = oa.getAreaUnit() != null ? oa.getAreaUnit() : "sqm";
          html.append("<div style='font-size:13px;color:#4a5568;margin-top:2px;'>")
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
      StringBuilder html, Property property, @Nullable PropertyCategory category) {
    boolean hasSafetyData =
        isTrue(property.getHasSmokeDetectors().orElse(null))
            || isTrue(property.getHasCoDetectors().orElse(null))
            || isTrue(property.getHasFireExtinguisher().orElse(null))
            || isTrue(property.getHasSprinklerSystem().orElse(null))
            || isTrue(property.getHasAlarmSystem().orElse(null))
            || isTrue(property.getHasSecurityCameras().orElse(null))
            || isTrue(property.getHasSecureEntry().orElse(null))
            || property.getSafetyNotes().filter(s -> !s.isBlank()).isPresent();

    boolean skipAccessibility = category == PropertyCategory.INDUSTRIAL;
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

    appendPageStart(html, "Safety &amp; Accessibility");

    if (hasSafetyData) {
      appendSectionTitle(html, "Safety &amp; Security");
      html.append(
          "<div style='background-color:#f7fafc;border:1px solid #e2e8f0;border-radius:6px;"
              + "padding:16px 18px;margin-bottom:10px;'>");
      appendCheckItem(html, "Smoke Detectors", property.getHasSmokeDetectors().orElse(null));
      appendCheckItem(html, "CO Detectors", property.getHasCoDetectors().orElse(null));
      appendCheckItem(html, "Fire Extinguisher", property.getHasFireExtinguisher().orElse(null));
      appendCheckItem(html, "Sprinkler System", property.getHasSprinklerSystem().orElse(null));
      appendCheckItem(html, "Alarm System", property.getHasAlarmSystem().orElse(null));
      appendCheckItem(html, "Security Cameras", property.getHasSecurityCameras().orElse(null));
      appendCheckItem(html, "Secure Entry", property.getHasSecureEntry().orElse(null));
      html.append("</div>");

      appendTextBlock(html, "Safety Notes", property.getSafetyNotes().orElse(null));
    }

    if (hasAccessibilityData) {
      appendSectionTitle(html, "Accessibility");
      html.append(
          "<div style='background-color:#f7fafc;border:1px solid #e2e8f0;border-radius:6px;"
              + "padding:16px 18px;margin-bottom:10px;'>");
      appendCheckItem(
          html, "Wheelchair Accessible", property.getIsWheelchairAccessible().orElse(null));
      appendCheckItem(html, "Elevator", property.getHasElevator().orElse(null));
      appendCheckItem(html, "Step-Free Entrance", property.getHasStepFreeEntrance().orElse(null));
      appendCheckItem(html, "Adapted Bathroom", property.getHasAdaptedBathroom().orElse(null));
      html.append("</div>");

      appendTextBlock(html, "Accessibility Notes", property.getAccessibilityNotes().orElse(null));
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
    html.append("<p style='font-size:13px;color:#718096;margin-bottom:16px;'>")
        .append(photoEntries.size())
        .append(" photo")
        .append(photoEntries.size() != 1 ? "s" : "")
        .append("</p>");

    html.append("<table style='width:100%;border-collapse:collapse;'>");
    for (int i = 0; i < photoEntries.size(); i++) {
      if (i % 3 == 0) {
        html.append("<tr>");
      }
      String[] entry = photoEntries.get(i);
      boolean isMain = "1".equals(entry[2]);

      html.append("<td style='width:33%;padding:6px;vertical-align:top;'>");
      html.append(
          "<div style='border:1px solid #e2e8f0;border-radius:6px;overflow:hidden;"
              + "background-color:#f7fafc;'>");
      html.append("<img src='")
          .append(entry[0])
          .append("' style='width:100%;height:140px;object-fit:cover;display:block;'/>");
      html.append("<div style='padding:6px 8px;font-size:11px;color:#2d3748;'>");
      if (isMain) {
        html.append(
            "<span"
                + " style='display:inline-block;background-color:#ebf8ff;color:#2b6cb0;padding:1px"
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
          "<div style='background-color:#f7fafc;border:1px solid #e2e8f0;border-radius:6px;"
              + "padding:20px;margin:15px 0;'>");
      html.append("<h3 style='margin:0 0 15px 0;color:#1a365d;font-size:18px;'>Year ")
          .append(entry.getKey())
          .append("</h3>");
      html.append("<table style='width:100%;border-collapse:collapse;'><tr>");
      html.append(
              "<td style='text-align:center;padding:10px;'><div"
                  + " style='font-size:10px;color:#718096;text-transform:uppercase;"
                  + "letter-spacing:1px;'>Income</div><div"
                  + " style='font-size:22px;font-weight:700;margin-top:4px;color:#059669;'>")
          .append(CurrencyUtils.formatCurrency(summary.income, ccy))
          .append("</div></td>");
      html.append(
              "<td style='text-align:center;padding:10px;'><div"
                  + " style='font-size:10px;color:#718096;text-transform:uppercase;"
                  + "letter-spacing:1px;'>Expenses</div><div"
                  + " style='font-size:22px;font-weight:700;margin-top:4px;color:#dc2626;'>")
          .append(CurrencyUtils.formatCurrency(summary.expenses, ccy))
          .append("</div></td>");
      html.append(
              "<td style='text-align:center;padding:10px;'><div"
                  + " style='font-size:10px;color:#718096;text-transform:uppercase;letter-spacing:1px;'>Net"
                  + " Profit</div><div"
                  + " style='font-size:22px;font-weight:700;margin-top:4px;color:#2b6cb0;'>")
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
    Map<UUID, Tenant> primaryTenants =
        contractPartyService.getPrimaryTenantsForContracts(contractIds, teamId);

    appendPageStart(html, "Contracts");
    html.append("<p style='font-size:13px;color:#718096;margin-bottom:12px;'>")
        .append(contracts.size())
        .append(" contract(s) on record</p>");

    html.append("<table class='payment-table'><thead><tr>");
    html.append(
        "<th>Contract ID</th><th>Tenant</th><th>Start Date</th><th>End"
            + " Date</th><th>Rent</th><th>Status</th>");
    html.append("</tr></thead><tbody>");

    for (Contract contract : contracts) {
      Tenant tenant = primaryTenants.get(contract.getId());
      String tenantName =
          tenant != null
              ? tenant.getFirstName() + tenant.getLastName().map(n -> " " + n).orElse("")
              : "Unknown";

      html.append("<tr>");
      html.append("<td>#").append(contract.getIdentifier().orElseThrow().value()).append("</td>");
      html.append("<td>").append(escapeHtml(tenantName)).append("</td>");
      html.append("<td>").append(contract.getStartDate()).append("</td>");
      html.append("<td>")
          .append(contract.getEndDate().map(Object::toString).orElse("Ongoing"))
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
                + "<div style='font-size:9px;color:#718096;text-transform:uppercase;"
                + "letter-spacing:0.8px;'>")
        .append(escapeHtml(label))
        .append("</div><div style='font-size:16px;font-weight:700;color:#1a365d;margin-top:2px;'>")
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
      return "#6b7280";
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
      default -> "#6b7280";
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
