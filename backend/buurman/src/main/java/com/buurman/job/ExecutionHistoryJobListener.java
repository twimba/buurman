package com.buurman.job;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.buurman.repository.JobExecutionHistoryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Lazy(false)
@Slf4j
@RequiredArgsConstructor
public class ExecutionHistoryJobListener implements JobListener {

  private final JobExecutionHistoryRepository repository;
  private final Scheduler scheduler;

  @PostConstruct
  public void register() throws SchedulerException {
    scheduler.getListenerManager().addJobListener(this);
    log.info("Registered ExecutionHistoryJobListener on scheduler");
  }

  @Override
  public String getName() {
    return "ExecutionHistoryJobListener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    context.put("startedAt", Instant.now());
    String jobName = context.getJobDetail().getKey().getName();
    try {
      UUID executionId =
          repository.insert(
              jobName,
              context.getJobDetail().getKey().getGroup(),
              context.getTrigger().getKey().getName(),
              context.getTrigger().getKey().getGroup(),
              (Instant) context.get("startedAt"),
              context.getScheduler().getSchedulerInstanceId());
      context.put("executionId", executionId);
    } catch (Exception e) {
      log.warn("Failed to record job execution start for {}: {}", jobName, e.getMessage());
    }
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {
    // no-op
  }

  @Override
  public void jobWasExecuted(
      JobExecutionContext context, @Nullable JobExecutionException exception) {
    String status = exception == null ? "SUCCESS" : "FAILED";
    Optional<String> errorMessage = Optional.ofNullable(exception).map(Throwable::getMessage);
    Object executionIdObj = context.get("executionId");

    if (executionIdObj != null) {
      try {
        repository.markCompleted(
            (UUID) executionIdObj,
            Instant.now(),
            context.getJobRunTime(),
            status,
            errorMessage.orElse(null));
      } catch (Exception e) {
        log.warn(
            "Failed to record job execution result for {}: {}",
            context.getJobDetail().getKey().getName(),
            e.getMessage());
      }
    } else {
      insertFallbackRecord(context, status, errorMessage);
    }
  }

  private void insertFallbackRecord(
      JobExecutionContext context, String status, Optional<String> errorMessage) {
    String jobName = context.getJobDetail().getKey().getName();
    try {
      Instant startedAt =
          Optional.ofNullable((Instant) context.get("startedAt"))
              .orElseGet(() -> Instant.now().minusMillis(context.getJobRunTime()));
      Optional<String> nodeId = Optional.empty();
      try {
        nodeId = Optional.of(context.getScheduler().getSchedulerInstanceId());
      } catch (SchedulerException ignored) {
        // best-effort
      }
      repository.insertCompleted(
          jobName,
          context.getJobDetail().getKey().getGroup(),
          context.getTrigger().getKey().getName(),
          context.getTrigger().getKey().getGroup(),
          startedAt,
          Instant.now(),
          context.getJobRunTime(),
          status,
          errorMessage,
          nodeId);
    } catch (Exception e) {
      log.warn("Failed to record fallback job execution for {}: {}", jobName, e.getMessage());
    }
  }
}
