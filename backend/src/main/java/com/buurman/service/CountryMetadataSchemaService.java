package com.buurman.service;

import static com.buurman.dto.response.CountryMetadataSchemaResponse.FieldType.BOOLEAN;
import static com.buurman.dto.response.CountryMetadataSchemaResponse.FieldType.DECIMAL;
import static com.buurman.dto.response.CountryMetadataSchemaResponse.FieldType.ENUM;
import static com.buurman.dto.response.CountryMetadataSchemaResponse.FieldType.INTEGER;
import static com.buurman.dto.response.CountryMetadataSchemaResponse.FieldType.STRING;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.domain.metadata.CountryMetadataRegistry;
import com.buurman.dto.response.CountryMetadataSchemaResponse;
import com.buurman.dto.response.CountryMetadataSchemaResponse.EnumValue;
import com.buurman.dto.response.CountryMetadataSchemaResponse.FieldSchema;
import com.buurman.dto.response.CountryMetadataSchemaResponse.FieldType;
import com.buurman.dto.response.CountryMetadataSchemaResponse.GroupSchema;
import com.buurman.dto.response.CountryMetadataSchemaResponse.ValidationSchema;

import jakarta.annotation.PostConstruct;

@Service
public class CountryMetadataSchemaService {

  private final Map<String, CountryMetadataSchemaResponse> cache = new ConcurrentHashMap<>();

  private static final ValidationSchema EMPTY_VALIDATION =
      new ValidationSchema(Optional.empty(), Optional.empty(), Optional.empty());

  private static final List<EnumValue> ENERGY_RATINGS =
      List.of(
          enumVal("A", "A"),
          enumVal("B", "B"),
          enumVal("C", "C"),
          enumVal("D", "D"),
          enumVal("E", "E"),
          enumVal("F", "F"),
          enumVal("G", "G"));

  @PostConstruct
  void validateSchemaFieldParity() {
    // Validate dedicated schemas
    for (Map.Entry<String, Class<? extends com.buurman.domain.metadata.ContractCountryMetadata>>
        entry : com.buurman.domain.metadata.CountryMetadataRegistry.getSchemaClasses().entrySet()) {
      validateParity(entry.getKey(), entry.getValue());
    }
    // Also validate the Generic fallback schema
    validateParity("GENERIC", com.buurman.domain.metadata.GenericContractMetadata.class);
  }

  private void validateParity(
      String code, Class<? extends com.buurman.domain.metadata.ContractCountryMetadata> clazz) {
    Set<String> recordFields =
        Arrays.stream(clazz.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());
    Set<String> schemaFields =
        getSchema(code).fields().stream().map(FieldSchema::name).collect(Collectors.toSet());
    if (!recordFields.equals(schemaFields)) {
      Set<String> missingInSchema =
          recordFields.stream().filter(f -> !schemaFields.contains(f)).collect(Collectors.toSet());
      Set<String> missingInRecord =
          schemaFields.stream().filter(f -> !recordFields.contains(f)).collect(Collectors.toSet());
      throw new IllegalStateException(
          "Schema/record field mismatch for "
              + code
              + ": missingInSchema="
              + missingInSchema
              + ", missingInRecord="
              + missingInRecord);
    }
  }

  public CountryMetadataSchemaResponse getSchema(String countryCode) {
    String code = countryCode.toUpperCase(Locale.ROOT);
    return cache.computeIfAbsent(code, this::buildSchema);
  }

  private CountryMetadataSchemaResponse buildSchema(String countryCode) {
    boolean dedicated = CountryMetadataRegistry.hasDedicatedSchema(countryCode);
    String countryName = CountryMetadataRegistry.getCountryName(countryCode);

    List<FieldSchema> fields;
    List<GroupSchema> groups;

    if (dedicated) {
      fields = buildFieldsForCountry(countryCode);
      groups = buildGroupsForCountry(countryCode);
    } else {
      fields = buildGenericFields();
      groups = buildGenericGroups();
    }

    return new CountryMetadataSchemaResponse(countryCode, countryName, dedicated, fields, groups);
  }

  private List<FieldSchema> buildFieldsForCountry(String countryCode) {
    return switch (countryCode) {
      case "NL" -> buildNlFields();
      case "DE" -> buildDeFields();
      case "FR" -> buildFrFields();
      case "BE" -> buildBeFields();
      case "PT" -> buildPtFields();
      case "ES" -> buildEsFields();
      case "IT" -> buildItFields();
      case "GB" -> buildUkFields();
      case "US" -> buildUsFields();
      case "AT" -> buildAtFields();
      case "CH" -> buildChFields();
      case "DK" -> buildDkFields();
      case "SE" -> buildSeFields();
      case "FI" -> buildFiFields();
      case "NO" -> buildNoFields();
      case "IE" -> buildIeFields();
      case "PL" -> buildPlFields();
      case "CZ" -> buildCzFields();
      case "HU" -> buildHuFields();
      case "RO" -> buildRoFields();
      case "BG" -> buildBgFields();
      case "SK" -> buildSkFields();
      case "SI" -> buildSiFields();
      case "HR" -> buildHrFields();
      case "LT" -> buildLtFields();
      case "LV" -> buildLvFields();
      case "EE" -> buildEeFields();
      case "GR" -> buildGrFields();
      case "MT" -> buildMtFields();
      case "CY" -> buildCyFields();
      case "LU" -> buildLuFields();
      case "RS" -> buildRsFields();
      case "BA" -> buildBaFields();
      case "AL" -> buildAlFields();
      case "ME" -> buildMeFields();
      case "MK" -> buildMkFields();
      case "XK" -> buildXkFields();
      case "CA" -> buildCaFields();
      case "MX" -> buildMxFields();
      case "BR" -> buildBrFields();
      case "AR" -> buildArFields();
      case "CL" -> buildClFields();
      case "CO" -> buildCoFields();
      case "PE" -> buildPeFields();
      case "UY" -> buildUyFields();
      default -> buildGenericFields();
    };
  }

  private List<GroupSchema> buildGroupsForCountry(String countryCode) {
    return switch (countryCode) {
      case "NL" ->
          List.of(
              group("classification", "Classification"),
              group("costs", "Costs & Services"),
              group("services", "Service Costs Breakdown"),
              group("eligibility", "Eligibility"));
      case "DE" ->
          List.of(
              group("rent", "Rent Regulation"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy Certificate"));
      case "FR" ->
          List.of(
              group("general", "General"),
              group("rent", "Rent Control"),
              group("diagnostics", "Diagnostics"),
              group("deposit", "Deposit"));
      case "BE" ->
          List.of(
              group("general", "General"),
              group("energy", "Energy Certificate"),
              group("deposit", "Deposit"),
              group("indexation", "Rent Indexation"));
      case "PT" ->
          List.of(
              group("regime", "Lease Regime"),
              group("tax", "Tax & Registration"),
              group("energy", "Energy Certificate"));
      case "ES" ->
          List.of(
              group("classification", "Classification"),
              group("deposit", "Deposit & Guarantees"),
              group("energy", "Energy"));
      case "IT" ->
          List.of(
              group("contract", "Contract Type"),
              group("tax", "Tax & Registration"),
              group("deposit", "Deposit"),
              group("energy", "Energy"));
      case "GB" ->
          List.of(
              group("tenancy", "Tenancy Details"),
              group("compliance", "Compliance Checks"),
              group("deposit", "Deposit"));
      case "US" ->
          List.of(
              group("location", "Location & Regulation"),
              group("deposit", "Security Deposit"),
              group("compliance", "Compliance"));
      case "AT" ->
          List.of(
              group("regulation", "Rent Regulation"),
              group("deposit", "Deposit"),
              group("energy", "Energy Certificate"));
      case "CH" ->
          List.of(group("location", "Location & Regulation"), group("costs", "Costs & Deposit"));
      case "DK" ->
          List.of(
              group("regulation", "Lease Regulation"),
              group("deposit", "Deposit & Prepayment"),
              group("energy", "Energy"));
      case "SE" ->
          List.of(
              group("regulation", "Rent Regulation"),
              group("deposit", "Deposit"),
              group("energy", "Energy"));
      case "FI" ->
          List.of(
              group("general", "General"), group("deposit", "Deposit"), group("energy", "Energy"));
      case "NO" ->
          List.of(
              group("regulation", "Lease Regulation"),
              group("deposit", "Deposit"),
              group("energy", "Energy"));
      case "IE" ->
          List.of(
              group("tenancy", "Tenancy Details"),
              group("deposit", "Deposit"),
              group("energy", "Energy"));
      case "PL" ->
          List.of(
              group("general", "General"), group("deposit", "Deposit"), group("energy", "Energy"));
      case "CZ" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "HU" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "RO" ->
          List.of(
              group("general", "General"), group("deposit", "Deposit"), group("energy", "Energy"));
      case "BG" ->
          List.of(
              group("general", "General"), group("deposit", "Deposit"), group("energy", "Energy"));
      case "SK" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "SI" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "HR" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "LT" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "LV" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "EE" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "GR" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "MT" ->
          List.of(
              group("tenancy", "Tenancy Details"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "CY" ->
          List.of(
              group("tenancy", "Tenancy Details"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "LU" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "RS" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "BA" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "AL" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "ME" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "MK" ->
          List.of(
              group("general", "General"),
              group("costs", "Costs & Deposit"),
              group("energy", "Energy"));
      case "XK" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "CA" ->
          List.of(
              group("location", "Location & Regulation"),
              group("deposit", "Security Deposit"),
              group("energy", "Energy"));
      case "MX" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "BR" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "AR" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "CL" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "CO" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "PE" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      case "UY" -> List.of(group("general", "General"), group("costs", "Costs & Deposit"));
      default -> buildGenericGroups();
    };
  }

  // --- NL ---
  private List<FieldSchema> buildNlFields() {
    return List.of(
        field(
            "sectorClassification",
            "Sector Classification",
            ENUM,
            false,
            List.of(
                enumVal("VRIJE_SECTOR", "Vrije Sector (Free)"),
                enumVal("GEREGULEERD", "Gereguleerd (Regulated)"),
                enumVal("MIDDENHUUR", "Middenhuur (Mid-segment)")),
            null,
            "classification",
            "Dutch housing sector classification determines rent regulation rules",
            null),
        field(
            "wwsPoints",
            "WWS Points",
            INTEGER,
            false,
            null,
            validation(0, 500, null),
            "classification",
            "Woningwaarderingsstelsel point score (0-500) determines if property is regulated",
            "points"),
        field(
            "energyLabel",
            "Energy Label",
            ENUM,
            false,
            List.of(
                enumVal("A++++", "A++++"),
                enumVal("A+++", "A+++"),
                enumVal("A++", "A++"),
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "classification",
            "Official energy label for the property",
            null),
        field(
            "liberalizationThreshold",
            "Liberalization Threshold",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "classification",
            "Monthly rent amount above which a property enters the free sector",
            "EUR"),
        field(
            "allInRent",
            "All-in Rent",
            BOOLEAN,
            false,
            null,
            null,
            "costs",
            "Whether the rent amount includes service costs and utilities",
            null),
        field(
            "totalServiceCostsAmount",
            "Total Service Costs Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly amount charged for included services",
            "EUR"),
        field(
            "serviceGas",
            "Gas Included",
            BOOLEAN,
            false,
            null,
            null,
            "services",
            "Whether gas costs are included in the rent",
            null),
        field(
            "serviceWater",
            "Water Included",
            BOOLEAN,
            false,
            null,
            null,
            "services",
            "Whether water costs are included in the rent",
            null),
        field(
            "serviceElectricity",
            "Electricity Included",
            BOOLEAN,
            false,
            null,
            null,
            "services",
            "Whether electricity costs are included in the rent",
            null),
        field(
            "serviceInternet",
            "Internet Included",
            BOOLEAN,
            false,
            null,
            null,
            "services",
            "Whether internet costs are included in the rent",
            null),
        field(
            "serviceCleaning",
            "Cleaning Included",
            BOOLEAN,
            false,
            null,
            null,
            "services",
            "Whether cleaning costs are included in the rent",
            null),
        field(
            "huurcommissieEligible",
            "Huurcommissie Eligible",
            BOOLEAN,
            false,
            null,
            null,
            "eligibility",
            "Whether tenants can appeal to the Rent Tribunal (Huurcommissie)",
            null),
        field(
            "huurtoeslagEligible",
            "Huurtoeslag Eligible",
            BOOLEAN,
            false,
            null,
            null,
            "eligibility",
            "Whether tenants may qualify for housing benefit (Huurtoeslag)",
            null));
  }

  // --- DE ---
  private List<FieldSchema> buildDeFields() {
    return List.of(
        field(
            "mietspiegelReference",
            "Mietspiegel Reference",
            STRING,
            false,
            null,
            null,
            "rent",
            "Reference to the local rent index (Mietspiegel) used for comparison",
            null),
        field(
            "mietpreisbremseApplicable",
            "Mietpreisbremse Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "rent",
            "Whether the rent brake (Mietpreisbremse) applies in this area",
            null),
        field(
            "rentType",
            "Rent Type",
            ENUM,
            false,
            List.of(
                enumVal("STANDARD", "Standard"),
                enumVal("STAFFELMIETE", "Staffelmiete (Graduated)"),
                enumVal("INDEXMIETE", "Indexmiete (Index-linked)")),
            null,
            "rent",
            "Type of rent adjustment mechanism agreed in the contract",
            null),
        field(
            "warmRent",
            "Warm Rent (incl. utilities)",
            BOOLEAN,
            false,
            null,
            null,
            "costs",
            "Whether the rent includes utilities (Warmmiete) or is cold rent (Kaltmiete)",
            null),
        field(
            "nebenkostenAmount",
            "Nebenkosten Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly ancillary costs (utilities, maintenance, etc.)",
            "EUR"),
        field(
            "kautionAmount",
            "Kaution Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount (max 3 months cold rent per BGB \u00a7551)",
            "EUR"),
        field(
            "kautionMonths",
            "Kaution Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Number of months of rent held as security deposit (max 3)",
            "months"),
        field(
            "energyCertificateType",
            "Energy Certificate Type",
            ENUM,
            false,
            List.of(enumVal("VERBRAUCH", "Verbrauchsausweis"), enumVal("BEDARF", "Bedarfsausweis")),
            null,
            "energy",
            "Energieausweis type \u2014 required for all rentals since GEG 2020",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G"),
                enumVal("H", "H")),
            null,
            "energy",
            "Energy efficiency class from the Energieausweis",
            null),
        field(
            "energyCertificateValue",
            "Energy Certificate Value",
            DECIMAL,
            false,
            null,
            validation(0, 500, null),
            "energy",
            "Final energy demand or consumption value",
            "kWh/m\u00b2a"));
  }

  // --- FR ---
  private List<FieldSchema> buildFrFields() {
    return List.of(
        field(
            "referenceRentPrice",
            "Reference Rent Price",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "rent",
            "Reference rent per m\u00b2 set by local authorities",
            "EUR/m\u00b2"),
        field(
            "maxRentPrice",
            "Maximum Rent Price",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "rent",
            "Maximum allowed rent per m\u00b2 (reference + 20%)",
            "EUR/m\u00b2"),
        field(
            "zoneTendue",
            "Zone Tendue (Tense Zone)",
            BOOLEAN,
            false,
            null,
            null,
            "rent",
            "Whether the property is in a high-demand area (zone tendue) subject to rent control",
            null),
        field(
            "loiAlurCompliant",
            "Loi ALUR Compliant",
            BOOLEAN,
            false,
            null,
            null,
            "rent",
            "Whether the lease complies with Loi ALUR requirements",
            null),
        field(
            "diagnosticDpe",
            "DPE Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "diagnostics",
            "Energy Performance Diagnostic rating (Diagnostic de Performance"
                + " \u00c9nerg\u00e9tique)",
            null),
        field(
            "leadPaintDiagnostic",
            "Lead Paint Diagnostic Done",
            BOOLEAN,
            false,
            null,
            null,
            "diagnostics",
            "Whether the lead paint diagnostic (CREP) has been completed",
            null),
        field(
            "asbestosDiagnostic",
            "Asbestos Diagnostic Done",
            BOOLEAN,
            false,
            null,
            null,
            "diagnostics",
            "Whether the asbestos diagnostic has been completed",
            null),
        field(
            "gasDiagnostic",
            "Gas Diagnostic Done",
            BOOLEAN,
            false,
            null,
            null,
            "diagnostics",
            "Whether the gas installation diagnostic has been completed (required for installations"
                + " >15 years)",
            null),
        field(
            "electricityDiagnostic",
            "Electricity Diagnostic Done",
            BOOLEAN,
            false,
            null,
            null,
            "diagnostics",
            "Whether the electrical installation diagnostic has been completed (required for"
                + " installations >15 years)",
            null),
        field(
            "erpDiagnostic",
            "Natural Risk Assessment",
            BOOLEAN,
            false,
            null,
            null,
            "diagnostics",
            "Whether the ERP (\u00c9tat des Risques et Pollutions) natural risk assessment has been"
                + " completed",
            null),
        field(
            "furnishedLease",
            "Furnished Lease",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Meubl\u00e9 (furnished) \u2014 affects contract duration (1yr vs 3yr), deposit cap,"
                + " and notice periods (Loi ALUR)",
            null),
        field(
            "cautionAmount",
            "Caution Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit (caution) \u2014 capped at 1 month unfurnished, 2 months furnished"
                + " (Art. 22 Loi ALUR)",
            "EUR"),
        field(
            "cautionMonths",
            "Caution Months",
            INTEGER,
            false,
            null,
            validation(0, 2, null),
            "deposit",
            "Deposit in months of rent \u2014 1 month unfurnished, 2 months furnished (Loi ALUR)",
            "months"));
  }

  // --- BE ---
  private List<FieldSchema> buildBeFields() {
    return List.of(
        field(
            "region",
            "Region",
            ENUM,
            false,
            List.of(
                enumVal("BRUSSELS", "Brussels"),
                enumVal("WALLONIA", "Wallonia"),
                enumVal("FLANDERS", "Flanders")),
            null,
            "general",
            "Belgian region governing tenancy law (different rules per region)",
            null),
        field(
            "indexationBase",
            "Indexation Base",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "general",
            "Base amount used for annual rent indexation calculation",
            "EUR"),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Energy Performance Certificate (EPC/PEB) rating \u2014 Flanders uses A+ to G scale",
            null),
        field(
            "energyCertificateNumber",
            "Energy Certificate Number",
            STRING,
            false,
            null,
            null,
            "energy",
            "Official energy certificate reference number",
            null),
        field(
            "registrationNumber",
            "Registration Number",
            STRING,
            false,
            null,
            null,
            "general",
            "Lease registration number with the Federal Public Service",
            null),
        field(
            "depositMonths",
            "Deposit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Deposit in months of rent \u2014 max 2 (Wallonia) or 3 (Brussels/Flanders), must be"
                + " held in blocked account",
            null),
        field(
            "depositType",
            "Deposit Type",
            ENUM,
            false,
            List.of(
                enumVal("BLOCKED_ACCOUNT", "Blocked Account (Geblokkeerde Rekening)"),
                enumVal("BANK_GUARANTEE", "Bank Guarantee (Bankwaarborg)"),
                enumVal("OCMW_GUARANTEE", "OCMW/CPAS Guarantee")),
            null,
            "deposit",
            "Type of deposit protection \u2014 blocked account is most common",
            null),
        field(
            "indexationBaseMonth",
            "Indexation Base Month",
            STRING,
            false,
            null,
            null,
            "indexation",
            "Reference month/year of the base health index (gezondheidsindex) for rent indexation"
                + " calculation",
            null));
  }

  // --- PT ---
  private List<FieldSchema> buildPtFields() {
    return List.of(
        field(
            "nrauRegime",
            "NRAU Regime",
            ENUM,
            false,
            List.of(
                enumVal("NRAU", "NRAU (current regime)"),
                enumVal("VINCULISTICO", "Vinculístico (old regime)"),
                enumVal("RAU", "RAU (transitional)")),
            null,
            "regime",
            "Applicable NRAU (Novo Regime do Arrendamento Urbano) lease regime",
            null),
        field(
            "oldLeaseRegime",
            "Old Lease Regime (Renda Antiga)",
            BOOLEAN,
            false,
            null,
            null,
            "regime",
            "Whether the lease falls under the old rent regime (Renda Antiga, pre-1990)",
            null),
        field(
            "updateCoefficient",
            "Update Coefficient",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "regime",
            "Annual rent update coefficient published by INE",
            null),
        field(
            "imiReference",
            "IMI Reference Number",
            STRING,
            false,
            null,
            null,
            "tax",
            "Municipal Property Tax (IMI) reference number",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("B-", "B-"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F")),
            null,
            "energy",
            "Certificado Energ\u00e9tico (SCE) rating \u2014 required for all rentals since 2013",
            null));
  }

  // --- ES ---
  private List<FieldSchema> buildEsFields() {
    return List.of(
        field(
            "viviendaHabitual",
            "Primary Residence",
            BOOLEAN,
            false,
            null,
            null,
            "classification",
            "Whether this is the tenant's primary/habitual residence",
            null),
        field(
            "zonaTensionada",
            "Zona Tensionada",
            BOOLEAN,
            false,
            null,
            null,
            "classification",
            "Whether the property is in a stressed housing market zone (limits rent increases)",
            null),
        field(
            "referencePriceIndex",
            "Reference Price Index",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "classification",
            "Official reference price index for the area",
            "EUR/m\u00b2"),
        field(
            "fianzaAmount",
            "Fianza Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Legal deposit (fianza) amount",
            "EUR"),
        field(
            "fianzaMonths",
            "Fianza Months",
            INTEGER,
            false,
            null,
            validation(0, 2, null),
            "deposit",
            "Number of months of fianza deposit (1 for residential, 2 for commercial per LAU Art."
                + " 36)",
            "months"),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy Efficiency Certificate rating",
            null),
        field(
            "garantiaAdicionalAmount",
            "Garant\u00eda Adicional Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Additional guarantee amount beyond fianza (LAU Art. 36.5, up to 2 months for"
                + " residential)",
            "EUR"),
        field(
            "garantiaAdicionalMonths",
            "Garant\u00eda Adicional Months",
            INTEGER,
            false,
            null,
            validation(0, 2, null),
            "deposit",
            "Additional guarantee in months of rent (max 2 per LAU Art. 36.5)",
            "months"));
  }

  // --- IT ---
  private List<FieldSchema> buildItFields() {
    return List.of(
        field(
            "contractCategory",
            "Contract Category",
            ENUM,
            false,
            List.of(
                enumVal("LIBERO", "Libero (4+4)"),
                enumVal("CONCORDATO", "Concordato (3+2)"),
                enumVal("TRANSITORIO", "Transitorio"),
                enumVal("STUDENTI", "Studenti")),
            null,
            "contract",
            "Italian lease category determining duration and renewal terms",
            null),
        field(
            "cedolareSecca",
            "Cedolare Secca",
            BOOLEAN,
            false,
            null,
            null,
            "tax",
            "Whether the flat-rate tax regime (Cedolare Secca) is elected",
            null),
        field(
            "cedolareRate",
            "Cedolare Rate (%)",
            DECIMAL,
            false,
            null,
            validation(0, 100, null),
            "tax",
            "Cedolare Secca tax rate (21% standard, 10% for concordato/affordable)",
            "%"),
        field(
            "registrationNumber",
            "Registration Number",
            STRING,
            false,
            null,
            null,
            "tax",
            "Agenzia delle Entrate lease registration number",
            null),
        field(
            "apeRating",
            "APE Rating",
            ENUM,
            false,
            List.of(
                enumVal("A4", "A4"),
                enumVal("A3", "A3"),
                enumVal("A2", "A2"),
                enumVal("A1", "A1"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Attestato di Prestazione Energetica (APE) energy rating (A4 highest, G lowest, per DM"
                + " 26/06/2015)",
            null),
        field(
            "depositoAmount",
            "Deposito Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit (deposito cauzionale) amount per Art. 11 L. 392/1978",
            "EUR"),
        field(
            "depositoMonths",
            "Deposito Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Deposit in months of rent (max 3 per Art. 11 L. 392/1978)",
            "months"));
  }

  // --- UK ---
  private List<FieldSchema> buildUkFields() {
    return List.of(
        field(
            "tenancyType",
            "Tenancy Type",
            ENUM,
            false,
            List.of(
                enumVal("AST", "Assured Shorthold Tenancy"),
                enumVal("PERIODIC", "Periodic Tenancy"),
                enumVal("REGULATED", "Regulated Tenancy"),
                enumVal("COMPANY_LET", "Company Let")),
            null,
            "tenancy",
            "Type of tenancy agreement governing the lease",
            null),
        field(
            "depositScheme",
            "Deposit Scheme",
            ENUM,
            false,
            List.of(
                enumVal("DPS", "Deposit Protection Service"),
                enumVal("MYDEPOSITS", "MyDeposits"),
                enumVal("TDS", "Tenancy Deposit Scheme")),
            null,
            "tenancy",
            "Government-approved tenancy deposit protection scheme",
            null),
        field(
            "epcRating",
            "EPC Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "tenancy",
            "Energy Performance Certificate rating (properties below E cannot be newly let since"
                + " April 2020)",
            null),
        field(
            "rightToRentChecked",
            "Right to Rent Checked",
            BOOLEAN,
            false,
            null,
            null,
            "compliance",
            "Whether the Right to Rent immigration check has been completed",
            null),
        field(
            "gasSafetyCertificate",
            "Gas Safety Certificate",
            BOOLEAN,
            false,
            null,
            null,
            "compliance",
            "Whether a valid Gas Safety Certificate (CP12) is in place",
            null),
        field(
            "electricalSafetyCertificate",
            "Electrical Safety Certificate",
            BOOLEAN,
            false,
            null,
            null,
            "compliance",
            "Whether a valid Electrical Installation Condition Report (EICR) is in place",
            null),
        field(
            "depositAmount",
            "Deposit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Deposit amount \u2014 capped at 5 weeks\u2019 rent (\u226450k/yr) or 6 weeks\u2019"
                + " rent (>50k/yr)",
            "GBP"));
  }

  // --- US ---
  private List<FieldSchema> buildUsFields() {
    return List.of(
        field(
            "state",
            "State",
            ENUM,
            false,
            List.of(
                enumVal("AL", "Alabama"),
                enumVal("AK", "Alaska"),
                enumVal("AZ", "Arizona"),
                enumVal("AR", "Arkansas"),
                enumVal("CA", "California"),
                enumVal("CO", "Colorado"),
                enumVal("CT", "Connecticut"),
                enumVal("DE", "Delaware"),
                enumVal("DC", "District of Columbia"),
                enumVal("FL", "Florida"),
                enumVal("GA", "Georgia"),
                enumVal("HI", "Hawaii"),
                enumVal("ID", "Idaho"),
                enumVal("IL", "Illinois"),
                enumVal("IN", "Indiana"),
                enumVal("IA", "Iowa"),
                enumVal("KS", "Kansas"),
                enumVal("KY", "Kentucky"),
                enumVal("LA", "Louisiana"),
                enumVal("ME", "Maine"),
                enumVal("MD", "Maryland"),
                enumVal("MA", "Massachusetts"),
                enumVal("MI", "Michigan"),
                enumVal("MN", "Minnesota"),
                enumVal("MS", "Mississippi"),
                enumVal("MO", "Missouri"),
                enumVal("MT", "Montana"),
                enumVal("NE", "Nebraska"),
                enumVal("NV", "Nevada"),
                enumVal("NH", "New Hampshire"),
                enumVal("NJ", "New Jersey"),
                enumVal("NM", "New Mexico"),
                enumVal("NY", "New York"),
                enumVal("NC", "North Carolina"),
                enumVal("ND", "North Dakota"),
                enumVal("OH", "Ohio"),
                enumVal("OK", "Oklahoma"),
                enumVal("OR", "Oregon"),
                enumVal("PA", "Pennsylvania"),
                enumVal("RI", "Rhode Island"),
                enumVal("SC", "South Carolina"),
                enumVal("SD", "South Dakota"),
                enumVal("TN", "Tennessee"),
                enumVal("TX", "Texas"),
                enumVal("UT", "Utah"),
                enumVal("VT", "Vermont"),
                enumVal("VA", "Virginia"),
                enumVal("WA", "Washington"),
                enumVal("WV", "West Virginia"),
                enumVal("WI", "Wisconsin"),
                enumVal("WY", "Wyoming")),
            null,
            "location",
            "US state where the property is located",
            null),
        field(
            "rentControlled",
            "Rent Controlled",
            BOOLEAN,
            false,
            null,
            null,
            "location",
            "Whether the property is subject to local rent control regulations",
            null),
        field(
            "rentControlJurisdiction",
            "Rent Control Jurisdiction",
            STRING,
            false,
            null,
            null,
            "location",
            "Name of the rent control jurisdiction (city/county)",
            null),
        field(
            "section8Eligible",
            "Section 8 Eligible",
            BOOLEAN,
            false,
            null,
            null,
            "compliance",
            "Whether the property accepts Section 8 Housing Choice Vouchers",
            null),
        field(
            "leadPaintDisclosure",
            "Lead Paint Disclosure",
            BOOLEAN,
            false,
            null,
            null,
            "compliance",
            "Whether the lead-based paint disclosure has been provided (required for pre-1978"
                + " buildings)",
            null),
        field(
            "securityDepositLimit",
            "Security Deposit Limit",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Maximum security deposit amount allowed by state law",
            "USD"),
        field(
            "securityDepositMonths",
            "Security Deposit Max Months",
            INTEGER,
            false,
            null,
            validation(0, 12, null),
            "deposit",
            "Maximum months of rent allowed as security deposit by state law",
            "months"));
  }

  // --- AT (Austria) ---
  private List<FieldSchema> buildAtFields() {
    return List.of(
        field(
            "mietrechtsgesetzCategory",
            "Rent Law Category",
            ENUM,
            false,
            List.of(
                enumVal("MRG", "MRG (Mietrechtsgesetz)"),
                enumVal("WGG", "WGG (Wohnungsgemeinn\u00fctzigkeitsgesetz)"),
                enumVal("ABGB", "ABGB (Allgemeines B\u00fcrgerliches Gesetzbuch)")),
            null,
            "regulation",
            "Applicable rent law regime",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A++", "A++"),
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Energieausweis rating per OIB Richtlinie 6",
            null),
        field(
            "betriebskostenAmount",
            "Betriebskosten Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "regulation",
            "Monthly operating costs (Betriebskosten)",
            "EUR"),
        field(
            "kautionMonths",
            "Kaution Months",
            INTEGER,
            false,
            null,
            validation(0, 6, null),
            "deposit",
            "Deposit in months of rent (typically 3\u20136)",
            "months"),
        field(
            "kautionAmount",
            "Kaution Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "EUR"),
        field(
            "befristung",
            "Fixed-Term (Befristung)",
            BOOLEAN,
            false,
            null,
            null,
            "regulation",
            "Whether the lease is fixed-term (min 3 years per MRG \u00a729)",
            null),
        field(
            "richtwertmiete",
            "Richtwertmiete",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "regulation",
            "Reference rent per m\u00b2 under MRG \u00a716",
            "EUR/m\u00b2"),
        field(
            "energyCertificateNumber",
            "Energy Certificate Number",
            STRING,
            false,
            null,
            null,
            "energy",
            "Official Energieausweis reference number",
            null));
  }

  // --- CH (Switzerland) ---
  private List<FieldSchema> buildChFields() {
    return List.of(
        field(
            "canton",
            "Canton",
            STRING,
            false,
            null,
            null,
            "location",
            "Two-letter cantonal code (e.g. ZH, BE, GE)",
            null),
        field(
            "mietrechtRegion",
            "Mietrecht Region",
            STRING,
            false,
            null,
            null,
            "location",
            "Rental law region for reference interest rate applicability",
            null),
        field(
            "nebenkostenAmount",
            "Nebenkosten Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly ancillary costs",
            "CHF"),
        field(
            "kautionMonths",
            "Kaution Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent (max 3 per OR Art. 257e)",
            "months"),
        field(
            "kautionAmount",
            "Kaution Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "CHF"),
        field(
            "referenzzinssatzApplicable",
            "Referenzzinssatz Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "location",
            "Whether the reference interest rate (Referenzzinssatz) applies for rent adjustments",
            null),
        field(
            "referenzzinssatz",
            "Referenzzinssatz",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "location",
            "Current reference interest rate (%)",
            "%"));
  }

  // --- DK (Denmark) ---
  private List<FieldSchema> buildDkFields() {
    return List.of(
        field(
            "lejelovType",
            "Lejelov Type",
            ENUM,
            false,
            List.of(
                enumVal("PRIVATE", "Private Rental"), enumVal("ALMEN", "Almen (Social Housing)")),
            null,
            "regulation",
            "Type of rental law (Lejeloven vs Almenlejeloven)",
            null),
        field(
            "energyLabel",
            "Energy Label",
            ENUM,
            false,
            List.of(
                enumVal("A2015", "A2015"),
                enumVal("A2010", "A2010"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Official energy label (Energim\u00e6rke)",
            null),
        field(
            "depositumAmount",
            "Depositum Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "DKK"),
        field(
            "depositumMonths",
            "Depositum Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Deposit in months of rent (max 3 per Lejeloven \u00a734)",
            "months"),
        field(
            "forudbetalingAmount",
            "Forudbetaling Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Prepaid rent amount",
            "DKK"),
        field(
            "forudbetalingMonths",
            "Forudbetaling Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Prepaid rent in months (max 3)",
            "months"),
        field(
            "huslejenaevnEligible",
            "Huslejen\u00e6vn Eligible",
            BOOLEAN,
            false,
            null,
            null,
            "regulation",
            "Whether tenants can appeal to the Rent Tribunal (Huslejen\u00e6vn)",
            null));
  }

  // --- SE (Sweden) ---
  private List<FieldSchema> buildSeFields() {
    return List.of(
        field(
            "hyrestyp",
            "Hyrestyp",
            ENUM,
            false,
            List.of(enumVal("PRIVATE", "Private Rental"), enumVal("KOMMUNAL", "Municipal Housing")),
            null,
            "regulation",
            "Type of rental (private vs municipal)",
            null),
        field(
            "bruksvardessystemApplicable",
            "Bruksv\u00e4rdessystem Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "regulation",
            "Whether the utility value system applies for rent setting",
            null),
        field(
            "energyDeclarationRating",
            "Energy Declaration Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energideklaration rating",
            null),
        field(
            "depositAmount",
            "Deposit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "SEK"),
        field(
            "depositMonths",
            "Deposit Months",
            INTEGER,
            false,
            null,
            validation(0, 6, null),
            "deposit",
            "Deposit in months of rent",
            "months"),
        field(
            "hyresnamndenEligible",
            "Hyresn\u00e4mnden Eligible",
            BOOLEAN,
            false,
            null,
            null,
            "regulation",
            "Whether the Rent Tribunal (Hyresn\u00e4mnden) has jurisdiction",
            null));
  }

  // --- FI (Finland) ---
  private List<FieldSchema> buildFiFields() {
    return List.of(
        field(
            "vuokrasopimustyyppi",
            "Vuokrasopimustyyppi",
            ENUM,
            false,
            List.of(
                enumVal("FIXED", "Fixed-term (M\u00e4\u00e4r\u00e4aikainen)"),
                enumVal("INDEFINITE", "Indefinite (Toistaiseksi voimassa)")),
            null,
            "general",
            "Lease type under Finnish Tenancy Act",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energiatodistus rating",
            null),
        field(
            "vakuusAmount",
            "Vakuus Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit (vakuus) amount",
            "EUR"),
        field(
            "vakuusMonths",
            "Vakuus Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Deposit in months of rent (typically 1\u20133)",
            "months"),
        field(
            "araRestricted",
            "ARA Restricted",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the property is under ARA (Housing Finance and Development Centre)"
                + " restrictions",
            null));
  }

  // --- NO (Norway) ---
  private List<FieldSchema> buildNoFields() {
    return List.of(
        field(
            "husleielovType",
            "Husleielov Type",
            ENUM,
            false,
            List.of(enumVal("RESIDENTIAL", "Residential"), enumVal("COMMERCIAL", "Commercial")),
            null,
            "regulation",
            "Lease type under the Husleieloven (Tenancy Act)",
            null),
        field(
            "energyLabel",
            "Energy Label",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Official energy label (Energimerke)",
            null),
        field(
            "depositumskontoAmount",
            "Depositumskonto Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "NOK"),
        field(
            "depositumskontoMonths",
            "Depositumskonto Months",
            INTEGER,
            false,
            null,
            validation(0, 6, null),
            "deposit",
            "Deposit in months of rent (max 6 per Husleieloven \u00a73-5)",
            "months"),
        field(
            "husleietvistnemnda",
            "Husleietvistnemnda",
            BOOLEAN,
            false,
            null,
            null,
            "regulation",
            "Whether the Rent Dispute Tribunal has jurisdiction",
            null),
        field(
            "kommunalBolig",
            "Kommunal Bolig",
            BOOLEAN,
            false,
            null,
            null,
            "regulation",
            "Whether this is municipal housing",
            null));
  }

  // --- IE (Ireland) ---
  private List<FieldSchema> buildIeFields() {
    return List.of(
        field(
            "tenancyType",
            "Tenancy Type",
            ENUM,
            false,
            List.of(
                enumVal("PART4", "Part 4 Tenancy"),
                enumVal("FIXED", "Fixed-Term"),
                enumVal("PERIODIC", "Periodic")),
            null,
            "tenancy",
            "Type of tenancy under Residential Tenancies Act",
            null),
        field(
            "berRating",
            "BER Rating",
            ENUM,
            false,
            List.of(
                enumVal("A1", "A1"),
                enumVal("A2", "A2"),
                enumVal("A3", "A3"),
                enumVal("B1", "B1"),
                enumVal("B2", "B2"),
                enumVal("B3", "B3"),
                enumVal("C1", "C1"),
                enumVal("C2", "C2"),
                enumVal("C3", "C3"),
                enumVal("D1", "D1"),
                enumVal("D2", "D2"),
                enumVal("E1", "E1"),
                enumVal("E2", "E2"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Building Energy Rating (BER) \u2014 mandatory for all rental properties",
            null),
        field(
            "depositAmount",
            "Deposit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "EUR"),
        field(
            "depositMonths",
            "Deposit Months",
            INTEGER,
            false,
            null,
            validation(0, 2, null),
            "deposit",
            "Deposit in months of rent (typically 1\u20132)",
            "months"),
        field(
            "rtbRegistered",
            "RTB Registered",
            BOOLEAN,
            false,
            null,
            null,
            "tenancy",
            "Whether the tenancy is registered with the Residential Tenancies Board",
            null),
        field(
            "rentPressureZone",
            "Rent Pressure Zone",
            BOOLEAN,
            false,
            null,
            null,
            "tenancy",
            "Whether the property is in a Rent Pressure Zone (RPZ)",
            null),
        field(
            "marketRentAmount",
            "Market Rent Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "tenancy",
            "Current market rent for comparison with RPZ limits",
            "EUR"),
        field(
            "berCertificateNumber",
            "BER Certificate Number",
            STRING,
            false,
            null,
            null,
            "energy",
            "Official BER certificate reference number",
            null));
  }

  // --- PL (Poland) ---
  private List<FieldSchema> buildPlFields() {
    return List.of(
        field(
            "rodzajNajmu",
            "Rodzaj Najmu",
            ENUM,
            false,
            List.of(
                enumVal("OKAZJONALNY", "Najem Okazjonalny"),
                enumVal("INSTYTUCJONALNY", "Najem Instytucjonalny"),
                enumVal("ZWYKLY", "Najem Zwyk\u0142y")),
            null,
            "general",
            "Type of rental agreement under Polish Civil Code",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "\u015awiadectwo charakterystyki energetycznej rating",
            null),
        field(
            "kaucjaAmount",
            "Kaucja Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit (kaucja) amount",
            "PLN"),
        field(
            "kaucjaMonths",
            "Kaucja Months",
            INTEGER,
            false,
            null,
            validation(0, 12, null),
            "deposit",
            "Deposit in months of rent (max 12 for najem okazjonalny)",
            "months"),
        field(
            "indexationApplicable",
            "Indexation Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether rent indexation clause applies",
            null),
        field(
            "energyCertificateNumber",
            "Energy Certificate Number",
            STRING,
            false,
            null,
            null,
            "energy",
            "Official energy certificate reference number",
            null),
        field(
            "czynszdodatkowy",
            "Czynsz Dodatkowy",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether additional charges (czynsz administracyjny) apply on top of rent",
            null));
  }

  // --- CZ (Czech Republic) ---
  private List<FieldSchema> buildCzFields() {
    return List.of(
        field(
            "najemniSmlouvaType",
            "N\u00e1jemn\u00ed Smlouva Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Czech Civil Code (\u00a72201\u2013\u00a72331)",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Pr\u016fkaz energetick\u00e9 n\u00e1ro\u010dnosti budovy rating",
            null),
        field(
            "kauceAmount",
            "Kauce Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (kauce) amount",
            "CZK"),
        field(
            "kauceMonths",
            "Kauce Months",
            INTEGER,
            false,
            null,
            validation(0, 6, null),
            "costs",
            "Deposit in months of rent (typically 1\u20133, max varies)",
            "months"),
        field(
            "sluzbyAmount",
            "Slu\u017eby Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly service charges (slu\u017eby)",
            "CZK"),
        field(
            "regulatedRent",
            "Regulated Rent",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the rent is subject to municipal regulation",
            null));
  }

  // --- HU (Hungary) ---
  private List<FieldSchema> buildHuFields() {
    return List.of(
        field(
            "berletiszerzodesType",
            "B\u00e9rleti Szerz\u0151d\u00e9s Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Hungarian Civil Code",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("AA++", "AA++"),
                enumVal("AA+", "AA+"),
                enumVal("AA", "AA"),
                enumVal("BB", "BB"),
                enumVal("CC", "CC"),
                enumVal("DD", "DD"),
                enumVal("EE", "EE"),
                enumVal("FF", "FF"),
                enumVal("GG", "GG"),
                enumVal("HH", "HH"),
                enumVal("II", "II"),
                enumVal("JJ", "JJ")),
            null,
            "energy",
            "Hungarian energy performance certificate rating (7/2006 TNM)",
            null),
        field(
            "kaucioAmount",
            "Kauci\u00f3 Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (kauci\u00f3) amount",
            "HUF"),
        field(
            "kaucioMonths",
            "Kauci\u00f3 Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent (typically 1\u20133)",
            "months"),
        field(
            "kozosKoltsegAmount",
            "K\u00f6z\u00f6s K\u00f6lts\u00e9g Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common costs (k\u00f6z\u00f6s k\u00f6lts\u00e9g)",
            "HUF"),
        field(
            "lakberApplicable",
            "Lakb\u00e9r Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether municipal rent (lakb\u00e9r) regulation applies",
            null));
  }

  // --- RO (Romania) ---
  private List<FieldSchema> buildRoFields() {
    return List.of(
        field(
            "contractType",
            "Contract Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Romanian Civil Code (Art. 1777\u20131835)",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Certificat de performan\u021b\u0103 energetic\u0103 rating",
            null),
        field(
            "garantieAmount",
            "Garan\u021bie Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit (garan\u021bie) amount",
            "RON"),
        field(
            "garantieMonths",
            "Garan\u021bie Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Deposit in months of rent",
            "months"),
        field(
            "anafRegistered",
            "ANAF Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the contract is registered with ANAF (fiscal authority)",
            null),
        field(
            "intretinereAmount",
            "\u00centre\u021binere Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Monthly maintenance/utility charges",
            "RON"));
  }

  // --- BG (Bulgaria) ---
  private List<FieldSchema> buildBgFields() {
    return List.of(
        field(
            "contractType",
            "Contract Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Bulgarian Obligations and Contracts Act",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "\u0421\u0435\u0440\u0442\u0438\u0444\u0438\u043a\u0430\u0442 \u0437\u0430"
                + " \u0435\u043d\u0435\u0440\u0433\u0438\u0439\u043d\u0438"
                + " \u0445\u0430\u0440\u0430\u043a\u0442\u0435\u0440\u0438\u0441\u0442\u0438\u043a\u0438"
                + " rating",
            null),
        field(
            "depozitAmount",
            "Depozit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "BGN"),
        field(
            "depozitMonths",
            "Depozit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "deposit",
            "Deposit in months of rent",
            "months"),
        field(
            "notarizedContract",
            "Notarized Contract",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the contract is notarized (required for registration)",
            null),
        field(
            "obshtiRazhodiAmount",
            "\u041e\u0431\u0449\u0438 \u0420\u0430\u0437\u0445\u043e\u0434\u0438 Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Monthly common charges",
            "BGN"));
  }

  // --- SK (Slovakia) ---
  private List<FieldSchema> buildSkFields() {
    return List.of(
        field(
            "najomnaZmluvaType",
            "N\u00e1jomn\u00e1 Zmluva Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Slovak Civil Code",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A0", "A0"),
                enumVal("A1", "A1"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Energetick\u00fd certifik\u00e1t rating",
            null),
        field(
            "kauciaAmount",
            "Kaucia Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (kaucia) amount",
            "EUR"),
        field(
            "kauciaMonths",
            "Kaucia Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent (typically 1\u20133)",
            "months"),
        field(
            "poplatkyAmount",
            "Poplatky Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly service charges (poplatky)",
            "EUR"),
        field(
            "regulatedRent",
            "Regulated Rent",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the rent is subject to regulation",
            null));
  }

  // --- SI (Slovenia) ---
  private List<FieldSchema> buildSiFields() {
    return List.of(
        field(
            "najemnaPogodbaTip",
            "Najemna Pogodba Tip",
            ENUM,
            false,
            List.of(
                enumVal("TRZNO", "Tr\u017eno (Market)"),
                enumVal("NEPROFITNO", "Neprofitno (Non-profit)"),
                enumVal("SLUZBENO", "Slu\u017ebeno (Service)")),
            null,
            "general",
            "Lease type under Slovenian Housing Act (SZ-1)",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A1", "A1"),
                enumVal("A2", "A2"),
                enumVal("B1", "B1"),
                enumVal("B2", "B2"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Energetska izkaznica rating",
            null),
        field(
            "varscinsAmount",
            "Var\u0161\u010dina Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (var\u0161\u010dina) amount",
            "EUR"),
        field(
            "varscinsMonths",
            "Var\u0161\u010dina Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "neprofitnoStanovanje",
            "Neprofitno Stanovanje",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether this is non-profit housing",
            null),
        field(
            "rezervniFondAmount",
            "Rezervni Fond Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly reserve fund contribution",
            "EUR"));
  }

  // --- HR (Croatia) ---
  private List<FieldSchema> buildHrFields() {
    return List.of(
        field(
            "ugovorType",
            "Ugovor Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Croatian Obligations Act",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Energetski certifikat rating",
            null),
        field(
            "jamcevinaAmount",
            "Jam\u010devina Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (jam\u010devina) amount",
            "EUR"),
        field(
            "jamcevinaMonths",
            "Jam\u010devina Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "poreznaUprava",
            "Porezna Uprava",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the contract is registered with the Tax Administration",
            null),
        field(
            "pricuvaAmount",
            "Pri\u010duva Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly reserve fund (pri\u010duva) contribution",
            "EUR"));
  }

  // --- LT (Lithuania) ---
  private List<FieldSchema> buildLtFields() {
    return List.of(
        field(
            "nuomosSutartisType",
            "Nuomos Sutartis Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Lithuanian Civil Code",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A++", "A++"),
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G")),
            null,
            "energy",
            "Energinio naudingumo sertifikatas rating",
            null),
        field(
            "uzstatasAmount",
            "U\u017estatas Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (u\u017estatas) amount",
            "EUR"),
        field(
            "uzstatasMonths",
            "U\u017estatas Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "registruCentras",
            "Registr\u0173 Centras",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the lease is registered with the Centre of Registers",
            null),
        field(
            "komunaliniaiAmount",
            "Komunaliniai Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly utility charges (komunaliniai mok\u0117\u0161\u010diai)",
            "EUR"));
  }

  // --- LV (Latvia) ---
  private List<FieldSchema> buildLvFields() {
    return List.of(
        field(
            "iresLigumsType",
            "\u012ares L\u012bgums Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Latvian Civil Law",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "\u0112kas energoefektivit\u0101tes sertifik\u0101ts rating",
            null),
        field(
            "drosibaNaudaAmount",
            "Dro\u0161\u012bbas Nauda Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (dro\u0161\u012bbas nauda) amount",
            "EUR"),
        field(
            "drosibaNaudaMonths",
            "Dro\u0161\u012bbas Nauda Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "zemesgramataRegistered",
            "Zemesgr\u0101mata Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the lease is registered in the Land Register (Zemesgr\u0101mata)",
            null),
        field(
            "komunalieAmount",
            "Komun\u0101lie Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly communal charges",
            "EUR"));
  }

  // --- EE (Estonia) ---
  private List<FieldSchema> buildEeFields() {
    return List.of(
        field(
            "uuerilepinguType",
            "\u00dc\u00fcrileping Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Estonian Law of Obligations Act",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G"),
                enumVal("H", "H")),
            null,
            "energy",
            "Energiam\u00e4rgis rating",
            null),
        field(
            "tagatisrahaAmount",
            "Tagatisraha Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (tagatisraha) amount",
            "EUR"),
        field(
            "tagatisrahaMonths",
            "Tagatisraha Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent (max 3 per V\u00d5S \u00a7308)",
            "months"),
        field(
            "kinnistusraamatRegistered",
            "Kinnistusraamat Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the lease is registered in the Land Register (Kinnistusraamat)",
            null),
        field(
            "kommunaalkuludAmount",
            "Kommunaalkulud Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly utility charges (kommunaalkulud)",
            "EUR"));
  }

  // --- GR (Greece) ---
  private List<FieldSchema> buildGrFields() {
    return List.of(
        field(
            "misthosisType",
            "M\u00edsthosis Type",
            ENUM,
            false,
            List.of(
                enumVal("RESIDENTIAL", "Residential"),
                enumVal("COMMERCIAL", "Commercial"),
                enumVal("PROFESSIONAL", "Professional")),
            null,
            "general",
            "Lease type under Greek Civil Code",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A+", "A+"),
                enumVal("A", "A"),
                enumVal("B+", "B+"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G"),
                enumVal("H", "H")),
            null,
            "energy",
            "\u03a0\u03b9\u03c3\u03c4\u03bf\u03c0\u03bf\u03b9\u03b7\u03c4\u03b9\u03ba\u03cc"
                + " \u0395\u03bd\u03b5\u03c1\u03b3\u03b5\u03b9\u03b1\u03ba\u03ae\u03c2"
                + " \u0391\u03c0\u03cc\u03b4\u03bf\u03c3\u03b7\u03c2 (PEA) rating",
            null),
        field(
            "eggysisAmount",
            "\u0395\u03b3\u03b3\u03cd\u03b7\u03c3\u03b7 Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (\u03b5\u03b3\u03b3\u03cd\u03b7\u03c3\u03b7) amount",
            "EUR"),
        field(
            "eggysisMonths",
            "\u0395\u03b3\u03b3\u03cd\u03b7\u03c3\u03b7 Months",
            INTEGER,
            false,
            null,
            validation(0, 2, null),
            "costs",
            "Deposit in months of rent (typically 2)",
            "months"),
        field(
            "enoikiostasiProtected",
            "Enoikiostasio Protected",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the lease is under rent control protection"
                + " (\u03b5\u03bd\u03bf\u03b9\u03ba\u03b9\u03bf\u03c3\u03c4\u03ac\u03c3\u03b9\u03bf)",
            null),
        field(
            "koinochristaAmount",
            "\u039a\u03bf\u03b9\u03bd\u03cc\u03c7\u03c1\u03b7\u03c3\u03c4\u03b1 Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common charges"
                + " (\u03ba\u03bf\u03b9\u03bd\u03cc\u03c7\u03c1\u03b7\u03c3\u03c4\u03b1)",
            "EUR"),
        field(
            "taxisRegistrationNumber",
            "TAXIS Registration Number",
            STRING,
            false,
            null,
            null,
            "general",
            "Lease registration number with TAXIS (tax authority)",
            null));
  }

  // --- MT (Malta) ---
  private List<FieldSchema> buildMtFields() {
    return List.of(
        field(
            "tenancyType",
            "Tenancy Type",
            ENUM,
            false,
            List.of(
                enumVal("PRIVATE", "Private Market"),
                enumVal("CONTROLLED", "Controlled (pre-1995)"),
                enumVal("SHORT_LET", "Short Let")),
            null,
            "tenancy",
            "Type of tenancy under Maltese rent laws",
            null),
        field(
            "epcRating",
            "EPC Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy Performance Certificate rating",
            null),
        field(
            "depositAmount",
            "Deposit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "EUR"),
        field(
            "depositMonths",
            "Deposit Months",
            INTEGER,
            false,
            null,
            validation(0, 2, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "housingAuthorityRegistered",
            "Housing Authority Registered",
            BOOLEAN,
            false,
            null,
            null,
            "tenancy",
            "Whether the lease is registered with the Housing Authority",
            null),
        field(
            "groundRent",
            "Ground Rent",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Annual ground rent payable",
            "EUR"),
        field(
            "rentalAgreementNumber",
            "Rental Agreement Number",
            STRING,
            false,
            null,
            null,
            "tenancy",
            "Official rental agreement registration number",
            null));
  }

  // --- CY (Cyprus) ---
  private List<FieldSchema> buildCyFields() {
    return List.of(
        field(
            "tenancyType",
            "Tenancy Type",
            ENUM,
            false,
            List.of(
                enumVal("STATUTORY", "Statutory Tenancy"), enumVal("CONTRACTUAL", "Contractual")),
            null,
            "tenancy",
            "Type of tenancy (statutory tenancies are rent-controlled)",
            null),
        field(
            "epcRating",
            "EPC Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy Performance Certificate rating",
            null),
        field(
            "depositAmount",
            "Deposit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "EUR"),
        field(
            "depositMonths",
            "Deposit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "rentTribunalEligible",
            "Rent Tribunal Eligible",
            BOOLEAN,
            false,
            null,
            null,
            "tenancy",
            "Whether the Rent Control Tribunal has jurisdiction",
            null),
        field(
            "commonExpensesAmount",
            "Common Expenses Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common area expenses",
            "EUR"),
        field(
            "municipalityRegistration",
            "Municipality Registration",
            STRING,
            false,
            null,
            null,
            "tenancy",
            "Municipal registration reference",
            null));
  }

  // --- LU (Luxembourg) ---
  private List<FieldSchema> buildLuFields() {
    return List.of(
        field(
            "bailType",
            "Bail Type",
            ENUM,
            false,
            List.of(
                enumVal("HABITATION", "Bail d'Habitation"),
                enumVal("COMMERCIAL", "Bail Commercial"),
                enumVal("MEUBLE", "Bail Meubl\u00e9")),
            null,
            "general",
            "Lease type under Luxembourg rental law",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            List.of(
                enumVal("A", "A"),
                enumVal("B", "B"),
                enumVal("C", "C"),
                enumVal("D", "D"),
                enumVal("E", "E"),
                enumVal("F", "F"),
                enumVal("G", "G"),
                enumVal("H", "H"),
                enumVal("I", "I")),
            null,
            "energy",
            "Certificat de performance \u00e9nerg\u00e9tique rating",
            null),
        field(
            "cautionAmount",
            "Caution Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (caution) amount",
            "EUR"),
        field(
            "cautionMonths",
            "Caution Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent (max 3)",
            "months"),
        field(
            "loyerMaxApplicable",
            "Loyer Max Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether rent cap regulation applies",
            null),
        field(
            "chargesAmount",
            "Charges Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly charges (charges locatives)",
            "EUR"),
        field(
            "registrationNumber",
            "Registration Number",
            STRING,
            false,
            null,
            null,
            "general",
            "Lease registration number",
            null));
  }

  // --- RS (Serbia) ---
  private List<FieldSchema> buildRsFields() {
    return List.of(
        field(
            "ugovorType",
            "Ugovor Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Serbian Law on Housing",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy certificate rating",
            null),
        field(
            "depozitAmount",
            "Depozit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "RSD"),
        field(
            "depozitMonths",
            "Depozit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "poreskaUpravaRegistered",
            "Poreska Uprava Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether the contract is registered with the Tax Administration",
            null),
        field(
            "komunalniTroskoviAmount",
            "Komunalni Tro\u0161kovi Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly utility charges",
            "RSD"));
  }

  // --- BA (Bosnia and Herzegovina) ---
  private List<FieldSchema> buildBaFields() {
    return List.of(
        field(
            "ugovorType",
            "Ugovor Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type",
            null),
        field(
            "entityRegion",
            "Entity/Region",
            ENUM,
            false,
            List.of(
                enumVal("FBH", "Federation of BiH"),
                enumVal("RS", "Republika Srpska"),
                enumVal("BD", "Br\u010dko District")),
            null,
            "general",
            "Entity or district where the property is located (different legal frameworks)",
            null),
        field(
            "depozitAmount",
            "Depozit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "BAM"),
        field(
            "depozitMonths",
            "Depozit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "poreskaUpravaRegistered",
            "Poreska Uprava Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with Tax Administration",
            null),
        field(
            "rezijeAmount",
            "Re\u017eije Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly utility charges (re\u017eije)",
            "BAM"));
  }

  // --- AL (Albania) ---
  private List<FieldSchema> buildAlFields() {
    return List.of(
        field(
            "kontrataTip",
            "Kontrata Tip",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Albanian Civil Code",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy certificate rating",
            null),
        field(
            "garanciaAmount",
            "Garancia Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (garanci) amount",
            "ALL"),
        field(
            "garanciaMonths",
            "Garancia Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "tatimoreRegistered",
            "Tatimore Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with Tax Authority (Drejtoria e Tatimeve)",
            null),
        field(
            "shpenzimet",
            "Shpenzimet",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common charges",
            "ALL"));
  }

  // --- ME (Montenegro) ---
  private List<FieldSchema> buildMeFields() {
    return List.of(
        field(
            "ugovorType",
            "Ugovor Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Montenegrin Obligations Act",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy certificate rating",
            null),
        field(
            "depozitAmount",
            "Depozit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "EUR"),
        field(
            "depozitMonths",
            "Depozit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "poreskaUpravaRegistered",
            "Poreska Uprava Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with Tax Administration",
            null),
        field(
            "komunalijeAmount",
            "Komunalije Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly utility charges",
            "EUR"));
  }

  // --- MK (North Macedonia) ---
  private List<FieldSchema> buildMkFields() {
    return List.of(
        field(
            "dogovorType",
            "Dogovor Type",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type under Macedonian Obligations Act",
            null),
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "energy",
            "Energy certificate rating",
            null),
        field(
            "depozitAmount",
            "Depozit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "MKD"),
        field(
            "depozitMonths",
            "Depozit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "ujpRegistered",
            "UJP Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with the Public Revenue Office (UJP)",
            null),
        field(
            "rezhiskiTroskoviAmount",
            "Re\u017eiski Tro\u0161kovi Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly overhead charges",
            "MKD"));
  }

  // --- XK (Kosovo) ---
  private List<FieldSchema> buildXkFields() {
    return List.of(
        field(
            "kontrataTip",
            "Kontrata Tip",
            ENUM,
            false,
            List.of(enumVal("DEFINITE", "Definite Term"), enumVal("INDEFINITE", "Indefinite")),
            null,
            "general",
            "Lease type",
            null),
        field(
            "depozitAmount",
            "Depozit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "EUR"),
        field(
            "depozitMonths",
            "Depozit Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "tatRegistered",
            "TAK Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with Kosovo Tax Administration (TAK)",
            null),
        field(
            "shpenzimetKomunale",
            "Shpenzimet Komunale",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly communal charges",
            "EUR"),
        field(
            "komunaRegistration",
            "Komuna Registration",
            STRING,
            false,
            null,
            null,
            "general",
            "Municipal registration reference",
            null));
  }

  // --- CA (Canada) ---
  private List<FieldSchema> buildCaFields() {
    return List.of(
        field(
            "province",
            "Province/Territory",
            ENUM,
            false,
            List.of(
                enumVal("AB", "Alberta"),
                enumVal("BC", "British Columbia"),
                enumVal("MB", "Manitoba"),
                enumVal("NB", "New Brunswick"),
                enumVal("NL", "Newfoundland and Labrador"),
                enumVal("NS", "Nova Scotia"),
                enumVal("NT", "Northwest Territories"),
                enumVal("NU", "Nunavut"),
                enumVal("ON", "Ontario"),
                enumVal("PE", "Prince Edward Island"),
                enumVal("QC", "Quebec"),
                enumVal("SK", "Saskatchewan"),
                enumVal("YT", "Yukon")),
            null,
            "location",
            "Province or territory where the property is located",
            null),
        field(
            "rentControlled",
            "Rent Controlled",
            BOOLEAN,
            false,
            null,
            null,
            "location",
            "Whether the property is subject to provincial rent control",
            null),
        field(
            "securityDepositAmount",
            "Security Deposit Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "deposit",
            "Security deposit amount",
            "CAD"),
        field(
            "securityDepositMonths",
            "Security Deposit Months",
            INTEGER,
            false,
            null,
            validation(0, 12, null),
            "deposit",
            "Deposit in months of rent",
            "months"),
        field(
            "tenancyBoardRegistered",
            "Tenancy Board Registered",
            BOOLEAN,
            false,
            null,
            null,
            "location",
            "Whether registered with provincial tenancy board (e.g. LTB Ontario)",
            null),
        field(
            "energyRating",
            "Energy Rating",
            STRING,
            false,
            null,
            null,
            "energy",
            "EnerGuide or provincial energy rating",
            null));
  }

  // --- MX (Mexico) ---
  private List<FieldSchema> buildMxFields() {
    return List.of(
        field(
            "estadoCode",
            "Estado",
            STRING,
            false,
            null,
            null,
            "general",
            "State code where the property is located",
            null),
        field(
            "contratoType",
            "Contrato Type",
            ENUM,
            false,
            List.of(
                enumVal("DEFINITE", "Tiempo Determinado"),
                enumVal("INDEFINITE", "Tiempo Indeterminado")),
            null,
            "general",
            "Lease type under Mexican Civil Code",
            null),
        field(
            "depositoAmount",
            "Dep\u00f3sito Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "MXN"),
        field(
            "depositoMonths",
            "Dep\u00f3sito Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "profecoRegistered",
            "PROFECO Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with consumer protection agency (PROFECO)",
            null),
        field(
            "mantenimientoAmount",
            "Mantenimiento Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly maintenance charges",
            "MXN"));
  }

  // --- BR (Brazil) ---
  private List<FieldSchema> buildBrFields() {
    return List.of(
        field(
            "tipoLocacao",
            "Tipo de Loca\u00e7\u00e3o",
            ENUM,
            false,
            List.of(
                enumVal("RESIDENCIAL", "Residencial"),
                enumVal("COMERCIAL", "Comercial"),
                enumVal("TEMPORADA", "Temporada")),
            null,
            "general",
            "Lease type under Lei do Inquilinato (Law 8.245/91)",
            null),
        field(
            "caucaoAmount",
            "Cau\u00e7\u00e3o Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit (cau\u00e7\u00e3o) amount",
            "BRL"),
        field(
            "caucaoMonths",
            "Cau\u00e7\u00e3o Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent (max 3 per Art. 38 Law 8.245/91)",
            "months"),
        field(
            "iptuIncluded",
            "IPTU Included",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether property tax (IPTU) is included in rent",
            null),
        field(
            "condominioAmount",
            "Condom\u00ednio Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly condominium fees",
            "BRL"),
        field(
            "registroImobiliario",
            "Registro Imobili\u00e1rio",
            STRING,
            false,
            null,
            null,
            "general",
            "Real estate registry number (matr\u00edcula do im\u00f3vel)",
            null),
        field(
            "seguroFianca",
            "Seguro Fian\u00e7a",
            BOOLEAN,
            false,
            null,
            null,
            "costs",
            "Whether a rental guarantee insurance (seguro fian\u00e7a) is used instead of cash"
                + " deposit",
            null));
  }

  // --- AR (Argentina) ---
  private List<FieldSchema> buildArFields() {
    return List.of(
        field(
            "tipoContrato",
            "Tipo de Contrato",
            ENUM,
            false,
            List.of(enumVal("HABITUAL", "Habitual"), enumVal("TEMPORARIO", "Temporario")),
            null,
            "general",
            "Lease type under Ley de Alquileres (27.551)",
            null),
        field(
            "depositoAmount",
            "Dep\u00f3sito Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "ARS"),
        field(
            "depositoMonths",
            "Dep\u00f3sito Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "registroPropiedad",
            "Registro Propiedad",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with the Property Registry",
            null),
        field(
            "expensasAmount",
            "Expensas Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common expenses (expensas)",
            "ARS"),
        field(
            "contratoInscripcion",
            "Contrato Inscripci\u00f3n",
            STRING,
            false,
            null,
            null,
            "general",
            "AFIP lease registration number",
            null),
        field(
            "actualizacionIpcApplicable",
            "Actualizaci\u00f3n IPC Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether annual IPC-based rent adjustment applies",
            null));
  }

  // --- CL (Chile) ---
  private List<FieldSchema> buildClFields() {
    return List.of(
        field(
            "tipoArriendo",
            "Tipo de Arriendo",
            ENUM,
            false,
            List.of(enumVal("HABITUAL", "Habitual"), enumVal("TEMPORAL", "Temporal")),
            null,
            "general",
            "Lease type under Chilean Civil Code",
            null),
        field(
            "garantiaAmount",
            "Garant\u00eda Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "CLP"),
        field(
            "garantiaMonths",
            "Garant\u00eda Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "siiRegistered",
            "SII Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with the Internal Revenue Service (SII)",
            null),
        field(
            "gastosComunes",
            "Gastos Comunes",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common expenses",
            "CLP"),
        field(
            "rolPropiedad",
            "Rol Propiedad",
            STRING,
            false,
            null,
            null,
            "general",
            "Property ROL number (tax identifier)",
            null),
        field(
            "reajusteIpcApplicable",
            "Reajuste IPC Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether IPC-based rent adjustment applies",
            null));
  }

  // --- CO (Colombia) ---
  private List<FieldSchema> buildCoFields() {
    return List.of(
        field(
            "tipoContrato",
            "Tipo de Contrato",
            ENUM,
            false,
            List.of(
                enumVal("VIVIENDA_URBANA", "Vivienda Urbana"), enumVal("COMERCIAL", "Comercial")),
            null,
            "general",
            "Lease type under Ley 820 de 2003",
            null),
        field(
            "depositoAmount",
            "Dep\u00f3sito Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "COP"),
        field(
            "depositoMonths",
            "Dep\u00f3sito Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "registraduriaInscribed",
            "Registradur\u00eda Inscribed",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with the Superintendencia de Notariado y Registro",
            null),
        field(
            "administracionAmount",
            "Administraci\u00f3n Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly administration fee",
            "COP"),
        field(
            "matriculaInmobiliaria",
            "Matr\u00edcula Inmobiliaria",
            STRING,
            false,
            null,
            null,
            "general",
            "Real estate registration number (matr\u00edcula inmobiliaria)",
            null),
        field(
            "estratoApplicable",
            "Estrato Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether socioeconomic stratification (estrato) applies to utility rates",
            null));
  }

  // --- PE (Peru) ---
  private List<FieldSchema> buildPeFields() {
    return List.of(
        field(
            "tipoContrato",
            "Tipo de Contrato",
            ENUM,
            false,
            List.of(
                enumVal("DEFINIDO", "Plazo Determinado"),
                enumVal("INDEFINIDO", "Plazo Indeterminado")),
            null,
            "general",
            "Lease type under Peruvian Civil Code (Art. 1666\u20131712)",
            null),
        field(
            "garantiaAmount",
            "Garant\u00eda Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "PEN"),
        field(
            "garantiaMonths",
            "Garant\u00eda Months",
            INTEGER,
            false,
            null,
            validation(0, 3, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "sunarpRegistered",
            "SUNARP Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with SUNARP (public records)",
            null),
        field(
            "mantenimientoAmount",
            "Mantenimiento Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly maintenance charges",
            "PEN"),
        field(
            "partidaRegistral",
            "Partida Registral",
            STRING,
            false,
            null,
            null,
            "general",
            "Property registration number in SUNARP",
            null));
  }

  // --- UY (Uruguay) ---
  private List<FieldSchema> buildUyFields() {
    return List.of(
        field(
            "garantiaType",
            "Garant\u00eda Type",
            ENUM,
            false,
            List.of(
                enumVal("DEPOSITO", "Dep\u00f3sito"),
                enumVal("BHU", "BHU (Banco Hipotecario)"),
                enumVal("PORTO", "Porto Seguro"),
                enumVal("ANDA", "ANDA"),
                enumVal("CONTADURIA", "Contadur\u00eda General de la Naci\u00f3n")),
            null,
            "general",
            "Type of rental guarantee required by Ley 18.795",
            null),
        field(
            "tipoContrato",
            "Tipo de Contrato",
            ENUM,
            false,
            List.of(enumVal("HABITUAL", "Habitual"), enumVal("TEMPORARIO", "Temporario")),
            null,
            "general",
            "Lease type",
            null),
        field(
            "depositoAmount",
            "Dep\u00f3sito Amount",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Security deposit amount",
            "UYU"),
        field(
            "depositoMonths",
            "Dep\u00f3sito Months",
            INTEGER,
            false,
            null,
            validation(0, 5, null),
            "costs",
            "Deposit in months of rent",
            "months"),
        field(
            "dgiRegistered",
            "DGI Registered",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether registered with the Tax Authority (DGI)",
            null),
        field(
            "gastosComunes",
            "Gastos Comunes",
            DECIMAL,
            false,
            null,
            validation(0, null, null),
            "costs",
            "Monthly common expenses",
            "UYU"),
        field(
            "padronNumero",
            "Padr\u00f3n N\u00famero",
            STRING,
            false,
            null,
            null,
            "general",
            "Property cadastral number (padr\u00f3n)",
            null));
  }

  // --- Generic ---
  private List<FieldSchema> buildGenericFields() {
    return List.of(
        field(
            "energyCertificateRating",
            "Energy Certificate Rating",
            ENUM,
            false,
            ENERGY_RATINGS,
            null,
            "general",
            "Energy Performance Certificate rating",
            null),
        field(
            "energyCertificateNumber",
            "Energy Certificate Number",
            STRING,
            false,
            null,
            null,
            "general",
            "Official energy certificate reference number",
            null),
        field(
            "contractRegistrationNumber",
            "Contract Registration Number",
            STRING,
            false,
            null,
            null,
            "general",
            "Official lease registration or recording number",
            null),
        field(
            "maxDepositMonths",
            "Max Deposit (Months)",
            DECIMAL,
            false,
            null,
            null,
            "general",
            "Maximum security deposit in months of rent",
            "months"),
        field(
            "rentIndexationApplicable",
            "Rent Indexation Applicable",
            BOOLEAN,
            false,
            null,
            null,
            "general",
            "Whether rent can be adjusted based on an official index",
            null),
        field(
            "notes",
            "Notes",
            STRING,
            false,
            null,
            null,
            "general",
            "Additional notes about country-specific requirements",
            null));
  }

  private List<GroupSchema> buildGenericGroups() {
    return List.of(group("general", "General Information"));
  }

  // --- Helpers ---

  private static FieldSchema field(
      String name,
      String label,
      FieldType type,
      boolean required,
      @Nullable List<EnumValue> enumValues,
      @Nullable ValidationSchema validation,
      String group) {
    return field(name, label, type, required, enumValues, validation, group, null, null);
  }

  private static FieldSchema field(
      String name,
      String label,
      FieldType type,
      boolean required,
      @Nullable List<EnumValue> enumValues,
      @Nullable ValidationSchema validation,
      String group,
      @Nullable String helpText,
      @Nullable String unit) {
    return new FieldSchema(
        name,
        label,
        type,
        required,
        enumValues != null ? enumValues : List.of(),
        validation != null ? validation : EMPTY_VALIDATION,
        group,
        Optional.ofNullable(helpText),
        Optional.ofNullable(unit));
  }

  private static EnumValue enumVal(String value, String label) {
    return new EnumValue(value, label);
  }

  private static ValidationSchema validation(
      @Nullable Integer min, @Nullable Integer max, @Nullable String pattern) {
    return new ValidationSchema(
        Optional.ofNullable(min), Optional.ofNullable(max), Optional.ofNullable(pattern));
  }

  private static GroupSchema group(String key, String label) {
    return new GroupSchema(key, label);
  }
}
