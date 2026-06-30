package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.dto.response.FeatureFlagState;
import com.buurman.repository.CalendarFeedRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.FeatureFlagOverrideRepository;
import com.buurman.repository.FeatureFlagRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagService")
class FeatureFlagServiceTest {

  @Mock private FeatureFlagRepository flagRepo;
  @Mock private FeatureFlagOverrideRepository overrideRepo;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private PhotoRepository photoRepository;
  @Mock private DocumentRepository documentRepository;
  @Mock private ExpenseRepository expenseRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private CalendarFeedRepository calendarFeedRepository;
  @Mock private SegmentEvaluator segmentEvaluator;
  @Mock private MetricsService metricsService;

  private FeatureFlagService service;

  @BeforeEach
  void setUp() {
    service =
        new FeatureFlagService(
            flagRepo,
            overrideRepo,
            teamRepository,
            teamMemberRepository,
            teamPreferencesRepository,
            propertyRepository,
            contractRepository,
            contactRepository,
            photoRepository,
            documentRepository,
            expenseRepository,
            paymentRepository,
            calendarFeedRepository,
            segmentEvaluator,
            Clock.systemUTC(),
            metricsService,
            new SimpleMeterRegistry());
  }

  @Test
  @DisplayName(
      "getAllEnvironmentFlags falls back to compile-time defaults when the DB is unavailable")
  void getAllEnvironmentFlags_fallsBackToDefaults_onRepoFailure() {
    when(flagRepo.findAll()).thenThrow(new RuntimeException("db down"));

    Map<String, FeatureFlagState> flags = service.getAllEnvironmentFlags();

    // Must not throw (regression guard for the fallback path) and must yield typed
    // FeatureFlagState defaults — never a raw map.
    assertThat(flags).isNotEmpty();
    assertThat(flags.values()).allSatisfy(state -> assertThat(state).isNotNull());
  }
}
