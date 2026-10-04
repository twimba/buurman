package com.buurman.service.backoffice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.RentFrequency;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.RentRegulationTenancyRule;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.CatalogCountry;
import com.buurman.domain.regulation.CatalogLateFee;
import com.buurman.domain.regulation.CatalogRegion;
import com.buurman.domain.regulation.CatalogRule;
import com.buurman.domain.regulation.CatalogTenancyRule;
import com.buurman.domain.regulation.RentRegulationCatalog;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.dto.response.RentRegulationCatalogDiff;
import com.buurman.dto.response.RentRegulationCatalogInfo;
import com.buurman.dto.response.RentRegulationCountryDiff;
import com.buurman.dto.response.RentRegulationDiffCounts;
import com.buurman.dto.response.RentRegulationDiffEntry;
import com.buurman.dto.response.RentRegulationDiffField;
import com.buurman.dto.response.RentRegulationReloadResult;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.TerminationNoticeRuleRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.util.SidGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reloads, inspects and exports the rent-regulation reference dataset against the canonical catalog
 * bundled with the application.
 *
 * <p>Reload is destructive: it wipes every country, region and rule and re-seeds them from the
 * bundled file, discarding any manual edits made through the other backoffice endpoints. The
 * dataset is a curated Buurman asset; the bundled file is its source of truth.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RentRegulationCatalogService {

  private final RentRegulationRepository repository;
  private final TerminationNoticeRuleRepository terminationRuleRepository;
  private final RentRegulationCatalogLoader loader;
  private final Clock clock;

  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationCatalogInfo catalogInfo() {
    RentRegulationCatalog catalog = loader.load();
    return new RentRegulationCatalogInfo(
        catalog.version(),
        catalog.generatedAt(),
        catalog.description(),
        catalog.countryCount(),
        catalog.regionCount(),
        catalog.ruleCount());
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationReloadResult reload(BackofficePrincipal principal) {
    RentRegulationCatalog catalog = loader.load();
    String actor = principal.getEmail().orElseGet(principal::getName);

    // rent_regulation_termination_rules has no representation in the bundled catalog (it is only
    // ever seeded by a one-off Flyway migration), so it must be snapshotted here — before the
    // wipe below — and reinserted afterwards, or a reload permanently deletes every
    // country's termination-notice rules.
    List<TerminationRuleSeed> terminationSeeds = captureTerminationRuleSeeds();

    repository.deleteAllReferenceData();

    int countries = 0;
    int regions = 0;
    int rules = 0;
    int tenancyRules = 0;

    Map<String, UUID> countryIdsByCode = new HashMap<>();
    Map<String, Map<String, UUID>> regionIdsByCountryCode = new HashMap<>();

    for (CatalogCountry country : safe(catalog.countries())) {
      UUID countryId = insertCountry(country, actor);
      countryIdsByCode.put(country.countryCode(), countryId);
      countries++;

      Map<String, UUID> regionIds = new HashMap<>();
      for (CatalogRegion region : safe(country.regions())) {
        UUID regionId = insertRegion(region, countryId, actor);
        regionIds.put(region.regionCode(), regionId);
        regions++;
      }
      regionIdsByCountryCode.put(country.countryCode(), regionIds);

      for (CatalogRule rule : safe(country.rules())) {
        insertRule(rule, countryId, regionIds, actor);
        rules++;
      }

      for (CatalogTenancyRule tenancyRule : safe(country.tenancyRules())) {
        insertTenancyRule(tenancyRule, countryId, regionIds, actor);
        tenancyRules++;
      }
    }

    int terminationRules =
        reinsertTerminationRules(terminationSeeds, countryIdsByCode, regionIdsByCountryCode);

    log.warn(
        "Backoffice user {} reloaded rent-regulation catalog v{} ({} countries, {} regions, {}"
            + " rules, {} tenancy rules, {} termination rules) — previous reference data"
            + " discarded",
        actor,
        catalog.version(),
        countries,
        regions,
        rules,
        tenancyRules,
        terminationRules);

    return new RentRegulationReloadResult(
        catalog.version(), catalog.generatedAt(), countries, regions, rules, tenancyRules);
  }

  /**
   * A termination rule snapshotted by country/region CODE rather than internal UUID, since
   * countries and regions are deleted and re-inserted with fresh random ids on every reload.
   */
  private record TerminationRuleSeed(
      @org.jspecify.annotations.Nullable String countryCode,
      @org.jspecify.annotations.Nullable String regionCode,
      TerminationGivenBy partyType,
      @org.jspecify.annotations.Nullable Integer minTenancyMonths,
      int noticeDays,
      boolean groundsRequired,
      List<String> groundsCodes,
      @org.jspecify.annotations.Nullable String sourceUrl,
      @org.jspecify.annotations.Nullable String notes) {}

  private List<TerminationRuleSeed> captureTerminationRuleSeeds() {
    Map<UUID, String> countryCodeById =
        repository.findAllCountries().stream()
            .collect(
                Collectors.toMap(
                    RentRegulationCountry::getId, RentRegulationCountry::getCountryCode));
    Map<UUID, String> regionCodeById =
        repository.findAllRegions().stream()
            .collect(
                Collectors.toMap(RentRegulationRegion::getId, RentRegulationRegion::getRegionCode));

    return terminationRuleRepository.findAll().stream()
        .map(
            rule ->
                new TerminationRuleSeed(
                    countryCodeById.get(rule.getCountryId()),
                    rule.getRegionId().map(regionCodeById::get).orElse(null),
                    rule.getPartyType(),
                    rule.getMinTenancyMonths().orElse(null),
                    rule.getNoticeDays(),
                    rule.isGroundsRequired(),
                    rule.getGroundsCodes(),
                    rule.getSourceUrl().orElse(null),
                    rule.getNotes().orElse(null)))
        .filter(seed -> seed.countryCode() != null)
        .toList();
  }

  private int reinsertTerminationRules(
      List<TerminationRuleSeed> seeds,
      Map<String, UUID> countryIdsByCode,
      Map<String, Map<String, UUID>> regionIdsByCountryCode) {
    int count = 0;
    for (TerminationRuleSeed seed : seeds) {
      UUID countryId = countryIdsByCode.get(seed.countryCode());
      if (countryId == null) {
        log.warn(
            "Dropping termination rule for country '{}' — no longer present in reloaded catalog",
            seed.countryCode());
        continue;
      }
      // A plain Optional.ofNullable(seed.regionCode()).map(lookup) would silently collapse to
      // empty when the lookup itself returns null — indistinguishable from seed.regionCode()
      // having been null to begin with. That would make a region that vanished from the reloaded
      // catalog look country-wide (no regionId) instead of being dropped, silently broadening a
      // region-specific legal notice-period rule to the whole country. Looked up and checked
      // explicitly instead, mirroring the country-miss branch above.
      Optional<UUID> regionId = Optional.empty();
      if (seed.regionCode() != null) {
        UUID resolvedRegionId =
            regionIdsByCountryCode
                .getOrDefault(seed.countryCode(), Map.of())
                .get(seed.regionCode());
        if (resolvedRegionId == null) {
          log.warn(
              "Dropping termination rule for country '{}' region '{}' — region no longer present"
                  + " in reloaded catalog",
              seed.countryCode(),
              seed.regionCode());
          continue;
        }
        regionId = Optional.of(resolvedRegionId);
      }

      terminationRuleRepository.save(
          TerminationNoticeRule.builder()
              .countryId(countryId)
              .regionId(regionId)
              .partyType(seed.partyType())
              .minTenancyMonths(Optional.ofNullable(seed.minTenancyMonths()))
              .noticeDays(seed.noticeDays())
              .groundsRequired(seed.groundsRequired())
              .groundsCodes(seed.groundsCodes())
              .sourceUrl(Optional.ofNullable(seed.sourceUrl()))
              .notes(Optional.ofNullable(seed.notes()))
              .build());
      count++;
    }
    return count;
  }

  /**
   * Serialises the current database reference data into the canonical catalog shape. This is the
   * maintainer tool used to regenerate the bundled JSON file after edits made through the
   * backoffice UI: export, review, commit.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationCatalog exportCurrent() {
    RentRegulationCatalog bundled = loader.load();
    return new RentRegulationCatalog(
        bundled.version(),
        LocalDate.now(clock).toString(),
        bundled.description(),
        currentCountries());
  }

  /** Reads the current database reference data into the catalog (CatalogCountry) shape. */
  private List<CatalogCountry> currentCountries() {
    List<RentRegulationCountry> countries = repository.findAllCountries();
    List<RentRegulationRegion> allRegions = repository.findAllRegions();
    Map<UUID, List<RentRegulationRegion>> regionsByCountry =
        allRegions.stream().collect(Collectors.groupingBy(RentRegulationRegion::getCountryId));
    Map<UUID, String> regionCodeById =
        allRegions.stream()
            .collect(
                Collectors.toMap(RentRegulationRegion::getId, RentRegulationRegion::getRegionCode));
    Map<UUID, List<RentRegulationRule>> rulesByCountry =
        repository.findAllRules().stream()
            .collect(Collectors.groupingBy(RentRegulationRule::getCountryId));
    Map<UUID, List<RentRegulationTenancyRule>> tenancyByCountry =
        repository.findAllTenancyRules().stream()
            .collect(Collectors.groupingBy(RentRegulationTenancyRule::getCountryId));

    return countries.stream()
        .sorted(Comparator.comparing(RentRegulationCountry::getCountryCode))
        .map(
            c ->
                toCatalogCountry(
                    c,
                    regionsByCountry.getOrDefault(c.getId(), List.of()),
                    rulesByCountry.getOrDefault(c.getId(), List.of()),
                    tenancyByCountry.getOrDefault(c.getId(), List.of()),
                    regionCodeById))
        .toList();
  }

  /**
   * Computes the difference between the current database reference data and the bundled catalog: a
   * preview of exactly what a reload would add, remove and change. Read-only — applies nothing.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationCatalogDiff diff() {
    RentRegulationCatalog bundled = loader.load();
    Map<String, CatalogCountry> target = byCode(bundled.countries());
    Map<String, CatalogCountry> current = byCode(currentCountries());

    List<RentRegulationCountryDiff> byCountry = new ArrayList<>();
    int cAdded = 0;
    int cRemoved = 0;
    int cChanged = 0;
    int[] rAgg = new int[3]; // regions added/removed/changed
    int[] ruAgg = new int[3]; // rules added/removed/changed

    List<String> codes =
        Stream.concat(target.keySet().stream(), current.keySet().stream())
            .distinct()
            .sorted()
            .toList();

    for (String code : codes) {
      CatalogCountry t = target.get(code);
      CatalogCountry c = current.get(code);

      if (t != null && c != null) {
        // Present in both — diff country fields, regions and rules.
        List<RentRegulationDiffEntry> entries = new ArrayList<>();
        List<RentRegulationDiffField> countryFields = countryFieldDiffs(c, t);
        if (!countryFields.isEmpty()) {
          entries.add(
              new RentRegulationDiffEntry("COUNTRY", "CHANGED", t.countryName(), countryFields));
        }
        int[] regionCounts = diffRegions(c, t, entries);
        int[] ruleCounts = diffRules(c, t, entries);

        rAgg[0] += regionCounts[0];
        rAgg[1] += regionCounts[1];
        rAgg[2] += regionCounts[2];
        ruAgg[0] += ruleCounts[0];
        ruAgg[1] += ruleCounts[1];
        ruAgg[2] += ruleCounts[2];

        if (!entries.isEmpty()) {
          cChanged++;
          byCountry.add(
              new RentRegulationCountryDiff(
                  code,
                  t.countryName(),
                  "MODIFIED",
                  new RentRegulationDiffCounts(ruleCounts[0], ruleCounts[1], ruleCounts[2]),
                  entries));
        }
      } else if (t != null) {
        cAdded++;
        int regions = safe(t.regions()).size();
        int rules = safe(t.rules()).size();
        int tenancyRules = safe(t.tenancyRules()).size();
        rAgg[0] += regions;
        ruAgg[0] += rules;
        byCountry.add(
            new RentRegulationCountryDiff(
                code,
                t.countryName(),
                "ADDED",
                new RentRegulationDiffCounts(rules, 0, 0),
                List.of(
                    new RentRegulationDiffEntry(
                        "COUNTRY",
                        "ADDED",
                        t.countryName()
                            + " — "
                            + rules
                            + " rules, "
                            + tenancyRules
                            + " tenancy rules, "
                            + regions
                            + " regions",
                        List.of()))));
      } else if (c != null) {
        cRemoved++;
        int regions = safe(c.regions()).size();
        int rules = safe(c.rules()).size();
        rAgg[1] += regions;
        ruAgg[1] += rules;
        byCountry.add(
            new RentRegulationCountryDiff(
                code,
                c.countryName(),
                "REMOVED",
                new RentRegulationDiffCounts(0, rules, 0),
                List.of(
                    new RentRegulationDiffEntry(
                        "COUNTRY",
                        "REMOVED",
                        c.countryName() + " — " + rules + " rules, " + regions + " regions",
                        List.of()))));
      }
    }

    return new RentRegulationCatalogDiff(
        bundled.version(),
        bundled.generatedAt(),
        new RentRegulationDiffCounts(cAdded, cRemoved, cChanged),
        new RentRegulationDiffCounts(rAgg[0], rAgg[1], rAgg[2]),
        new RentRegulationDiffCounts(ruAgg[0], ruAgg[1], ruAgg[2]),
        byCountry);
  }

  private static Map<String, CatalogCountry> byCode(List<CatalogCountry> countries) {
    Map<String, CatalogCountry> map = new LinkedHashMap<>();
    for (CatalogCountry c : safe(countries)) {
      map.put(c.countryCode(), c);
    }
    return map;
  }

  private List<RentRegulationDiffField> countryFieldDiffs(CatalogCountry cur, CatalogCountry tgt) {
    List<RentRegulationDiffField> fields = new ArrayList<>();
    addFieldDiff(fields, "countryName", cur.countryName(), tgt.countryName());
    addFieldDiff(
        fields,
        "hasRegionalRegulations",
        String.valueOf(cur.hasRegionalRegulations()),
        String.valueOf(tgt.hasRegionalRegulations()));
    addFieldDiff(fields, "summary", cur.summary(), tgt.summary());
    addFieldDiff(
        fields,
        "lateFeePolicy",
        Optional.ofNullable(cur.lateFee()).map(l -> nv(l.policy())).orElse(""),
        Optional.ofNullable(tgt.lateFee()).map(l -> nv(l.policy())).orElse(""));
    addFieldDiff(
        fields,
        "lateFeeMaxPercentage",
        Optional.ofNullable(cur.lateFee()).map(l -> nv(l.maxPercentage())).orElse(""),
        Optional.ofNullable(tgt.lateFee()).map(l -> nv(l.maxPercentage())).orElse(""));
    addFieldDiff(
        fields,
        "lateFeeNotes",
        Optional.ofNullable(cur.lateFee()).map(l -> nv(l.notes())).orElse(""),
        Optional.ofNullable(tgt.lateFee()).map(l -> nv(l.notes())).orElse(""));
    addFieldDiff(
        fields, "formalNoticeDays", nv(cur.formalNoticeDays()), nv(tgt.formalNoticeDays()));

    Map<String, CatalogTenancyRule> curTenancy = new LinkedHashMap<>();
    for (CatalogTenancyRule t : safe(cur.tenancyRules())) {
      curTenancy.put(tenancyRuleKey(t), t);
    }
    Map<String, CatalogTenancyRule> tgtTenancy = new LinkedHashMap<>();
    for (CatalogTenancyRule t : safe(tgt.tenancyRules())) {
      tgtTenancy.put(tenancyRuleKey(t), t);
    }
    Set<String> keys = new LinkedHashSet<>(curTenancy.keySet());
    keys.addAll(tgtTenancy.keySet());
    for (String key : keys) {
      addFieldDiff(
          fields,
          "tenancyRule[" + key + "]",
          Optional.ofNullable(curTenancy.get(key)).map(this::describeTenancyRule).orElse(""),
          Optional.ofNullable(tgtTenancy.get(key)).map(this::describeTenancyRule).orElse(""));
    }

    return fields;
  }

  /**
   * Identity of a tenancy rule for diffing purposes: topic + label, scoped by region when the rule
   * is region-specific. Two rules that share a topic and label but apply to different regions (e.g.
   * a {@code REGISTRATION} / "Rent registry filing" rule for both {@code US-DC} and {@code US-NY})
   * must never collide on the same key, or one silently disappears from the reload preview.
   *
   * <p>National rules (no region) keep the plain {@code TOPIC/label} form; region-scoped rules
   * insert the region code between topic and label, e.g. {@code REGISTRATION/US-DC/Rent registry
   * filing}, so the diff output reads sensibly either way.
   */
  private static String tenancyRuleKey(CatalogTenancyRule rule) {
    return Optional.ofNullable(rule.regionCode())
        .map(region -> rule.topic().name() + "/" + region + "/" + rule.label())
        .orElseGet(() -> rule.topic().name() + "/" + rule.label());
  }

  private String describeTenancyRule(CatalogTenancyRule rule) {
    StringBuilder sb = new StringBuilder(rule.value());
    Optional.ofNullable(rule.regionCode())
        .ifPresent(r -> sb.append(" (region ").append(r).append(")"));
    Optional.ofNullable(rule.effectiveFrom())
        .ifPresent(d -> sb.append(" (from ").append(d).append(")"));
    Optional.ofNullable(rule.legalBasis()).ifPresent(b -> sb.append(" [").append(b).append("]"));
    Optional.ofNullable(rule.sourceUrl()).ifPresent(u -> sb.append(" <").append(u).append(">"));
    Optional.ofNullable(rule.notes()).ifPresent(n -> sb.append(" — ").append(n));
    return sb.toString();
  }

  private int[] diffRegions(
      CatalogCountry cur, CatalogCountry tgt, List<RentRegulationDiffEntry> entries) {
    Map<String, CatalogRegion> c = new LinkedHashMap<>();
    for (CatalogRegion r : safe(cur.regions())) {
      c.put(r.regionCode(), r);
    }
    Map<String, CatalogRegion> t = new LinkedHashMap<>();
    for (CatalogRegion r : safe(tgt.regions())) {
      t.put(r.regionCode(), r);
    }
    int added = 0;
    int removed = 0;
    int changed = 0;
    for (String key :
        Stream.concat(t.keySet().stream(), c.keySet().stream()).distinct().sorted().toList()) {
      CatalogRegion tr = t.get(key);
      CatalogRegion cr = c.get(key);
      if (tr != null && cr != null) {
        List<RentRegulationDiffField> fields = new ArrayList<>();
        addFieldDiff(fields, "regionName", cr.regionName(), tr.regionName());
        addFieldDiff(fields, "summary", cr.summary(), tr.summary());
        if (!fields.isEmpty()) {
          changed++;
          entries.add(new RentRegulationDiffEntry("REGION", "CHANGED", regionLabel(tr), fields));
        }
      } else if (tr != null) {
        added++;
        entries.add(new RentRegulationDiffEntry("REGION", "ADDED", regionLabel(tr), List.of()));
      } else if (cr != null) {
        removed++;
        entries.add(new RentRegulationDiffEntry("REGION", "REMOVED", regionLabel(cr), List.of()));
      }
    }
    return new int[] {added, removed, changed};
  }

  private int[] diffRules(
      CatalogCountry cur, CatalogCountry tgt, List<RentRegulationDiffEntry> entries) {
    Map<String, CatalogRule> c = new LinkedHashMap<>();
    for (CatalogRule r : safe(cur.rules())) {
      c.put(ruleKey(r), r);
    }
    Map<String, CatalogRule> t = new LinkedHashMap<>();
    for (CatalogRule r : safe(tgt.rules())) {
      t.put(ruleKey(r), r);
    }
    int added = 0;
    int removed = 0;
    int changed = 0;
    for (String key :
        Stream.concat(t.keySet().stream(), c.keySet().stream()).distinct().sorted().toList()) {
      CatalogRule tr = t.get(key);
      CatalogRule cr = c.get(key);
      if (tr != null && cr != null) {
        List<RentRegulationDiffField> fields = ruleValueDiffs(cr, tr);
        if (!fields.isEmpty()) {
          changed++;
          entries.add(new RentRegulationDiffEntry("RULE", "CHANGED", ruleLabel(tr), fields));
        }
      } else if (tr != null) {
        added++;
        entries.add(
            new RentRegulationDiffEntry("RULE", "ADDED", ruleLabel(tr), ruleValueFields(tr)));
      } else if (cr != null) {
        removed++;
        entries.add(
            new RentRegulationDiffEntry("RULE", "REMOVED", ruleLabel(cr), ruleValueFields(cr)));
      }
    }
    return new int[] {added, removed, changed};
  }

  /** Identity of a rule = its scope (everything that decides which tenancy it applies to). */
  private static String ruleKey(CatalogRule r) {
    return String.join(
        "|",
        nv(r.regionCode()),
        String.valueOf(r.year()),
        nv(r.propertyCategory()),
        nv(r.regime()),
        nv(r.propertyType()),
        nv(r.contractType()),
        nv(r.taxRegime()),
        nv(r.tenancyPhase()),
        nv(r.buildYearMin()),
        nv(r.buildYearMax()),
        nv(r.epcClassMin()),
        nv(r.epcClassMax()),
        nv(r.contractSignedAfter()),
        nv(r.contractSignedBefore()),
        nv(r.landlordMinProperties()),
        nv(r.areaCode()),
        nv(r.effectiveDate()));
  }

  private static String ruleLabel(CatalogRule r) {
    StringBuilder sb = new StringBuilder().append(r.year());
    String scope = r.regime() != null ? r.regime() : r.propertyCategory();
    if (scope != null) {
      sb.append(" · ").append(scope);
    }
    sb.append(" · ").append(r.regionCode() != null ? "region " + r.regionCode() : "national");
    if (r.areaCode() != null) {
      sb.append(" · ").append(r.areaCode());
    }
    if (r.contractType() != null) {
      sb.append(" · ").append(r.contractType());
    }
    return sb.toString();
  }

  private static String regionLabel(CatalogRegion r) {
    return r.regionCode() + " — " + r.regionName();
  }

  /** Value fields of a rule (the things a CHANGED entry reports). */
  private static List<RentRegulationDiffField> ruleValueDiffs(CatalogRule cur, CatalogRule tgt) {
    List<RentRegulationDiffField> fields = new ArrayList<>();
    addNumberDiff(
        fields, "maxIncreasePercentage", cur.maxIncreasePercentage(), tgt.maxIncreasePercentage());
    addFieldDiff(fields, "maxIncreaseType", nv(cur.maxIncreaseType()), nv(tgt.maxIncreaseType()));
    addFieldDiff(fields, "indexName", cur.indexName(), tgt.indexName());
    addNumberDiff(fields, "indexValue", cur.indexValue(), tgt.indexValue());
    addFieldDiff(
        fields, "noticePeriodDays", nv(cur.noticePeriodDays()), nv(tgt.noticePeriodDays()));
    addFieldDiff(fields, "frequency", nv(cur.frequency()), nv(tgt.frequency()));
    addFieldDiff(
        fields, "additionalConditions", cur.additionalConditions(), tgt.additionalConditions());
    addFieldDiff(fields, "sourceUrl", cur.sourceUrl(), tgt.sourceUrl());
    addFieldDiff(fields, "notes", cur.notes(), tgt.notes());
    return fields;
  }

  /** A compact "current value" snapshot for an added/removed rule. */
  private static List<RentRegulationDiffField> ruleValueFields(CatalogRule r) {
    List<RentRegulationDiffField> fields = new ArrayList<>();
    fields.add(new RentRegulationDiffField("maxIncrease", "", nv(r.maxIncreasePercentage())));
    fields.add(new RentRegulationDiffField("type", "", nv(r.maxIncreaseType())));
    if (r.sourceUrl() != null) {
      fields.add(new RentRegulationDiffField("source", "", r.sourceUrl()));
    }
    return fields;
  }

  private static void addFieldDiff(
      List<RentRegulationDiffField> fields, String name, String before, String after) {
    String b = before == null ? "" : before;
    String a = after == null ? "" : after;
    if (!b.equals(a)) {
      fields.add(new RentRegulationDiffField(name, b, a));
    }
  }

  /**
   * Compares two decimals by numeric value, not text — {@code 2.00} (DB scale) and {@code 2.0}
   * (catalog) are the same value and must not register as a change.
   */
  private static void addNumberDiff(
      List<RentRegulationDiffField> fields, String name, BigDecimal before, BigDecimal after) {
    boolean equal =
        (before == null && after == null)
            || (before != null && after != null && before.compareTo(after) == 0);
    if (!equal) {
      fields.add(
          new RentRegulationDiffField(
              name,
              before == null ? "" : before.toPlainString(),
              after == null ? "" : after.toPlainString()));
    }
  }

  private static String nv(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  // ==================== Catalog → domain (insert) ====================

  private UUID insertCountry(CatalogCountry country, String actor) {
    RentRegulationCountry domain =
        RentRegulationCountry.builder()
            .identifier(Optional.of(SidGenerator.newRentRegulationCountryId()))
            .countryCode(country.countryCode())
            .countryName(country.countryName())
            .hasRegionalRegulations(country.hasRegionalRegulations())
            .summary(Optional.ofNullable(country.summary()))
            .lastReviewedAt(Optional.ofNullable(country.lastReviewedAt()).map(Instant::parse))
            .lateFeePolicy(
                Optional.ofNullable(country.lateFee())
                    .map(CatalogLateFee::policy)
                    .orElse(LateFeePolicy.UNKNOWN))
            .lateFeeMaxPercentage(
                Optional.ofNullable(country.lateFee()).map(CatalogLateFee::maxPercentage))
            .lateFeeNotes(Optional.ofNullable(country.lateFee()).map(CatalogLateFee::notes))
            .formalNoticeDays(Optional.ofNullable(country.formalNoticeDays()))
            .createdBy(Optional.of(actor))
            .updatedBy(Optional.of(actor))
            .build();
    return repository.saveCountry(domain).getId();
  }

  private UUID insertRegion(CatalogRegion region, UUID countryId, String actor) {
    RentRegulationRegion domain =
        RentRegulationRegion.builder()
            .identifier(Optional.of(SidGenerator.newRentRegulationRegionId()))
            .countryId(countryId)
            .regionCode(region.regionCode())
            .regionName(region.regionName())
            .summary(Optional.ofNullable(region.summary()))
            .createdBy(Optional.of(actor))
            .updatedBy(Optional.of(actor))
            .build();
    return repository.saveRegion(domain).getId();
  }

  private void insertRule(
      CatalogRule rule, UUID countryId, Map<String, UUID> regionIds, String actor) {
    Optional<UUID> regionId =
        Optional.ofNullable(rule.regionCode())
            .map(
                code ->
                    Optional.ofNullable(regionIds.get(code))
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Catalog rule references unknown region '"
                                        + code
                                        + "' in country "
                                        + countryId)));

    RentRegulationRule domain =
        RentRegulationRule.builder()
            .identifier(Optional.of(SidGenerator.newRentRegulationRuleId()))
            .countryId(countryId)
            .regionId(regionId)
            .year(rule.year())
            .propertyCategory(rule.propertyCategory())
            .maxIncreasePercentage(Optional.ofNullable(rule.maxIncreasePercentage()))
            .maxIncreaseType(rule.maxIncreaseType())
            .indexName(Optional.ofNullable(rule.indexName()))
            .indexValue(Optional.ofNullable(rule.indexValue()))
            .effectiveDate(Optional.ofNullable(rule.effectiveDate()).map(LocalDate::parse))
            .noticePeriodDays(Optional.ofNullable(rule.noticePeriodDays()))
            .frequency(Optional.ofNullable(rule.frequency()).orElse(RentFrequency.ANNUAL))
            .additionalConditions(Optional.ofNullable(rule.additionalConditions()))
            .sourceUrl(Optional.ofNullable(rule.sourceUrl()))
            .notes(Optional.ofNullable(rule.notes()))
            .regime(Optional.ofNullable(rule.regime()))
            .propertyType(Optional.ofNullable(rule.propertyType()))
            .contractType(Optional.ofNullable(rule.contractType()))
            .taxRegime(Optional.ofNullable(rule.taxRegime()))
            .tenancyPhase(Optional.ofNullable(rule.tenancyPhase()))
            .buildYearMin(Optional.ofNullable(rule.buildYearMin()))
            .buildYearMax(Optional.ofNullable(rule.buildYearMax()))
            .epcClassMin(Optional.ofNullable(rule.epcClassMin()))
            .epcClassMax(Optional.ofNullable(rule.epcClassMax()))
            .contractSignedAfter(
                Optional.ofNullable(rule.contractSignedAfter()).map(LocalDate::parse))
            .contractSignedBefore(
                Optional.ofNullable(rule.contractSignedBefore()).map(LocalDate::parse))
            .landlordMinProperties(Optional.ofNullable(rule.landlordMinProperties()))
            .areaCode(Optional.ofNullable(rule.areaCode()))
            .createdBy(Optional.of(actor))
            .updatedBy(Optional.of(actor))
            .build();
    repository.saveRule(domain);
  }

  private void insertTenancyRule(
      CatalogTenancyRule rule, UUID countryId, Map<String, UUID> regionIds, String actor) {
    Optional<UUID> regionId =
        Optional.ofNullable(rule.regionCode())
            .map(
                code ->
                    Optional.ofNullable(regionIds.get(code))
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Catalog tenancy rule references unknown region '"
                                        + code
                                        + "' in country "
                                        + countryId)));

    repository.saveTenancyRule(
        RentRegulationTenancyRule.builder()
            .identifier(Optional.of(SidGenerator.newRentRegulationTenancyRuleId()))
            .countryId(countryId)
            .regionId(regionId)
            .topic(rule.topic())
            .label(rule.label())
            .value(rule.value())
            .effectiveFrom(Optional.ofNullable(rule.effectiveFrom()).map(LocalDate::parse))
            .legalBasis(Optional.ofNullable(rule.legalBasis()))
            .sourceUrl(Optional.ofNullable(rule.sourceUrl()))
            .notes(Optional.ofNullable(rule.notes()))
            .createdBy(Optional.of(actor))
            .updatedBy(Optional.of(actor))
            .build());
  }

  // ==================== Domain → catalog (export) ====================

  private CatalogCountry toCatalogCountry(
      RentRegulationCountry country,
      List<RentRegulationRegion> regions,
      List<RentRegulationRule> rules,
      List<RentRegulationTenancyRule> tenancyRules,
      Map<UUID, String> regionCodeById) {
    List<CatalogRegion> catalogRegions =
        regions.stream()
            .sorted(Comparator.comparing(RentRegulationRegion::getRegionCode))
            .map(
                r ->
                    new CatalogRegion(
                        r.getRegionCode(), r.getRegionName(), r.getSummary().orElse(null)))
            .toList();

    List<CatalogRule> catalogRules =
        rules.stream()
            .map(r -> toCatalogRule(r, regionCodeById))
            .sorted(
                Comparator.comparingInt(CatalogRule::year)
                    .reversed()
                    .thenComparing(
                        CatalogRule::regionCode, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(
                        CatalogRule::propertyCategory,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(CatalogRule::maxIncreaseType, Comparator.comparing(Enum::name)))
            .toList();

    List<CatalogTenancyRule> catalogTenancyRules =
        tenancyRules.stream()
            .map(
                t ->
                    new CatalogTenancyRule(
                        t.getTopic(),
                        t.getRegionId().map(regionCodeById::get).orElse(null),
                        t.getLabel(),
                        t.getValue(),
                        t.getEffectiveFrom().map(LocalDate::toString).orElse(null),
                        t.getLegalBasis().orElse(null),
                        t.getSourceUrl().orElse(null),
                        t.getNotes().orElse(null)))
            .sorted(
                Comparator.comparing((CatalogTenancyRule t) -> t.topic().name())
                    .thenComparing(CatalogTenancyRule::label))
            .toList();

    return new CatalogCountry(
        country.getCountryCode(),
        country.getCountryName(),
        country.isHasRegionalRegulations(),
        country.getSummary().orElse(null),
        country.getLastReviewedAt().map(Instant::toString).orElse(null),
        catalogRegions.isEmpty() ? null : catalogRegions,
        catalogRules.isEmpty() ? null : catalogRules,
        country.getLateFeePolicy() == LateFeePolicy.UNKNOWN
                && country.getLateFeeMaxPercentage().isEmpty()
                && country.getLateFeeNotes().isEmpty()
            ? null
            : new CatalogLateFee(
                country.getLateFeePolicy(),
                country.getLateFeeMaxPercentage().orElse(null),
                country.getLateFeeNotes().orElse(null)),
        country.getFormalNoticeDays().orElse(null),
        catalogTenancyRules.isEmpty() ? null : catalogTenancyRules);
  }

  private CatalogRule toCatalogRule(RentRegulationRule rule, Map<UUID, String> regionCodeById) {
    return new CatalogRule(
        rule.getRegionId().map(regionCodeById::get).orElse(null),
        rule.getYear(),
        rule.getPropertyCategory(),
        rule.getMaxIncreasePercentage().orElse(null),
        rule.getMaxIncreaseType(),
        rule.getIndexName().orElse(null),
        rule.getIndexValue().orElse(null),
        rule.getEffectiveDate().map(LocalDate::toString).orElse(null),
        rule.getNoticePeriodDays().orElse(null),
        rule.getFrequency(),
        rule.getAdditionalConditions().orElse(null),
        rule.getSourceUrl().orElse(null),
        rule.getNotes().orElse(null),
        rule.getRegime().orElse(null),
        rule.getPropertyType().orElse(null),
        rule.getContractType().orElse(null),
        rule.getTaxRegime().orElse(null),
        rule.getTenancyPhase().orElse(null),
        rule.getBuildYearMin().orElse(null),
        rule.getBuildYearMax().orElse(null),
        rule.getEpcClassMin().orElse(null),
        rule.getEpcClassMax().orElse(null),
        rule.getContractSignedAfter().map(LocalDate::toString).orElse(null),
        rule.getContractSignedBefore().map(LocalDate::toString).orElse(null),
        rule.getLandlordMinProperties().orElse(null),
        rule.getAreaCode().orElse(null));
  }

  private static <T> List<T> safe(List<T> list) {
    return list == null ? List.of() : list;
  }
}
