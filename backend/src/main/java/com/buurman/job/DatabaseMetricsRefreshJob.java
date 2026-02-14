package com.buurman.job;

import com.buurman.service.DatabaseMetricsService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class DatabaseMetricsRefreshJob implements Job {

    private final DatabaseMetricsService databaseMetricsService;

    public DatabaseMetricsRefreshJob(DatabaseMetricsService databaseMetricsService) {
        this.databaseMetricsService = databaseMetricsService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            databaseMetricsService.refreshCounts();
        } catch (Exception e) {
            throw new JobExecutionException("Database metrics refresh failed", e);
        }
    }
}
