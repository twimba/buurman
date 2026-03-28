package com.buurman.job;

import java.time.Duration;
import java.time.Instant;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.MetricsService;

import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class RateLimitCleanupJob implements Job {

  private static final int CLEANUP_BATCH_SIZE = 100;

  private final ExpiredEntriesCleaner rateLimitExpiredEntriesCleaner;
  private final MetricsService metricsService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      Instant start = Instant.now();
      int removed = rateLimitExpiredEntriesCleaner.removeExpired(CLEANUP_BATCH_SIZE);
      Duration duration = Duration.between(start, Instant.now());

      context.put("itemsProcessed", removed);
      metricsService.incrementCounterBy("ratelimit.cleanup.purged.total", removed);
      metricsService.recordTimer("ratelimit.cleanup.duration_seconds", duration);

      if (removed > 0) {
        log.info("Rate limit cleanup: removed {} expired bucket entries", removed);
      }
    } catch (Exception e) {
      throw new JobExecutionException("Rate limit bucket cleanup failed", e);
    }
  }
}
