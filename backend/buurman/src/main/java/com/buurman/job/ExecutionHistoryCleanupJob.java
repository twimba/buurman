package com.buurman.job;

import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Clock;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.repository.JobExecutionHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class ExecutionHistoryCleanupJob implements Job {

  private final JobExecutionHistoryRepository repository;
  private final Clock clock;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      int deleted = repository.deleteOlderThan(clock.instant().minus(48, HOURS));
      context.put("itemsProcessed", deleted);
      if (deleted > 0) {
        log.info("Cleaned up {} job execution history entries older than 48 hours", deleted);
      }
    } catch (Exception e) {
      throw new JobExecutionException("Execution history cleanup failed", e);
    }
  }
}
