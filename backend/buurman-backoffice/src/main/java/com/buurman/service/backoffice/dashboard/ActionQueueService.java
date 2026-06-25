package com.buurman.service.backoffice.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.ScheduledJobResponse;
import com.buurman.dto.response.backoffice.dashboard.ActionQueueResponse;
import com.buurman.dto.response.backoffice.dashboard.ActionQueueResponse.ActionItem;
import com.buurman.repository.backoffice.DashboardAggregateRepository;
import com.buurman.repository.backoffice.DashboardLayoutRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeSchedulerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds the support/safety action queue from outbox, impersonation and scheduler signals, applying
 * per-user snoozes. Backoffice is platform-wide, so signals span all teams.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActionQueueService {

  private static final Duration IMPERSONATION_MAX_RUNTIME = Duration.ofHours(4);
  private static final Duration BACKLOG_AGE_THRESHOLD = Duration.ofMinutes(30);
  private static final int MAX_SNOOZE_HOURS = 24 * 365;

  private final DashboardAggregateRepository aggregateRepository;
  private final DashboardLayoutRepository layoutRepository;
  private final BackofficeSchedulerService schedulerService;
  private final Clock clock;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public ActionQueueResponse getActionQueue(BackofficePrincipal principal) {
    Map<String, LocalDateTime> snoozes = layoutRepository.activeSnoozes(principal.userId());
    List<ActionItem> items =
        rawItems().stream().filter(item -> !snoozes.containsKey(item.key())).toList();
    return new ActionQueueResponse(PanelStatus.LIVE, Optional.empty(), Optional.empty(), items);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public ActionQueueResponse snooze(BackofficePrincipal principal, String itemKey, int hours) {
    int clampedHours = Math.min(MAX_SNOOZE_HOURS, Math.max(1, hours));
    layoutRepository.snooze(
        principal.userId(), itemKey, LocalDateTime.now(clock).plusHours(clampedHours));
    return getActionQueue(principal);
  }

  /** Open (non-snoozed) action-item count — feeds the status-strip alerts pillar. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public int openCount(BackofficePrincipal principal) {
    return getActionQueue(principal).items().size();
  }

  private List<ActionItem> rawItems() {
    List<ActionItem> items = new ArrayList<>();

    long deadLetter = aggregateRepository.outboxDeadLetter();
    if (deadLetter > 0) {
      items.add(
          new ActionItem(
              "OUTBOX:deadletter",
              "crit",
              "outbox",
              deadLetter + " outbox message(s) dead-lettered",
              "/notifications?status=FAILED",
              ageMinutes(aggregateRepository.oldestDeadLetterCreatedAt()),
              Optional.empty()));
    }

    long backlog = aggregateRepository.outboxBacklog();
    Optional<LocalDateTime> oldest = aggregateRepository.oldestBacklogCreatedAt();
    boolean backlogStuck =
        backlog > 0
            && oldest
                .map(
                    ts ->
                        Duration.between(ts, LocalDateTime.now(clock))
                                .compareTo(BACKLOG_AGE_THRESHOLD)
                            > 0)
                .orElse(false);
    if (backlogStuck) {
      items.add(
          new ActionItem(
              "OUTBOX:backlog",
              backlog > 100 ? "crit" : "warn",
              "outbox",
              backlog + " outbox message(s) pending > 30m",
              "/notifications?status=PENDING",
              ageMinutes(oldest),
              Optional.empty()));
    }

    long longRunning =
        aggregateRepository.countLongRunningImpersonations(
            LocalDateTime.now(clock).minus(IMPERSONATION_MAX_RUNTIME));
    if (longRunning > 0) {
      items.add(
          new ActionItem(
              "IMP:longrunning",
              "warn",
              "impersonation",
              longRunning + " impersonation session(s) active > 4h",
              "/impersonation?status=ACTIVE",
              0,
              Optional.empty()));
    }

    items.addAll(failedSchedulerJobs());
    return items;
  }

  private List<ActionItem> failedSchedulerJobs() {
    try {
      List<ActionItem> result = new ArrayList<>();
      for (ScheduledJobResponse job : schedulerService.listAllJobs()) {
        if ("ERROR".equals(job.triggerState()) || "BLOCKED".equals(job.triggerState())) {
          result.add(
              new ActionItem(
                  "JOB:" + job.triggerState() + ":" + job.jobName(),
                  "crit",
                  "scheduler",
                  "Job " + job.jobName() + " is " + job.triggerState(),
                  "/scheduler?jobName=" + job.jobName(),
                  0,
                  Optional.empty()));
        }
      }
      return result;
    } catch (SchedulerException e) {
      log.warn("Could not read scheduler state for action queue", e);
      return List.of();
    }
  }

  private long ageMinutes(Optional<LocalDateTime> since) {
    return since.map(ts -> Duration.between(ts, LocalDateTime.now(clock)).toMinutes()).orElse(0L);
  }
}
