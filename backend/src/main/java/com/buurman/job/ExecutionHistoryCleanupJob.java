package com.buurman.job;

import com.buurman.repository.JobExecutionHistoryRepository;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.HOURS;

@Component
@DisallowConcurrentExecution
public class ExecutionHistoryCleanupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(ExecutionHistoryCleanupJob.class);

    private final JobExecutionHistoryRepository repository;
    private final Clock clock;

    public ExecutionHistoryCleanupJob(JobExecutionHistoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            int deleted = repository.deleteOlderThan(clock.instant().minus(48, HOURS));
            if (deleted > 0) {
                log.info("Cleaned up {} job execution history entries older than 48 hours", deleted);
            }
        } catch (Exception e) {
            throw new JobExecutionException("Execution history cleanup failed", e);
        }
    }
}
