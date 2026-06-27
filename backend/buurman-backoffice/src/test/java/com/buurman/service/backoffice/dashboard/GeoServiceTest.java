package com.buurman.service.backoffice.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.dashboard.CountryStats;
import com.buurman.dto.response.backoffice.dashboard.GeoResponse;
import com.buurman.dto.response.backoffice.dashboard.PropertyLocationsResponse;
import com.buurman.repository.backoffice.DashboardAggregateRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeoService")
class GeoServiceTest {

  private static final int MAX_MAP_POINTS = 50_000;

  @Mock private DashboardAggregateRepository aggregateRepository;
  @InjectMocks private GeoService service;

  @Test
  @DisplayName("getGeo returns a LIVE envelope with per-country stats")
  void getGeoLive() {
    when(aggregateRepository.countryStats())
        .thenReturn(
            List.of(
                new CountryStats("NL", 10, 42, 30, 1_250_00L),
                new CountryStats("DE", 3, 5, 4, 400_00L)));
    when(aggregateRepository.teamsWithoutCountry()).thenReturn(5L);

    GeoResponse response = service.getGeo();

    assertThat(response.status()).isEqualTo(PanelStatus.LIVE);
    assertThat(response.countries()).hasSize(2);
    assertThat(response.countries()).extracting(CountryStats::code).containsExactly("NL", "DE");
    assertThat(response.countries().get(0).monthlyValueEurMinor()).isEqualTo(1_250_00L);
    assertThat(response.teamsWithoutCountry()).isEqualTo(5L);
  }

  @Test
  @DisplayName("getPropertyLocations maps [lat,lng] coords and reports not-capped under the limit")
  void propertyLocationsUnderLimit() {
    when(aggregateRepository.propertyCoordinates(MAX_MAP_POINTS + 1))
        .thenReturn(List.of(new double[] {52.1, 4.9}, new double[] {48.0, 2.3}));

    PropertyLocationsResponse response = service.getPropertyLocations();

    assertThat(response.capped()).isFalse();
    assertThat(response.total()).isEqualTo(2);
    assertThat(response.points()).hasSize(2);
    assertThat(response.points().get(0).lat()).isEqualTo(52.1);
    assertThat(response.points().get(0).lng()).isEqualTo(4.9);
  }

  @Test
  @DisplayName("getPropertyLocations caps at the max and flags it")
  void propertyLocationsCapped() {
    List<double[]> overLimit =
        IntStream.range(0, MAX_MAP_POINTS + 1).mapToObj(i -> new double[] {1.0, 2.0}).toList();
    when(aggregateRepository.propertyCoordinates(MAX_MAP_POINTS + 1)).thenReturn(overLimit);

    PropertyLocationsResponse response = service.getPropertyLocations();

    assertThat(response.capped()).isTrue();
    assertThat(response.total()).isEqualTo(MAX_MAP_POINTS);
    assertThat(response.points()).hasSize(MAX_MAP_POINTS);
  }
}
