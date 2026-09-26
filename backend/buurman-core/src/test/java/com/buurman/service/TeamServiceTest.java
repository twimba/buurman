package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamRole;
import com.buurman.dto.response.TeamResponse;
import com.buurman.mapper.TeamMapper;
import com.buurman.repository.TeamInvitationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService")
class TeamServiceTest {

  @Mock private TeamRepository teamRepository;
  @Mock private UnitRepository unitRepository;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;
  @Mock private PaymentReminderService paymentReminderService;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private TeamInvitationRepository invitationRepository;
  @Mock private UserRepository userRepository;
  @Mock private TeamMapper teamMapper;
  @Mock private NotificationService notificationService;
  @Mock private MetricsService metricsService;
  @Mock private AppProperties appProperties;

  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private static final UUID TEAM_ID = UUID.randomUUID();

  private TeamService service() {
    return new TeamService(
        teamRepository,
        unitRepository,
        teamPreferencesRepository,
        paymentReminderService,
        teamMemberRepository,
        invitationRepository,
        userRepository,
        teamMapper,
        notificationService,
        metricsService,
        appProperties,
        clock);
  }

  private Team team() {
    return Team.builder()
        .id(TEAM_ID)
        .identifier(Optional.of(Sid.of("team_01HZQX7V8B3K5M2N4P6R9T0W")))
        .name("Landlord Co")
        .demo(false)
        .createdAt(Instant.now(clock))
        .updatedAt(Instant.now(clock))
        .build();
  }

  private UserPrincipal principal() {
    return new UserPrincipal(
        UUID.randomUUID(),
        "user_01HZQX7V8B3K5M2N4P6R9T0W",
        "keycloak-id",
        "landlord@test.io",
        "Landlord",
        TEAM_ID,
        "team_01HZQX7V8B3K5M2N4P6R9T0W",
        TeamRole.TEAM_ADMIN);
  }

  @Nested
  @DisplayName("getCurrentTeam")
  class GetCurrentTeam {

    @Test
    @DisplayName(
        "reports billableUnitCount as the sum of units across all of the team's properties, "
            + "implicit units included")
    void reportsBillableUnitCountAcrossProperties() {
      // One property with 1 unit, another with 6 - countActiveByTeamId already sums across both,
      // implicit units included (it does not filter on is_implicit).
      when(teamRepository.getById(TEAM_ID)).thenReturn(team());
      when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of());
      when(unitRepository.countActiveByTeamId(TEAM_ID)).thenReturn(7);
      when(teamMapper.toResponse(any(Team.class), anyLong(), eq(7)))
          .thenReturn(
              new TeamResponse(
                  team().getIdentifier().orElseThrow(), "Landlord Co", 0L, 7, Instant.now(clock)));

      TeamResponse response = service().getCurrentTeam(principal());

      assertThat(response.billableUnitCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("relays a zero count when the team's only units belong to a soft-deleted property")
    void relaysZeroWhenOnlyPropertyIsSoftDeleted() {
      // UnitRepository.countActiveByTeamId already excludes units of a soft-deleted property
      // (it joins on properties.deleted_at IS NULL). TeamService must not re-derive or override
      // that figure - it just passes through whatever the repository reports.
      when(teamRepository.getById(TEAM_ID)).thenReturn(team());
      when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of());
      when(unitRepository.countActiveByTeamId(TEAM_ID)).thenReturn(0);
      when(teamMapper.toResponse(any(Team.class), anyLong(), eq(0)))
          .thenReturn(
              new TeamResponse(
                  team().getIdentifier().orElseThrow(), "Landlord Co", 0L, 0, Instant.now(clock)));

      TeamResponse response = service().getCurrentTeam(principal());

      assertThat(response.billableUnitCount()).isZero();
    }
  }
}
