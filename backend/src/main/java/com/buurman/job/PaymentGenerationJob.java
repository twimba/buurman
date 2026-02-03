package com.buurman.job;

import com.buurman.domain.Team;
import com.buurman.repository.TeamRepository;
import com.buurman.service.PaymentSchedulingService;
import com.buurman.util.Constants;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Quartz job that generates future payments for all active contracts.
 * Runs every 4 hours to ensure payments are created ahead of time.
 */
@Component
@DisallowConcurrentExecution
public class PaymentGenerationJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PaymentGenerationJob.class);

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PaymentSchedulingService schedulingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting scheduled payment generation job");
        long startTime = System.currentTimeMillis();

        try {
            // Find all teams with auto-generation enabled
            List<Team> teams = teamRepository.findAllWithAutoGenerationEnabled();

            if (teams.isEmpty()) {
                log.info("No teams with auto-generation enabled, skipping payment generation");
                return;
            }

            int totalPaymentsGenerated = 0;
            int teamsProcessed = 0;
            int teamsFailed = 0;

            for (Team team : teams) {
                try {
                    log.debug("Processing team: {}", team.getIdentifier());

                    int count = schedulingService.generateFuturePaymentsForTeam(
                            team.getId(),
                            Constants.SYSTEM_USER_ID
                    );

                    totalPaymentsGenerated += count;
                    teamsProcessed++;

                    if (count > 0) {
                        log.info("Generated {} payments for team {}", count, team.getIdentifier());
                    }

                } catch (Exception e) {
                    teamsFailed++;
                    log.error("Failed to generate payments for team {}: {}",
                            team.getIdentifier(), e.getMessage(), e);
                    // Continue with other teams (fault isolation)
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Payment generation job completed in {}ms: {} teams processed, {} teams failed, {} payments generated",
                    duration, teamsProcessed, teamsFailed, totalPaymentsGenerated);

        } catch (Exception e) {
            log.error("Payment generation job failed with exception", e);
            throw new JobExecutionException("Payment generation job failed", e);
        }
    }
}
