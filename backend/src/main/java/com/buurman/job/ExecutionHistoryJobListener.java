package com.buurman.job;

import java.time.Instant;
import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Component;

import com.buurman.repository.JobExecutionHistoryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
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
    String jobName = context.getJobDetail().getKey().getName();
    try {
      UUID executionId =
          repository.insert(
              jobName,
              context.getJobDetail().getKey().getGroup(),
              context.getTrigger().getKey().getName(),
              context.getTrigger().getKey().getGroup(),
              Instant.now(),
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
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception) {
    Object executionIdObj = context.get("executionId");
    if (executionIdObj == null) {
      return;
    }

    UUID executionId = (UUID) executionIdObj;
    String status = exception == null ? "SUCCESS" : "FAILED";
    String errorMessage = exception != null ? exception.getMessage() : null;

    try {
      repository.markCompleted(
          executionId, Instant.now(), context.getJobRunTime(), status, errorMessage);
    } catch (Exception e) {
      log.warn(
          "Failed to record job execution result for {}: {}",
          context.getJobDetail().getKey().getName(),
          e.getMessage());
    }
  }
}
