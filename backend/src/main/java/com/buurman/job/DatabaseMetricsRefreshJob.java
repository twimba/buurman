package com.buurman.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.service.DatabaseMetricsService;

import lombok.RequiredArgsConstructor;

@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class DatabaseMetricsRefreshJob implements Job {

  private final DatabaseMetricsService databaseMetricsService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      databaseMetricsService.refreshCounts();
    } catch (Exception e) {
      throw new JobExecutionException("Database metrics refresh failed", e);
    }
  }
}
