package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.regulation.CatalogCountry;
import com.buurman.domain.regulation.RentRegulationCatalog;
import com.buurman.domain.regulation.TerminationNoticeRule;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.repository.TerminationNoticeRuleRepository;
import com.buurman.security.BackofficePrincipal;

/**
 * Regression coverage for the data-loss bug where {@code reload} wiped {@code
 * rent_regulation_termination_rules} via {@code deleteAllReferenceData} but never re-inserted it,
 * because that table has no representation in the bundled catalog. A reload must snapshot the table
 * beforehand and reinsert it against the freshly-generated country ids afterwards.
 */
@DisplayName("RentRegulationCatalogService.reload")
class RentRegulationCatalogServiceTest {

  private final RentRegulationRepository repository = mock(RentRegulationRepository.class);
  private final TerminationNoticeRuleRepository terminationRuleRepository =
      mock(TerminationNoticeRuleRepository.class);
  private final RentRegulationCatalogLoader loader = mock(RentRegulationCatalogLoader.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private final RentRegulationCatalogService service =
      new RentRegulationCatalogService(repository, terminationRuleRepository, loader, clock);

  private static final UUID OLD_COUNTRY_ID = UUID.randomUUID();
  private static final UUID NEW_COUNTRY_ID = UUID.randomUUID();

  @Test
  @DisplayName("reinserts termination rules, remapped to the freshly-generated country id")
  void reload_preservesTerminationRules() {
    // Bundled catalog: a single country, no regions/rules/tenancy rules — the minimum needed to
    // exercise the country-insert path that generates a brand new country id.
    CatalogCountry catalogCountry =
        new CatalogCountry("NL", "Netherlands", false, null, null, null, null, null, null, null);
    RentRegulationCatalog catalog =
        new RentRegulationCatalog("v1", "2026-03-01", "test catalog", List.of(catalogCountry));
    when(loader.load()).thenReturn(catalog);

    // The country as it exists BEFORE the reload — this is what deleteAllReferenceData is about
    // to wipe.
    RentRegulationCountry existingCountry =
        RentRegulationCountry.builder().id(OLD_COUNTRY_ID).countryCode("NL").build();
    when(repository.findAllCountries()).thenReturn(List.of(existingCountry));
    when(repository.findAllRegions()).thenReturn(List.of());

    // A termination rule seeded against the OLD country id — this is the row that must survive.
    TerminationNoticeRule existingRule =
        TerminationNoticeRule.builder()
            .id(UUID.randomUUID())
            .countryId(OLD_COUNTRY_ID)
            .partyType(TerminationGivenBy.LANDLORD)
            .minTenancyMonths(Optional.of(0))
            .noticeDays(90)
            .groundsRequired(true)
            .groundsCodes(List.of("OWN_USE", "RENOVATION"))
            .notes(Optional.of("BUUR-105 ticket-stated figure"))
            .build();
    when(terminationRuleRepository.findAll()).thenReturn(List.of(existingRule));

    // Reload re-inserts the country with a brand new, different id.
    when(repository.saveCountry(any(RentRegulationCountry.class)))
        .thenAnswer(
            invocation -> {
              RentRegulationCountry country = invocation.getArgument(0);
              country.setId(NEW_COUNTRY_ID);
              return country;
            });
    when(terminationRuleRepository.save(any(TerminationNoticeRule.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BackofficePrincipal principal =
        new BackofficePrincipal(
            UUID.randomUUID().toString(),
            Optional.of("admin@buurman.io"),
            Optional.empty(),
            Optional.empty());

    service.reload(principal);

    verify(repository).deleteAllReferenceData();

    ArgumentCaptor<TerminationNoticeRule> captor =
        ArgumentCaptor.forClass(TerminationNoticeRule.class);
    verify(terminationRuleRepository).save(captor.capture());

    TerminationNoticeRule reinserted = captor.getValue();
    assertThat(reinserted.getCountryId())
        .as("termination rule must be re-pointed at the new country id, not the deleted one")
        .isEqualTo(NEW_COUNTRY_ID)
        .isNotEqualTo(OLD_COUNTRY_ID);
    assertThat(reinserted.getPartyType()).isEqualTo(TerminationGivenBy.LANDLORD);
    assertThat(reinserted.getMinTenancyMonths()).contains(0);
    assertThat(reinserted.getNoticeDays()).isEqualTo(90);
    assertThat(reinserted.isGroundsRequired()).isTrue();
    assertThat(reinserted.getGroundsCodes()).containsExactly("OWN_USE", "RENOVATION");
    assertThat(reinserted.getNotes()).contains("BUUR-105 ticket-stated figure");
  }

  @Test
  @DisplayName("drops a termination rule whose country is no longer in the reloaded catalog")
  void reload_dropsTerminationRuleForRemovedCountry() {
    // The bundled catalog no longer contains the country the existing termination rule belongs
    // to — there is nothing left to attach it to, so it must be dropped rather than crash.
    RentRegulationCatalog catalog =
        new RentRegulationCatalog("v1", "2026-03-01", "test catalog", List.of());
    when(loader.load()).thenReturn(catalog);

    RentRegulationCountry existingCountry =
        RentRegulationCountry.builder().id(OLD_COUNTRY_ID).countryCode("XX").build();
    when(repository.findAllCountries()).thenReturn(List.of(existingCountry));
    when(repository.findAllRegions()).thenReturn(List.of());

    TerminationNoticeRule existingRule =
        TerminationNoticeRule.builder()
            .id(UUID.randomUUID())
            .countryId(OLD_COUNTRY_ID)
            .partyType(TerminationGivenBy.TENANT)
            .noticeDays(90)
            .groundsRequired(false)
            .build();
    when(terminationRuleRepository.findAll()).thenReturn(List.of(existingRule));

    BackofficePrincipal principal =
        new BackofficePrincipal(
            UUID.randomUUID().toString(),
            Optional.of("admin@buurman.io"),
            Optional.empty(),
            Optional.empty());

    service.reload(principal);

    verify(repository).deleteAllReferenceData();
    verify(terminationRuleRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName(
      "drops a region-specific termination rule rather than silently promoting it to"
          + " country-wide when its region is no longer in the reloaded catalog — a plain"
          + " Optional.map(lookup) cannot distinguish \"no region\" from \"region vanished\","
          + " since a null-returning mapper collapses to empty either way")
  void reload_dropsRegionSpecificRuleForRemovedRegion() {
    // The reloaded catalog still has the country, but with no regions at all — the region the
    // existing rule is scoped to is gone.
    CatalogCountry catalogCountry =
        new CatalogCountry(
            "NL", "Netherlands", false, null, null, List.of(), null, null, null, null);
    RentRegulationCatalog catalog =
        new RentRegulationCatalog("v1", "2026-03-01", "test catalog", List.of(catalogCountry));
    when(loader.load()).thenReturn(catalog);

    UUID oldRegionId = UUID.randomUUID();
    RentRegulationCountry existingCountry =
        RentRegulationCountry.builder().id(OLD_COUNTRY_ID).countryCode("NL").build();
    RentRegulationRegion existingRegion =
        RentRegulationRegion.builder()
            .id(oldRegionId)
            .countryId(OLD_COUNTRY_ID)
            .regionCode("NH")
            .regionName("North Holland")
            .build();
    when(repository.findAllCountries()).thenReturn(List.of(existingCountry));
    when(repository.findAllRegions()).thenReturn(List.of(existingRegion));

    TerminationNoticeRule regionSpecificRule =
        TerminationNoticeRule.builder()
            .id(UUID.randomUUID())
            .countryId(OLD_COUNTRY_ID)
            .regionId(Optional.of(oldRegionId))
            .partyType(TerminationGivenBy.LANDLORD)
            .noticeDays(90)
            .groundsRequired(false)
            .build();
    when(terminationRuleRepository.findAll()).thenReturn(List.of(regionSpecificRule));

    when(repository.saveCountry(any(RentRegulationCountry.class)))
        .thenAnswer(
            invocation -> {
              RentRegulationCountry country = invocation.getArgument(0);
              country.setId(NEW_COUNTRY_ID);
              return country;
            });

    BackofficePrincipal principal =
        new BackofficePrincipal(
            UUID.randomUUID().toString(),
            Optional.of("admin@buurman.io"),
            Optional.empty(),
            Optional.empty());

    service.reload(principal);

    verify(repository).deleteAllReferenceData();
    // Must be dropped, not re-inserted with an empty regionId (which would wrongly make this a
    // country-wide rule).
    verify(terminationRuleRepository, org.mockito.Mockito.never()).save(any());
  }
}
