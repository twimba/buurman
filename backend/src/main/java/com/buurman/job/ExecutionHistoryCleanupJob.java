package com.buurman.job;

import com.buurman.repository.JobExecutionHistoryRepository;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.HOURS;

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
            if (deleted > 0) {
                log.info("Cleaned up {} job execution history entries older than 48 hours", deleted);
            }
        } catch (Exception e) {
            throw new JobExecutionException("Execution history cleanup failed", e);
        }
    }
}
