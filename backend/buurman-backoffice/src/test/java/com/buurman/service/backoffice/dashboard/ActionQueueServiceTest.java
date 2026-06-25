package com.buurman.service.backoffice.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.dto.response.backoffice.dashboard.ActionQueueResponse;
import com.buurman.repository.backoffice.DashboardAggregateRepository;
import com.buurman.repository.backoffice.DashboardLayoutRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeSchedulerService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActionQueueService")
class ActionQueueServiceTest {

  @Mock private DashboardAggregateRepository aggregateRepository;
  @Mock private DashboardLayoutRepository layoutRepository;
  @Mock private BackofficeSchedulerService schedulerService;
  @Spy private Clock clock = Clock.systemUTC();
  @InjectMocks private ActionQueueService service;

  private final BackofficePrincipal principal =
      new BackofficePrincipal(
          UUID.randomUUID().toString(), Optional.empty(), Optional.empty(), Optional.empty());

  /** Stubs a world with no backlog/impersonation/scheduler issues. */
  private void stubQuietWorld() throws Exception {
    when(aggregateRepository.outboxBacklog()).thenReturn(0L);
    when(aggregateRepository.oldestBacklogCreatedAt()).thenReturn(Optional.empty());
    when(aggregateRepository.countLongRunningImpersonations(any())).thenReturn(0L);
    when(schedulerService.listAllJobs()).thenReturn(List.of());
  }

  @Test
  @DisplayName("emits a critical item when the outbox has dead-lettered messages")
  void emitsDeadLetterItem() throws Exception {
    stubQuietWorld();
    LocalDateTime now = LocalDateTime.now(clock);
    when(aggregateRepository.outboxDeadLetter()).thenReturn(3L);
    when(aggregateRepository.oldestDeadLetterCreatedAt())
        .thenReturn(Optional.of(now.minusMinutes(10)));
    when(layoutRepository.activeSnoozes(any())).thenReturn(Map.of());

    ActionQueueResponse response = service.getActionQueue(principal);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).key()).isEqualTo("OUTBOX:deadletter");
    assertThat(response.items().get(0).severity()).isEqualTo("crit");
  }

  @Test
  @DisplayName("filters out items the user has snoozed")
  void filtersSnoozedItems() throws Exception {
    stubQuietWorld();
    LocalDateTime now = LocalDateTime.now(clock);
    when(aggregateRepository.outboxDeadLetter()).thenReturn(3L);
    when(aggregateRepository.oldestDeadLetterCreatedAt())
        .thenReturn(Optional.of(now.minusMinutes(10)));
    when(layoutRepository.activeSnoozes(any()))
        .thenReturn(Map.of("OUTBOX:deadletter", now.plusHours(1)));

    ActionQueueResponse response = service.getActionQueue(principal);

    assertThat(response.items()).isEmpty();
  }

  @Test
  @DisplayName("clamps a sub-1 snooze up to at least one hour")
  void clampsSnoozeLowerBound() throws Exception {
    stubQuietWorld();
    when(aggregateRepository.outboxDeadLetter()).thenReturn(0L);
    when(layoutRepository.activeSnoozes(any())).thenReturn(Map.of());

    service.snooze(principal, "SOME:key", 0);

    ArgumentCaptor<LocalDateTime> until = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(layoutRepository).snooze(eq(principal.userId()), eq("SOME:key"), until.capture());
    assertThat(until.getValue())
        .isAfter(LocalDateTime.now(clock))
        .isBefore(LocalDateTime.now(clock).plusHours(2));
  }
}
